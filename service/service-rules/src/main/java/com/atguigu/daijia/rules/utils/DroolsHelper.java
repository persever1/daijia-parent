package com.atguigu.daijia.rules.utils;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

/**
 * @FileName DroolsHelper
 * @Description
 * @Author mark
 * @date 2024-08-13
 **/
public class DroolsHelper {

    /**
     * 根据规则字符串加载规则并返回KieSession对象
     *
     * @param drlStr 规则字符串内容
     * @return 创建的KieSession对象
     * @throws RuntimeException 如果规则编译失败，则抛出运行时异常
     */
    public static KieSession loadForRule(String drlStr) {
        // 获取KieServices实例
        KieServices kieServices = KieServices.Factory.get();

        // 创建一个新的KieFileSystem实例
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        // 将规则字符串写入到文件系统中
        kieFileSystem.write("src/main/resources/rules/" + drlStr.hashCode() + ".drl", drlStr);

        // 将KieFileSystem加入到KieBuilder
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        // 编译此时的builder中所有的规则
        kieBuilder.buildAll();
        // 检查编译结果是否有错误消息
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build Errors:\n" + kieBuilder.getResults().toString());
        }

        // 创建一个新的KieContainer实例
        KieContainer kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
        // 从KieContainer中创建并返回一个新的KieSession
        return kieContainer.newKieSession();
    }

}