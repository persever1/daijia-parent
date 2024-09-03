package com.atguigu.daijia.customer.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @FileName WxConfigOperator
 * @Description
 * @Author mark
 * @date 2024-07-27
 **/
@Component
public class WxConfigOperator {

    @Autowired
    private WxConfigProperties wxConfigProperties;

    /**
     * 创建并配置微信小程序服务实例。
     *
     * 该方法通过读取配置属性文件中的小程序APPID和Secret，初始化WxMaConfig，并将其设置到WxMaService实例中。
     * 这样做的目的是为了提供一个可以用于处理微信小程序相关业务的服务对象，包括但不限于签名、获取access_token等操作。
     *
     * @return WxMaService 微信小程序服务实例，配置了APPID和Secret。
     */
    @Bean
    public WxMaService wxMaService() {
        // 初始化微信小程序配置
        //微信小程序id和秘钥
        WxMaDefaultConfigImpl wxMaConfig = new WxMaDefaultConfigImpl();
        wxMaConfig.setAppid(wxConfigProperties.getAppId());
        wxMaConfig.setSecret(wxConfigProperties.getSecret());

        // 创建并配置微信小程序服务实例
        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(wxMaConfig);
        return service;
    }

}
