package com.atguigu.daijia.dispatch.xxl.job;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.ExceptionUtil;
import com.atguigu.daijia.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.model.entity.dispatch.XxlJobLog;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @FileName JobHandler
 * @Description
 * @Author mark
 * @date 2024-08-13
 **/

@Slf4j
@Component
public class JobHandler {

    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;

    @Autowired
    private NewOrderService newOrderService;

    @XxlJob("newOrderTaskHandler")
    public void newOrderTaskHandler() {
        // 记录日志，指示新的订单调度任务开始执行
        log.info("新订单调度任务：{}", XxlJobHelper.getJobId());

        // 初始化定时任务日志对象
        XxlJobLog xxlJobLog = new XxlJobLog();
        // 设置任务ID
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        // 记录任务开始时间
        long startTime = System.currentTimeMillis();
        try {
            // 执行新订单任务
            newOrderService.executeTask(XxlJobHelper.getJobId());

            // 任务执行成功，设置状态为成功
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            // 任务执行失败，设置状态为失败
            xxlJobLog.setStatus(0);
            // 记录异常信息
            xxlJobLog.setError(ExceptionUtil.getAllExceptionMsg(e));
            // 记录错误日志
            log.error("定时任务执行失败，任务id为：{}", XxlJobHelper.getJobId());
            // 打印异常堆栈信息
            e.printStackTrace();
        } finally {
            // 计算任务执行耗时
            int times = (int) (System.currentTimeMillis() - startTime);
            // 设置任务执行耗时
            xxlJobLog.setTimes(times);
            // 将任务日志插入数据库
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }

}
