package com.atguigu.daijia.driver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @FileName WxMaProperties
 * @Description
 * @Author mark
 * @date 2024-07-30
 **/


/**
 * 微信小程序配置属性类。
 * 使用@ConfigurationProperties注解将YAML或Properties配置文件中的键值对映射到这个类的属性上。
 * 前缀"wx.miniapp"指定配置文件中与此类对应的配置段的前缀。
 * 使用@Component注解将这个类标记为Spring Bean，使其可以在Spring应用上下文中被管理和注入。
 */
@Data
@Component
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxMaProperties {

    /**
     * 小程序的AppID，用于标识小程序的身份。
     */
    private String appId;

    /**
     * 小程序的AppSecret，用于验证小程序的身份和获取访问权限。
     */
    private String secret;
}

