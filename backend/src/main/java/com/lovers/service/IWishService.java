package com.lovers.service;

import com.lovers.model.Wish;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IWishService {
    Wish create(Long coupleId, String title, String category, BigDecimal targetAmount, LocalDate targetDate);
    Wish updateProgress(Long wishId, BigDecimal currentAmount);
    Wish achieve(Long wishId);
    List<Wish> listByCouple(Long coupleId);
}
