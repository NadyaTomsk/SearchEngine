package main.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class ResultError {
    @Getter
    private boolean result = false;
    @Setter
    @Getter
    private String error;

    public ResultError(ResultData resultData) {
        error = resultData.getError();
    }

    public ResultError(String text) {
        error = text;
    }
}
