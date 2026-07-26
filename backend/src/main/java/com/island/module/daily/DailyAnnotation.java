package com.island.module.daily;

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
@TableName("daily_annotation")
public class DailyAnnotation {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("user_id")
	private Long userId;

	@TableField("article_id")
	private Long articleId;

	@TableField("start_offset")
	private Integer startOffset;

	@TableField("end_offset")
	private Integer endOffset;

	@TableField("selected_text")
	private String selectedText;

	private String color = "moss";

	private String note;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
}
