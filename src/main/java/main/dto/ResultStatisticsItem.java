package main.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
public class ResultStatisticsItem {
    @Getter
    @Setter
    private Total total;
    @Getter
    @Setter
    private List<Detailed> detailed = new ArrayList<>();

    @Data
    @ToString
    public static class Total {
        @Getter
        @Setter
        private long sites;
        @Getter
        @Setter
        private long pages;
        @Getter
        @Setter
        private long lemmas;
        @Getter
        @Setter
        private boolean isIndexing;
    }

    @Data
    public static class Detailed {
        @Getter
        @Setter
        private String url;
        @Getter
        @Setter
        private String name;
        @Getter
        @Setter
        private String status;
        @Getter
        @Setter
        private Long statusTime;
        @Getter
        @Setter
        private String error;
        @Getter
        @Setter
        private long pages;
        @Getter
        @Setter
        private long lemmas;
    }
}
