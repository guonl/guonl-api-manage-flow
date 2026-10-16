package com.guonl.config;

import com.guonl.common.Constants;
import com.guonl.dao.SysConfigDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动初始化：sys_config 中无 auth.password 时写入 BCrypt(默认密码 admin123)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigInitRunner implements ApplicationRunner {

    private final SysConfigDao sysConfigDao;

    @Override
    public void run(ApplicationArguments args) {
        String password = sysConfigDao.selectByKey(Constants.CFG_PASSWORD) == null
                ? null : sysConfigDao.selectByKey(Constants.CFG_PASSWORD).getConfigValue();
        if (password == null || password.isBlank()) {
            String encoded = new BCryptPasswordEncoder().encode(Constants.DEFAULT_PASSWORD);
            sysConfigDao.upsert(Constants.CFG_PASSWORD, encoded, "登录密码（BCrypt）");
            log.info("已初始化默认登录密码 admin/{}，请尽快修改", Constants.DEFAULT_PASSWORD);
        }
    }
}
