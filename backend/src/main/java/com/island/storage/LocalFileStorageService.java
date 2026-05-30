package com.island.storage;

import com.island.common.BusinessException;
import com.island.config.IslandProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "island.upload.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

	private final IslandProperties islandProperties;

	@Override
	public String getStorageType() {
		return "local";
	}

	@Override
	public void upload(InputStream input, String objectKey, String contentType, long sizeBytes) {
		try {
			Path target = resolvePath(objectKey);
			Files.createDirectories(target.getParent());
			Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new BusinessException("本地存储上传失败: " + e.getMessage());
		}
	}

	@Override
	public void delete(String objectKey) {
		try {
			Path target = resolvePath(objectKey);
			Files.deleteIfExists(target);
		} catch (IOException e) {
			throw new BusinessException("本地存储删除失败: " + e.getMessage());
		}
	}

	@Override
	public String resolvePublicUrl(String bucket, String objectKey) {
		return "/uploads/" + objectKey;
	}

	private Path resolvePath(String objectKey) {
		return Path.of(islandProperties.getUpload().getDir(), objectKey);
	}
}
