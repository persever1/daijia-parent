package com.atguigu.daijia.common.config.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * Redis配置类
 *
 */
@Configuration
@EnableCaching
public class RedisConfig {

    // 定义一个自定义的KeyGenerator bean，用于生成缓存键
    // 使用默认标签做缓存
    @Bean
    public KeyGenerator keyGenerator() {
        return new KeyGenerator() {
            /**
             * 生成缓存键。
             *
             * @param target 调用方法的对象
             * @param method 被调用的方法
             * @param params 方法的参数
             * @return 生成的缓存键字符串
             */
            @Override
            public Object generate(Object target, Method method, Object... params) {
                // 初始化一个StringBuilder用于构建缓存键
                StringBuilder sb = new StringBuilder();
                // 添加目标对象的类名到缓存键
                sb.append(target.getClass().getName());
                // 添加方法名到缓存键
                sb.append(method.getName());
                // 遍历方法参数，并将参数的字符串表示添加到缓存键
                for (Object obj : params) {
                    sb.append(obj.toString());
                }
                // 返回构建完成的缓存键字符串
                return sb.toString();
            }
        };
    }


    /**
     * 创建RedisTemplate Bean，用于操作Redis数据库。
     * 该方法配置了Redis模板的序列化和反序列化方式，以及与Redis连接工厂的绑定。
     *
     * @param redisConnectionFactory Redis连接工厂，用于创建Redis连接。
     * @return RedisTemplate实例，配置了键值的序列化方式。
     */
    @Bean
    @Primary
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 使用StringRedisSerializer对键进行序列化。
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // 使用GenericJackson2JsonRedisSerializer对值进行序列化，支持JSON格式。
        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();

        // 配置键、值、哈希键、哈希值的序列化方式。
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(genericJackson2JsonRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(genericJackson2JsonRedisSerializer);

        // 初始化RedisTemplate，配置已完成。
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    /**
     * 配置Redis缓存管理器。
     * 使用Redis作为缓存后端，并配置序列化方式为JSON，以便支持多种类型的缓存对象。
     * 这里设置了缓存的默认过期时间以及序列化工具，为整个应用的缓存策略提供统一配置。
     *
     * @param factory RedisConnectionFactory实例，用于构建Redis缓存管理器。
     * @return 返回配置好的RedisCacheManager实例。
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // 配置字符串序列化器，用于序列化和反序列化Redis的键。
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        // 配置JSON序列化器，用于序列化和反序列化Redis的值。
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);

        // 配置JSON序列化器的行为，使其能够序列化任何属性和类型。
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // 配置缓存的默认设置，包括过期时间和序列化方式。
        // 配置序列化（解决乱码的问题）,过期时间600秒
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(600000)) // 设置缓存条目的默认过期时间。
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer)) // 配置键的序列化方式。
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer)) // 配置值的序列化方式。
                .disableCachingNullValues(); // 禁止缓存null值。

        // 基于给定的配置构建RedisCacheManager实例。
        RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
                .cacheDefaults(config) // 设置默认的缓存配置。
                .build();
        return cacheManager;
    }

}
