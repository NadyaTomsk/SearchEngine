package main.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

public class ResultSearch {
    @Setter
    @Getter
    private boolean result;
    @Setter
    @Getter
    private long count;
    @Getter
    private Set<ResultSearchItem> data;

    public void setData(Set<ResultSearchItem> set) {
        data = Set.copyOf(set);
    }

    public void addData(Set<ResultSearchItem> set) {
        data.addAll(set);
    }

    public ResultSearch(ResultData data, int limit) {
        result = data.isResult();
        this.data = Set.copyOf(data.getSearchResult().stream().limit(limit).toList());
        count = data.getCountSearchResults();
    }

    public ResultSearch() {
    }
}
