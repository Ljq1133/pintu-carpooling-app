package com.pintu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoPointVO {
    private String name;
    private String address;
    private BigDecimal lng;
    private BigDecimal lat;
}
