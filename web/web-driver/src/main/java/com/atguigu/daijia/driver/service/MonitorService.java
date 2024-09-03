package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import org.springframework.web.multipart.MultipartFile;

public interface MonitorService {
    public Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm);
}
