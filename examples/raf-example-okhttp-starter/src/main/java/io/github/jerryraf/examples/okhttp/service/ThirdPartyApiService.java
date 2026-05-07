package io.github.jerryraf.examples.okhttp.service;

import com.raf.framework.autoconfigure.common.exception.InfrastructureException;
import com.raf.framework.okhttp.HttpExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * 第三方 API 调用服务
 * 演示多渠道 OkHttp 客户端的使用
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThirdPartyApiService {

    private final HttpExecutor httpExecutor;

    /**
     * 调用 GitHub API 获取用户信息（GET，无认证）
     */
    public String getGithubUser(String username) {
        String url = "https://api.github.com/users/" + username;
        try (Response response = httpExecutor.get("github", url)) {
            if (!response.isSuccessful()) {
                throw new InfrastructureException("GitHub API 调用失败，状态码: " + response.code());
            }
            String body = Objects.requireNonNull(response.body()).string();
            log.info("GitHub 用户信息获取成功，username={}", username);
            return body;
        } catch (IOException e) {
            throw new InfrastructureException("GitHub API 网络异常", e);
        }
    }

    /**
     * 调用支付服务创建支付单（POST JSON，Bearer Token 认证）
     */
    public String createPayment(String orderJson) {
        String url = "https://payment.internal/api/v1/payments";
        try (Response response = httpExecutor.postJson("payment", url, orderJson)) {
            if (!response.isSuccessful()) {
                throw new InfrastructureException("支付服务调用失败，状态码: " + response.code());
            }
            String body = Objects.requireNonNull(response.body()).string();
            log.info("支付单创建成功");
            return body;
        } catch (IOException e) {
            throw new InfrastructureException("支付服务网络异常", e);
        }
    }

    /**
     * 调用短信服务发送短信（POST 表单，Basic 认证）
     */
    public String sendSms(String phone, String content) {
        String url = "https://sms.provider.com/api/send";
        Map<String, String> form = Map.of(
            "phone", phone,
            "content", content,
            "sign", "RAF"
        );
        try (Response response = httpExecutor.postForm("sms", url, form)) {
            if (!response.isSuccessful()) {
                throw new InfrastructureException("短信服务调用失败，状态码: " + response.code());
            }
            String body = Objects.requireNonNull(response.body()).string();
            log.info("短信发送成功，phone={}", phone);
            return body;
        } catch (IOException e) {
            throw new InfrastructureException("短信服务网络异常", e);
        }
    }

    /**
     * 带自定义请求头的 GET 请求
     */
    public String getWithHeaders(String channel, String url, Map<String, String> headers) {
        try (Response response = httpExecutor.get(channel, url, headers, null)) {
            if (!response.isSuccessful()) {
                throw new InfrastructureException("API 调用失败，状态码: " + response.code());
            }
            return Objects.requireNonNull(response.body()).string();
        } catch (IOException e) {
            throw new InfrastructureException("API 网络异常", e);
        }
    }
}
