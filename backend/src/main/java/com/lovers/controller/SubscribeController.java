package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscribe")
public class SubscribeController {

    @Value("${wechat.template.status}")
    private String statusTemplateId;

    @Value("${wechat.template.anniversary}")
    private String anniversaryTemplateId;

    @Value("${wechat.template.todo}")
    private String todoTemplateId;

    @Value("${wechat.template.task}")
    private String taskTemplateId;

    @Value("${wechat.template.wish}")
    private String wishTemplateId;

    /**
     * 获取可订阅的模板列表
     * GET /api/subscribe/templates
     */
    @GetMapping("/templates")
    public Result<List<Map<String, Object>>> getSubscribeTemplates() {
        UserContext.getUserId(); // 确保已登录
        List<Map<String, Object>> templates = List.of(
                Map.of(
                        "id", "status",
                        "name", "对方状态通知",
                        "desc", "当对方更新状态时向你发送提醒",
                        "templateId", statusTemplateId,
                        "icon", "🔔"
                ),
                Map.of(
                        "id", "anniversary",
                        "name", "纪念日提醒",
                        "desc", "纪念日临近时发送提醒",
                        "templateId", anniversaryTemplateId,
                        "icon", "💝"
                ),
                Map.of(
                        "id", "todo",
                        "name", "待办到期提醒",
                        "desc", "待办事项到期前发送提醒",
                        "templateId", todoTemplateId,
                        "icon", "✅"
                ),
                Map.of(
                        "id", "task",
                        "name", "任务确认提醒",
                        "desc", "对方打卡后提醒你确认",
                        "templateId", taskTemplateId,
                        "icon", "🎯"
                ),
                Map.of(
                        "id", "wish",
                        "name", "愿望截止提醒",
                        "desc", "愿望目标到期前发送提醒",
                        "templateId", wishTemplateId,
                        "icon", "✨"
                )
        );
        return Result.success(templates);
    }
}
