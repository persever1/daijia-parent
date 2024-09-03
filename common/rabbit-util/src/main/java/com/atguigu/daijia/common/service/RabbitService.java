package com.atguigu.daijia.common.service;


import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息到指定的交换机和路由键
     *
     * @param exchange   交换机名称，用于消息的交换和分发
     * @param routingkey 路由键名称，用于消息的路由，决定消息发送到哪个队列
     * @param message    需要发送的消息对象
     * @return 返回true，表示消息已发送
     */
    public boolean sendMessage(String exchange, String routingkey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingkey, message);
        return true;
    }
}

