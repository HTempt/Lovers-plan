package com.lovers.service;

import java.util.Map;

public interface IWechatSubscribeService {
    String getAccessToken();
    boolean sendSubscribeMessage(String openid, String templateId, String page, Map<String, Object> data);
    boolean sendAnniversaryReminder(String openid, String title, int daysLeft, String date);
    boolean sendTodoReminder(String openid, String title, String deadline);
    boolean sendTaskConfirmReminder(String openid, String taskTitle, String partnerName);
    boolean sendWishDeadlineReminder(String openid, String wishTitle, String deadline);
    boolean sendStatusReminder(String openid, String partnerName, String statusName, String mood);
}
