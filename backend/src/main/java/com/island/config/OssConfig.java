package com.island.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.island.common.BusinessException;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(name = "island.upload.storage-type", havingValue = "oss")
public class OssConfig {

	private OSS ossClient;

	@Bean
	public OSS ossClient(IslandProperties islandProperties) {
		IslandProperties.Oss oss = islandProperties.getOss();
		if (!StringUtils.hasText(oss.getAccessKeyId()) || !StringUtils.hasText(oss.getAccessKeySecret())) {
			throw new BusinessException("OSS 已启用但未配置 access-key-id / access-key-secret，请在 application-dev.yml 中填写");
		}
		ClientBuilderConfiguration clientConfig = new ClientBuilderConfiguration();
		clientConfig.setSignatureVersion(SignVersion.V4);
		this.ossClient = OSSClientBuilder.create()
				.endpoint(oss.getEndpoint())
				.credentialsProvider(new DefaultCredentialProvider(oss.getAccessKeyId(), oss.getAccessKeySecret()))
				.clientConfiguration(clientConfig)
				.region(oss.getRegion())
				.build();
		return this.ossClient;
	}

	@PreDestroy
	public void shutdown() {
		if (ossClient != null) {
			ossClient.shutdown();
		}
	}
}
