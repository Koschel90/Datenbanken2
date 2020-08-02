package de.hda.fbi.db2.stud.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.Objects;
import javax.persistence.*;
/**
 * Category Class.
 */

@Entity
@Table(name = "Category", schema = "myschema")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(unique = true, nullable = false)
    private String name;

    @OneToMany(mappedBy = "categoryFromQuestion", cascade = { CascadeType.PERSIST })
    private List<Question> questionList = new ArrayList();

    @ManyToMany(mappedBy = "selectedCategory")
    private List<Match> matches = new ArrayList<Match>();

    public Category(){// Default Constructor

    }

    public Category(List<Question> questions, String name) {
        this.name = name;
        this.questionList = questions;
    }

    public int getId() {
        return id;
    }

    public List<Question> getQuestionList() {
        return questionList;
    }

    public String getName() {
        return name;
    }

    public void output() {
        for (int i = 0; i < questionList.size(); i++) {
            questionList.get(i).output();
        }
    }

    public void setMatch(Match matchObj){
      matches.add(matchObj);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Category category = (Category) o;
        return id == category.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

