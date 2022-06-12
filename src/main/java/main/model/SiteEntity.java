package main.model;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;

@Entity
@Table(name = "site")
public class SiteEntity {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Setter
    @Getter
    @Nullable
    @Enumerated(EnumType.STRING)
    //@Column(columnDefinition = "enum")
    private StatusType status;
    @Getter
    @Setter
    @Column(name = "status_time")
    private Long statusTime;
    @Setter
    @Getter
    @Column(name = "last_error", length = 65535, columnDefinition = "TEXT")
    private String lastError;
    @Setter
    @Getter
    private String url;
    @Setter
    @Getter
    private String name;
}
