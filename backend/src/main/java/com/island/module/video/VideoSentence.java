package com.island.module.video;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("video_sentence")
public class VideoSentence {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("video_id")
	private Long videoId;

	private Integer seq;

	@TableField("start_ms")
	private Integer startMs;

	@TableField("end_ms")
	private Integer endMs;

	@TableField("text_en")
	private String textEn;

	@TableField("text_zh")
	private String textZh;
}
