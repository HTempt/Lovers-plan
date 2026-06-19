package com.lovers.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovers.service.IWechatSubscribeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WechatSubscribeServiceImpl implements IWechatSubscribeService {

    private static final Logger log = LoggerFactory.getLogger(WechatSubscribeServiceImpl.class);
    private static final String ACCESS_TOKEN_KEY = "wechat:access_token";
    private static final long TOKEN_EXPIRE_SECONDS = 7000; // 稍小于2小时

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    @Value("${wechat.template.anniversary}")
    private String anniversaryTemplateId;

    @Value("${wechat.template.todo}")
    private String todoTemplateId;

    @Value("${wechat.template.task}")
    private String taskTemplateId;

    @Value("${wechat.template.wish}")
    private String wishTemplateId;

    @Value("${wechat.template.status}")
    private String statusTemplateId;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取 access_token（从缓存获取，如不存在则重新请求）
     */
    public String getAccessToken() {
        String cached = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (cached != null) {
            return cached;
        }
        return refreshAccessToken();
    }

    /**
     * 刷新 access_token
     */
    private String refreshAccessToken() {
        try {
            String url = "https://api.weixin.qq.com/cgi-bin/token"
                    + "?grant_type=client_credential"
                    + "&appid=" + appId
                    + "&secret=" + appSecret;

            Map response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("access_token")) {
                String token = (String) response.get("access_token");
                redisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, token, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
                return token;
            } else {
                log.error("Failed to refresh access_token: {}", response);
                return null;
            }
        } catch (Exception e) {
            log.error("Error refreshing access_token", e);
            return null;
        }
    }

    /**
     * 发送订阅消息
     */
    public boolean sendSubscribeMessage(String openid, String templateId, String page, Map<String, Object> data) {
        String token = getAccessToken();
        if (token == null) {
            log.warn("No access_token available, cannot send message");
            return false;
        }

        try {
            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + token;

            Map<String, Object> body = Map.of(
                    "touser", openid,
                    "template_id", templateId,
                    "page", page != null ? page : "pages/index/index",
                    "data", data
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            Map response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && Integer.valueOf(0).equals(response.get("errcode"))) {
                log.info("Subscribe message sent successfully to {}", openid);
                return true;
            } else {
                log.warn("Failed to send subscribe message: {}", response);
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending subscribe message", e);
            return false;
        }
    }

    /**
     * 发送纪念日提醒
     */
    public boolean sendAnniversaryReminder(String openid, String title, int daysLeft, String date) {
        Map<String, Object> data = Map.of(
                "thing1", Map.of("value", title),
                "date2", Map.of("value", date),
                "number3", Map.of("value", String.valueOf(daysLeft))
        );
        return sendSubscribeMessage(openid, anniversaryTemplateId, "pages/anniversary/anniversary", data);
    }

    /**
     * 发送待办到期提醒
     */
    public boolean sendTodoReminder(String openid, String title, String deadline) {
        Map<String, Object> data = Map.of(
                "thing1", Map.of("value", title),
                "date2", Map.of("value", deadline)
        );
        return sendSubscribeMessage(openid, todoTemplateId, "pages/todo/todo", data);
    }

    /**
     * 发送任务确认提醒
     */
    public boolean sendTaskConfirmReminder(String openid, String taskTitle, String partnerName) {
        Map<String, Object> data = Map.of(
                "thing1", Map.of("value", taskTitle),
                "thing2", Map.of("value", partnerName + "已打卡，请确认")
        );
        return sendSubscribeMessage(openid, taskTemplateId, "pages/task/task", data);
    }

    /**
     * 发送愿望截止提醒
     */
    public boolean sendWishDeadlineReminder(String openid, String wishTitle, String deadline) {
        Map<String, Object> data = Map.of(
                "thing1", Map.of("value", wishTitle),
                "date2", Map.of("value", deadline)
        );
        return sendSubscribeMessage(openid, wishTemplateId, "pages/wish/wish", data);
    }

    /**
     * 发送状态变更通知（给另一半）
     */
    public boolean sendStatusReminder(String openid, String partnerName, String statusName, String mood) {
        Map<String, Object> data = Map.of(
                "thing1", Map.of("value", partnerName),
                "thing2", Map.of("value", statusName),
                "thing3", Map.of("value", mood != null && !mood.isEmpty() ? mood : "无")
        );
        return sendSubscribeMessage(openid, statusTemplateId, "pages/index/index", data);
    }
}
