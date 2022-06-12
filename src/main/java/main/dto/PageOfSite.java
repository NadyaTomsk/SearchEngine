package main.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Comparator;

@AllArgsConstructor
@NoArgsConstructor
public class PageOfSite implements Comparator<PageOfSite> {
    @Getter
    private int id;
    @Getter
    @Setter
    private String path;
    @Getter
    @Setter
    private String content;
    @Getter
    @Setter
    private int code;
    @Getter
    @Setter
    private int siteId;
    @Getter
    @Setter
    private String error;

    @Override
    public int compare(PageOfSite o1, PageOfSite o2) {
        return o1.getPath().compareTo(o2.getPath());
    }

}
