package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {
    @Autowired
    private LocationFeignClient locationFeignClient;


    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    /**
     * 更新司机位置信息
     * 只有在司机处于接单状态时，才允许更新其位置
     *
     * @param updateDriverLocationForm 包含司机ID和新位置信息的表单
     * @return 更新操作是否成功的布尔值
     * @throws GuiguException 如果司机未开启接单状态，抛出异常
     */
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        // 获取司机的设置信息，以检查其是否处于接单状态
        DriverSet driverSet = driverInfoFeignClient.getDriverSet(updateDriverLocationForm.getDriverId()).getData();
        // 检查司机是否已开启接单
        if(driverSet.getServiceStatus().intValue() == 1) {
            // 如果司机处于接单状态，调用位置服务更新司机位置
            return locationFeignClient.updateDriverLocation(updateDriverLocationForm).getData();
        } else {
            // 如果司机未开启接单状态，抛出异常
            throw new GuiguException(ResultCodeEnum.NO_START_SERVICE);
        }
    }

    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        return locationFeignClient.updateOrderLocationToCache(updateOrderLocationForm).getData();
    }

    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {
        return locationFeignClient.saveOrderServiceLocation(orderLocationServiceFormList).getData();
    }

}
