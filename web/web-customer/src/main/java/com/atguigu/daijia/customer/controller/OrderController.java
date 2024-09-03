package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.payment.CreateWxPaymentForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {
    @Autowired
    private OrderService orderService;


    /**
     * 预估订单数据接口
     * 该接口用于根据提供的预估订单表单数据，预估生成订单信息
     * 使用了GuiguLogin注解，表示需要用户登录后才能进行操作
     *
     * @param expectOrderForm 预估订单表单数据，包含了预估订单所需的信息
     * @return 返回一个Result对象，其中包含预估订单的详细信息
     */
    @Operation(summary = "预估订单数据")
    @GuiguLogin
    @PostMapping("/expectOrder")
    public Result<ExpectOrderVo> expectOrder(@RequestBody ExpectOrderForm expectOrderForm) {
        return Result.ok(orderService.expectOrder(expectOrderForm));
    }

    // 定义乘客下单操作的API接口
    @Operation(summary = "乘客下单")
    // 限制只有登录用户才能访问此接口
    @GuiguLogin
    // 指定HTTP请求方法为POST，用于提交订单数据
    @PostMapping("/submitOrder")
    public Result<Long> submitOrder(@RequestBody SubmitOrderForm submitOrderForm) {
        // 从上下文中获取当前登录用户的ID，并设置到订单表单中
        submitOrderForm.setCustomerId(AuthContextHolder.getUserId());
        // 调用订单服务的提交订单方法，处理订单提交逻辑，并返回结果
        return Result.ok(orderService.submitOrder(submitOrderForm));
    }
    /**
     * 查询订单状态
     * 通过GET请求方式，根据订单ID查询订单的状态
     * 使用了GuiguLogin注解，表示此接口需要用户登录后才能访问
     *
     * @param orderId 订单ID，通过路径变量传入
     * @return 返回一个Result对象，包含订单状态的整数类型数据
     */
    @Operation(summary = "查询订单状态")
    @GuiguLogin
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderStatus(orderId));
    }
    @Operation(summary = "乘客端查找当前订单")
    @GuiguLogin
    @GetMapping("/searchCustomerCurrentOrder")
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder() {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.searchCustomerCurrentOrder(customerId));
    }
    @Operation(summary = "获取订单信息")
    @GuiguLogin
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getOrderInfo(orderId, customerId));
    }
    @Operation(summary = "根据订单id获取司机基本信息")
    @GuiguLogin
    @GetMapping("/getDriverInfo/{orderId}")
    public Result<DriverInfoVo> getDriverInfo(@PathVariable Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getDriverInfo(orderId, customerId));
    }
    @Operation(summary = "司机赶往代驾起始点：获取订单经纬度位置")
    @GuiguLogin
    @GetMapping("/getCacheOrderLocation/{orderId}")
    public Result<OrderLocationVo> getOrderLocation(@PathVariable Long orderId) {
        return Result.ok(orderService.getCacheOrderLocation(orderId));
    }
    @Operation(summary = "代驾服务：获取订单服务最后一个位置信息")
    @GuiguLogin
    @GetMapping("/getOrderServiceLastLocation/{orderId}")
    public Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderServiceLastLocation(orderId));
    }
    @Operation(summary = "创建微信支付")
    @GuiguLogin
    @PostMapping("/createWxPayment")
    public Result<WxPrepayVo> createWxPayment(@RequestBody CreateWxPaymentForm createWxPaymentForm) {
        Long customerId = AuthContextHolder.getUserId();
        createWxPaymentForm.setCustomerId(customerId);
        return Result.ok(orderService.createWxPayment(createWxPaymentForm));
    }
    @Operation(summary = "支付状态查询")
    @GuiguLogin
    @GetMapping("/queryPayStatus/{orderNo}")
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo) {
        return Result.ok(orderService.queryPayStatus(orderNo));
    }
}

