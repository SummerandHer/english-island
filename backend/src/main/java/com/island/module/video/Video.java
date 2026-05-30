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
@TableName("video")
public class Video {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("series_id")
	private Long seriesId;

	private String title;

	private String description;

	@TableField("cover_url")
	private String coverUrl;

	@TableField("storage_type")
	private String storageType = "embed";

	private String provider = "bilibili";

	@TableField("source_url")
	private String sourceUrl;

	@TableField("embed_bvid")
	private String embedBvid;

	@TableField("embed_aid")
	private Long embedAid;

	@TableField("embed_cid")
	private Long embedCid;

	@TableField("play_url")
	private String playUrl;

	@TableField("duration_sec")
	private Integer durationSec;

	private String difficulty = "medium";

	@TableField("is_vip")
	private Integer isVip = 0;

	@TableField("view_count")
	private Integer viewCount = 0;

	@TableField("sort_order")
	private Integer sortOrder = 0;

	@TableField("license_note")
	private String licenseNote;

	private Integer status = 1;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;
}
