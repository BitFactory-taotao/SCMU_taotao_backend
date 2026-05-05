package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员登录请求参数
 */
@Data
public class AdminLoginRequest {

	/**
	 * 管理员账号
	 */
	@NotBlank(message = "管理员账号不能为空")
	private String adminId;

	/**
	 * 登录密码
	 */
	@NotBlank(message = "登录密码不能为空")
	private String password;
}

