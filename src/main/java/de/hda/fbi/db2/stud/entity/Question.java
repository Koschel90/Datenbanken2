package de.hda.fbi.db2.stud.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.Objects;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Id;


/**
 * Question Class.
 */

@Entity
@Table(name = "Question", schema = "myschema")
public class Question {

    @Id
    private int id;
    @Column(nullable = false)
    private String name;

    @ManyToOne
    private Category categoryFromQuestion;

    @ElementCollection
    @OrderColumn
    @CollectionTable(name = "Question_Answers", joinColumns = @JoinColumn(name = "Question_id"),
        schema = "myschema"
    )
    private List<String> answers = new ArrayList();

    @Column(nullable = false)
    private int correctAnswer;

    @ManyToMany(mappedBy = "selectedQuestion")
    private List<Match> matches = new ArrayList<Match>();

    public Question () { // Default constructor
    }

    Question(int id, String name, ArrayList<String> data, String rightAnswer) {
        this.name = name;
        answers.add(data.get(0));
        answers.add(data.get(1));
        answers.add(data.get(2));
        answers.add(data.get(3));
        int tmp = Integer.parseInt(rightAnswer);
        correctAnswer = tmp;
        this.id = id;
    }

    public Collection<Match> getMyMatches(){
        return  matches;
    }

    public String getName() {
        return name;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public int getId() {
        return id;
    }

    public int getCorrectAnswer() {
        return correctAnswer;
    }

    public void output() {
        System.out.println(name);
    }

    public void setCategoryFromQuestion(Category categoryFromQuestion) {
        this.categoryFromQuestion = categoryFromQuestion;
    }

    public Category getCategoryFromQuestion() {
        return categoryFromQuestion;
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
        Question question = (Question) o;
        return id == question.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
