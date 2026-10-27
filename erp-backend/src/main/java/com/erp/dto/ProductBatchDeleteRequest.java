package com.erp.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductBatchDeleteRequest {
    private List<Long> ids;
}

