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
@TableName("video_series")
public class VideoSeries {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String title;

	private String description;

	@TableField("cover_url")
	private String coverUrl;

	@TableField("sort_order")
	private Integer sortOrder = 0;

	private Integer status = 1;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
}
