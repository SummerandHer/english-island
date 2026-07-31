package com.island.module.video;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("video_tag_rel")
public class VideoTagRel {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("video_id")
	private Long videoId;

	@TableField("tag_id")
	private Integer tagId;
}
