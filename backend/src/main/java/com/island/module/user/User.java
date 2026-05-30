package com.island.module.user;

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
@TableName("`user`")
public class User {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String email;

	private String phone;

	@TableField("password_hash")
	private String passwordHash;

	private String nickname = "岛民";

	@TableField("avatar_url")
	private String avatarUrl;

	@TableField("vip_level")
	private Integer vipLevel = 0;

	@TableField("vip_expire_at")
	private LocalDateTime vipExpireAt;

	private Integer status = 1;

	@TableField("last_login_at")
	private LocalDateTime lastLoginAt;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;

	public boolean isVipActive() {
		if (vipLevel == null || vipLevel < 1) {
			return false;
		}
		return vipExpireAt == null || vipExpireAt.isAfter(LocalDateTime.now());
	}
}
