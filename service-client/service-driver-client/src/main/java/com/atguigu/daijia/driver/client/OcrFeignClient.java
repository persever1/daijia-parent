package com.atguigu.daijia.driver.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.atguigu.daijia.model.vo.driver.IdCardOcrVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(value = "service-driver")
public interface OcrFeignClient {

    /**
     * 身份证识别
     * @param file
     * @return
     */
    @PostMapping(value = "/ocr/idCardOcr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)//consumes = MediaType.MULTIPART_FORM_DATA_VALUE 表示上传文件的格式
    Result<IdCardOcrVo> idCardOcr(@RequestPart("file") MultipartFile file);
    /**
     * 驾驶证识别
     * @param file
     * @return
     */
    @PostMapping(value = "/ocr/driverLicenseOcr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)//consumes = MediaType.MULTIPART_FORM_DATA_VALUE 表示上传文件的格式
    Result<DriverLicenseOcrVo> driverLicenseOcr(@RequestPart("file") MultipartFile file);
}