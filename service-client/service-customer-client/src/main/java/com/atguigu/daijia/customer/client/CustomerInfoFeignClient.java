package com.atguigu.daijia.customer.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-customer")
public interface CustomerInfoFeignClient {

    /**
     * 处理用户登录请求。
     *
     * 通过用户的登录凭证（code）来换取用户的登录信息，并返回用户的ID。
     * 此接口适用于移动端或Web端的用户登录场景，其中code由前端页面传递而来，
     * 通常是经过一定加密或认证流程后得到的临时凭证。
     *
     * @param code 用户登录凭证，用于识别用户身份。
     * @return Result<Long> 包含操作结果和用户ID的响应体。成功时，resultData为用户的ID；失败时，resultData为null。
     */
    @GetMapping("/customer/info/login/{code}")
    public Result<Long> login(@PathVariable String code);
    /**
     * 获取客户登录信息
     * @param customerId
     * @return
     */
    @GetMapping("/customer/info/getCustomerLoginInfo/{customerId}")
    Result<CustomerLoginVo> getCustomerLoginInfo(@PathVariable("customerId") Long customerId);
    /**
     * 更新客户微信手机号码
     * @param updateWxPhoneForm
     * @return
     */
    @PostMapping("/customer/info/updateWxPhoneNumber")
    Result<Boolean> updateWxPhoneNumber(@RequestBody UpdateWxPhoneForm updateWxPhoneForm);
    /**
     * 获取客户OpenId
     * @param customerId
     * @return
     */
    @GetMapping("/customer/info/getCustomerOpenId/{customerId}")
    Result<String> getCustomerOpenId(@PathVariable("customerId") Long customerId);
}