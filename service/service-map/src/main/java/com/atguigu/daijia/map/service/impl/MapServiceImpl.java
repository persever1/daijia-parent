package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {
    @Autowired
    private RestTemplate restTemplate;

    @Value("${tencent.map.key}")
    private String key;    // 腾讯地图服务

    @Override
    /**
     * 计算驾车路线
     * @param calculateDrivingLineForm 包含了起点和终点经纬度以及API密钥的表单数据
     * @return 返回一个包含驾车路线信息的对象，包括距离、预计时间和路线多边形
     * @throws GuiguException 如果地图服务返回的状态码不为0，表示请求失败，则抛出异常
     */
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        // 腾讯地图API的URL，用于请求驾车路线
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";

        // 创建一个映射，用于存储请求参数
        Map<String, String> map = new HashMap<>();
        // 设置起点，将纬度和经度拼接成腾讯地图API所需的格式
        map.put("from", calculateDrivingLineForm.getStartPointLatitude() + "," + calculateDrivingLineForm.getStartPointLongitude());
        // 设置终点，同样将纬度和经度拼接成所需的格式
        map.put("to", calculateDrivingLineForm.getEndPointLatitude() + "," + calculateDrivingLineForm.getEndPointLongitude());
        // 设置API密钥
        map.put("key", key);

        // 使用RestTemplate发送GET请求并接收返回的JSON对象
        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);
        // 检查返回的状态码，如果不为0，则表示请求失败
        if(result.getIntValue("status") != 0) {
            // 抛出异常，表示地图服务请求失败
            throw new GuiguException(ResultCodeEnum.MAP_FAIL);
        }

        // 解析返回的JSON对象，获取第一条驾车路线（默认为最佳路线）
        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);
        // 创建驾车路线VO对象，用于存储驾车路线信息
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        // 计算距离，单位转换为千米，并保留两位小数
        drivingLineVo.setDistance(route.getBigDecimal("distance").divide(new BigDecimal(1000)).setScale(2, RoundingMode.HALF_UP));
        // 设置预计时间，单位为秒
        drivingLineVo.setDuration(route.getBigDecimal("duration"));
        // 设置路线多边形，用于标识驾车路线
        drivingLineVo.setPolyline(route.getJSONArray("polyline"));
        // 返回驾车路线VO对象
        return drivingLineVo;
    }


}
