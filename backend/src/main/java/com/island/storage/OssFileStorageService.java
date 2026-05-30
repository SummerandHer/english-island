package com.island.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.island.common.BusinessException;
import com.island.config.IslandProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "island.upload.storage-type", havingValue = "oss")
public class OssFileStorageService implements FileStorageService {

	private final OSS ossClient;
	private final IslandProperties islandProperties;

	@Override
	public String getStorageType() {
		return "oss";
	}

	@Override
	public void upload(InputStream input, String objectKey, String contentType, long sizeBytes) {
		try {
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentType(contentType);
			metadata.setContentLength(sizeBytes);
			ossClient.putObject(bucket(), objectKey, input, metadata);
		} catch (Exception e) {
			throw new BusinessException("OSS 上传失败: " + e.getMessage());
		}
	}

	@Override
	public void delete(String objectKey) {
		try {
			ossClient.deleteObject(bucket(), objectKey);
		} catch (Exception e) {
			throw new BusinessException("OSS 删除失败: " + e.getMessage());
		}
	}

	@Override
	public String resolvePublicUrl(String bucket, String objectKey) {
		IslandProperties.Oss oss = islandProperties.getOss();
		String baseUrl = oss.getPublicBaseUrl();
		if (baseUrl != null && !baseUrl.isBlank()) {
			String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
			return normalized + "/" + objectKey;
		}
		String bucketName = bucket != null && !bucket.isBlank() ? bucket : bucket();
		String host = oss.getEndpoint().replaceFirst("^https?://", "");
		return "https://" + bucketName + "." + host + "/" + objectKey;
	}

	private String bucket() {
		return islandProperties.getOss().getBucket();
	}
}
