package com.island.module.video.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UpdateSentencesRequest {

	@NotEmpty
	@Valid
	private List<PublishVideoRequest.SentenceDraft> sentences;
}
