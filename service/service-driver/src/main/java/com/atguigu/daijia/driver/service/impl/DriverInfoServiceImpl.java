package com.atguigu.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.mapper.*;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.*;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.*;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverInfoService {


    @Autowired
    private DriverInfoMapper driverInfoMapper;

    @Autowired
    private DriverAccountMapper driverAccountMapper;

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private DriverSetMapper driverSetMapper;

    @Autowired
    private DriverLoginLogMapper driverLoginLogMapper;

    /**
     * 小程序用户登录方法
     * 通过微信小程序的code换取openId，并根据openId查询或创建司机信息，最后记录登录日志并返回司机ID
     *
     * @param code 小程序授权码
     * @return 司机ID
     * @throws GuiguException 如果微信code解析失败或司机信息查询/创建出现问题，则抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long login(String code) {
        String openId = null;
        try {
            // 通过微信小程序code获取session信息，包括openId
            //获取openId
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            openId = sessionInfo.getOpenid();
            log.info("【小程序授权】openId={}", openId);
        } catch (Exception e) {
            e.printStackTrace();
            // 抛出自定义异常，表示微信code解析错误
            throw new GuiguException(ResultCodeEnum.WX_CODE_ERROR);
        }

        // 根据openId查询司机信息
        DriverInfo driverInfo = this.getOne(new LambdaQueryWrapper<DriverInfo>().eq(DriverInfo::getWxOpenId, openId));
        if (driverInfo == null) {
            // 如果司机信息不存在，则创建新的司机信息
            driverInfo = new DriverInfo();
            driverInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            driverInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            driverInfo.setWxOpenId(openId);
            this.save(driverInfo);

            // 初始化司机设置和账户信息
            // 初始化默认设置
            DriverSet driverSet = new DriverSet();
            driverSet.setDriverId(driverInfo.getId());
            driverSet.setOrderDistance(new BigDecimal(0)); // 0表示无限制
            driverSet.setAcceptDistance(new BigDecimal(SystemConstant.ACCEPT_DISTANCE)); // 默认接单范围：5公里
            driverSet.setIsAutoAccept(0); // 0表示否，1表示是
            driverSetMapper.insert(driverSet);

            // 初始化司机账户
            DriverAccount driverAccount = new DriverAccount();
            driverAccount.setDriverId(driverInfo.getId());
            driverAccountMapper.insert(driverAccount);
        }

        // 记录司机登录日志
        // 登录日志
        DriverLoginLog driverLoginLog = new DriverLoginLog();
        driverLoginLog.setDriverId(driverInfo.getId());
        driverLoginLog.setMsg("小程序登录");
        driverLoginLogMapper.insert(driverLoginLog);

        // 返回司机ID
        return driverInfo.getId();
    }

    /**
     * 根据司机ID获取司机登录信息。
     *
     * @param driverId 司机的唯一标识ID。
     * @return DriverLoginVo 对象，包含司机的登录信息。
     */
    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        // 根据driverId查询司机详细信息
        DriverInfo driverInfo = this.getById(driverId);
        // 创建一个新的DriverLoginVo对象，用于存储要返回的司机登录信息
        DriverLoginVo driverLoginVo = new DriverLoginVo();
        // 将DriverInfo对象的属性复制到DriverLoginVo对象中，实现数据的转换
        BeanUtils.copyProperties(driverInfo, driverLoginVo);

        // 判断司机是否已创建人脸库，通过检查driverInfo中的faceModelId是否有值来确定
        // isArchiveFace用于标识司机是否已录入人脸信息，用于接单时的人脸识别验证
        //是否创建人脸库人员，接单时做人脸识别判断
        Boolean isArchiveFace = StringUtils.hasText(driverInfo.getFaceModelId());
        // 将司机是否创建人脸库的信息设置到DriverLoginVo对象中
        driverLoginVo.setIsArchiveFace(isArchiveFace);

        // 返回包含司机登录信息的DriverLoginVo对象
        return driverLoginVo;
    }

    @Autowired
    private CosService cosService;

    /**
     * 根据司机ID获取司机认证信息
     * 此方法首先通过ID查询司机基本信息，然后构建一个司机认证信息对象（DriverAuthInfoVo），
     * 将司机基本信息复制到认证信息对象中，并通过云服务获取相关证件的显示URL
     *
     * @param driverId 司机ID
     * @return 包含司机认证信息及证件图片显示URL的DriverAuthInfoVo对象
     */
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        // 根据司机ID查询司机基本信息
        DriverInfo driverInfo = this.getById(driverId);
        // 创建一个空的司机认证信息对象
        DriverAuthInfoVo driverAuthInfoVo = new DriverAuthInfoVo();
        // 将司机基本信息复制到认证信息对象中
        BeanUtils.copyProperties(driverInfo, driverAuthInfoVo);
        // 通过云服务将证件背面URL转换为显示URL
        driverAuthInfoVo.setIdcardBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardBackUrl()));
        // 通过云服务将证件正面URL转换为显示URL
        driverAuthInfoVo.setIdcardFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardFrontUrl()));
        // 通过云服务将手持证件URL转换为显示URL
        driverAuthInfoVo.setIdcardHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardHandUrl()));
        // 通过云服务将驾驶证正面URL转换为显示URL
        driverAuthInfoVo.setDriverLicenseFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseFrontUrl()));
        // 通过云服务将驾驶证背面URL转换为显示URL
        driverAuthInfoVo.setDriverLicenseBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseBackUrl()));
        // 通过云服务将手持驾驶证URL转换为显示URL
        driverAuthInfoVo.setDriverLicenseHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseHandUrl()));
        // 返回构建好的司机认证信息对象
        return driverAuthInfoVo;
    }

    /**
     * 更新司机认证信息
     * <p>
     * 本方法通过注解声明了事务性操作，并指定在发生异常时回滚，确保数据的一致性
     * 它接收一个包含更新信息的表单对象，将其中的信息复制到一个DriverInfo实体中，
     * 然后尝试根据DriverInfo实体的ID更新数据库中的司机信息
     *
     * @param updateDriverAuthInfoForm 包含要更新的司机认证信息的表单对象
     * @return 更新操作的执行结果，true表示更新成功，false表示更新失败
     */
    @Transactional(rollbackFor = Exception.class)// 声明事务性操作，发生异常时回滚
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        // 创建一个空的DriverInfo对象，用于存储从表单复制的司机信息
        DriverInfo driverInfo = new DriverInfo();
        // 设置DriverInfo对象的ID，与表单中指定的司机ID相对应
        driverInfo.setId(updateDriverAuthInfoForm.getDriverId());
        // 将表单中的属性复制到DriverInfo对象中，这一步简化了手动设置属性的步骤
        BeanUtils.copyProperties(updateDriverAuthInfoForm, driverInfo);
        // 调用updateById方法尝试更新数据库中的司机信息，返回更新操作的结果
        return this.updateById(driverInfo);
    }

    @Autowired
    private TencentCloudProperties tencentCloudProperties;

    /**
     * 创建人员信息
     * 文档地址
     * https://cloud.tencent.com/document/api/867/45014
     * https://console.cloud.tencent.com/api/explorer?Product=iai&Version=2020-03-03&Action=CreatePerson
     *
     * @param driverFaceModelForm 人员信息表单
     * @return 操作是否成功
     */
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        // 根据驱动程序ID获取驱动程序信息
        DriverInfo driverInfo = this.getById(driverFaceModelForm.getDriverId());
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(), clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            CreatePersonRequest req = new CreatePersonRequest();
            // 设置人员组ID
            req.setGroupId(tencentCloudProperties.getPersionGroupId());
            // 设置人员基本信息
            req.setPersonId(String.valueOf(driverInfo.getId()));
            req.setGender(Long.parseLong(driverInfo.getGender()));
            req.setQualityControl(4L);
            req.setUniquePersonControl(4L);
            req.setPersonName(driverInfo.getName());
            req.setImage(driverFaceModelForm.getImageBase64());

            // 发起请求并获取响应
            CreatePersonResponse resp = client.CreatePerson(req);
            // 输出json格式的字符串回包
            System.out.println(CreatePersonResponse.toJsonString(resp));
            // 如果返回的人脸ID不为空，则保存到数据库
            if (StringUtils.hasText(resp.getFaceId())) {
                driverInfo.setFaceModelId(resp.getFaceId());
                this.updateById(driverInfo);
            }
        } catch (TencentCloudSDKException e) {
            // 异常处理：打印异常信息
            System.out.println(e.toString());
            return false;
        }
        // 操作成功
        return true;
    }

    /**
     * 根据司机ID获取司机设置信息
     * <p>
     * 此方法通过司机ID查询数据库，返回对应的司机设置信息对象如果找不到匹配的设置，
     * 返回null这个方法使用Lambda表达式来创建查询包装器，使代码更加简洁和类型安全
     *
     * @param driverId 司机的唯一标识ID
     * @return 司机设置信息的DriverSet对象，如果找不到则返回null
     */
    @Override
    public DriverSet getDriverSet(Long driverId) {
        // 创建一个查询包装器，并设置查询条件为根据司机ID查询
        LambdaQueryWrapper<DriverSet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DriverSet::getDriverId, driverId);
        // 使用MyBatis Plus的Mapper接口方法，根据查询包装器查询数据库，并返回查询结果
        return driverSetMapper.selectOne(queryWrapper);
    }

    @Autowired
    private DriverFaceRecognitionMapper driverFaceRecognitionMapper;

    /**
     * 根据司机ID检查当天是否已进行人脸认证
     * <p>
     * 此方法通过查询数据库中当天是否有与给定司机ID相关的人脸认证记录来判断是否已进行人脸认证
     * 它使用Lambda表达式构建查询条件，以提高代码的可读性和安全性
     *
     * @param driverId 司机的ID，用于唯一标识司机
     * @return 如果当天存在人脸认证记录，则返回true；否则返回false
     */
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        // 构建查询条件，筛选特定司机ID和当天日期的人脸认证记录
        LambdaQueryWrapper<DriverFaceRecognition> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(DriverFaceRecognition::getDriverId, driverId);
        queryWrapper.eq(DriverFaceRecognition::getFaceDate, new DateTime().toString("yyyy-MM-dd"));

        // 使用构建的查询条件查询数据库，并返回查询到的记录数
        long count = driverFaceRecognitionMapper.selectCount(queryWrapper);

        // 如果查询到的记录数不为0，则表示已进行人脸认证，返回true；否则返回false
        return count != 0;
    }

    /**
     * 人脸验证
     * 文档地址：
     * https://cloud.tencent.com/document/api/867/44983
     * https://console.cloud.tencent.com/api/explorer?Product=iai&Version=2020-03-03&Action=VerifyFace
     *
     * @param driverFaceModelForm 人脸验证表单数据
     * @return true表示人脸验证通过，false表示人脸验证未通过
     */
    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，
            // 建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(),
                                             tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(), clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            VerifyFaceRequest req = new VerifyFaceRequest();
            req.setImage(driverFaceModelForm.getImageBase64());
            req.setPersonId(String.valueOf(driverFaceModelForm.getDriverId()));
            // 返回的resp是一个VerifyFaceResponse的实例，与请求对象对应
            VerifyFaceResponse resp = client.VerifyFace(req);
            // 输出json格式的字符串回包
            System.out.println(VerifyFaceResponse.toJsonString(resp));
            // 检查人脸是否匹配
            if (resp.getIsMatch()) {
                // 进行活体检查
                if (this.detectLiveFace(driverFaceModelForm.getImageBase64())) {
                    // 插入人脸认证记录
                    DriverFaceRecognition driverFaceRecognition = new DriverFaceRecognition();
                    driverFaceRecognition.setDriverId(driverFaceModelForm.getDriverId());
                    driverFaceRecognition.setFaceDate(new Date());
                    driverFaceRecognitionMapper.insert(driverFaceRecognition);
                    // 返回认证通过
                    return true;
                }
                ;
            }
        } catch (TencentCloudSDKException e) {
            // 输出异常信息
            System.out.println(e.toString());
        }
        // 如果认证未通过或者发生异常，则抛出自定义异常
        throw new GuiguException(ResultCodeEnum.FACE_FAIL);
    }


    /**
     * 人脸静态活体检测
     * 文档地址：
     * https://cloud.tencent.com/document/api/867/48501
     * https://console.cloud.tencent.com/api/explorer?Product=iai&Version=2020-03-03&Action=DetectLiveFace
     *
     * @param imageBase64 Base64编码的图像数据
     * @return 返回检测结果，true表示检测到活体，false表示未检测到活体或发生错误
     */
    private Boolean detectLiveFace(String imageBase64) {
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(), clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DetectLiveFaceRequest req = new DetectLiveFaceRequest();
            req.setImage(imageBase64);
            // 返回的resp是一个DetectLiveFaceResponse的实例，与请求对象对应
            DetectLiveFaceResponse resp = client.DetectLiveFace(req);
            // 输出json格式的字符串回包
            System.out.println(DetectLiveFaceResponse.toJsonString(resp));
            // 判断返回结果中的IsLiveness字段，确定是否检测到活体
            if (resp.getIsLiveness()) {
                return true;
            }
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }
        // 如果检测失败或发生异常，则返回false
        return false;
    }
    /**
     * 更新司机服务状态
     *
     * @param driverId 司机ID
     * @param status 服务状态
     * @return 更新操作是否成功
     */
    @Transactional
    @Override
    public Boolean updateServiceStatus(Long driverId, Integer status) {
        // 创建查询构造器，用于匹配司机ID
        LambdaQueryWrapper<DriverSet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DriverSet::getDriverId, driverId);

        // 初始化DriverSet对象，仅设置服务状态
        DriverSet driverSet = new DriverSet();
        driverSet.setServiceStatus(status);

        // 执行更新操作
        driverSetMapper.update(driverSet, queryWrapper);

        // 返回操作成功标识
        return true;
    }
    /**
     * 根据驾驶员ID获取驾驶员信息
     * 此方法首先通过ID查询驾驶员信息，然后将查询到的信息转换为DriverInfoVo对象，
     * 并计算驾驶员的驾龄，最后将驾龄设置到DriverInfoVo对象中并返回
     *
     * @param driverId 驾驶员ID，用于查询特定驾驶员的信息
     * @return 返回DriverInfoVo对象，包含驾驶员信息和驾龄
     */
    @Override
    public DriverInfoVo getDriverInfo(Long driverId) {
        //通过ID查询驾驶员信息
        DriverInfo driverInfo = this.getById(driverId);
        //创建DriverInfoVo对象，用于返回
        DriverInfoVo driverInfoVo = new DriverInfoVo();
        //将查询到的驾驶员信息复制到DriverInfoVo对象中
        BeanUtils.copyProperties(driverInfo, driverInfoVo);
        //计算驾龄
        Integer driverLicenseAge = new DateTime().getYear() - new DateTime(driverInfo.getDriverLicenseIssueDate()).getYear() + 1;
        //设置驾龄到DriverInfoVo对象中
        driverInfoVo.setDriverLicenseAge(driverLicenseAge);
        //返回包含驾龄的DriverInfoVo对象
        return driverInfoVo;
    }
    @Override
    public String getDriverOpenId(Long driverId) {
        DriverInfo driverInfo = this.getOne(new LambdaQueryWrapper<DriverInfo>().eq(DriverInfo::getId, driverId).select(DriverInfo::getWxOpenId));
        return driverInfo.getWxOpenId();
    }
}