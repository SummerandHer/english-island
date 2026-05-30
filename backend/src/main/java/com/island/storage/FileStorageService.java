package com.island.storage;

import java.io.InputStream;

/**
 * 文件存储抽象，支持 local / oss 切换。
 */
public interface FileStorageService {

	String getStorageType();

	void upload(InputStream input, String objectKey, String contentType, long sizeBytes);

	void delete(String objectKey);

	String resolvePublicUrl(String bucket, String objectKey);
}
