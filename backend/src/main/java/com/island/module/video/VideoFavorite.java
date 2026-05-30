package com.island.module.video;

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
@TableName("video_favorite")
public class VideoFavorite {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("user_id")
	private Long userId;

	@TableField("video_id")
	private Long videoId;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
}
