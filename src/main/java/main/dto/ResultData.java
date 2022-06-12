package main.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.TreeSet;

@Data
public class ResultData {
    @Setter
    @Getter
    private boolean result;
    @Getter
    @Setter
    private ResultStatisticsItem statistics;
    @Setter
    @Getter
    private String error;
    @Setter
    private Set<ResultSearchItem> searchResult = new TreeSet<>();
    @Setter
    @Getter
    private long countSearchResults;

    public void setSearchResult(Set<ResultSearchItem> set) {
        searchResult = Set.copyOf(set);
    }

}
