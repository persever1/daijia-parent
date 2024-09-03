package com.atguigu.daijia.dispatch.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.dispatch.mapper.OrderJobMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.dispatch.xxl.client.XxlJobClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.dispatch.OrderJob;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {
    @Autowired
    private XxlJobClient xxlJobClient;

    @Autowired
    private OrderJobMapper orderJobMapper;

    /**
     * 添加并启动新订单任务
     * 本方法的主要目标是为新订单创建并启动一个任务如果订单对应的任务不存在，则创建新任务并关联订单
     * 使用了事务注解，确保在遇到异常时能够回滚，保证数据的一致性
     *
     * @param newOrderTaskVo 订单任务信息的封装，包含订单ID等
     * @return 返回任务ID，用于跟踪任务状态
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
        // 检查是否已存在对应订单ID的任务
        OrderJob orderJob = orderJobMapper.selectOne(new LambdaQueryWrapper<OrderJob>().eq(OrderJob::getOrderId, newOrderTaskVo.getOrderId()));
        if(null == orderJob) {
            // 如果不存在，则添加并启动新任务
            Long jobId = xxlJobClient.addAndStart("newOrderTaskHandler", "", "0 0/1 * * * ?", "新订单任务,订单id："+newOrderTaskVo.getOrderId());

            // 记录订单与任务的关联信息
            orderJob = new OrderJob();
            orderJob.setOrderId(newOrderTaskVo.getOrderId());
            orderJob.setJobId(jobId);
            orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));
            orderJobMapper.insert(orderJob);
        }
        // 返回任务ID
        return orderJob.getJobId();
    }

    @Autowired
    private LocationFeignClient locationFeignClient;

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    /**
     * 执行指定的任务
     * 该方法主要负责查询订单状态，搜索附近的司机，并向他们分发订单信息
     * @param jobId 任务ID
     * @return 布尔值，表示任务是否执行成功
     */
    public Boolean executeTask(Long jobId) {
        // 获取任务参数
        OrderJob orderJob = orderJobMapper.selectOne(new LambdaQueryWrapper<OrderJob>().eq(OrderJob::getJobId, jobId));
        if (null == orderJob) {
            return true;
        }
        NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(orderJob.getParameter(), NewOrderTaskVo.class);

        // 查询订单状态，如果该订单还在接单状态，继续执行；如果不在接单状态，则停止定时调度
        Integer orderStatus = orderInfoFeignClient.getOrderStatus(newOrderTaskVo.getOrderId()).getData();
        if (orderStatus.intValue() != OrderStatus.WAITING_ACCEPT.getStatus().intValue()) {
            xxlJobClient.stopJob(jobId);
            log.info("停止任务调度: {}", JSON.toJSONString(newOrderTaskVo));
            return true;
        }

        // 搜索附近满足条件的司机
        SearchNearByDriverForm searchNearByDriverForm = new SearchNearByDriverForm();
        searchNearByDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
        searchNearByDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
        searchNearByDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());
        List<NearByDriverVo> nearByDriverVoList = locationFeignClient.searchNearByDriver(searchNearByDriverForm).getData();
        // 给司机派发订单信息
        nearByDriverVoList.forEach(driver -> {
            // 记录司机id，防止重复推送订单信息
            String repeatKey = RedisConstant.DRIVER_ORDER_REPEAT_LIST + newOrderTaskVo.getOrderId();
            boolean isMember = redisTemplate.opsForSet().isMember(repeatKey, driver.getDriverId());
            if (!isMember) {
                // 记录该订单已放入司机临时容器
                redisTemplate.opsForSet().add(repeatKey, driver.getDriverId());
                // 过期时间：15分钟，新订单15分钟没人接单自动取消
                redisTemplate.expire(repeatKey, RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME, TimeUnit.MINUTES);

                NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
                newOrderDataVo.setOrderId(newOrderTaskVo.getOrderId());
                newOrderDataVo.setStartLocation(newOrderTaskVo.getStartLocation());
                newOrderDataVo.setEndLocation(newOrderTaskVo.getEndLocation());
                newOrderDataVo.setExpectAmount(newOrderTaskVo.getExpectAmount());
                newOrderDataVo.setExpectDistance(newOrderTaskVo.getExpectDistance());
                newOrderDataVo.setExpectTime(newOrderTaskVo.getExpectTime());
                newOrderDataVo.setFavourFee(newOrderTaskVo.getFavourFee());
                newOrderDataVo.setDistance(driver.getDistance());
                newOrderDataVo.setCreateTime(newOrderTaskVo.getCreateTime());

                // 将消息保存到司机的临时队列里面，司机接单了会定时轮询到他的临时队列获取订单消息
                String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driver.getDriverId();
                redisTemplate.opsForList().leftPush(key, JSONObject.toJSONString(newOrderDataVo));
                // 过期时间：1分钟，1分钟未消费，自动过期
                // 注：司机端开启接单，前端每5秒（远小于1分钟）拉取1次“司机临时队列”里面的新订单消息
                redisTemplate.expire(key, RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME, TimeUnit.MINUTES);
                log.info("该新订单信息已放入司机临时队列: {}", JSON.toJSONString(newOrderDataVo));
            }
        });
        return true;
    }
    /**
     * 根据司机ID查询新的订单队列数据
     * 该方法通过在Redis中查询与司机相关的订单临时列表来获取数据，然后将其解析为NewOrderDataVo对象列表
     *
     * @param driverId 司机ID，用于构建Redis键名，以定位特定司机的订单数据
     * @return 返回一个NewOrderDataVo对象列表，包含司机的新订单数据
     */
    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        // 初始化一个空的列表，用于存储解析后的订单数据
        List<NewOrderDataVo> list = new ArrayList<>();
        // 构建Redis中司机订单数据的键名
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        // 获取Redis中该键对应的列表的长度
        long size = redisTemplate.opsForList().size(key);
        // 如果列表不为空，则进行循环处理
        if(size > 0) {
            // 循环处理列表中的每个元素
            for(int i=0; i<size; i++) {
                // 从列表的左侧弹出一个字符串元素
                String content = (String)redisTemplate.opsForList().leftPop(key);
                // 将字符串内容解析为NewOrderDataVo对象
                NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content, NewOrderDataVo.class);
                // 将解析后的对象添加到列表中
                list.add(newOrderDataVo);
            }
        }
        // 返回解析后的订单数据列表
        return list;
    }

    /**
     * 清除司机的新订单队列数据
     *
     * 当司机开启服务时，系统会自动创建一个新的订单容器，此方法旨在直接删除该容器，
     * 以便在需要时（如重新开启服务）重新创建新的订单队列
     *
     * @param driverId 司机的ID，用于构造Redis键名
     * @return 总是返回true，表示操作成功
     */
    @Override
    public Boolean clearNewOrderQueueData(Long driverId) {
        // 构造Redis中司机订单数据的键名
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        // 直接删除订单数据，以便在需要时重新创建订单队列
        redisTemplate.delete(key);
        return true;
    }

}
