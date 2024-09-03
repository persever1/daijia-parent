package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.order.*;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.order.mapper.OrderBillMapper;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderProfitsharingMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.atguigu.daijia.order.service.OrderMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    //使用Spring的事务管理，确保操作的原子性
    @Transactional(rollbackFor = {Exception.class})
    //实现保存订单信息的功能
    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        //初始化OrderInfo对象，准备保存订单信息
        OrderInfo orderInfo = new OrderInfo();
        //将表单数据复制到OrderInfo对象，简化数据转换过程
        BeanUtils.copyProperties(orderInfoForm, orderInfo);
        //生成唯一的订单号，使用UUID，确保订单号的唯一性
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
        //设置订单状态为等待接单，解释订单状态的含义
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        //将生成的订单号设置到订单信息中
        orderInfo.setOrderNo(orderNo);
        //将订单信息插入数据库，实现持久化
        orderInfoMapper.insert(orderInfo);
        this.sendDelayMessage(orderInfo.getId());

        //记录日志，解释记录日志的目的，用于跟踪订单处理过程
        this.log(orderInfo.getId(), orderInfo.getStatus());

        //使用Redis设置接单标识，标识订单状态的变化
        //接单标识，标识不存在了说明不在等待接单状态了
        redisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK, "0", RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);
        //返回订单ID，作为操作的结果
        return orderInfo.getId();
    }


    /**
     * 记录订单状态日志
     *
     * @param orderId 订单ID
     * @param status  订单状态
     */
    public void log(Long orderId, Integer status) {
        // 创建订单状态日志对象
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        // 设置订单ID
        orderStatusLog.setOrderId(orderId);
        // 设置订单状态
        orderStatusLog.setOrderStatus(status);
        // 设置操作时间
        orderStatusLog.setOperateTime(new Date());
        // 插入订单状态日志到数据库
        orderStatusLogMapper.insert(orderStatusLog);
    }

    /**
     * 根据订单ID获取订单状态
     * <p>
     * 此方法通过订单ID查询数据库，以获取订单的状态信息它使用了Lambda表达式来构建查询条件，
     * 仅选择订单状态字段进行查询如果找不到对应的订单信息，将返回一个特定的默认状态值，
     * 用于指示出现了空订单的情况
     *
     * @param orderId 订单ID，用于标识具体的订单
     * @return 订单状态，如果找不到对应的订单信息，返回一个特定的默认状态值
     */
    @Override
    public Integer getOrderStatus(Long orderId) {
        // 使用Lambda表达式构建查询条件，确保只查询需要的字段，提高查询效率
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.select(OrderInfo::getStatus);

        // 执行查询，获取订单状态信息
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);

        // 如果未找到订单信息，返回一个特定的默认状态值，用于指示空订单的情况
        if (null == orderInfo) {
            // 返回NULL_ORDER状态的值，用于后续处理空订单的情况
            return OrderStatus.NULL_ORDER.getStatus();
        }

        // 返回查询到的订单状态
        return orderInfo.getStatus();
    }

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 司机抢单方法
     *
     * @param driverId 司机的ID
     * @param orderId  订单的ID
     * @return 返回抢单是否成功
     */
    //使用分布式事务，确保在发生异常时能够回滚
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        //抢单成功或取消订单，都会删除该key，redis判断，减少数据库压力
        if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        // 初始化分布式锁，创建一个RLock实例
        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);
        try {
            /**
             * TryLock是一种非阻塞式的分布式锁，实现原理：Redis的SETNX命令
             * 参数：
             *     waitTime：等待获取锁的时间
             *     leaseTime：加锁的时间
             */
            boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME, RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            //获取到锁
            if (flag) {
                //二次判断，防止重复抢单
                if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
                    //抢单失败
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }

                //修改订单状态
                //update order_info set status = 2, driver_id = #{driverId} where id = #{id}
                //修改字段
                OrderInfo orderInfo = new OrderInfo();
                orderInfo.setId(orderId);
                orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
                orderInfo.setAcceptTime(new Date());
                orderInfo.setDriverId(driverId);
                int rows = orderInfoMapper.updateById(orderInfo);
                if (rows != 1) {
                    //抢单失败
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }

                //记录日志
                this.log(orderId, orderInfo.getStatus());

                //删除redis订单标识
                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            }
        } catch (InterruptedException e) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
        return true;
    }

    /**
     * 查询客户的当前订单信息
     *
     * @param customerId 客户ID
     * @return 返回当前订单信息对象，包括订单状态、订单ID和是否存在当前订单的标志
     * <p>
     * 该方法通过查询数据库中客户的订单信息，来获取客户当前的订单详情
     * 它会根据客户的ID和订单的状态，查找最新的一个订单，并返回该订单的基本信息
     */
    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        // 创建查询条件构造器
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        // 匹配客户ID
        queryWrapper.eq(OrderInfo::getCustomerId, customerId);
        // 定义订单状态数组，包含从接单到未支付完成的各种状态
        // 说明：乘客端支付完订单，乘客端主要流程就走完（当前这些节点，乘客端会调整到相应的页面处理逻辑）
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus(),
                OrderStatus.UNPAID.getStatus()
        };
        // 条件：订单状态在上述数组中
        queryWrapper.in(OrderInfo::getStatus, statusArray);
        // 按订单ID降序排列
        queryWrapper.orderByDesc(OrderInfo::getId);
        // 限制查询结果只返回一条最新记录
        queryWrapper.last("limit 1");
        // 执行查询，获取最新的一个订单信息
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        // 创建当前订单信息VO对象，用于封装返回的结果
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        // 如果查询到订单信息，则设置VO对象的属性
        if (null != orderInfo) {
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            // 如果未查询到订单信息，则设置不存在当前订单的标志
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        // 返回当前订单信息VO对象
        return currentOrderInfoVo;
    }

    /**
     * 查询司机当前的订单信息
     *
     * @param driverId 司机ID
     * @return 返回司机当前的订单信息封装在CurrentOrderInfoVo对象中
     * <p>
     * 根据司机ID查询司机当前正在进行或最近完成的订单信息主要流程节点包括：
     * 1. 订单被接受
     * 2. 司机到达
     * 3. 更新购物车信息
     * 4. 开始服务
     * 5. 结束服务
     * 只查询最新的一个订单
     */
    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        // 创建查询条件对象
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        // 匹配司机ID
        queryWrapper.eq(OrderInfo::getDriverId, driverId);
        // 司机发送完账单，司机端主要流程就走完（当前这些节点，司机端会调整到相应的页面处理逻辑）
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus()
        };
        // 匹配订单状态在指定数组中的订单
        queryWrapper.in(OrderInfo::getStatus, statusArray);
        // 按订单ID降序排列
        queryWrapper.orderByDesc(OrderInfo::getId);
        // 限制查询一条记录
        queryWrapper.last("limit 1");
        // 执行查询获取订单信息
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        // 创建当前订单信息VO对象
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        // 判断查询结果是否为空，如果不为空则设置VO对象的属性
        if (null != orderInfo) {
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            // 如果查询结果为空，则设置没有当前订单的标志
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        // 返回当前订单信息VO对象
        return currentOrderInfoVo;
    }

    /**
     * 驾驶员到达起始地点后更新订单状态
     * 此方法确保订单状态准确反映驾驶员已到达的情况，并记录相应的时间
     *
     * @param orderId  订单ID，用于识别特定的订单
     * @param driverId 驾驶员ID，确保只有指定的驾驶员能更新订单状态
     * @return 返回更新操作是否成功的布尔值
     * @throws GuiguException 当更新操作失败时抛出异常，确保上层调用能意识到操作失败
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        //创建查询条件对象，用于后续的数据库查询
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        //确保订单ID匹配传入的ID
        queryWrapper.eq(OrderInfo::getId, orderId);
        //确保驾驶员ID匹配传入的ID
        queryWrapper.eq(OrderInfo::getDriverId, driverId);

        //准备更新用的订单对象
        OrderInfo updateOrderInfo = new OrderInfo();
        //设置订单状态为“驾驶员已到达”
        updateOrderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
        //记录驾驶员到达时间
        updateOrderInfo.setArriveTime(new Date());
        //执行更新操作
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        //检查更新操作是否成功
        if (row == 1) {
            //记录日志，包括订单ID和当前状态
            this.log(orderId, OrderStatus.DRIVER_ARRIVED.getStatus());
        } else {
            //更新失败时抛出异常
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        //返回成功标识
        return true;
    }
    /**
     * 根据 UpdateOrderCartForm 更新订单购物车信息
     * 此方法通过事务处理，确保订单信息更新的原子性
     * 如果更新失败，将抛出异常，回滚更改
     *
     * @param updateOrderCartForm 包含订单ID、司机ID等信息的表单，用于更新订单的购物车信息
     * @return 成功更新返回 true
     * @throws GuiguException 如果订单更新失败，则抛出此异常
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        // 初始化查询包装器，用于后续根据订单ID和司机ID查询订单
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, updateOrderCartForm.getOrderId());
        queryWrapper.eq(OrderInfo::getDriverId, updateOrderCartForm.getDriverId());

        // 创建一个OrderInfo对象，用于存储从表单复制的属性值
        OrderInfo updateOrderInfo = new OrderInfo();
        // 将表单属性复制到订单信息对象
        BeanUtils.copyProperties(updateOrderCartForm, updateOrderInfo);
        // 设置订单状态为更新代驾车辆信息
        updateOrderInfo.setStatus(OrderStatus.UPDATE_CART_INFO.getStatus());
        // 执行更新操作，只能更新自己的订单
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            // 订单更新成功，记录日志
            this.log(updateOrderCartForm.getOrderId(), OrderStatus.UPDATE_CART_INFO.getStatus());
        } else {
            // 订单更新失败，抛出异常
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    @Autowired
    private OrderMonitorService orderMonitorService;
    /**
     * 根据 StartDriveForm 表单信息启动行程
     *
     * @param startDriveForm 包含行程启动信息的表单
     * @return 成功更新返回 true
     * @throws GuiguException 当订单状态更新失败时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        // 根据订单ID和司机ID查询订单信息
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, startDriveForm.getOrderId());
        queryWrapper.eq(OrderInfo::getDriverId, startDriveForm.getDriverId());

        // 准备更新的订单信息
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
        updateOrderInfo.setStartServiceTime(new Date());

        // 更新订单状态
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if (row == 1) {
            // 订单状态更新成功，记录日志
            this.log(startDriveForm.getOrderId(), OrderStatus.START_SERVICE.getStatus());
        } else {
            // 更新失败，抛出异常
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

        // 初始化订单监控统计数据
        OrderMonitor orderMonitor = new OrderMonitor();
        orderMonitor.setOrderId(startDriveForm.getOrderId());
        orderMonitorService.saveOrderMonitor(orderMonitor);

        return true;
    }
    @Override
    public Long getOrderNumByTime(String startTime, String endTime) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(OrderInfo::getStartServiceTime, startTime);
        queryWrapper.lt(OrderInfo::getStartServiceTime, endTime);
        Long count = orderInfoMapper.selectCount(queryWrapper);
        return count;
    }
    @Autowired
    private OrderBillMapper orderBillMapper;

    @Autowired
    private OrderProfitsharingMapper orderProfitsharingMapper;

    //使用事务控制，确保数据的一致性，遇到异常会回滚
    @Transactional(rollbackFor = Exception.class)
    //结束行程方法，更新订单信息并生成账单
    @Override
    public Boolean endDrive(UpdateOrderBillForm updateOrderBillForm) {
        //根据订单ID和司机ID查询订单，确保只有对应的司机可以结束订单
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, updateOrderBillForm.getOrderId());
        queryWrapper.eq(OrderInfo::getDriverId, updateOrderBillForm.getDriverId());
        //准备更新的订单信息，包括状态、实际金额、优惠费、结束服务时间、实际距离
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.END_SERVICE.getStatus());
        updateOrderInfo.setRealAmount(updateOrderBillForm.getTotalAmount());
        updateOrderInfo.setFavourFee(updateOrderBillForm.getFavourFee());
        updateOrderInfo.setEndServiceTime(new Date());
        updateOrderInfo.setRealDistance(updateOrderBillForm.getRealDistance());
        //执行更新，如果成功更新了一条记录，则继续后续操作
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            //更新成功后，记录日志
            this.log(updateOrderBillForm.getOrderId(), OrderStatus.END_SERVICE.getStatus());

            //根据表单数据创建实际账单对象，并设置额外字段，然后插入数据库
            OrderBill orderBill = new OrderBill();
            BeanUtils.copyProperties(updateOrderBillForm, orderBill);
            orderBill.setOrderId(updateOrderBillForm.getOrderId());
            orderBill.setPayAmount(orderBill.getTotalAmount());
            orderBillMapper.insert(orderBill);

            //根据表单数据创建分账信息对象，并设置额外字段，然后插入数据库
            OrderProfitsharing orderProfitsharing = new OrderProfitsharing();
            BeanUtils.copyProperties(updateOrderBillForm, orderProfitsharing);
            orderProfitsharing.setOrderId(updateOrderBillForm.getOrderId());
            orderProfitsharing.setRuleId(updateOrderBillForm.getProfitsharingRuleId());
            orderProfitsharing.setStatus(1);
            orderProfitsharingMapper.insert(orderProfitsharing);
        } else {
            //如果更新订单信息失败，抛出异常
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }
    @Override
    public PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectCustomerOrderPage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }
    @Override
    public PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectDriverOrderPage(pageParam, driverId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }
    @Override
    public OrderBillVo getOrderBillInfo(Long orderId) {
        OrderBill orderBill = orderBillMapper.selectOne(new LambdaQueryWrapper<OrderBill>().eq(OrderBill::getOrderId, orderId));
        OrderBillVo orderBillVo = new OrderBillVo();
        BeanUtils.copyProperties(orderBill, orderBillVo);
        return orderBillVo;
    }
    /**
     * 根据订单ID获取订单分成信息
     *
     * 本方法通过调用数据库访问层（orderProfitsharingMapper）获取特定订单的分成信息，并将其转换为一个易于前端处理的视图对象（OrderProfitsharingVo）
     * 主要用于展示订单的分成详情，不直接处理业务逻辑，而是依赖传入的订单ID从数据库中重新加载数据
     *
     * @param orderId 订单ID，用于标识哪个订单的分成信息需要被获取
     * @return 返回一个OrderProfitsharingVo对象，其中包含指定订单的分成信息
     *         如果数据库中不存在对应订单ID的分成信息，则返回null
     */
    @Override
    public OrderProfitsharingVo getOrderProfitsharing(Long orderId) {
        // 从数据库中查询指定订单ID的分成信息
        OrderProfitsharing orderProfitsharing = orderProfitsharingMapper.selectOne(new LambdaQueryWrapper<OrderProfitsharing>().eq(OrderProfitsharing::getOrderId, orderId));

        // 创建一个用于返回的视图对象，用于封装订单分成信息
        OrderProfitsharingVo orderProfitsharingVo = new OrderProfitsharingVo();

        // 将数据库中加载的分成信息复制到视图对象中，便于前端展示
        BeanUtils.copyProperties(orderProfitsharing, orderProfitsharingVo);

        // 返回封装了订单分成信息的视图对象
        return orderProfitsharingVo;
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        //更新订单信息
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.eq(OrderInfo::getDriverId, driverId);
        //更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.UNPAID.getStatus());
        //只能更新自己的订单
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            //记录日志
            this.log(orderId, OrderStatus.UNPAID.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }
    @Override
    public OrderPayVo getOrderPayVo(String orderNo, Long customerId) {
        OrderPayVo orderPayVo = orderInfoMapper.selectOrderPayVo(orderNo, customerId);
        if(null != orderPayVo) {
            String content = orderPayVo.getStartLocation() + " 到 " + orderPayVo.getEndLocation();
            orderPayVo.setContent(content);
        }
        return orderPayVo;
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateOrderPayStatus(String orderNo) {
        //查询订单，判断订单状态，如果已更新支付状态，直接返回
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        queryWrapper.select(OrderInfo::getId, OrderInfo::getDriverId, OrderInfo::getStatus);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        if(null == orderInfo || orderInfo.getStatus().intValue() == OrderStatus.PAID.getStatus().intValue()) return true;

        //更新订单状态
        LambdaQueryWrapper<OrderInfo> updateQueryWrapper = new LambdaQueryWrapper<>();
        updateQueryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        //更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.PAID.getStatus());
        updateOrderInfo.setPayTime(new Date());
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            //记录日志
            this.log(orderInfo.getId(), OrderStatus.PAID.getStatus());
        } else {
            log.error("订单支付回调更新订单状态失败，订单号为：" + orderNo);
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    @Override
    public OrderRewardVo getOrderRewardFee(String orderNo) {
        //查询订单
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo).select(OrderInfo::getId,OrderInfo::getDriverId));
        //账单
        OrderBill orderBill = orderBillMapper.selectOne(new LambdaQueryWrapper<OrderBill>().eq(OrderBill::getOrderId, orderInfo.getId()).select(OrderBill::getRewardFee));
        OrderRewardVo orderRewardVo = new OrderRewardVo();
        orderRewardVo.setOrderId(orderInfo.getId());
        orderRewardVo.setDriverId(orderInfo.getDriverId());
        orderRewardVo.setRewardFee(orderBill.getRewardFee());
        return orderRewardVo;
    }
    @Override
    public void orderCancel(long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if(orderInfo.getStatus() == OrderStatus.WAITING_ACCEPT.getStatus()) {
            // 设置更新数据
            OrderInfo orderInfoUpt = new OrderInfo();
            orderInfoUpt.setId(orderId);
            orderInfoUpt.setStatus(OrderStatus.CANCEL_ORDER.getStatus());
            // 执行更新方法
            int rows = orderInfoMapper.updateById(orderInfoUpt);

            if(rows == 1) {
                //删除redis订单标识
                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            }
        }
    }
    /**
     * 发送延迟消息
     */
    private void sendDelayMessage(Long orderId) {
        try {
            //  创建一个队列
            RBlockingDeque<Object> blockingDeque = redissonClient
                    .getBlockingDeque("queue_cancel");
            //  将队列放入延迟队列中
            RDelayedQueue<Object> delayedQueue = redissonClient
                    .getDelayedQueue(blockingDeque);
            //  发送的内容
            delayedQueue.offer(orderId.toString(),
                    15, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateCouponAmount(Long orderId, BigDecimal couponAmount) {
        int row = orderBillMapper.updateCouponAmount(orderId, couponAmount);
        if(row != 1) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }
}
