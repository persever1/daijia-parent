package com.atguigu.daijia.driver.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.service.CiService;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {


    @Autowired
    private TencentCloudProperties tencentCloudProperties;

    /**
     * 获取一个私有的COSClient实例。
     * 这个方法用于初始化并返回一个COSClient对象，该对象配置了访问腾讯云对象存储（COS）所需的认证信息和客户端配置。
     *
     * @return COSClient 返回一个配置好的COSClient实例，用于后续的COS操作。
     */
    private COSClient getPrivateCOSClient() {
        // 初始化COS认证信息，使用SecretId和SecretKey进行身份验证。
        COSCredentials cred = new BasicCOSCredentials(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());

        // 创建客户端配置对象，并设置区域和协议。
        ClientConfig clientConfig = new ClientConfig(new Region(tencentCloudProperties.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);

        // 使用认证信息和客户端配置初始化COSClient对象。
        COSClient cosClient = new COSClient(cred, clientConfig);

        // 返回初始化好的COSClient实例。
        return cosClient;
    }


    @Autowired
    private CiService ciService;

    /**
     * 文件上传至COS（兼容图片审核）
     *
     * @param file  待上传的文件
     * @param path  文件保存路径
     * @return  返回上传后的文件信息
     */
    @SneakyThrows
    @Override
    public CosUploadVo upload(MultipartFile file, String path) {
        // 获取私有的COS客户端实例
        COSClient cosClient = this.getPrivateCOSClient();

        // 初始化元数据信息
        ObjectMetadata meta = new ObjectMetadata();
        // 设置内容长度
        meta.setContentLength(file.getSize());
        // 设置内容编码方式
        meta.setContentEncoding("UTF-8");
        // 设置内容类型
        meta.setContentType(file.getContentType());

        // 构造文件类型字符串
        String fileType = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        // 生成上传路径和文件名
        String uploadPath = "/driver/" + path + "/" + UUID.randomUUID().toString().replaceAll("-", "") + fileType;
        // 创建上传对象请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(tencentCloudProperties.getBucketPrivate(), uploadPath, file.getInputStream(), meta);
        // 设置存储类型为标准存储
        putObjectRequest.setStorageClass(StorageClass.Standard);
        // 上传文件并获取结果
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        // 日志输出上传结果
        log.info(JSON.toJSONString(putObjectResult));
        // 关闭COS客户端
        cosClient.shutdown();

        // 图片审核
        Boolean isAuditing = ciService.imageAuditing(uploadPath);
        // 如果审核不通过，则删除文件并抛出异常
        if(!isAuditing) {
            cosClient.deleteObject(tencentCloudProperties.getBucketPrivate(), uploadPath);
            throw new GuiguException(ResultCodeEnum.IMAGE_AUDITION_FAIL);
        }

        // 封装返回对象
        CosUploadVo cosUploadVo = new CosUploadVo();
        // 设置文件访问路径
        cosUploadVo.setUrl(path);
        // 设置文件临时访问URL
        cosUploadVo.setShowUrl(this.getImageUrl(path));
        return cosUploadVo;
    }


    /**
     * 根据文件路径获取私有Bucket中文件的预签名URL。
     * 预签名URL用于授权访问私有文件，确保URL在一段时间内有效。
     *
     * @param path 文件在COS中的路径。
     * @return 文件的预签名URL，如果路径为空或无效，则返回空字符串。
     */
    @Override
    public String getImageUrl(String path) {
        // 检查路径是否有效，无效则直接返回空字符串
        if(!StringUtils.hasText(path)) return "";

        // 获取私有的COS客户端实例
        COSClient cosClient = getPrivateCOSClient();
        // 创建请求，指定获取私有bucket中文件的预签名URL
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(tencentCloudProperties.getBucketPrivate(),
                        path, HttpMethodName.GET);
        // 设置预签名URL的过期时间，这里设置为15分钟后过期
        // 设置临时URL有效期为15分钟
        Date expiration = new DateTime().plusMinutes(15).toDate();
        request.setExpiration(expiration);
        // 生成预签名URL
        URL url = cosClient.generatePresignedUrl(request);
        // 关闭COS客户端，释放资源
        cosClient.shutdown();
        // 返回预签名URL
        return url.toString();
    }

}
