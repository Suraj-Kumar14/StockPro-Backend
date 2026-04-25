package com.stockpro.web.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiErrorResponse {

    private String error;
    private String message;
    private String path;
}
