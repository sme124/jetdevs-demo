package com.narola.jetdevstest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class APIErrorResponse {
    private String errMessage;
    private Exception errDetails;
}
