#!/usr/bin/env python3
"""
ISLAND 视频 ASR 脚本：从音频/视频提取句级英文字幕与时间轴。

输出 JSON 到 stdout，日志（中文）输出到 stderr，供 Spring Boot ProcessBuilder 解析。

依赖：
  pip install faster-whisper
  FFmpeg（抽音频）

环境变量（可选）：
  HF_ENDPOINT=https://hf-mirror.com   # 国内下载模型镜像
  ISLAND_WHISPER_MODEL=small          # tiny|base|small|medium
  ISLAND_WHISPER_DEVICE=cuda          # cuda|cpu
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable


@dataclass
class SentenceCue:
    seq: int
    start_ms: int
    end_ms: int
    text_en: str


def log(msg: str) -> None:
    print(f"[视频解析] {msg}", file=sys.stderr, flush=True)


def run_ffmpeg(ffmpeg: str, args: list[str]) -> None:
    cmd = [ffmpeg, *args]
    log(f"执行 FFmpeg: {' '.join(cmd)}")
    proc = subprocess.run(cmd, capture_output=True, text=True)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip() or "FFmpeg 失败")
    if proc.stderr:
        for line in proc.stderr.splitlines()[-5:]:
            if line.strip():
                log(f"FFmpeg: {line.strip()}")


def extract_audio(ffmpeg: str, input_path: Path, wav_path: Path) -> None:
    log(f"正在从媒体提取 16kHz 单声道音频: {input_path.name}")
    run_ffmpeg(
        ffmpeg,
        [
            "-y",
            "-i",
            str(input_path),
            "-vn",
            "-acodec",
            "pcm_s16le",
            "-ar",
            "16000",
            "-ac",
            "1",
            str(wav_path),
        ],
    )
    log(f"音频提取完成，大小 {wav_path.stat().st_size // 1024} KB")


def probe_duration_ms(ffmpeg: str, input_path: Path) -> int | None:
    ffprobe = Path(ffmpeg).with_name("ffprobe.exe" if os.name == "nt" else "ffprobe")
    if not ffprobe.exists():
        return None
    cmd = [
        str(ffprobe),
        "-v",
        "error",
        "-show_entries",
        "format=duration",
        "-of",
        "default=noprint_wrappers=1:nokey=1",
        str(input_path),
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    if proc.returncode != 0:
        return None
    try:
        return int(float(proc.stdout.strip()) * 1000)
    except ValueError:
        return None


def merge_to_sentences(segments: Iterable, max_words: int = 25) -> list[SentenceCue]:
    """将 Whisper 分段按标点与词数合并为句级 cue。"""
    cues: list[SentenceCue] = []
    buf_words: list[str] = []
    buf_start: float | None = None
    buf_end: float = 0.0
    sentence_end = re.compile(r"[.!?]\s*$")

    def flush():
        nonlocal buf_words, buf_start, buf_end
        text = " ".join(buf_words).strip()
        if not text or buf_start is None:
            buf_words = []
            buf_start = None
            return
        cues.append(
            SentenceCue(
                seq=len(cues) + 1,
                start_ms=int(buf_start * 1000),
                end_ms=int(max(buf_end, buf_start + 0.3) * 1000),
                text_en=text,
            )
        )
        buf_words = []
        buf_start = None

    for seg in segments:
        text = (seg.text or "").strip()
        if not text:
            continue
        if buf_start is None:
            buf_start = seg.start
        buf_end = seg.end
        buf_words.extend(text.split())
        ends_sentence = bool(sentence_end.search(text))
        too_long = len(buf_words) >= max_words
        if ends_sentence or too_long:
            flush()

    flush()
    return cues


def transcribe(
    audio_path: Path,
    *,
    model_name: str,
    device: str,
    language: str,
) -> list[SentenceCue]:
    from faster_whisper import WhisperModel

    compute_type = "float16" if device == "cuda" else "int8"
    log(f"加载 Whisper 模型 {model_name}（设备 {device}，精度 {compute_type}）")
    model = WhisperModel(model_name, device=device, compute_type=compute_type)
    log("开始语音识别，请稍候…")
    segments, info = model.transcribe(
        str(audio_path),
        language=language,
        word_timestamps=True,
        vad_filter=True,
        vad_parameters=dict(min_silence_duration_ms=500),
    )
    log(f"识别语言 {info.language}，时长约 {info.duration:.1f} 秒")
    segment_list = list(segments)
    log(f"Whisper 原始分段数: {len(segment_list)}")
    cues = merge_to_sentences(segment_list)
    log(f"合并后句级条数: {len(cues)}")
    return cues


def main() -> int:
    parser = argparse.ArgumentParser(description="ISLAND Whisper 句级字幕提取")
    parser.add_argument("--input", required=True, help="视频或音频文件路径")
    parser.add_argument("--ffmpeg", default="ffmpeg", help="ffmpeg 可执行文件路径")
    parser.add_argument("--model", default=os.environ.get("ISLAND_WHISPER_MODEL", "small"))
    parser.add_argument("--device", default=os.environ.get("ISLAND_WHISPER_DEVICE", "cuda"))
    parser.add_argument("--language", default="en")
    parser.add_argument("--keep-audio", action="store_true", help="保留临时 wav")
    args = parser.parse_args()

    input_path = Path(args.input).resolve()
    if not input_path.exists():
        log(f"输入文件不存在: {input_path}")
        return 1

    ffmpeg = args.ffmpeg
    if not Path(ffmpeg).exists() and ffmpeg == "ffmpeg":
        win_default = Path(r"E:\software\ffmpeg\bin\ffmpeg.exe")
        if win_default.exists():
            ffmpeg = str(win_default)

    log(f"开始处理: {input_path.name}")
    duration_ms = probe_duration_ms(ffmpeg, input_path)

    suffix = input_path.suffix.lower()
    audio_only = suffix in {".wav", ".mp3", ".m4a", ".flac", ".ogg"}

    with tempfile.TemporaryDirectory(prefix="island_asr_") as tmp:
        wav_path = Path(tmp) / "audio.wav"
        if audio_only:
            log("输入为音频文件，转换为 16kHz WAV")
            run_ffmpeg(
                ffmpeg,
                ["-y", "-i", str(input_path), "-ar", "16000", "-ac", "1", str(wav_path)],
            )
        else:
            extract_audio(ffmpeg, input_path, wav_path)

        if args.keep_audio:
            keep = input_path.with_suffix(".extracted.wav")
            keep.write_bytes(wav_path.read_bytes())
            log(f"已保留调试音频: {keep}")

        try:
            cues = transcribe(
                wav_path,
                model_name=args.model,
                device=args.device,
                language=args.language,
            )
        except Exception as e:
            if args.device == "cuda":
                log(f"CUDA 识别失败 ({e})，回退到 CPU")
                cues = transcribe(
                    wav_path,
                    model_name=args.model,
                    device="cpu",
                    language=args.language,
                )
            else:
                raise

    if not cues:
        log("警告：未识别到任何句子，请检查视频是否含清晰英文旁白")
        return 2

    result = {
        "durationMs": duration_ms,
        "sentenceCount": len(cues),
        "sentences": [asdict(c) for c in cues],
    }
    print(json.dumps(result, ensure_ascii=False))
    log("识别完成，结果已输出 JSON")
    return 0


if __name__ == "__main__":
    sys.exit(main())
