package com.island.module.post;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.island.common.BusinessException;
import com.island.common.PageResult;
import com.island.config.IslandProperties;
import com.island.module.post.mapper.FileAssetMapper;
import com.island.module.post.mapper.PostImageMapper;
import com.island.module.post.mapper.PostMapper;
import com.island.module.user.User;
import com.island.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PostService {

	private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png", "image/webp");

	private final PostMapper postMapper;
	private final PostImageMapper postImageMapper;
	private final FileAssetMapper fileAssetMapper;
	private final UserMapper userMapper;
	private final IslandProperties islandProperties;

	public PageResult<PostDto> listFeed(int page, int size) {
		Page<Post> pageObj = postMapper.selectPage(new Page<>(page, size),
				new LambdaQueryWrapper<Post>()
						.eq(Post::getStatus, 1)
						.orderByDesc(Post::getCreatedAt));
		List<PostDto> items = pageObj.getRecords().stream().map(this::toDto).toList();
		return new PageResult<>(items, pageObj.getTotal(), page, size);
	}

	@Transactional
	public PostDto createPost(Long userId, CreatePostRequest request) {
		if (request.content() == null || request.content().isBlank()) {
			throw new BusinessException("帖子内容不能为空");
		}
		Post post = new Post();
		post.setUserId(userId);
		post.setContent(request.content().trim());
		postMapper.insert(post);

		int order = 0;
		if (request.fileIds() != null) {
			for (Long fileId : request.fileIds()) {
				FileAsset asset = fileAssetMapper.selectById(fileId);
				if (asset == null) {
					throw new BusinessException("图片不存在: " + fileId);
				}
				if (!Objects.equals(asset.getUserId(), userId)) {
					throw new BusinessException(403, "无权使用该图片");
				}
				PostImage pi = new PostImage();
				pi.setPostId(post.getId());
				pi.setFileId(fileId);
				pi.setSortOrder(order++);
				postImageMapper.insert(pi);
			}
		}
		return toDto(post);
	}

	@Transactional
	public FileUploadResult uploadImage(Long userId, MultipartFile file) {
		if (file.isEmpty()) {
			throw new BusinessException("文件为空");
		}
		String mime = file.getContentType();
		if (mime == null || !ALLOWED_MIME.contains(mime)) {
			throw new BusinessException("仅支持 jpeg/png/webp 图片");
		}
		if (file.getSize() > 5 * 1024 * 1024) {
			throw new BusinessException("图片不能超过 5MB");
		}
		try {
			String ext = switch (mime) {
				case "image/png" -> ".png";
				case "image/webp" -> ".webp";
				default -> ".jpg";
			};
			String filename = UUID.randomUUID() + ext;
			Path dir = Path.of(islandProperties.getUpload().getDir(), "posts");
			Files.createDirectories(dir);
			Path target = dir.resolve(filename);
			Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

			FileAsset asset = new FileAsset();
			asset.setUserId(userId);
			asset.setObjectKey("posts/" + filename);
			asset.setOriginalName(file.getOriginalFilename());
			asset.setMimeType(mime);
			asset.setSizeBytes((int) file.getSize());
			fileAssetMapper.insert(asset);

			return new FileUploadResult(asset.getId(), "/uploads/" + asset.getObjectKey());
		} catch (IOException e) {
			throw new BusinessException("上传失败: " + e.getMessage());
		}
	}

	private PostDto toDto(Post post) {
		User author = userMapper.selectById(post.getUserId());
		List<String> images = postImageMapper.selectList(new LambdaQueryWrapper<PostImage>()
						.eq(PostImage::getPostId, post.getId())
						.orderByAsc(PostImage::getSortOrder))
				.stream()
				.map(pi -> fileAssetMapper.selectById(pi.getFileId()))
				.filter(Objects::nonNull)
				.map(f -> "/uploads/" + f.getObjectKey())
				.toList();
		return new PostDto(
				post.getId(),
				post.getUserId(),
				author != null ? author.getNickname() : "岛民",
				author != null ? author.getAvatarUrl() : null,
				post.getContent(),
				images,
				post.getLikeCount(),
				post.getCreatedAt()
		);
	}

	public record CreatePostRequest(String content, List<Long> fileIds) {}

	public record FileUploadResult(Long fileId, String url) {}

	public record PostDto(
			Long id, Long userId, String authorNickname, String authorAvatar,
			String content, List<String> images, int likeCount,
			java.time.LocalDateTime createdAt
	) {}
}
