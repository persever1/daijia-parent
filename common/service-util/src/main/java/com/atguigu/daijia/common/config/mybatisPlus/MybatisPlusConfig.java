package com.atguigu.daijia.common.config.mybatisPlus;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MybatisPlus配置类
 *
 */
@EnableTransactionManagement
@Configuration
@MapperScan("com.atguigu.daijia.*.mapper")
public class MybatisPlusConfig {

    /**
     * 配置Mybatis Plus的拦截器，用于支持乐观锁功能。
     *
     * @return MybatisPlusInterceptor 实例，配置了乐观锁拦截器。
     */
    @Bean
    public MybatisPlusInterceptor optimisticLockerInnerInterceptor() {
        // 创建MybatisPlusInterceptor实例
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加PaginationInnerInterceptor到拦截器链中，用于支持分页查询
        // 向Mybatis过滤器链中添加分页拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 返回配置好的拦截器实例
        return interceptor;
    }

}
