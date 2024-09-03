package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerController {

    @Autowired
    private CustomerService customerInfoService;

    /**
     * 小程序授权登录接口
     * 通过微信小程序登录授权流程获取用户信息并完成登录
     *
     * @param code 小程序登录授权后返回的code，用于换取用户信息
     * @return 登录结果，包含用户信息或错误信息
     */
    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> wxLogin(@PathVariable String code) {
        return Result.ok(customerInfoService.login(code));
    }
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取客户登录信息
     * 通过token从Redis中获取客户ID，并根据客户ID查询详细的登录信息
     *
     * @param token 客户的登录令牌
     * @return 客户的登录信息封装在CustomerLoginVo对象中
     */
    @Operation(summary = "获取客户登录信息")
    @GetMapping("/getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo(@RequestHeader(value="token") String token) {
        // 从Redis中获取与token对应的客户ID
        String customerId = (String)redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX+token);
        // 根据客户ID获取详细的登录信息，并封装在Result对象中返回
        return Result.ok(customerInfoService.getCustomerLoginInfo(Long.parseLong(customerId)));
    }
    // 企业版可以获取手机号码
//    @Operation(summary = "更新用户微信手机号")
//    @GuiguLogin
//    @PostMapping("/updateWxPhone")
//    public Result updateWxPhone(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
//        updateWxPhoneForm.setCustomerId(AuthContextHolder.getUserId());
//        return Result.ok(customerInfoService.updateWxPhoneNumber(updateWxPhoneForm));
//    }
    //个人版无法获取手机号
    @Operation(summary = "更新用户微信手机号")
    @GuiguLogin
    @PostMapping("/updateWxPhone")
    public Result updateWxPhone(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
        updateWxPhoneForm.setCustomerId(AuthContextHolder.getUserId());
        //customerInfoService.updateWxPhoneNumber(updateWxPhoneForm);
        return Result.ok(true);
    }
}

