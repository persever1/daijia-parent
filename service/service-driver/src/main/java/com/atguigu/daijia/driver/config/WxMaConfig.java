package com.atguigu.daijia.driver.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @FileName WxMaConfig
 * @Description
 * @Author mark
 * @date 2024-07-30
 **/

@Component
public class WxMaConfig {

    @Autowired
    private WxMaProperties wxMaProperties;

    /**
     * 创建并配置微信小程序服务
     *
     * @return 配置好的微信小程序服务实例
     */
    @Bean
    public WxMaService wxMaService() {
        // 初始化微信小程序配置
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        // 设置小程序的AppID
        config.setAppid(wxMaProperties.getAppId());
        // 设置小程序的密钥
        config.setSecret(wxMaProperties.getSecret());

        // 创建微信小程序服务实例
        WxMaService service = new WxMaServiceImpl();
        // 为服务实例设置配置
        service.setWxMaConfig(config);
        return service;
    }

}