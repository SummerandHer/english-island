package com.island.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendCodeRequest {

	@NotBlank
	@Pattern(regexp = "^[a-zA-Z0-9._%+-]+@qq\\.com$", message = "仅支持 QQ 邮箱")
	private String email;

	@NotBlank
	@Pattern(regexp = "^(register|reset_password)$", message = "无效的场景")
	private String scene;
}
