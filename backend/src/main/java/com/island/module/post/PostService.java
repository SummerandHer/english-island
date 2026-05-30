package com.island.module.post;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.island.common.BusinessException;
import com.island.common.PageResult;
import com.island.module.file.FileService;
import com.island.module.file.dto.FileDto;
import com.island.module.post.mapper.FileAssetMapper;
import com.island.module.post.mapper.PostImageMapper;
import com.island.module.post.mapper.PostMapper;
import com.island.module.user.User;
import com.island.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostMapper postMapper;
	private final PostImageMapper postImageMapper;
	private final FileAssetMapper fileAssetMapper;
	private final UserMapper userMapper;
	private final FileService fileService;

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

	public FileUploadResult uploadImage(Long userId, MultipartFile file) {
		FileDto dto = fileService.upload(userId, file, "posts");
		return new FileUploadResult(dto.id(), dto.url());
	}

	private PostDto toDto(Post post) {
		User author = userMapper.selectById(post.getUserId());
		List<String> images = postImageMapper.selectList(new LambdaQueryWrapper<PostImage>()
						.eq(PostImage::getPostId, post.getId())
						.orderByAsc(PostImage::getSortOrder))
				.stream()
				.map(pi -> fileAssetMapper.selectById(pi.getFileId()))
				.filter(Objects::nonNull)
				.map(fileService::resolveUrl)
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
