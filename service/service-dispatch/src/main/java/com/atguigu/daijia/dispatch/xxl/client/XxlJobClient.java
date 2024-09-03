
package com.atguigu.daijia.dispatch.xxl.client;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.xxl.config.XxlJobClientConfig;
import com.atguigu.daijia.model.entity.dispatch.XxlJobInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @FileName XxlJobClient
 * @Description https://dandelioncloud.cn/article/details/1598865461087518722
 * @Author mark
 * @date 2024-08-13
 **/

@Slf4j
@Component
public class XxlJobClient {

    @Autowired
    private XxlJobClientConfig xxlJobClientConfig;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 添加执行任务
     *
     * @param executorHandler 执行器处理器，用于指定处理任务的类
     * @param param 任务参数，用于传递给执行器的具体执行参数
     * @param corn 定时表达式，用于按定时任务执行
     * @param desc 任务描述，用于说明任务的用途和特点
     * @return 返回新增任务的ID
     * @throws GuiguException 自定义异常，当添加任务失败时抛出
     */
    @SneakyThrows
    public Long addJob(String executorHandler, String param, String corn, String desc) {
        // 初始化任务信息对象
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        // 设置任务组ID
        xxlJobInfo.setJobGroup(xxlJobClientConfig.getJobGroupId());
        // 设置任务描述
        xxlJobInfo.setJobDesc(desc);
        // 设置任务作者
        xxlJobInfo.setAuthor("qy");
        // 设置任务调度类型为CRON
        xxlJobInfo.setScheduleType("CRON");
        // 设置任务调度CRON表达式
        xxlJobInfo.setScheduleConf(corn);
        // 设置任务类型为BEAN
        xxlJobInfo.setGlueType("BEAN");
        // 设置执行器处理器
        xxlJobInfo.setExecutorHandler(executorHandler);
        // 设置执行器参数
        xxlJobInfo.setExecutorParam(param);
        // 设置执行器路由策略为第一个
        xxlJobInfo.setExecutorRouteStrategy("FIRST");
        // 设置执行器阻塞处理策略为串行执行
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        // 设置触发失败策略为立即触发一次
        xxlJobInfo.setMisfireStrategy("FIRE_ONCE_NOW");
        // 设置执行器超时时间
        xxlJobInfo.setExecutorTimeout(0);
        // 设置执行失败重试次数
        xxlJobInfo.setExecutorFailRetryCount(0);

        // 初始化HTTP请求头
        HttpHeaders headers = new HttpHeaders();
        // 设置请求头内容类型为JSON
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HTTP请求实体
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        // 构造请求URL
        String url = xxlJobClientConfig.getAddUrl();
        // 发送HTTP请求，添加任务
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        // 判断HTTP响应状态码和返回码是否成功
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            // 记录成功日志
            log.info("增加xxl执行任务成功,返回信息:{}", response.getBody().toJSONString());
            // 返回任务ID
            return response.getBody().getLong("content");
        }
        // 记录失败日志，抛出自定义异常
        log.info("调用xxl增加执行任务失败:{}", response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }


