package com.atguigu.daijia.customer.service;

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

public interface OrderService {

    Long submitOrder(SubmitOrderForm submitOrderForm);

    Integer getOrderStatus(Long orderId);

    ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm);

    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);

    OrderInfoVo getOrderInfo(Long orderId, Long customerId);

    DriverInfoVo getDriverInfo(Long orderId, Long customerId);

    OrderLocationVo getCacheOrderLocation(Long orderId);

    OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId);

    WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm);

    Boolean queryPayStatus(String orderNo);
}
