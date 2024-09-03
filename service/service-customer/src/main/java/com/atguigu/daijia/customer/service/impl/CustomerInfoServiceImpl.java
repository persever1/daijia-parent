package com.atguigu.daijia.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.alibaba.fastjson.JSON;
import com.atguigu.daijia.customer.mapper.CustomerInfoMapper;
import com.atguigu.daijia.customer.mapper.CustomerLoginLogMapper;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.customer.CustomerLoginLog;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {
    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private CustomerInfoMapper customerInfoMapper;

    @Autowired
    private CustomerLoginLogMapper customerLoginLogMapper;

    /**
     * 用户通过微信小程序登录。
     * 使用微信提供的code换取用户的openid，进而识别用户身份。
     * 如果是首次登录，则创建新用户记录；否则，直接使用已有的用户信息。
     * 每次登录都会记录一条登录日志。
     *
     * @param code 微信小程序登录时返回的code，用于换取用户信息。
     * @return 用户的ID，用于后续的身份验证和信息关联。
     */
    //微信小程序登录接口
    @Override
    public Long login(String code) {
        // 尝试获取用户的openid，这是微信用户的一个唯一标识
        //1 获取code值，使用微信工具包对象，获取微信唯一标识openid
        String openid = null;
        try {
            WxMaJscode2SessionResult sessionInfo =
                    wxMaService.getUserService().getSessionInfo(code);
            openid = sessionInfo.getOpenid();
        } catch (WxErrorException e) {
            // 如果获取openid失败，抛出运行时异常
            throw new RuntimeException(e);
        }

        // 根据openid查询数据库中是否已有该用户的信息
        //2 根据openid查询数据库表，判断是否第一次登录
        //如果openid不存在返回null，如果存在返回一条记录
        //select * from customer_info ci where ci.wx_open_id = ''
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerInfo::getWxOpenId, openid);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);

        // 如果是首次登录，则创建新用户，并设置默认的昵称和头像
        //3 如果第一次登录，添加信息到用户表
        if (customerInfo == null) {
            // 创建一个新的CustomerInfo实例
            customerInfo = new CustomerInfo();
            // 设置一个唯一的昵称，基于当前系统时间戳
            customerInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            // 设置默认头像URL，使用阿里云提供的默认帅气头像
            customerInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            // 将从微信接口获取到的OpenID赋值给customerInfo对象
            customerInfo.setWxOpenId(openid);

            customerInfoMapper.insert(customerInfo);
        }

        // 记录用户的登录日志
        //4 记录登录日志信息
        // 创建一个CustomerLoginLog对象，用于记录客户登录信息
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();

        // 设置登录记录的客户ID，以便关联到具体的客户
        customerLoginLog.setCustomerId(customerInfo.getId());

        // 设置登录记录的信息为"小程序登录"，描述了登录的来源或方式
        customerLoginLog.setMsg("小程序登录");

        // 将登录记录插入到数据库中，保存客户的登录历史
        customerLoginLogMapper.insert(customerLoginLog);


        // 返回用户的ID，作为登录成功后的标识
        //5 返回用户id
        return customerInfo.getId();
    }

    /**
     * 根据客户ID获取客户登录信息。
     * <p>
     * 本方法通过查询客户详情，并将其转换为CustomerLoginVo对象返回，以供客户登录使用。
     * 主要包括客户的基本信息和是否绑定了手机号码的状态。
     *
     * @param customerId 客户的ID，用于查询特定客户的信息。
     * @return CustomerLoginVo 对象，包含客户登录所需的信息。
     */
    @Override
    public CustomerLoginVo getCustomerLoginInfo(Long customerId) {
        // 根据客户ID查询客户详细信息
        CustomerInfo customerInfo = this.getById(customerId);
        // 初始化CustomerLoginVo对象，用于存储将要返回的客户登录信息
        CustomerLoginVo customerInfoVo = new CustomerLoginVo();
        // 将查询到的客户基本信息复制到CustomerLoginVo对象中
        BeanUtils.copyProperties(customerInfo, customerInfoVo);
        // 判断客户是否绑定了手机号码，并设置到CustomerLoginVo对象中
        // 判断是否绑定手机号码，如果未绑定，小程序端发起绑定事件
        Boolean isBindPhone = StringUtils.hasText(customerInfo.getPhone());
        customerInfoVo.setIsBindPhone(isBindPhone);
        // 返回处理后的CustomerLoginVo对象
        return customerInfoVo;
    }

    /**
     * 更新微信用户的电话号码。
     *
     * 通过微信API获取用户的电话号码信息，并更新关联的客户信息。
     * 使用了SneakyThrows注解来静默处理可能的异常，确保方法的事务性。
     *
     * @param updateWxPhoneForm 包含用于更新电话号码的必要信息，如客户ID和微信授权码。
     * @return 返回布尔值，表示更新操作是否成功。
     */
    @SneakyThrows
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        // 使用微信API获取用户的电话号码信息
        // 调用微信 API 获取用户的手机号
        WxMaPhoneNumberInfo phoneInfo = wxMaService.getUserService().getPhoneNoInfo(updateWxPhoneForm.getCode());
        String phoneNumber = phoneInfo.getPhoneNumber();
        // 记录获取到的电话号码信息
        log.info("phoneInfo:{}", JSON.toJSONString(phoneInfo));

        // 创建一个新的客户信息对象，用于更新电话号码
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setId(updateWxPhoneForm.getCustomerId());
        customerInfo.setPhone(phoneNumber);
        // 更新客户信息，并返回更新结果
        return this.updateById(customerInfo);

    }
    @Override
    public String getCustomerOpenId(Long customerId) {
        CustomerInfo customerInfo = this.getOne(new LambdaQueryWrapper<CustomerInfo>().eq(CustomerInfo::getId, customerId).select(CustomerInfo::getWxOpenId));
        return customerInfo.getWxOpenId();
    }
}
