package main.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@NoArgsConstructor
public class ResultSearchItem implements Comparable<ResultSearchItem> {

    @Getter
    @Setter
    private String site;
    @Getter
    @Setter
    private String siteName;
    @Getter
    @Setter
    private String uri;
    @Getter
    @Setter
    private String title;
    @Getter
    @Setter
    private String snippet;
    @Getter
    @Setter
    private float relevance;

    @Override
    public int compareTo(@NotNull ResultSearchItem o) {
        if (this.getRelevance() == o.getRelevance()) {
            return uri.compareTo(o.uri);
        }
        return (-1) * Float.compare(this.getRelevance(), o.getRelevance());
    }

}
