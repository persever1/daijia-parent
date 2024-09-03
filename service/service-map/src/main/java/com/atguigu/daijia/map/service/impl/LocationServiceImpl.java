package com.atguigu.daijia.map.service.impl;


import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.util.LocationUtil;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.repository.OrderServiceLocationRepository;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.entity.map.OrderServiceLocation;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 更新司机位置信息
     * 该方法使用Redis的GEO功能来存储和操作地理位置信息
     * 主要用于在乘客下单后，通过Redis GEO功能计算5公里范围内的接单司机
     *
     * @param updateDriverLocationForm 包含司机位置信息的表单，包括经度、纬度和司机ID
     * @return 布尔值，表示位置信息更新是否成功
     */
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        // 创建Point对象，用于存储经纬度信息
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(), updateDriverLocationForm.getLatitude().doubleValue());
        // 使用RedisTemplate的opsForGeo()方法添加司机的地理位置信息到Redis中
        redisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION, point, updateDriverLocationForm.getDriverId().toString());
        // 返回操作成功标志
        return true;
    }


    /**
     * 移除司机的位置信息
     * <p>
     * 该方法通过司机ID从Redis的地理空间中移除司机的位置信息主要应用于司机下线或退出登录场景
     * 使用了Redis的Geo操作来实现位置信息的移除
     *
     * @param driverId 司机的ID
     * @return 操作状态，固定返回true，表示操作成功
     */
    @Override
    public Boolean removeDriverLocation(Long driverId) {
        // 移除Redis中指定司机ID的位置信息
        redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
        // 返回操作状态
        return true;
    }

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    /**
     * 根据地理位置搜索附近的司机
     *
     * @param searchNearByDriverForm 包含搜索条件的表单对象，主要包括经度、纬度和订单里程
     * @return 返回一个包含附近司机信息的列表，每个司机信息包括司机ID和距离
     */
    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
        // 搜索经纬度位置5公里以内的司机
        // 定义经纬度点
        Point point = new Point(searchNearByDriverForm.getLongitude().doubleValue(), searchNearByDriverForm.getLatitude().doubleValue());
        // 定义距离：5公里(系统配置)
        Distance distance = new Distance(SystemConstant.NEARBY_DRIVER_RADIUS, RedisGeoCommands.DistanceUnit.KILOMETERS);
        // 定义以point点为中心，distance为距离这么一个范围
        Circle circle = new Circle(point, distance);

        // 定义GEO参数
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance() // 包含距离
                .includeCoordinates() // 包含坐标
                .sortAscending(); // 排序：升序

        // 1.GEORADIUS获取附近范围内的信息
        GeoResults<RedisGeoCommands.GeoLocation<String>> result = this.redisTemplate.opsForGeo().radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);

        // 2.收集信息，存入list
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = result.getContent();

        // 3.返回计算后的信息
        List<NearByDriverVo> list = new ArrayList();
        if (!CollectionUtils.isEmpty(content)) {
            Iterator<GeoResult<RedisGeoCommands.GeoLocation<String>>> iterator = content.iterator();
            while (iterator.hasNext()) {
                GeoResult<RedisGeoCommands.GeoLocation<String>> item = iterator.next();

                // 司机id
                Long driverId = Long.parseLong(item.getContent().getName());
                // 当前距离
                BigDecimal currentDistance = new BigDecimal(item.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP);
                log.info("司机：{}，距离：{}", driverId, item.getDistance().getValue());

                // 获取司机接单设置参数
                DriverSet driverSet = driverInfoFeignClient.getDriverSet(driverId).getData();
                // 接单里程判断，acceptDistance==0：不限制，
                if (driverSet.getAcceptDistance().doubleValue() != 0 && driverSet.getAcceptDistance().subtract(currentDistance).doubleValue() < 0) {
                    continue;
                }
                // 订单里程判断，orderDistance==0：不限制
                if (driverSet.getOrderDistance().doubleValue() != 0 && driverSet.getOrderDistance().subtract(searchNearByDriverForm.getMileageDistance()).doubleValue() < 0) {
                    continue;
                }

                // 满足条件的附近司机信息
                NearByDriverVo nearByDriverVo = new NearByDriverVo();
                nearByDriverVo.setDriverId(driverId);
                nearByDriverVo.setDistance(currentDistance);
                list.add(nearByDriverVo);
            }
        }
        return list;
    }
    /**
     * 更新订单位置到缓存
     *
     * @param updateOrderLocationForm 包含订单ID、经度和纬度的表单
     * @return 总是返回true，表示更新操作成功
     *
     * 说明：
     * 该方法接收一个UpdateOrderLocationForm对象，从中提取经度和纬度信息，
     * 并连同订单ID一起存入Redis缓存。使用Redis的字符串数据结构来存储订单位置信息，
     * 键名为一个根据订单ID和固定前缀组合成的字符串。此操作确保了订单位置信息可以在系统中被快速访问和更新。
     */
    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        // 创建一个新的OrderLocationVo对象来存储位置信息
        OrderLocationVo orderLocationVo = new OrderLocationVo();
        // 设置经度
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());
        // 设置纬度
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());
        // 将订单位置信息保存到Redis缓存中，键名包含前缀和订单ID
        redisTemplate.opsForValue().set(RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId(), orderLocationVo);
        // 返回成功更新的标志
        return true;
    }
    /**
     * 从缓存中获取订单位置信息
     * 本方法通过Redis存储的订单位置数据的键，从Redis缓存中获取指定订单的位置信息
     * 使用了RedisTemplate进行缓存操作，实现了对订单位置信息的快速检索
     *
     * @param orderId 订单ID，用作Redis缓存键的一部分，以实现对特定订单位置信息的查询
     * @return 返回OrderLocationVo对象，该对象包含了从缓存中获取的订单位置信息如果缓存中不存在对应数据，则返回null
     */
    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        OrderLocationVo orderLocationVo = (OrderLocationVo)redisTemplate.opsForValue().get(RedisConstant.UPDATE_ORDER_LOCATION + orderId);
        return orderLocationVo;
    }
    @Autowired
    private OrderServiceLocationRepository orderServiceLocationRepository;

    /**
     * 保存订单服务位置信息
     *
     * @param orderLocationServiceFormList 订单服务位置表单列表，包含待保存的订单服务位置信息
     * @return 返回布尔值，表示保存操作是否成功
     *
     * 该方法将表单列表中的数据转换为OrderServiceLocation实体类列表，并将其保存到数据库中
     * 它主要用于在创建或更新订单时，确保订单的服务位置信息是最新的
     */
    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {
        // 初始化OrderServiceLocation对象列表
        List<OrderServiceLocation> list = new ArrayList<>();
        // 遍历表单列表，转换为OrderServiceLocation对象并设置其属性
        orderLocationServiceFormList.forEach(item -> {
            OrderServiceLocation orderServiceLocation = new OrderServiceLocation();
            // 复制表单数据到实体类对象
            BeanUtils.copyProperties(item, orderServiceLocation);
            // 生成新的ID，确保每个OrderServiceLocation对象都有唯一的标识
            orderServiceLocation.setId(ObjectId.get().toString());
            // 设置创建时间，记录对象创建的时间戳
            orderServiceLocation.setCreateTime(new Date());
            // 将对象添加到列表中
            list.add(orderServiceLocation);
        });
        // 保存所有OrderServiceLocation对象到数据库中
        orderServiceLocationRepository.saveAll(list);
        // 返回操作成功标识
        return true;
    }
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据订单ID获取最新的订单服务位置信息
     * 本方法通过查询数据库中特定订单ID的记录，找出最近创建的位置信息
     * 使用MongoDB的查询和排序功能来实现
     *
     * @param orderId 订单ID，用于定位特定的订单记录
     * @return OrderServiceLastLocationVo 返回最新的订单服务位置信息，封装在自定义的VO中
     */
    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        //初始化查询对象
        Query query = new Query();
        //设置查询条件为指定的订单ID
        query.addCriteria(Criteria.where("orderId").is(orderId));
        //设置查询排序为创建时间降序，以便找到最近的记录
        query.with(Sort.by(Sort.Order.desc("createTime")));
        //限制查询结果数量为1，即只需获取最新的那条记录
        query.limit(1);

        //执行查询，获取最新的订单服务位置信息
        OrderServiceLocation orderServiceLocation = mongoTemplate.findOne(query, OrderServiceLocation.class);

        //封装返回对象
        OrderServiceLastLocationVo orderServiceLastLocationVo = new OrderServiceLastLocationVo();
        //将查询结果的属性复制到返回对象中，便于调用方使用
        BeanUtils.copyProperties(orderServiceLocation, orderServiceLastLocationVo);
        return orderServiceLastLocationVo;
    }
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    /**
     * 计算订单的实际行驶距离
     *
     * @param orderId 订单ID
     * @return 订单的实际行驶距离（BigDecimal类型）
     */
    @Override
    public BigDecimal calculateOrderRealDistance(Long orderId) {
        // 根据订单ID获取按创建时间升序的服务位置列表
        List<OrderServiceLocation> orderServiceLocationList = orderServiceLocationRepository.findByOrderIdOrderByCreateTimeAsc(orderId);
        // 初始化实际行驶距离为0
        double realDistance = 0;
        // 检查列表是否非空
        if(!CollectionUtils.isEmpty(orderServiceLocationList)) {
            // 遍历列表计算相邻两个地点之间的距离并累加
            for (int i = 0, size=orderServiceLocationList.size()-1; i < size; i++) {
                // 获取当前和下一个服务位置
                OrderServiceLocation location1 = orderServiceLocationList.get(i);
                OrderServiceLocation location2 = orderServiceLocationList.get(i+1);

                // 使用LocationUtil类计算两个地点之间的距离
                double distance = LocationUtil.getDistance(location1.getLatitude().doubleValue(), location1.getLongitude().doubleValue(), location2.getLatitude().doubleValue(), location2.getLongitude().doubleValue());
                // 累加距离
                realDistance += distance;
            }
        }
        // 如果实际行驶距离为0，则通过调用接口获取订单信息，并返回预期里程加上模拟的额外距离
        if(realDistance == 0) {
            return orderInfoFeignClient.getOrderInfo(orderId).getData().getExpectDistance().add(new BigDecimal("5"));
        }
        // 返回计算得到的实际行驶距离
        return new BigDecimal(realDistance);
    }

}
