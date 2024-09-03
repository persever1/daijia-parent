package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant; // 导入Redis常量
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient; // 导入司机信息Feign客户端
import com.atguigu.daijia.driver.service.DriverService; // 导入司机服务接口
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm; // 导入司机人脸模型表单
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm; // 导入更新司机认证信息表单
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo; // 导入司机认证信息视图对象
import com.atguigu.daijia.model.vo.driver.DriverLoginVo; // 导入司机登录视图对象
import lombok.SneakyThrows; // 允许忽略检查异常
import lombok.extern.slf4j.Slf4j; // 使用Slf4j日志框架
import org.springframework.beans.factory.annotation.Autowired; // 自动注入依赖
import org.springframework.data.redis.core.RedisTemplate; // Redis模板
import org.springframework.stereotype.Service; // 定义服务类

import java.util.concurrent.TimeUnit; // 导入时间单位类

import java.util.UUID; // 导入通用唯一识别码工具


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {


    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 使用代码进行登录。
     *
     * 本方法实现了通过给定的登录代码来获取驾驶员ID，并以此生成一个登录令牌（token）。这个登录令牌是唯一的，
     * 并且会被存储在Redis中，以供后续验证驾驶员的登录状态使用。
     *
     * @param code 登录代码，用于识别驾驶员身份。
     * @return 生成的登录令牌。
     */
    @SneakyThrows
    @Override
    public String login(String code) {
        // 通过调用远程服务driverInfoFeignClient，使用登录代码获取驾驶员ID。
        // 获取openId
        Long driverId = driverInfoFeignClient.login(code).getData();

        // 生成一个唯一的token，并移除其中的连字符，以确保token的格式一致性。
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        // 将token与驾驶员ID的关联存储在Redis中，设置过期时间。
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token, driverId.toString(), RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);

        // 返回生成的token。
        return token;
    }


    /**
     * 根据司机ID获取司机登录信息。
     *
     * 通过调用driverInfoFeignClient的getDriverLoginInfo方法，传递司机ID来获取司机的登录信息。
     * 此方法主要用于在登录过程中获取司机的相关信息，以便进行登录验证和权限校验。
     *
     * @param driverId 司机的唯一标识ID，用于定位和获取特定司机的信息。
     * @return DriverLoginVo 对象，包含司机的登录信息。如果无法获取到信息，则可能返回null。
     */
    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        // 调用driverInfoFeignClient的getDriverLoginInfo方法获取司机登录信息，并返回其中的数据部分
        return driverInfoFeignClient.getDriverLoginInfo(driverId).getData();
    }
    /**
     * 根据司机ID获取司机认证信息
     * 该方法通过调用远程服务获取司机的认证信息
     * 主要用于在需要展示或进一步处理司机认证信息时获取数据
     *
     * @param driverId 司机ID，用于标识特定的司机
     * @return 返回DriverAuthInfoVo类型的对象，包含司机的认证信息
     */
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        return driverInfoFeignClient.getDriverAuthInfo(driverId).getData();
    }
    /**
     * 更新司机认证信息
     *
     * @param updateDriverAuthInfoForm 包含更新司机认证信息的表单数据
     * @return 返回更新操作的结果，true表示成功，false表示失败
     */
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        // 调用Feign客户端向远程服务发送请求，更新司机认证信息，并返回操作结果
        return driverInfoFeignClient.UpdateDriverAuthInfo(updateDriverAuthInfoForm).getData();
    }
    /**
     * 创建司机人脸模型
     * 通过调用远程服务来创建司机人脸模型，该方法主要负责将本地的数据封装成请求并发送给远程服务处理
     *
     * @param driverFaceModelForm 司机人脸模型表单，包含了创建人脸模型所需的所有信息
     * @return 返回创建结果，true表示成功创建，false表示创建失败
     */
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.creatDriverFaceModel(driverFaceModelForm).getData();
    }

    /**
     * 判断指定的司机是否启用了人脸识别
     * 本方法通过调用远程服务来获取司机的人脸识别状态，是对此服务调用的一个封装
     *
     * @param driverId 司机的ID，用于唯一标识一个司机
     * @return 如果司机启用了人脸识别，返回true；否则返回false
     */
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        // 调用远程服务获取司机的人脸识别状态，并返回该状态
        return driverInfoFeignClient.isFaceRecognition(driverId).getData();
    }

    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.verifyDriverFace(driverFaceModelForm).getData();
    }

    @Autowired
    private LocationFeignClient locationFeignClient;

    @Autowired
    private NewOrderFeignClient newOrderDispatchFeignClient;

    /**
     * 启动服务
     *
     * @param driverId 司机ID
     * @return 布尔值，表示服务是否启动成功
     */
    @Override
    public Boolean startService(Long driverId) {
        // 获取司机登录信息以检查认证状态
        DriverLoginVo driverLoginVo = driverInfoFeignClient.getDriverLoginInfo(driverId).getData();
        // 判断司机认证状态是否正确，否则抛出异常
        if(driverLoginVo.getAuthStatus().intValue() != 2) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }

        // 检查当日是否进行了人脸识别
        Boolean isFaceRecognition = driverInfoFeignClient.isFaceRecognition(driverId).getData();
        // 如果当日未进行人脸识别，抛出异常
        if(!isFaceRecognition) {
            throw new GuiguException(ResultCodeEnum.FACE_ERROR);
        }

        // 更新司机的服务状态为接单状态
        driverInfoFeignClient.updateServiceStatus(driverId, 1);

        // 删除司机的位置信息，以便重新获取
        locationFeignClient.removeDriverLocation(driverId);

        // 清空司机的新订单队列数据，确保订单分配的准确性
        newOrderDispatchFeignClient.clearNewOrderQueueData(driverId);
        return true;
    }
    /**
     * 停止司机服务
     * 该方法用于处理司机停止服务的相关操作，包括更新司机服务状态、删除司机位置信息以及清空新订单队列
     *
     * @param driverId 司机ID，用于标识特定的司机
     * @return 固定返回true，表示服务停止操作完成
     */
    @Override
    public Boolean stopService(Long driverId) {
        //更新司机接单状态，设置为不接单
        driverInfoFeignClient.updateServiceStatus(driverId, 0);

        //删除司机位置信息，以避免下线司机的位置被错误跟踪
        locationFeignClient.removeDriverLocation(driverId);

        //清空司机新订单队列，确保不会有新订单分派给已经下线的司机
        newOrderDispatchFeignClient.clearNewOrderQueueData(driverId);
        return true;
    }


}
