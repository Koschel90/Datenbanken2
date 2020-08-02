package de.hda.fbi.db2.stud.entity;

import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
//import java.util.Date;

import java.util.Objects;
import java.util.Scanner;
import javax.persistence.*;


/**
 * Match Class.
 */

@Entity
@Table(name = "Match", schema = "myschema")
@NamedQueries({
    @NamedQuery(name = "Match.numberOfMatches",
        query = "select count(m.id) from Match m")})
public class Match {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @Temporal(TemporalType.DATE)
  @Column(nullable = false)
  private Date matchStart = new Date();
  @Temporal(TemporalType.DATE)
  private Date matchEnd = new Date();

  private int numberOfCorrectAnswers;

  @ManyToOne
  @JoinColumn(nullable = false)
  private Player playingGamer;

  @ManyToMany
  @JoinTable(name = "Match_Question",
      joinColumns = @JoinColumn(name = "Match_id"),
      inverseJoinColumns = @JoinColumn(name = "Question_id"),
      schema = "myschema"
  )
  private List<Question> selectedQuestion = new ArrayList<Question>();
  @ManyToMany
  @JoinTable(name = "Match_Category",
      joinColumns = @JoinColumn(name = "Match_id"),
      inverseJoinColumns = @JoinColumn(name = "Category_id"),
      schema = "myschema"
  )
  private List<Category> selectedCategory = new ArrayList<Category>();

  @ElementCollection
  @OrderColumn
  @CollectionTable(name = "Match_Answers", joinColumns = @JoinColumn(name = "Match_id"),
      schema = "myschema"
  )
  private List<Integer> selectedAnswers = new ArrayList<Integer>();


  public Match(){
    this.numberOfCorrectAnswers = 0;
  }

  public Match(Player playerObj){
    this.playingGamer = playerObj;
    this.numberOfCorrectAnswers = 0;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setSelectedQuestion(List<Question> selectedQuestion) {
    this.selectedQuestion = selectedQuestion;
  }

  public void setSelectedCategory(List<Category> selectedCategory) {
    this.selectedCategory = selectedCategory;
  }

  public int getNumberOfCorrectAnswers() {
    return numberOfCorrectAnswers;
  }

  public void setNumberOfCorrectAnswers(int numberOfCorrectAnswers) {
    this.numberOfCorrectAnswers = numberOfCorrectAnswers;
  }

  public List<Question> getSelectedQuestion() {
    return selectedQuestion;
  }

  public void setMatchStart(Date matchStart) {
    this.matchStart = new Date(matchStart.getTime());
  }

  public void setMatchEnd(Date matchEnd) {
    this.matchEnd = new Date(matchEnd.getTime());
  }

  public List<Category> getSelectedCategory() {
    return selectedCategory;
  }

  public void play(){
    int counter = 0;
    System.out.println("######## QUIZGAME - Play ########");
    for (Question q : selectedQuestion){
      counter++;
      System.out.print("Question " + counter + " from ");
      System.out.println(selectedQuestion.size() + ": " + q.getName());
      outputAnswersOfQuestion(q);
      boolean chosenRightAnswer = choseAnswer(q);
      if (chosenRightAnswer){
        numberOfCorrectAnswers++;
      }
    }

    outputResult(numberOfCorrectAnswers, selectedQuestion.size());
    Date tmp = new Date();
    this.matchEnd = tmp;
}

  public void outputResult(int numberOfRightAnswers, int numberOfQuestions){
    System.out.print("Result: you answered " + numberOfRightAnswers);
    System.out.println(" out of " +  numberOfQuestions + " correctly");
  }

  void outputAnswersOfQuestion(Question q){
    int answerCounter = 1;
    for (String a : q.getAnswers()){
      System.out.println("Answer " + answerCounter + ":" + a);
      answerCounter++;
    }
  }

  public boolean choseAnswer(Question q){
    Scanner inputValue = null;
    int input = -1;
    while (input < 1 || input > 4){
      try {
        inputValue = new Scanner(new InputStreamReader(System.in, "utf-8"));
        input = inputValue.nextInt();
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      if (input < 1 || input > 4){
        System.out.println("number is out of range!");
      }
    }
    addSelectedAnswer(input);
    return controlAnswer(q, input);
  }

  void addSelectedAnswer(int answer){
    selectedAnswers.add(answer);
  }

  public boolean controlAnswer(Question q, int selectedAnswer){
    if (q.getCorrectAnswer() == selectedAnswer){
      System.out.println("Answer is correct!");
      return true;
    } else {
      System.out.print("Answer is wrong - The right answer is: ");
      List<String> answers = q.getAnswers();
      int correctAnswerNumber = q.getCorrectAnswer();
      String correctAnswer = answers.get(correctAnswerNumber - 1);
      System.out.println(correctAnswer);
    }
    return false;
  }

  public void outputAllQuestions(){
    for (Question q : selectedQuestion){
      q.output();
    }
  }

  public int getId() {
    return id;
  }

  public Date getMatchStart() {
    return new Date(matchStart.getTime());
  }

  public Date getMatchEnd() {
    return new Date(matchEnd.getTime());
  }

  public List<Integer> getSelectedAnswers() {
    return selectedAnswers;
  }

  public Player getPlayingGamer() {
    return playingGamer;
  }

  public void setPlayingGamer(Player playingGamer) {
    this.playingGamer = playingGamer;
  }

  public void setSelectedAnswers(List<Integer> selectedAnswers) {
    this.selectedAnswers = selectedAnswers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Match match = (Match) o;
    return id == match.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
