package com.atguigu.daijia.driver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    /**
     * 配置线程池组件
     *
     * @return 配置好的 ThreadPoolExecutor 实例
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {

        // 动态获取服务器核数
        int processors = Runtime.getRuntime().availableProcessors();

        // 创建线程池执行器
        // 核心线程数设置为服务器核数+1，对于IO密集型任务，通常设置为2n，CPU密集型任务设置为n+1
        // 最大线程数同样设置为processors+1，意味着线程池可以创建的最大线程数数量
        // 空闲线程存活时间设置为0秒，表示如果线程池中的线程数量超过核心线程数，多余的线程在运行完成后将立即被终止
        // 使用ArrayBlockingQueue作为工作队列，队列大小设置为3，当队列满时，如果线程数小于核心线程数，将创建新线程执行任务
        // 使用默认的线程工厂来创建线程
        // 拒绝策略设置为AbortPolicy，当任务无法提交时抛出异常
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                processors+1,
                processors+1,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(3),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        return threadPoolExecutor;

    }

}
