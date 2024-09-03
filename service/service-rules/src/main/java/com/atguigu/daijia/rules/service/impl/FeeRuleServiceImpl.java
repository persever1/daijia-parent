package com.atguigu.daijia.rules.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.daijia.model.entity.rule.FeeRule;
import com.atguigu.daijia.model.form.rules.FeeRuleRequest;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponse;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.rules.mapper.FeeRuleMapper;
import com.atguigu.daijia.rules.service.FeeRuleService;
import com.atguigu.daijia.rules.utils.DroolsHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class FeeRuleServiceImpl implements FeeRuleService {



    @Autowired
    private FeeRuleMapper feeRuleMapper;

    @Override
    /**
     * 计算订单费用
     * 根据传入的订单信息和费用规则请求表单，计算出订单费用
     *
     * @param feeRuleRequestForm 费用规则请求表单，包含订单距离、开始时间、等待分钟等信息
     * @return 返回计算后的订单费用规则响应对象
     */
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm feeRuleRequestForm) {
        // 封装传入对象
        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();
        feeRuleRequest.setDistance(feeRuleRequestForm.getDistance());
        feeRuleRequest.setStartTime(new DateTime(feeRuleRequestForm.getStartTime()).toString("HH:mm:ss"));
        feeRuleRequest.setWaitMinute(feeRuleRequestForm.getWaitMinute());
        log.info("传入参数：{}", JSON.toJSONString(feeRuleRequest));

        // 获取最新订单费用规则
        FeeRule feeRule = feeRuleMapper.selectOne(new LambdaQueryWrapper<FeeRule>().orderByDesc(FeeRule::getId).last("limit 1"));
        // 根据费用规则加载Drools规则
        KieSession kieSession = DroolsHelper.loadForRule(feeRule.getRule());

        // 封装返回对象
        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();
        // 向Drools会话中设置全局变量，用于规则访问
        kieSession.setGlobal("feeRuleResponse", feeRuleResponse);
        // 设置订单对象
        kieSession.insert(feeRuleRequest);
        // 触发规则
        kieSession.fireAllRules();
        // 中止会话
        kieSession.dispose();
        log.info("计算结果：{}", JSON.toJSONString(feeRuleResponse));

        // 封装返回对象
        FeeRuleResponseVo feeRuleResponseVo = new FeeRuleResponseVo();
        // 设置费用规则ID
        feeRuleResponseVo.setFeeRuleId(feeRule.getId());
        // 复制计算结果属性到返回对象
        BeanUtils.copyProperties(feeRuleResponse, feeRuleResponseVo);
        return feeRuleResponseVo;
    }
}
