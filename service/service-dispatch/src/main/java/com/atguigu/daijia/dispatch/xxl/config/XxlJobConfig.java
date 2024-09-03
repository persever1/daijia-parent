package com.atguigu.daijia.dispatch.xxl.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @FileName XxlJobConfig
 * @Description
 * @Author mark
 * @date 2024-08-13
 **/

@Configuration
public class XxlJobConfig {
    // 定义一个日志对象，使用Spring的通用日志框架SLF4J，专门用于XXL-Job配置类的日志记录
    private Logger logger = LoggerFactory.getLogger(XxlJobConfig.class);

    // 配置XXL-Job的管理端地址，用于与调度中心通信
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    // 配置访问XXL-Job管理端的令牌，用于安全验证
    @Value("${xxl.job.accessToken}")
    private String accessToken;

    // 配置XXL-Job执行器的应用名称，用于在管理端显示和区分不同的执行器
    @Value("${xxl.job.executor.appname}")
    private String appname;

    // 配置XXL-Job执行器的地址，用于管理端与执行器之间的通信
    @Value("${xxl.job.executor.address}")
    private String address;

    // 配置XXL-Job执行器的IP地址，用于确定执行器在网络中的位置
    @Value("${xxl.job.executor.ip}")
    private String ip;

    // 配置XXL-Job执行器的端口号，用于执行器与管理端之间的通信
    @Value("${xxl.job.executor.port}")
    private int port;

    // 配置XXL-Job执行器的日志路径，用于存储执行器运行时的日志文件
    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    // 配置XXL-Job执行器日志文件的保留天数，用于控制日志的清理策略
    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;


    /**
     * 创建并配置 XxlJob 的执行器 Bean.
     *
     * @return 配置好的 XxlJobSpringExecutor 实例
     */
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        // 日志记录：xxl-job 配置初始化
        logger.info(">>>>>>>>>>> xxl-job 配置初始化.");

        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        // 设置作业调度中心的地址
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        // 设置应用名称
        xxlJobSpringExecutor.setAppname(appname);
        // 设置执行器地址
        xxlJobSpringExecutor.setAddress(address);
        // 设置执行器 IP
        xxlJobSpringExecutor.setIp(ip);
        // 设置执行器端口
        xxlJobSpringExecutor.setPort(port);
        // 设置访问令牌
        xxlJobSpringExecutor.setAccessToken(accessToken);
        // 设置日志保存路径
        xxlJobSpringExecutor.setLogPath(logPath);
        // 设置日志保留天数
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        return xxlJobSpringExecutor;
    }


    /**
     * 针对多网卡、容器内部署等情况，可借助 "spring-cloud-commons" 提供的 "InetUtils" 组件灵活定制注册IP；
     *
     *      1、引入依赖：
     *          <dependency>
     *             <groupId>org.springframework.cloud</groupId>
     *             <artifactId>spring-cloud-commons</artifactId>
     *             <version>${version}</version>
     *         </dependency>
     *
     *      2、配置文件，或者容器启动变量
     *          spring.cloud.inetutils.preferred-networks: 'xxx.xxx.xxx.'
     *
     *      3、获取IP
     *          String ip_ = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
     */


}