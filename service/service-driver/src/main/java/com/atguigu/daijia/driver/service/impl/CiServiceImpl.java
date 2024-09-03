package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.service.CiService;
import com.atguigu.daijia.model.vo.order.TextAuditingVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ciModel.auditing.*;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CiServiceImpl implements CiService {

    @Autowired
    private TencentCloudProperties tencentCloudProperties;

    /**
     * 获取私有的COSClient实例
     * 该方法用于创建并返回一个COSClient对象，该对象用于与腾讯云对象存储服务（COS）进行交互
     * 它使用了腾讯云的密钥信息进行身份验证，并根据预配置的区域信息设置客户端配置
     *
     * @return COSClient 用于操作腾讯云对象存储的客户端实例
     */
    private COSClient getPrivateCOSClient() {
        // 使用从配置文件中获取的密钥创建COS凭证
        COSCredentials cred = new BasicCOSCredentials(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());
        // 创建客户端配置，指定区域
        ClientConfig clientConfig = new ClientConfig(new Region(tencentCloudProperties.getRegion()));
        // 设置通信协议为HTTPS以增强安全性
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 使用凭证和配置创建COSClient实例
        COSClient cosClient = new COSClient(cred, clientConfig);
        // 返回COSClient实例
        return cosClient;
    }

    /**
     * 图片审核方法
     * 本方法用于对指定路径的图片进行内容审核，审核内容包括色情、广告、恐怖和政治等维度
     * 如果图片内容在任何维度上被标记为非正常（即存在违规内容），则方法返回false，否则返回true
     *
     * @param path 图片的路径
     * @return Boolean 返回审核结果，true表示图片内容正常，false表示图片内容存在违规
     */
    @Override
    public Boolean imageAuditing(String path) {
        // 创建COSClient对象，用于后续的图片审核操作
        COSClient cosClient = this.getPrivateCOSClient();

        // 创建任务请求对象
        ImageAuditingRequest request = new ImageAuditingRequest();

        // 添加请求参数
        // 设置请求 bucket
        request.setBucketName(tencentCloudProperties.getBucketPrivate());
        // 设置 bucket 中的图片位置
        request.setObjectKey(path);

        // 调用接口,获取任务响应对象
        ImageAuditingResponse response = cosClient.imageAuditing(request);
        // 关闭COSClient
        cosClient.shutdown();

        // 判断审核结果，如果任一维度的审核结果不是正常（即存在违规内容），则返回false
        if (!response.getPornInfo().getHitFlag().equals("0")
                || !response.getAdsInfo().getHitFlag().equals("0")
                || !response.getTerroristInfo().getHitFlag().equals("0")
                || !response.getPoliticsInfo().getHitFlag().equals("0")
        ) {
            return false;
        }

        // 图片内容在所有审核维度上均正常，返回true
        return true;
    }
    /**
     * 文本内容审核
     *
     * @param content 待审核的文本内容
     * @return 返回审核结果的TextAuditingVo对象
     */
    @Override
    public TextAuditingVo textAuditing(String content) {
        // 检查输入内容是否为空
        if(!StringUtils.hasText(content)) {
            TextAuditingVo textAuditingVo = new TextAuditingVo();
            textAuditingVo.setResult("0");
            return textAuditingVo;
        }
        // 获取私有的COS客户端
        COSClient cosClient = this.getPrivateCOSClient();

        // 创建文本审核请求对象
        TextAuditingRequest request = new TextAuditingRequest();
        request.setBucketName(tencentCloudProperties.getBucketPrivate());

        // 将文本内容转换为Base64字符串
        byte[] encoder = org.apache.commons.codec.binary.Base64.encodeBase64(content.getBytes());
        String contentBase64 = new String(encoder);
        request.getInput().setContent(contentBase64);
        request.getConf().setDetectType("all");

        // 发起文本审核任务并获取响应结果
        TextAuditingResponse response = cosClient.createAuditingTextJobs(request);
        AuditingJobsDetail detail = response.getJobsDetail();
        TextAuditingVo textAuditingVo = new TextAuditingVo();
        // 检查审核任务的状态
        if ("Success".equals(detail.getState())) {
            // 获取审核结果: 0（审核正常），1 （判定为违规敏感文件），2（疑似敏感，建议人工复核）
            String result = detail.getResult();

            // 存储违规关键词
            StringBuffer keywords = new StringBuffer();
            List<SectionInfo> sectionInfoList = detail.getSectionList();
            for (SectionInfo info : sectionInfoList) {
                // 获取色情、违法和辱骂内容的关键词
                String pornInfoKeyword = info.getPornInfo().getKeywords();
                String illegalInfoKeyword = info.getIllegalInfo().getKeywords();
                String abuseInfoKeyword = info.getAbuseInfo().getKeywords();
                // 将有效的关键词添加到结果中
                if (pornInfoKeyword.length() > 0) {
                    keywords.append(pornInfoKeyword).append(",");
                }
                if (illegalInfoKeyword.length() > 0) {
                    keywords.append(illegalInfoKeyword).append(",");
                }
                if (abuseInfoKeyword.length() > 0) {
                    keywords.append(abuseInfoKeyword).append(",");
                }
            }
            // 设置审核结果和关键词
            textAuditingVo.setResult(result);
            textAuditingVo.setKeywords(keywords.toString());
        }
        return textAuditingVo;
    }

}
