package com.guonl.controller;

import com.guonl.common.ApiResult;
import com.guonl.common.BizException;
import com.guonl.common.Constants;
import com.guonl.dao.SysConfigDao;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 登录认证
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysConfigDao sysConfigDao;

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class PasswordRequest {
        private String oldPassword;
        private String newPassword;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody LoginRequest req, HttpSession session) {
        if (req.getUsername() == null || req.getPassword() == null) {
            throw new BizException(4001, "用户名或密码不能为空");
        }
        String cfgUser = configValue(Constants.CFG_USERNAME);
        String cfgPwd = configValue(Constants.CFG_PASSWORD);
        if (!req.getUsername().equals(cfgUser)) {
            throw new BizException(4001, "用户名或密码错误");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // 兼容首启未初始化场景：密码为空时仅允许默认密码
        boolean match = (cfgPwd == null || cfgPwd.isBlank())
                ? Constants.DEFAULT_PASSWORD.equals(req.getPassword())
                : encoder.matches(req.getPassword(), cfgPwd);
        if (!match) {
            throw new BizException(4001, "用户名或密码错误");
        }
        session.setAttribute(Constants.SESSION_USER, req.getUsername());
        return ApiResult.ok(Map.of("username", req.getUsername()));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResult.ok();
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me(HttpSession session) {
        Object user = session.getAttribute(Constants.SESSION_USER);
        if (user == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return ApiResult.ok(Map.of("username", user));
    }

    @PutMapping("/password")
    public ApiResult<Void> changePassword(@RequestBody PasswordRequest req, HttpSession session) {
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            throw new BizException(400, "新密码长度不能少于6位");
        }
        String cfgPwd = configValue(Constants.CFG_PASSWORD);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (cfgPwd != null && !cfgPwd.isBlank() && !encoder.matches(req.getOldPassword(), cfgPwd)) {
            throw new BizException(400, "原密码错误");
        }
        sysConfigDao.upsert(Constants.CFG_PASSWORD, encoder.encode(req.getNewPassword()), "登录密码（BCrypt）");
        session.invalidate();
        return ApiResult.ok();
    }

    private String configValue(String key) {
        var cfg = sysConfigDao.selectByKey(key);
        return cfg == null ? null : cfg.getConfigValue();
    }
}
