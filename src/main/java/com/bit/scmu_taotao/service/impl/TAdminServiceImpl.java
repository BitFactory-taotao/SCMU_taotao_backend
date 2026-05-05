package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.dto.admin.AdminLoginRequest;
import com.bit.scmu_taotao.entity.TAdmin;
import com.bit.scmu_taotao.mapper.TAdminMapper;
import com.bit.scmu_taotao.service.TAdminService;
import com.bit.scmu_taotao.util.TokenUtil;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class TAdminServiceImpl extends ServiceImpl<TAdminMapper, TAdmin> implements TAdminService {

	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Autowired
	private TokenUtil tokenUtil;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Result login(AdminLoginRequest request) {
		try {
			if (request == null) {
				return Result.fail(400, "请求体不能为空");
			}
			String adminId = request.getAdminId() == null ? null : request.getAdminId().trim();
			String password = request.getPassword() == null ? null : request.getPassword().trim();
			log.info("管理员登录请求：adminId={}", adminId);
			if (!StringUtils.hasText(adminId) || !StringUtils.hasText(password)) {
				return Result.fail(400, "管理员账号或密码不能为空");
			}
			TAdmin admin = this.getOne(new LambdaQueryWrapper<TAdmin>().eq(TAdmin::getAdminId, adminId).last("limit 1"), false);
			if (admin == null || !Objects.equals(admin.getPassword(), password)) {
				log.warn("管理员登录失败：adminId={}", adminId);
				return Result.fail(401, "管理员账号或密码错误");
			}
			LocalDateTime now = LocalDateTime.now();
			admin.setLastLogin(now);
			this.updateById(admin);
			String token = tokenUtil.generateAdminToken(adminId);
			Map<String, Object> data = new HashMap<>();
			data.put("token", token);
			data.put("nickname", admin.getNickname());
			data.put("lastLogin", now.format(FMT));
			log.info("管理员登录成功：adminId={}", adminId);
			return Result.ok("登录成功", data);
		} catch (Exception e) {
			log.error("管理员登录异常：{}", e.getMessage(), e);
			return Result.fail("登录失败，请稍后重试");
		}
	}

	@Override
	public Result getProfile() {
		try {
			String adminId = UserContext.getUserId();
			log.info("获取管理员个人信息：adminId={}", adminId);
			if (!StringUtils.hasText(adminId)) {
				return Result.fail(401, "管理员未登录");
			}
			TAdmin admin = this.getOne(new LambdaQueryWrapper<TAdmin>().eq(TAdmin::getAdminId, adminId).last("limit 1"), false);
			if (admin == null) {
				return Result.fail(404, "管理员信息不存在");
			}
			Map<String, Object> data = new HashMap<>();
			data.put("adminId", admin.getAdminId());
			data.put("nickname", admin.getNickname());
			data.put("lastLogin", admin.getLastLogin() == null ? null : admin.getLastLogin().format(FMT));
			return Result.ok("请求成功", data);
		} catch (Exception e) {
			log.error("获取管理员个人信息失败：{}", e.getMessage(), e);
			return Result.fail("获取管理员个人信息失败，请稍后重试");
		}
	}

	@Override
	public Result logout(String authorization) {
		try {
			String token = extractBearerToken(authorization);
			if (!StringUtils.hasText(token)) {
				return Result.fail(400, "Token 不能为空");
			}
			String tokenValue = tokenUtil.validateToken(token);
			if (!StringUtils.hasText(tokenValue) || !tokenValue.startsWith("ADMIN:")) {
				return Result.fail(401, "Token 无效或已过期");
			}
			tokenUtil.invalidateToken(token);
			log.info("管理员退出登录：adminId={}", tokenValue.substring(6));
			return Result.ok("已安全退出登录", null);
		} catch (Exception e) {
			log.error("管理员退出登录失败：{}", e.getMessage(), e);
			return Result.fail("退出登录失败，请稍后重试");
		}
	}

	private String extractBearerToken(String authorization) {
		if (!StringUtils.hasText(authorization)) {
			return null;
		}
		String trimmed = authorization.trim();
		return trimmed.startsWith("Bearer ") ? trimmed.substring(7) : trimmed;
	}
}

