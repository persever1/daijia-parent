package com.atguigu.daijia.rules.config;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @FileName DroolsConfig
 * @Description
 * @Author mark
 * @date 2024-08-01
 **/
@Slf4j
@Configuration
public class DroolsConfig {
    // 制定规则文件的路径
    private static final String RULES_CUSTOMER_RULES_DRL = "rules/FeeRule.drl";

    /**
     * 配置KieContainer Bean
     * KieContainer是KieServices的实例，用于加载和管理KieModule
     * 本方法通过KieServices创建一个新的KieFileSystem，将规则文件写入该文件系统，
     * 然后构建KieModule，并返回对应的KieContainer实例
     *
     * @return KieContainer实例，用于规则引擎的运行和管理
     */
    @Bean
    public KieContainer kieContainer() {
        // 获取KieServices的实例
        KieServices kieServices = KieServices.Factory.get();

        // 创建一个新的KieFileSystem实例
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        // 将规则文件写入KieFileSystem
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_CUSTOMER_RULES_DRL));
        // 创建KieBuilder实例并指定KieFileSystem
        KieBuilder kb = kieServices.newKieBuilder(kieFileSystem);
        // 构建所有的KieModule
        kb.buildAll();

        // 获取构建的KieModule
        KieModule kieModule = kb.getKieModule();
        // 根据KieModule创建KieContainer实例
        KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
        // 返回KieContainer实例
        return kieContainer;
    }

}
