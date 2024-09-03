package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "位置API接口管理")
@RestController
@RequestMapping(value="/location")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationController {
    @Autowired
    private LocationService locationService;

    // 开启接单服务：更新司机经纬度位置
    // 该接口需要用户登录鉴权，更新司机的经纬度信息
    @Operation(summary = "开启接单服务：更新司机经纬度位置")
    @GuiguLogin
    @PostMapping("/updateDriverLocation")
    public Result<Boolean> updateDriverLocation(@RequestBody UpdateDriverLocationForm updateDriverLocationForm) {
        // 从安全上下文中获取当前用户的ID，用于确定司机身份
        Long driverId = AuthContextHolder.getUserId();
        // 将获取到的司机ID设置到表单中，以确保更新位置信息的司机是正确的
        updateDriverLocationForm.setDriverId(driverId);
        // 调用位置服务，更新司机的经纬度位置，并将操作结果封装到Result对象中返回
        return Result.ok(locationService.updateDriverLocation(updateDriverLocationForm));
    }
    @Operation(summary = "司机赶往代驾起始点：更新订单位置到Redis缓存")
    @GuiguLogin
    @PostMapping("/updateOrderLocationToCache")
    public Result updateOrderLocationToCache(@RequestBody UpdateOrderLocationForm updateOrderLocationForm) {
        return Result.ok(locationService.updateOrderLocationToCache(updateOrderLocationForm));
    }
    @Operation(summary = "开始代驾服务：保存代驾服务订单位置")
    @PostMapping("/saveOrderServiceLocation")
    public Result<Boolean> saveOrderServiceLocation(@RequestBody List<OrderServiceLocationForm> orderLocationServiceFormList) {
        return Result.ok(locationService.saveOrderServiceLocation(orderLocationServiceFormList));
    }

}

