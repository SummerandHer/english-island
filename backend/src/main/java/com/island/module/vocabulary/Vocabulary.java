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
@TableName("vocabulary")
public class Vocabulary {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String word;

	private String phonetic;

	@TableField("part_of_speech")
	private String partOfSpeech;

	@TableField("meaning_zh")
	private String meaningZh;

	@TableField("example_en")
	private String exampleEn;

	@TableField("example_zh")
	private String exampleZh;

	private String collocation;

	@TableField("phrases_json")
	private String phrasesJson;

	private String difficulty = "cet4";

	@TableField("freq_rank")
	private Integer freqRank;

	@TableField("source_note")
	private String sourceNote;

	@TableField(value = "created_at")
	private LocalDateTime createdAt;
}
