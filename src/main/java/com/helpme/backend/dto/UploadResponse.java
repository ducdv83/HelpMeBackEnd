package com.helpme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private String url; // URL để hiển thị: /v1/files/orders/abc.jpg
    private String path; // Relative path để lưu DB: orders/abc.jpg
    private String filename; // Tên file gốc
    private long size; // Kích thước file (bytes)
}