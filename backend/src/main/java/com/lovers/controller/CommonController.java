package com.lovers.controller;

import com.lovers.common.Result;
import com.xhinliang.lunarcalendar.LunarCalendar;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/common")
public class CommonController {

    /**
     * 获取当前农历日期
     * GET /api/common/lunar
     */
    @GetMapping("/lunar")
    public Result<Object> getLunarDate() {
        LocalDate now = LocalDate.now();
        LunarCalendar lunar = LunarCalendar.obtainCalendar(
                now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        return Result.success("农历" + lunar.getLunarMonth() + "月" + lunar.getLunarDay());
    }
}
