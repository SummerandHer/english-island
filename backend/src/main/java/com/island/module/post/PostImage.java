package com.island.module.post;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("post_image")
public class PostImage {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("post_id")
	private Long postId;

	@TableField("file_id")
	private Long fileId;

	@TableField("sort_order")
	private Integer sortOrder = 0;
}
