package com.island.module.vocabulary;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("dict_entry")
public class DictEntry {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String word;

	private String phonetic;

	@TableField("meaning_zh")
	private String meaningZh;

	private String pos;

	private String tags;

	private Integer frq;

	private Integer bnc;

	private String source;

	@TableField("created_at")
	private LocalDateTime createdAt;
}
