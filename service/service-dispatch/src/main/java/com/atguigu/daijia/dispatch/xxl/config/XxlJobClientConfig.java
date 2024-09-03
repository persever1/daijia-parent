package com.atguigu.daijia.dispatch.xxl.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
/**
 * @FileName XxlJobClientConfig
 * @Description
 * @Author mark
 * @date 2024-08-13
 **/


/**
 * XxlJob客户端配置类
 * 该类用于配置XxlJob客户端的相关属性，包括与服务器交互的API地址等
 */
@Data
@Component
@ConfigurationProperties(prefix = "xxl.job.client")
public class XxlJobClientConfig {

    /**
     * 任务组ID：用于将任务归属于特定的组
     */
    private Integer jobGroupId;

    /**
     * 添加任务的API地址
     */
    private String addUrl;

    /**
     * 删除任务的API地址
     */
    private String removeUrl;

    /**
     * 启动任务的API地址
     */
    private String startJobUrl;

    /**
     * 停止任务的API地址
     */
    private String stopJobUrl;

    /**
     * 添加并启动任务的API地址
     */
    private String addAndStartUrl;
}

