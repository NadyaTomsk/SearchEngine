package main.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@ToString
@Entity
@Table(name = "fields")
public class FieldEntity {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Getter
    @Setter
    @Column(nullable = false)
    private String name;
    @Getter
    @Setter
    @Column(nullable = false)
    private String selector;
    @Getter
    @Setter
    @Column(nullable = false)
    private float weight;

}
