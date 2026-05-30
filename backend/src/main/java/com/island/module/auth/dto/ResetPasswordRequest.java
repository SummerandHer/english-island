package com.island.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

	@NotBlank
	@Pattern(regexp = "^[a-zA-Z0-9._%+-]+@qq\\.com$", message = "仅支持 QQ 邮箱")
	private String email;

	@NotBlank
	@Size(min = 6, max = 6, message = "验证码为 6 位")
	private String code;

	@NotBlank @Size(min = 6, max = 64)
	private String newPassword;
}
