package com.lms.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Dữ liệu biểu mẫu chấm điểm; các ràng buộc được kiểm tra trước khi gọi service.
public class GradeForm {

    // Điểm bắt buộc, không âm và tối đa hai chữ số thập phân.
    @NotNull(message = "Vui lòng nhập điểm")
    @DecimalMin(value = "0.0", message = "Điểm không được âm")
    @Digits(integer = 5, fraction = 2, message = "Điểm tối đa có 2 chữ số thập phân")
    private BigDecimal score;

    // Nhận xét không bắt buộc, giới hạn độ dài để phù hợp với dữ liệu lưu trữ.
    @Size(max = 5000, message = "Nhận xét không được vượt quá 5000 ký tự")
    private String feedback;

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
