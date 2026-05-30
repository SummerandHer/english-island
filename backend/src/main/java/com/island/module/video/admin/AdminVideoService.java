package com.island.module.video.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.island.common.BusinessException;
import com.island.common.PageResult;
import com.island.config.IslandProperties;
import com.island.module.file.FileService;
import com.island.module.file.dto.FileDto;
import com.island.module.post.FileAsset;
import com.island.module.post.mapper.FileAssetMapper;
import com.island.module.video.Video;
import com.island.module.video.VocabCounter;
import com.island.module.video.VideoSentence;
import com.island.module.video.VideoSeries;
import com.island.module.video.admin.dto.*;
import com.island.module.video.ai.SentenceZhDraftService;
import com.island.module.video.asr.WhisperAsrService;
import com.island.module.video.mapper.VideoMapper;
import com.island.module.video.mapper.VideoSentenceMapper;
import com.island.module.video.mapper.VideoSeriesMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVideoService {

	private final VideoMapper videoMapper;
	private final VideoSentenceMapper sentenceMapper;
	private final VideoSeriesMapper seriesMapper;
	private final FileAssetMapper fileAssetMapper;
	private final FileService fileService;
	private final WhisperAsrService whisperAsrService;
	private final SentenceZhDraftService sentenceZhDraftService;
	private final IslandProperties islandProperties;

	public ParseVideoResponse parseUpload(Long adminId, MultipartFile file, boolean generateZh) {
		log.info("[视频解析] 收到上传，管理员 id={}，文件={}，大小={} bytes",
				adminId, file.getOriginalFilename(), file.getSize());
		Path tempMedia = null;
		try {
			tempMedia = Files.createTempFile("island_upload_", resolveSuffix(file.getOriginalFilename()));
			file.transferTo(tempMedia);
			log.info("[视频解析] 临时文件已写入: {}", tempMedia);

			log.info("[视频解析] 开始语音识别…");
			WhisperAsrService.AsrResult asr = whisperAsrService.transcribe(tempMedia);
			List<String> logs = new ArrayList<>(asr.logs());

			List<ParseVideoResponse.SentenceDraft> drafts = new ArrayList<>();
			List<String> enLines = asr.sentences().stream().map(WhisperAsrService.SentenceCue::textEn).toList();
			List<String> zhLines = generateZh ? sentenceZhDraftService.generateDrafts(enLines) : enLines.stream().map(e -> "").toList();

			for (int i = 0; i < asr.sentences().size(); i++) {
				var s = asr.sentences().get(i);
				String zh = i < zhLines.size() ? zhLines.get(i) : "";
				drafts.add(new ParseVideoResponse.SentenceDraft(s.seq(), s.startMs(), s.endMs(), s.textEn(), zh));
			}

			log.info("[视频解析] 上传视频到存储…");
			String mime = file.getContentType() != null ? file.getContentType() : "video/mp4";
			FileDto uploaded = fileService.uploadFromPath(
					adminId, tempMedia, file.getOriginalFilename(), mime, "videos");
			log.info("[视频解析] 存储完成 url={}", uploaded.url());

			Integer durationSec = asr.durationMs() != null ? (int) Math.ceil(asr.durationMs() / 1000.0) : null;
			FileAsset asset = fileAssetMapper.selectById(uploaded.id());

			return new ParseVideoResponse(
					asset.getObjectKey(),
					uploaded.url(),
					uploaded.id(),
					asr.durationMs(),
					durationSec,
					drafts,
					logs
			);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("[视频解析] 失败", e);
			throw new BusinessException("视频解析失败: " + e.getMessage());
		} finally {
			if (tempMedia != null) {
				try {
					Files.deleteIfExists(tempMedia);
				} catch (Exception ignored) {
				}
			}
		}
	}

	@Transactional
	public Long publish(Long adminId, PublishVideoRequest req) {
		validatePublish(req);
		FileAsset asset = requireVideoAsset(req.getVideoObjectKey());

		Video video = new Video();
		video.setTitle(req.getTitle().trim());
		video.setDescription(req.getDescription());
		video.setCoverUrl(req.getCoverUrl());
		video.setSeriesId(req.getSeriesId());
		// 自托管视频统一记为 oss（本地存储时 playUrl 指向 /uploads）
		video.setStorageType("oss");
		video.setProvider("self");
		String playUrl = req.getPlayUrl() != null && !req.getPlayUrl().isBlank()
				? req.getPlayUrl() : fileService.resolveUrl(asset);
		video.setPlayUrl(playUrl);
		video.setSourceUrl(playUrl);
		video.setDifficulty(req.getDifficulty());
		video.setIsVip(req.getIsVip() != null ? req.getIsVip() : 0);
		video.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
		video.setStatus(req.getStatus());
		if (req.getSentences() != null && !req.getSentences().isEmpty()) {
			int lastEnd = req.getSentences().get(req.getSentences().size() - 1).getEndMs();
			video.setDurationSec((int) Math.ceil(lastEnd / 1000.0));
		}
		videoMapper.insert(video);
		saveSentences(video.getId(), req.getSentences());
		log.info("[视频解析] 视频已发布 id={} title={}", video.getId(), video.getTitle());
		return video.getId();
	}

	public PageResult<AdminVideoSummary> list(int page, int size, Integer status) {
		var wrapper = new LambdaQueryWrapper<Video>().orderByDesc(Video::getCreatedAt);
		if (status != null) {
			wrapper.eq(Video::getStatus, status);
		}
		Page<Video> p = videoMapper.selectPage(new Page<>(page, size), wrapper);
		List<AdminVideoSummary> items = p.getRecords().stream().map(v -> {
			Long cnt = sentenceMapper.selectCount(new LambdaQueryWrapper<VideoSentence>()
					.eq(VideoSentence::getVideoId, v.getId()));
			return new AdminVideoSummary(
					v.getId(), v.getTitle(), v.getCoverUrl(), v.getStorageType(),
					v.getStatus(), v.getDurationSec(), v.getIsVip() == 1,
					cnt != null ? cnt.intValue() : 0, v.getCreatedAt());
		}).toList();
		return new PageResult<>(items, p.getTotal(), page, size);
	}

	public AdminVideoDetail getDetail(Long id) {
		Video video = requireVideo(id);
		var sentences = sentenceMapper.selectList(new LambdaQueryWrapper<VideoSentence>()
						.eq(VideoSentence::getVideoId, id)
						.orderByAsc(VideoSentence::getSeq))
				.stream()
				.map(s -> new AdminVideoDetail.SentenceView(
						s.getId(), s.getSeq(), s.getStartMs(), s.getEndMs(), s.getTextEn(), s.getTextZh()))
				.toList();
		return new AdminVideoDetail(
				video.getId(), video.getSeriesId(), video.getTitle(), video.getDescription(),
				video.getCoverUrl(), video.getStorageType(), video.getProvider(),
				video.getSourceUrl(), video.getEmbedBvid(), video.getPlayUrl(),
				video.getDurationSec(), video.getDifficulty(), video.getIsVip() == 1,
				video.getSortOrder(), video.getStatus(), video.getCreatedAt(), sentences);
	}

	@Transactional
	public void update(Long id, UpdateVideoRequest req) {
		Video video = requireVideo(id);
		if (req.getTitle() != null) video.setTitle(req.getTitle());
		if (req.getDescription() != null) video.setDescription(req.getDescription());
		if (req.getCoverUrl() != null) video.setCoverUrl(req.getCoverUrl());
		if (req.getSeriesId() != null) video.setSeriesId(req.getSeriesId());
		if (req.getDifficulty() != null) video.setDifficulty(req.getDifficulty());
		if (req.getIsVip() != null) video.setIsVip(req.getIsVip());
		if (req.getSortOrder() != null) video.setSortOrder(req.getSortOrder());
		if (req.getStatus() != null) video.setStatus(req.getStatus());
		videoMapper.updateById(video);
	}

	@Transactional
	public void updateSentences(Long videoId, UpdateSentencesRequest req) {
		requireVideo(videoId);
		if (req.getSentences().size() < islandProperties.getAdmin().getMinSentencesOnPublish()) {
			throw new BusinessException("至少需要 " + islandProperties.getAdmin().getMinSentencesOnPublish() + " 句字幕");
		}
		sentenceMapper.delete(new LambdaQueryWrapper<VideoSentence>().eq(VideoSentence::getVideoId, videoId));
		saveSentences(videoId, req.getSentences());
	}

	@Transactional
	public List<ParseVideoResponse.SentenceDraft> generateZhDraft(Long videoId) {
		var sentences = sentenceMapper.selectList(new LambdaQueryWrapper<VideoSentence>()
				.eq(VideoSentence::getVideoId, videoId)
				.orderByAsc(VideoSentence::getSeq));
		if (sentences.isEmpty()) {
			throw new BusinessException("视频尚无字幕");
		}
		List<String> en = sentences.stream().map(VideoSentence::getTextEn).toList();
		List<String> zh = sentenceZhDraftService.generateDrafts(en);
		var drafts = new ArrayList<ParseVideoResponse.SentenceDraft>();
		for (int i = 0; i < sentences.size(); i++) {
			VideoSentence s = sentences.get(i);
			String textZh = i < zh.size() ? zh.get(i) : "";
			drafts.add(new ParseVideoResponse.SentenceDraft(
					s.getSeq(), s.getStartMs(), s.getEndMs(), s.getTextEn(), textZh));
		}
		return drafts;
	}

	public List<VideoSeries> listSeries() {
		return seriesMapper.selectList(new LambdaQueryWrapper<VideoSeries>()
				.orderByAsc(VideoSeries::getSortOrder));
	}

	@Transactional
	public Long createSeries(SeriesRequest req) {
		VideoSeries s = new VideoSeries();
		s.setTitle(req.getTitle().trim());
		s.setDescription(req.getDescription());
		s.setCoverUrl(req.getCoverUrl());
		s.setSortOrder(req.getSortOrder());
		s.setStatus(req.getStatus());
		seriesMapper.insert(s);
		return s.getId();
	}

	@Transactional
	public void updateSeries(Long id, SeriesRequest req) {
		VideoSeries s = seriesMapper.selectById(id);
		if (s == null) {
			throw new BusinessException(404, "系列不存在");
		}
		s.setTitle(req.getTitle().trim());
		s.setDescription(req.getDescription());
		s.setCoverUrl(req.getCoverUrl());
		s.setSortOrder(req.getSortOrder());
		s.setStatus(req.getStatus());
		seriesMapper.updateById(s);
	}

	private void validatePublish(PublishVideoRequest req) {
		if (req.getTitle() == null || req.getTitle().isBlank()) {
			throw new BusinessException("标题不能为空");
		}
		if (req.getCoverUrl() == null || req.getCoverUrl().isBlank()) {
			throw new BusinessException("封面不能为空");
		}
		int min = islandProperties.getAdmin().getMinSentencesOnPublish();
		if (req.getSentences() == null || req.getSentences().size() < min) {
			throw new BusinessException("至少需要 " + min + " 句字幕");
		}
	}

	private void saveSentences(Long videoId, List<PublishVideoRequest.SentenceDraft> drafts) {
		int seq = 1;
		for (PublishVideoRequest.SentenceDraft d : drafts) {
			VideoSentence s = new VideoSentence();
			s.setVideoId(videoId);
			s.setSeq(d.getSeq() != null ? d.getSeq() : seq);
			s.setStartMs(d.getStartMs());
			s.setEndMs(d.getEndMs());
			s.setTextEn(d.getTextEn().trim());
			s.setTextZh(d.getTextZh() != null ? d.getTextZh().trim() : "");
			sentenceMapper.insert(s);
			seq++;
		}
		refreshVocabCount(videoId, drafts.stream().map(PublishVideoRequest.SentenceDraft::getTextEn).toList());
	}

	private void refreshVocabCount(Long videoId, List<String> englishTexts) {
		int count = VocabCounter.countUniqueWords(englishTexts);
		videoMapper.update(null, new LambdaUpdateWrapper<Video>()
				.eq(Video::getId, videoId)
				.set(Video::getVocabCount, count));
	}

	private FileAsset requireVideoAsset(String objectKey) {
		FileAsset asset = fileAssetMapper.selectOne(new LambdaQueryWrapper<FileAsset>()
				.eq(FileAsset::getObjectKey, objectKey)
				.last("LIMIT 1"));
		if (asset == null) {
			throw new BusinessException("视频文件未找到，请重新上传解析");
		}
		return asset;
	}

	private Video requireVideo(Long id) {
		Video video = videoMapper.selectById(id);
		if (video == null) {
			throw new BusinessException(404, "视频不存在");
		}
		return video;
	}

	private String resolveSuffix(String name) {
		if (name != null && name.contains(".")) {
			return name.substring(name.lastIndexOf('.'));
		}
		return ".mp4";
	}
}
