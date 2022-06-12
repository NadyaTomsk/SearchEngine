package main.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class ResultStatistics {
    @Setter
    @Getter
    private boolean result = true;
    @Getter
    @Setter
    private ResultStatisticsItem statistics;

    public ResultStatistics(ResultData resultData) {
        statistics = resultData.getStatistics();
    }

}
