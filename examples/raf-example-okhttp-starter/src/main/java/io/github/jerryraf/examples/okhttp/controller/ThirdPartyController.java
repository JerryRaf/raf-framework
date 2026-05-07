package io.github.jerryraf.examples.okhttp.controller;

import com.raf.framework.autoconfigure.common.annotation.ResponseResult;
import io.github.jerryraf.examples.okhttp.service.ThirdPartyApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OkHttp 多渠道调用演示接口
 */
@RestController
@RequestMapping("/api/third-party")
@ResponseResult
@RequiredArgsConstructor
public class ThirdPartyController {

    private final ThirdPartyApiService thirdPartyApiService;

    /** 调用 GitHub API（GET，无认证） */
    @GetMapping("/github/users/{username}")
    public String getGithubUser(@PathVariable String username) {
        return thirdPartyApiService.getGithubUser(username);
    }

    /** 调用支付服务（POST JSON，Bearer Token） */
    @PostMapping("/payment")
    public String createPayment(@RequestBody String orderJson) {
        return thirdPartyApiService.createPayment(orderJson);
    }

    /** 调用短信服务（POST 表单，Basic 认证） */
    @PostMapping("/sms")
    public String sendSms(
        @RequestParam String phone,
        @RequestParam String content
    ) {
        return thirdPartyApiService.sendSms(phone, content);
    }
}