    /**
     * 启动任务
     *
     * @param jobId 任务ID
     * @return 如果任务启动成功，返回true；否则抛出异常
     */
    public Boolean startJob(Long jobId) {
        // 创建一个XxlJobInfo对象，并设置其ID，用于指定要启动的任务
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        // 设置HTTP请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HTTP请求实体，包含请求头和请求体
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        // 获取启动任务的URL
        String url = xxlJobClientConfig.getStartJobUrl();
        // 发送HTTP请求，等待服务器响应
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        // 检查HTTP响应状态码和服务器返回的代码，以确定请求是否成功
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            // 如果任务启动成功，记录日志并返回true
            log.info("启动xxl执行任务成功:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        // 如果任务启动失败，记录日志并抛出异常
        log.info("启动xxl执行任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

    /**
     * 停止执行指定的任务
     *
     * @param jobId 任务ID，用于标识具体要停止的任务
     * @return 如果任务停止成功，则返回true；否则抛出异常
     * @throws GuiguException 当任务停止失败时抛出此自定义异常
     */
    public Boolean stopJob(Long jobId) {
        // 初始化一个XxlJobInfo对象，用于承载将要停止的任务ID
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        // 设置HTTP请求头，指定请求体格式为JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HTTP请求实体，携带XxlJobInfo对象和请求头
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        // 构建请求的URL，用于停止任务
        String url = xxlJobClientConfig.getStopJobUrl();
        // 发送HTTP POST请求，获取响应
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);

        // 检查HTTP响应状态码和返回码是否都为200，表示请求成功
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            // 如果请求成功，记录日志并返回true
            log.info("停止xxl执行任务成功:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        // 如果请求失败，记录日志并抛出异常
        log.info("停止xxl执行任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

    /**
     * 删除定时任务
     *
     * 本方法通过调用XxlJob的RESTful API来删除指定的任务这个过程包括创建请求头、设置请求体、发送HTTP请求，
     * 并根据响应结果判断删除操作是否成功如果操作失败，则抛出异常
     *
     * @param jobId 需要删除的任务的ID
     * @return 如果删除成功，返回true；否则，抛出GuiguException异常
     * @throws GuiguException 如果删除任务失败，抛出此异常，表示需要进一步处理错误情况
     */
    public Boolean removeJob(Long jobId) {
        // 创建XxlJobInfo对象并设置其ID，作为删除操作的目标
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        // 初始化HTTP请求头，并设置内容类型为JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HTTP请求实体，包含请求头和请求体
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        // 获取用于删除任务的URL
        String url = xxlJobClientConfig.getRemoveUrl();
        // 发送HTTP POST请求，获取响应体
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);

        // 检查HTTP响应状态码和返回码是否表示成功
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            // 如果成功，记录日志并返回true
            log.info("删除xxl执行任务成功:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        // 如果操作失败，记录日志并抛出异常
        log.info("删除xxl执行任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

    /**
     * 添加并启动定时任务
     *
     * @param executorHandler 任务处理器
     * @param param 任务参数
     * @param corn 定时表达式
     * @param desc 任务描述
     * @return 任务ID
     * @throws GuiguException 如果添加并启动任务失败
     */
    public Long addAndStart(String executorHandler, String param, String corn, String desc) {
        // 创建一个新的任务信息对象
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        // 设置任务所属组ID
        xxlJobInfo.setJobGroup(xxlJobClientConfig.getJobGroupId());
        // 设置任务描述
        xxlJobInfo.setJobDesc(desc);
        // 设置任务作者
        xxlJobInfo.setAuthor("qy");
        // 设置调度类型为CRON
        xxlJobInfo.setScheduleType("CRON");
        // 设置定时表达式
        xxlJobInfo.setScheduleConf(corn);
        // 设置任务类型为BEAN
        xxlJobInfo.setGlueType("BEAN");
        // 设置任务处理器
        xxlJobInfo.setExecutorHandler(executorHandler);
        // 设置任务参数
        xxlJobInfo.setExecutorParam(param);
        // 设置任务路由策略为第一个
        xxlJobInfo.setExecutorRouteStrategy("FIRST");
        // 设置任务阻塞处理策略为串行执行
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        // 设置触发失败策略为立即触发一次
        xxlJobInfo.setMisfireStrategy("FIRE_ONCE_NOW");
        // 设置任务执行超时时间，默认为0，表示不限制
        xxlJobInfo.setExecutorTimeout(0);
        // 设置任务执行失败重试次数，默认为0，表示不重试
        xxlJobInfo.setExecutorFailRetryCount(0);

        // 创建HTTP请求头
        HttpHeaders headers = new HttpHeaders();
        // 设置请求体类型为JSON
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建HTTP请求实体
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        // 构建请求URL
        String url = xxlJobClientConfig.getAddAndStartUrl();
        // 发送HTTP请求，添加并启动任务
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        // 判断请求是否成功，且服务端返回的代码表示成功
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            // 日志记录成功信息
            log.info("增加并开始执行xxl任务成功,返回信息:{}", response.getBody().toJSONString());
            // 返回任务ID
            return response.getBody().getLong("content");
        }
        // 日志记录失败信息
        log.info("增加并开始执行xxl任务失败:{}", response.getBody().toJSONString());
        // 抛出异常表示添加并启动任务失败
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

}