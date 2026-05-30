package com.island.module.post;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("file_asset")
public class FileAsset {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("user_id")
	private Long userId;

	@TableField("storage_type")
	private String storageType = "local";

	private String bucket;

	@TableField("object_key")
	private String objectKey;

	@TableField("original_name")
	private String originalName;

	@TableField("mime_type")
	private String mimeType;

	@TableField("size_bytes")
	private Integer sizeBytes = 0;

	private Integer width;

	private Integer height;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
}
