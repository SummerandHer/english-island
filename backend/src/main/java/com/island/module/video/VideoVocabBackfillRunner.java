package com.island.module.video;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.island.module.video.mapper.VideoMapper;
import com.island.module.video.mapper.VideoSentenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时回填 vocab_count=0 的历史视频（一次性，数据量小）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
class VideoVocabBackfillRunner implements ApplicationRunner {

	private final VideoMapper videoMapper;
	private final VideoSentenceMapper sentenceMapper;

	@Override
	public void run(ApplicationArguments args) {
		List<Video> pending = videoMapper.selectList(new LambdaQueryWrapper<Video>()
				.eq(Video::getVocabCount, 0));
		if (pending.isEmpty()) {
			return;
		}
		int updated = 0;
		for (Video video : pending) {
			var sentences = sentenceMapper.selectList(new LambdaQueryWrapper<VideoSentence>()
					.eq(VideoSentence::getVideoId, video.getId())
					.select(VideoSentence::getTextEn));
			if (sentences.isEmpty()) {
				continue;
			}
			int count = VocabCounter.countUniqueWords(sentences.stream().map(VideoSentence::getTextEn).toList());
			videoMapper.update(null, new LambdaUpdateWrapper<Video>()
					.eq(Video::getId, video.getId())
					.set(Video::getVocabCount, count));
			updated++;
		}
		if (updated > 0) {
			log.info("[视频] 回填 vocab_count 完成，共 {} 条", updated);
		}
	}
}
