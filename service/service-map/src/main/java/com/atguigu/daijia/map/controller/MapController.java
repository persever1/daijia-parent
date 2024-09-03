package com.atguigu.daijia.map.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "地图API接口管理")
@RestController
@RequestMapping("/map")
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapController {

    @Autowired
    private MapService mapService;

    /**
     * 计算驾驶线路接口
     *
     * 该接口通过POST请求接收一个JSON格式的表单数据，该数据包含计算驾驶线路所需的信息，
     * 如起点、终点等。然后调用地图服务（mapService）的calculateDrivingLine方法进行线路计算，
     * 并将计算结果封装在Result对象中返回。
     *
     * @param calculateDrivingLineForm 驾驶线路计算表单，包含计算驾驶线路所需的信息
     * @return 包含驾驶线路信息的Result对象，成功时result.getCode()为200
     */
    @Operation(summary = "计算驾驶线路")
    @PostMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(mapService.calculateDrivingLine(calculateDrivingLineForm));
    }
}

