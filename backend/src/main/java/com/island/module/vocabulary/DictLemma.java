package com.island.module.vocabulary;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("dict_lemma")
public class DictLemma {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String variant;

	private String lemma;

	private String relation;
}
