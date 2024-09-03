package com.atguigu.daijia.order.service;

import com.atguigu.daijia.model.entity.order.OrderMonitor;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderMonitorService extends IService<OrderMonitor> {
    Long saveOrderMonitor(OrderMonitor orderMonitor);

    OrderMonitor getOrderMonitor(Long orderId);

    Boolean updateOrderMonitor(OrderMonitor orderMonitor);

    Boolean saveOrderMonitorRecord(OrderMonitorRecord orderMonitorRecord);
}
