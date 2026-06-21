package com.cropdeal.adminservice.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ReportFilterRequest {

    private String status;      // order or payment status filter
    private String cropType;    // VEGETABLE, FRUIT, GRAIN, OTHER
    private Long dealerId;
    private Long farmerId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;
}
