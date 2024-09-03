package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/customer/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoController {

	@Autowired
	private CustomerInfoService customerInfoService;

	/**
	 * 通过客户ID获取客户基本信息。
	 * <p>
	 * 该接口使用@GetMapping注解，表明这是一个GET请求的处理方法。接口路径为/getCustomerInfo/{customerId}，其中{customerId}是一个路径变量，
	 * 用于接收并提取客户ID。方法返回Result<CustomerInfo>对象，其中CustomerInfo是客户信息的数据实体类，Result是对操作结果进行封装的类，
	 * 包含操作成功与否的状态和相关信息。
	 *
	 * @param customerId 客户ID，作为路径变量，通过@PathVariable注解绑定到方法参数上，用于指定要获取的客户信息的唯一标识。
	 * @return 返回一个Result对象，其中包含操作状态和客户信息。如果操作成功，Result的状态为OK，同时包含对应的客户信息；
	 *         如果操作失败，Result的状态为错误码，信息为空。
	 */
	@Operation(summary = "获取客户基本信息")
	@GetMapping("/getCustomerInfo/{customerId}")
	public Result<CustomerInfo> getCustomerInfo(@PathVariable Long customerId) {
	    // 调用customerInfoService的getById方法，通过customerId获取客户信息，并将结果封装进Result对象中返回。
	    return Result.ok(customerInfoService.getById(customerId));
	}

	/**
	 * 通过微信小程序的授权代码进行登录。
	 *
	 * 此接口接收微信小程序回调提供的授权代码，然后通过该代码与微信服务器交互，
	 * 获取用户的唯一标识符openId和sessionKey。进一步地，这些信息将用于识别和验证用户身份，
	 * 完成登录流程。
	 *
	 * @param code 微信小程序授权回调中提供的代码，用于换取用户标识符。
	 * @return 返回一个包含用户ID的结果对象。如果登录成功，结果对象中的数据部分将是用户的ID；
	 *         如果登录失败，结果对象中将包含错误信息。
	 */
	//微信小程序登录接口
	@Operation(summary = "小程序授权登录")
	@GetMapping("/login/{code}")
	public Result<Long> login(@PathVariable String code) {
	    return Result.ok(customerInfoService.login(code));
	}
	/**
	 * 通过客户ID获取客户登录信息。
	 * <p>
	 * 本接口提供了一个方法，用于根据客户的唯一标识符获取该客户的登录信息。
	 * 这对于需要验证客户身份或需要访问客户登录状态的场景非常有用。
	 *
	 * @param customerId 客户的唯一标识符，用于定位和检索特定客户的登录信息。
	 * @return 包含客户登录信息的结果对象。如果操作成功，结果对象将包含所需的登录信息；
	 *         如果操作失败，结果对象将包含错误代码和消息。
	 */
	@Operation(summary = "获取客户登录信息")
	@GetMapping("/getCustomerLoginInfo/{customerId}")
	public Result<CustomerLoginVo> getCustomerLoginInfo(@PathVariable Long customerId) {
	    // 调用客户信息服务，获取指定客户的登录信息
	    return Result.ok(customerInfoService.getCustomerLoginInfo(customerId));
	}
	/**
	 * 更新客户微信手机号码
	 *
	 * @param updateWxPhoneForm 包含更新微信手机号所需信息的表单对象
	 * @return 返回操作结果，成功为true，失败为false
	 */
	@Operation(summary = "更新客户微信手机号码")
	@PostMapping("/updateWxPhoneNumber")
	public Result<Boolean> updateWxPhoneNumber(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
	    // 调用客户信息服务层的方法来更新微信手机号码，并将结果包装在Result对象中返回
	    return Result.ok(customerInfoService.updateWxPhoneNumber(updateWxPhoneForm));
	}
	@Operation(summary = "获取客户OpenId")
	@GetMapping("/getCustomerOpenId/{customerId}")
	public Result<String> getCustomerOpenId(@PathVariable Long customerId) {
		return Result.ok(customerInfoService.getCustomerOpenId(customerId));
	}

}

