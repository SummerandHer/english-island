package com.island.module.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.island.common.BusinessException;
import com.island.common.PageResult;
import com.island.config.IslandProperties;
import com.island.module.file.dto.FileDto;
import com.island.module.post.FileAsset;
import com.island.module.post.mapper.FileAssetMapper;
import com.island.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

	private static final Set<String> ALLOWED_IMAGE_MIME = Set.of(
			"image/jpeg", "image/png", "image/webp", "image/gif");
	private static final Set<String> ALLOWED_VIDEO_MIME = Set.of(
			"video/mp4", "video/webm", "video/quicktime", "video/x-msvideo");

	private final FileAssetMapper fileAssetMapper;
	private final FileStorageService fileStorageService;
	private final IslandProperties islandProperties;

	@Transactional
	public FileDto upload(Long userId, MultipartFile file) {
		return upload(userId, file, null);
	}

	@Transactional
	public FileDto upload(Long userId, MultipartFile file, String folderOverride) {
		validateFile(file);
		String mime = Objects.requireNonNull(file.getContentType());
		String folder = folderOverride != null ? folderOverride : resolveFolder(mime);
		String ext = resolveExtension(mime, file.getOriginalFilename());
		String objectKey = folder + "/" + UUID.randomUUID() + ext;

		try {
			fileStorageService.upload(file.getInputStream(), objectKey, mime, file.getSize());
		} catch (IOException e) {
			throw new BusinessException("读取上传文件失败: " + e.getMessage());
		}

		FileAsset asset = new FileAsset();
		asset.setUserId(userId);
		asset.setStorageType(fileStorageService.getStorageType());
		if ("oss".equals(fileStorageService.getStorageType())) {
			asset.setBucket(islandProperties.getOss().getBucket());
		}
		asset.setObjectKey(objectKey);
		asset.setOriginalName(file.getOriginalFilename());
		asset.setMimeType(mime);
		asset.setSizeBytes(safeSize(file.getSize()));
		fileAssetMapper.insert(asset);

		return toDto(asset);
	}

	public FileDto getById(Long id) {
		return toDto(requireAsset(id));
	}

	public PageResult<FileDto> listByUser(Long userId, int page, int size) {
		Page<FileAsset> pageObj = fileAssetMapper.selectPage(new Page<>(page, size),
				new LambdaQueryWrapper<FileAsset>()
						.eq(FileAsset::getUserId, userId)
						.orderByDesc(FileAsset::getCreatedAt));
		List<FileDto> items = pageObj.getRecords().stream().map(this::toDto).toList();
		return new PageResult<>(items, pageObj.getTotal(), page, size);
	}

	@Transactional
	public void delete(Long userId, Long id) {
		FileAsset asset = requireAsset(id);
		if (!Objects.equals(asset.getUserId(), userId)) {
			throw new BusinessException(403, "无权删除该文件");
		}
		fileStorageService.delete(asset.getObjectKey());
		fileAssetMapper.deleteById(id);
	}

	public String resolveUrl(FileAsset asset) {
		return fileStorageService.resolvePublicUrl(asset.getBucket(), asset.getObjectKey());
	}

	private FileAsset requireAsset(Long id) {
		FileAsset asset = fileAssetMapper.selectById(id);
		if (asset == null) {
			throw new BusinessException(404, "文件不存在");
		}
		return asset;
	}

	private void validateFile(MultipartFile file) {
		if (file.isEmpty()) {
			throw new BusinessException("文件为空");
		}
		String mime = file.getContentType();
		if (mime == null || (!ALLOWED_IMAGE_MIME.contains(mime) && !ALLOWED_VIDEO_MIME.contains(mime))) {
			throw new BusinessException("仅支持图片（jpeg/png/webp/gif）或视频（mp4/webm/mov/avi）");
		}
		long maxBytes = islandProperties.getUpload().getMaxSizeMb() * 1024 * 1024;
		if (file.getSize() > maxBytes) {
			throw new BusinessException("文件不能超过 " + islandProperties.getUpload().getMaxSizeMb() + "MB");
		}
	}

	private String resolveFolder(String mime) {
		if (ALLOWED_IMAGE_MIME.contains(mime)) {
			return "images";
		}
		return "videos";
	}

	private String resolveExtension(String mime, String originalName) {
		return switch (mime) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			case "image/gif" -> ".gif";
			case "video/webm" -> ".webm";
			case "video/quicktime" -> ".mov";
			case "video/x-msvideo" -> ".avi";
			case "video/mp4" -> ".mp4";
			default -> {
				if (originalName != null && originalName.contains(".")) {
					yield originalName.substring(originalName.lastIndexOf('.'));
				}
				yield ".jpg";
			}
		};
	}

	private int safeSize(long size) {
		if (size > Integer.MAX_VALUE) {
			throw new BusinessException("文件过大");
		}
		return (int) size;
	}

	private FileDto toDto(FileAsset asset) {
		return new FileDto(
				asset.getId(),
				asset.getUserId(),
				asset.getStorageType(),
				asset.getOriginalName(),
				asset.getMimeType(),
				asset.getSizeBytes(),
				resolveUrl(asset),
				asset.getCreatedAt()
		);
	}
}
