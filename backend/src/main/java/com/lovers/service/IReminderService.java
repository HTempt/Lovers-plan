package com.lovers.service;

public interface IReminderService {
    void checkAnniversaryReminders();
    void checkTodoDeadlineReminders();
    void checkTaskPendingConfirmation();
    void checkWishDeadlineReminders();
}
