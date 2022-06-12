package main.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;

@Entity
@Table(name = "pages", uniqueConstraints = {@UniqueConstraint(columnNames = {"url_path", "site_id"})},
        indexes = {@Index(name = "index_pagePath", columnList = "url_path")})
@SQLInsert(sql = "INSERT INTO pages (code,  content, url_path,site_id,id) VALUES (?,?,?,?,?) as new(a,b,c,d,e) ON DUPLICATE KEY UPDATE content = new.b, code = new.a")
public class PageEntity implements Comparable<PageEntity> {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private int id;
    @Getter
    @Setter
    @Column(name = "url_path")
    private String path;
    @Getter
    @Setter
    @Column(length = 16777215, columnDefinition = "mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;
    @Getter
    @Setter
    private int code;
    @Getter
    @Setter
    @ManyToOne(cascade = CascadeType.MERGE, optional = false)
    @JoinColumn(nullable = false, name = "site_id", referencedColumnName = "id", updatable = false)
    private SiteEntity site;

    @Getter
    @Setter
    @Column(name = "percent_rank", insertable = false)
    private float percentRank;
    @Getter
    @Setter
    @Column(name = "lemma_list", insertable = false)
    private String lemmaList;

    public static int compare(PageEntity o1, PageEntity o2) {
        return o1.getPath().compareTo(o2.getPath());
    }

    @Override
    public int compareTo(PageEntity o) {
        return this.path.compareTo(o.getPath());
    }

    @Override
    public String toString() {
        return "PageEntity{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", code=" + code +
                ", siteId=" + site.getId() +
                ", siteUrl=" + site.getUrl() +
                '}';
    }
}
