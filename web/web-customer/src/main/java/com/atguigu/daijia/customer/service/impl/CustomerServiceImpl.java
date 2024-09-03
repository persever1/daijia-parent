package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerServiceImpl implements CustomerService {

    //注入远程调用接口
    @Autowired
    private CustomerInfoFeignClient client;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 用户登录方法
     * 通过接收一个登录码（code），完成用户身份验证并返回登录令牌（token）
     *
     * @param code 用户登录时获取的验证码，用于标识用户
     * @return 返回生成的登录令牌（token），用于后续请求的身份验证
     * @throws GuiguException 如果登录失败或无法识别用户，则抛出异常
     */
    @Override
    public String login(String code) throws GuiguException {
        // 调用远程服务进行登录验证，并获取登录结果
        //1 拿着code进行远程调用，返回用户id
        Result<Long> loginResult = client.login(code);

        // 检查登录结果是否成功，如果不成功则抛出异常
        //2 判断如果返回失败了，返回错误提示
        Integer codeResult = loginResult.getCode();
        if (codeResult != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 从登录结果中提取用户ID
        //3 获取远程调用返回用户id
        Long customerId = loginResult.getData();

        // 检查用户ID是否为空，如果为空则抛出异常
        //4 判断返回用户id是否为空，如果为空，返回错误提示
        if (customerId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 生成唯一的登录令牌
        //5 生成token字符串
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        // 将用户ID与令牌绑定，并设置过期时间，存储到Redis中
        //6 把用户id放到Redis，设置过期时间
        // key:token  value:customerId
        //redisTemplate.opsForValue().set(token,customerId.toString(),30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                customerId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);

        // 返回生成的登录令牌
        //7 返回token
        return token;
    }

    /**
     * 根据客户ID获取客户登录信息
     * 此方法通过调用远程服务获取客户登录信息，并处理可能的异常情况
     *
     * @param customerId 客户ID，用于标识特定的客户
     * @return 返回CustomerLoginVo对象，包含客户的登录信息
     * @throws GuiguException 当服务调用失败或返回的数据显示错误时，抛出自定义异常
     */
    @Override
    public CustomerLoginVo getCustomerLoginInfo(Long customerId) {
        // 调用远程服务获取客户登录信息
        Result<CustomerLoginVo> result = client.getCustomerLoginInfo(customerId);

        // 检查服务调用结果，如果返回码不是200，则抛出异常
        if (result.getCode().intValue() != 200) {
            throw new GuiguException(result.getCode(), result.getMessage());
        }

        // 获取服务返回的客户登录信息
        CustomerLoginVo customerLoginVo = result.getData();

        // 如果客户登录信息为空，则抛出异常
        if (null == customerLoginVo) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 返回客户登录信息
        return customerLoginVo;
    }

    /**
     * 更新微信手机号码
     *
     * @param updateWxPhoneForm 包含微信用户信息的对象，用于更新手机号码
     * @return 返回更新操作的结果，总是返回true，表示更新操作已完成
     */
    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        client.updateWxPhoneNumber(updateWxPhoneForm);
        return true;
    }
//    @Override
//    public String login(String code) {
//        Long customerId = client.login(code).getData();
//
//        String token = UUID.randomUUID().toString().replaceAll("-", "");
//        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX+token, customerId.toString(), RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
//        return token;
//    }
//
//    @Override
//    public CustomerLoginVo getCustomerLoginInfo(Long customerId) {
//        return client.getCustomerLoginInfo(customerId).getData();
//    }
}
