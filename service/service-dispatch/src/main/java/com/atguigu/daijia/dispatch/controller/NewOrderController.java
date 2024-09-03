package com.atguigu.daijia.dispatch.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "司机新订单接口管理")
@RestController
@RequestMapping("/dispatch/newOrder")
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderController {
    @Autowired
    private NewOrderService newOrderService;

    // 添加并开始新订单任务调度
    // 该接口通过POST请求，接收新订单任务信息（NewOrderTaskVo对象），并返回调度任务的ID
    @PostMapping("/addAndStartTask")
    public Result<Long> addAndStartTask(@RequestBody NewOrderTaskVo newOrderTaskVo) {
        // 调用newOrderService的addAndStartTask方法，传入新订单任务信息，返回调度任务的ID
        return Result.ok(newOrderService.addAndStartTask(newOrderTaskVo));
    }
    // 查询司机新订单数据的接口
    @Operation(summary = "查询司机新订单数据")
    @GetMapping("/findNewOrderQueueData/{driverId}")
    public Result<List<NewOrderDataVo>> findNewOrderQueueData(@PathVariable Long driverId) {
        // 根据司机ID查询新订单数据
        return Result.ok(newOrderService.findNewOrderQueueData(driverId));
    }

    /**
     * 清空指定司机的新订单队列数据
     *
     * @param driverId 司机ID，用于标识需要清空队列数据的司机
     * @return 返回操作结果，包含一个布尔值，表示是否成功清空队列数据
     */
    @Operation(summary = "清空新订单队列数据")
    @GetMapping("/clearNewOrderQueueData/{driverId}")
    public Result<Boolean> clearNewOrderQueueData(@PathVariable Long driverId) {
        return Result.ok(newOrderService.clearNewOrderQueueData(driverId));
    }

}

