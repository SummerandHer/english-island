package com.island.module.reading;

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
@TableName("reading_chapter")
public class ReadingChapter {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String title;

	private String slug;

	private String summary;

	@TableField("content_html")
	private String contentHtml;

	@TableField("sort_order")
	private Integer sortOrder = 0;

	@TableField("is_vip")
	private Integer isVip = 0;

	private Integer status = 1;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;
}
