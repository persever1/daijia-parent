package com.atguigu.daijia.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * 全局Filter，统一处理会员登录与外部不允许访问的服务
 * </p>
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {


    /**
     * 过滤器的执行逻辑。
     * 该方法重写了GatewayFilter的filter方法，旨在处理服务器web交换所涉及的过滤逻辑。
     * 在此实现中，它直接调用GatewayFilterChain的filter方法，将控制权传递给下一个过滤器或终端处理程序。
     * 这种实现可能是为了实现某些特定的行为，比如监控、日志记录等，而不改变原有的请求处理流程。
     *
     * @param exchange 表示当前的服务器Web交换信息，包含请求和响应等细节。
     * @param chain    表示过滤器链，用于继续或结束过滤器链的执行。
     * @return 返回一个Mono<Void>对象，表示异步处理结果，此处表示过滤操作完成后不返回任何内容。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange);
    }

    /**
     * 获取当前对象的顺序。
     * <p>
     * 本方法重写了父类中的getOrder方法，返回值为0，表示当前对象的执行顺序。
     * 在使用注解处理器时，顺序通常很重要，因为它决定了处理程序的执行序列。
     * 返回0意味着这个处理程序应该在所有其他处理程序之前执行。
     *
     * @return 返回0，表示当前对象的执行顺序。
     */
    @Override
    public int getOrder() {
        return 0;
    }

}
