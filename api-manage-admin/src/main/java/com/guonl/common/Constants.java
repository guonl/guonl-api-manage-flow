package com.guonl.common;

/**
 * 系统常量
 */
public final class Constants {

    private Constants() {
    }

    /** Session 中的登录用户 key */
    public static final String SESSION_USER = "ACCESS_USER";

    /** sys_config key：登录账号 */
    public static final String CFG_USERNAME = "auth.username";
    /** sys_config key：登录密码（BCrypt） */
    public static final String CFG_PASSWORD = "auth.password";
    /** 默认登录密码（首次启动写入 sys_config） */
    public static final String DEFAULT_PASSWORD = "admin123";

    /** sys_config key：AI 配置 */
    public static final String CFG_AI_BASE_URL = "ai.base-url";
    public static final String CFG_AI_API_KEY = "ai.api-key";
    public static final String CFG_AI_MODEL = "ai.model";
    public static final String CFG_AI_TEMPERATURE = "ai.temperature";

    /** 接口来源 */
    public static final int SOURCE_SYNC = 1;
    public static final int SOURCE_MANUAL = 2;
    public static final int SOURCE_DEBUG_SAVE = 3;

    /** 接口状态 */
    public static final int URL_STATUS_ENABLED = 1;
    public static final int URL_STATUS_DISABLED = 0;
    public static final int URL_STATUS_OFFLINE = 2;
}
