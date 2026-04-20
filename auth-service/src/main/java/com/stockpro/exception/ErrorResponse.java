package com.stockpro.exception;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
	public LocalDateTime dateTime;
	public int status;
	public String error;
	public String message;
	public String path;
}
