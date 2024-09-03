package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.atguigu.daijia.order.service.OrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value="/driver/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoController {

    @Autowired
    private DriverInfoService driverInfoService;

    /**
     * 小程序授权登录接口。
     * 通过接收小程序端传递的授权码，实现用户的登录认证。
     *
     * @param code 小程序授权码，用于换取用户登录信息。
     * @return 返回登录结果，包含用户ID等信息。
     */
    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<Long> login(@PathVariable String code) {
        // 调用驱动信息服务的登录方法，传入小程序授权码，返回登录结果
        return Result.ok(driverInfoService.login(code));
    }
    /**
     * 通过司机ID获取司机登录信息。
     * <p>
     * 本接口提供根据司机ID获取司机登录详情的功能。这对于需要验证司机身份或展示司机登录状态的场景非常有用。
     *
     * @param driverId 司机的唯一标识ID。路径参数，必须提供，用于精确识别要查询的司机。
     * @return Result<DriverLoginVo> 包含司机登录信息的结果对象。如果操作成功，DriverLoginVo包含司机的登录详情；
     *         如果操作失败，Result对象的错误码和错误信息将提供进一步的细节。
     */
    @Operation(summary = "获取司机登录信息")
    @GetMapping("/getDriverLoginInfo/{driverId}")
    public Result<DriverLoginVo> getDriverLoginInfo(@PathVariable Long driverId) {
        // 调用driverInfoService的服务方法获取司机的登录信息，并将结果封装在Result对象中返回
        return Result.ok(driverInfoService.getDriverLoginInfo(driverId));
    }
    @Operation(summary = "获取司机认证信息")
    @GetMapping("/getDriverAuthInfo/{driverId}")
    Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable("driverId") Long driverId) {
        return Result.ok(driverInfoService.getDriverAuthInfo(driverId));
    }
    @Operation(summary = "更新司机认证信息")
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> UpdateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        return Result.ok(driverInfoService.updateDriverAuthInfo(updateDriverAuthInfoForm));
    }
    @Operation(summary = "创建司机人脸模型")
    @PostMapping("/creatDriverFaceModel")
    public Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        return Result.ok(driverInfoService.creatDriverFaceModel(driverFaceModelForm));
    }
    @Operation(summary = "获取司机设置信息")
    @GetMapping("/getDriverSet/{driverId}")
    public Result<DriverSet> getDriverSet(@PathVariable Long driverId) {
        return Result.ok(driverInfoService.getDriverSet(driverId));
    }
    /**
     * 判断司机当日是否进行过人脸识别
     *
     * @param driverId 司机ID
     * @return 包含布尔值的结果对象，指示司机当日是否进行过人脸识别
     */
    @Operation(summary = "判断司机当日是否进行过人脸识别")
    @GetMapping("/isFaceRecognition/{driverId}")
    Result<Boolean> isFaceRecognition(@PathVariable("driverId") Long driverId) {
        return Result.ok(driverInfoService.isFaceRecognition(driverId));
    }
    @Operation(summary = "验证司机人脸")
    @PostMapping("/verifyDriverFace")
    public Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        return Result.ok(driverInfoService.verifyDriverFace(driverFaceModelForm));
    }
    @Operation(summary = "更新接单状态")
    @GetMapping("/updateServiceStatus/{driverId}/{status}")
    public Result<Boolean> updateServiceStatus(@PathVariable Long driverId, @PathVariable Integer status) {
        return Result.ok(driverInfoService.updateServiceStatus(driverId, status));
    }
    @Autowired
    private OrderInfoService orderInfoService;
    @Operation(summary = "司机抢单")
    @GetMapping("/robNewOrder/{driverId}/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable Long driverId, @PathVariable Long orderId) {
        return Result.ok(orderInfoService.robNewOrder(driverId, orderId));
    }
    @Operation(summary = "获取司机基本信息")
    @GetMapping("/getDriverInfo/{driverId}")
    public Result<DriverInfoVo> getDriverInfo(@PathVariable Long driverId) {
        return Result.ok(driverInfoService.getDriverInfo(driverId));
    }
    @Operation(summary = "获取司机OpenId")
    @GetMapping("/getDriverOpenId/{driverId}")
    public Result<String> getDriverOpenId(@PathVariable Long driverId) {
        return Result.ok(driverInfoService.getDriverOpenId(driverId));
    }
}

