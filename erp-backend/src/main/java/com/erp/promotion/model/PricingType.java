package com.erp.promotion.model;

public enum PricingType {
    PERCENT_DISCOUNT, // Giảm theo % giá gốc (vd: mua >= 10 cái giảm 15%)
    FIXED_PRICE       // Giá sỉ cố định (vd: mua >= 50 cái giá 180.000đ/cái)
}
