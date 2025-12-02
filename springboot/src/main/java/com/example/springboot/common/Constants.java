package com.example.springboot.common;

/**
 * 全局常量定义类
 * 用于存放全局可复用的系统级常量。
 */
public class Constants {

    /**
     * 患者最大允许爽约次数，超过即自动加入黑名单
     */
    public static final int MAX_NO_SHOW_COUNT = 3;

    /**
     * 默认分页大小（如果系统里有分页）
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Token 过期时间（举例）
     */
    public static final long TOKEN_EXPIRATION_HOURS = 24;

    /**
     * 默认系统时区
     */
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
}
