package com.example.demo.dto;

import java.math.BigDecimal;

public interface DailyRevenue {
    String getDate();
    BigDecimal getRevenue();
    Long getOrderCount();
}
