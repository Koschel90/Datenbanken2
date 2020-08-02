package de.hda.fbi.db2.stud.entity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.hda.fbi.db2.tools.CsvDataReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;


/**
 * Quizgame Class.
 */

public class QuizGame {

  private ArrayList<Player> playerList = new ArrayList<>();
  private ArrayList<Category> categoryList = new ArrayList<>();
  private String nameOfActivePlayer;
  private List<String[]> data;

  private static EntityManagerFactory emFactory =
      Persistence.createEntityManagerFactory("postgresPU");

  public QuizGame(List<String[]> data) {
    this.data = data;
    databaseQuery();
  }

  private void databaseQuery() {
    EntityManager em = emFactory.createEntityManager();
    try {
      //category:
      List categoryResult = em.createQuery("select c from Category c").getResultList();
      this.categoryList = new ArrayList<>(categoryResult);
      //User:
      List playerResult = em.createQuery("select p from Player p").getResultList();
      this.playerList = new ArrayList<>(playerResult);
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      em.close();
    }
  }

  private void initializePlayer() {
    EntityManager em = emFactory.createEntityManager();
    EntityTransaction et = null;
    try {
      et = em.getTransaction();
      et.begin(); //open transaction

      //set players:-
      Player obj1 = new Player("Beti");
      Player obj2 = new Player("Fredde");
      playerList.add(obj1);
      playerList.add(obj2);
      //persist:
      em.persist(obj1);
      em.persist(obj2);

      et.commit(); //sending to DB
    } catch (Exception ex) {
      if (et != null) {
        et.rollback();
      }
      ex.printStackTrace();
    } finally {
      em.close();
    }
  }

  private void initializeQuestionAndCategoryFromFile() {
    EntityManager em = emFactory.createEntityManager();
    EntityTransaction et = null;
    try {
      et = em.getTransaction();
      et.begin(); //open transaction

      //Sort by category:
      ListMultimap<String, Question> multimap = ArrayListMultimap.create();
      //Add data

      for (int i = 1; i < data.size(); i++) {
        ArrayList<String> answerList = new ArrayList<>();
        answerList.add(data.get(i)[2]);
        answerList.add(data.get(i)[3]);
        answerList.add(data.get(i)[4]);
        answerList.add(data.get(i)[5]);
        int id = Integer.parseInt(data.get(i)[0]);
        Question obj = new Question(id, data.get(i)[1], answerList, data.get(i)[6]);
        boolean value = multimap.put(data.get(i)[7], obj);
      }

      //transfer to category
      for (String key : multimap.keySet()) {
        List<Question> questionList = multimap.get(key);
        Category obj = new Category(questionList, key);
        //add foreign key in all question objects:
        for (Question q : questionList) {
          q.setCategoryFromQuestion(obj);
          //persist:
          em.persist(q);
        }
        //persist:
        em.persist(obj);
        categoryList.add(obj);
      }
      et.commit(); //sending to DB
    } catch (Exception ex) {
      if (et != null) {
        et.rollback();
      }
      ex.printStackTrace();
    } finally {
      em.close();
    }
  }

  private void createPlayers(List<String[]> players, int matchNumber) {
    int batchSize = 25;

    EntityManager em = emFactory.createEntityManager();
    EntityTransaction et = null;
    try {
      et = em.getTransaction();
      et.begin(); //open transaction

      for (int i = 0; i < players.size(); i++) {
        Player player = new Player(players.get(i)[0]);
        playerList.add(player); //save player

        //create matches:
        createMatches(player, matchNumber);
        //player persist - last index of playerList:
        em.persist(playerList.get(playerList.size() - 1));

        if (i > 0 && i % batchSize == 0) {
          em.flush();
          em.clear();
          et.commit();
          et.begin();
        }
      }

      et.commit(); //sending to DB
    } catch (Exception ex) {
      if (et != null) {
        et.rollback();
      }
      ex.printStackTrace();
    } finally {
      em.close();
    }
  }

  private void createMatches(Player player, int matchNumber) {
    List<Match> matchList = new ArrayList<>();
    for (int i = 0; i < matchNumber; i++) {
      Match matchObj = initializeMatch(player);
      matchList.add(matchObj);
    }
    for (Match m : matchList) { //save matches
      playerList.get(playerList.size() - 1).setNewMatch(m);
    }
  }

  //Important: categoryCounter is 3 =>  Match with 3 category

  private Match initializeMatch(Player player) {
    Match matchObj = new Match(player); //new Match

    matchObj = setDates(matchObj);

    List<Category> category = setCategoryForNewMatch();
    matchObj.setSelectedCategory(category);
    //generate questions:
    List<Question> selectedQuestion = generateQuestions(category);
    Collections.shuffle(selectedQuestion, new Random()); //random sort:
    matchObj.setSelectedQuestion(selectedQuestion);
    //generate answers:
    matchObj = generateSelectedAnswers(matchObj, selectedQuestion.size());
    //save matchObj in Category and Question:
    //saveMatchInCategoryAndQuestion(matchObj);
    return matchObj;
  }

  public void saveMatchInCategoryAndQuestion(Match matchObj) {
    List<Category> category = matchObj.getSelectedCategory();
    for (Category matchCategory : category) {
      for (Category c : categoryList) {
        if (matchCategory.getName().equals(c.getName())) {
          c.setMatch(matchObj);
          //save in question:
          List<Question> matchQuestions = matchObj.getSelectedQuestion();
          for (Question matchQuestion : matchQuestions) {
            for (Question q : c.getQuestionList()) {
              if (matchQuestion.getName().equals(q.getName())) {
                q.setMatch(matchObj);
                break;
              }
            }
          }
          break;
        }
      }
    }
  }

  public Match generateSelectedAnswers(Match matchObj, int answerNumber) {
    int correctAnswer = 0;
    for (int i = 0; i < answerNumber; i++) {
      int min = 1;
      int max = 4;
      int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
      matchObj.addSelectedAnswer(randomNum);
      //correctAnswer?:
      if (matchObj.getSelectedQuestion().get(i).getCorrectAnswer() == randomNum){
        correctAnswer++;
      }
    }
    matchObj.setNumberOfCorrectAnswers(correctAnswer);
    return matchObj;
  }


  private Match setDates(Match matchObj) {

    SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd");

    long offset = Timestamp.valueOf("2019-01-01 00:00:00").getTime();
    long end = Timestamp.valueOf("2000-01-01 00:00:00").getTime();
    long diff = end - offset + 1;
    Timestamp rand = new Timestamp(offset + (long) (Math.random() * diff));

    try {
      Date date = df.parse(rand.toString());

      matchObj.setMatchStart(date);
      matchObj.setMatchEnd(date);

    } catch (ParseException e) {
      e.printStackTrace();
    }

    return matchObj;
  }


  private List<Category> setCategoryForNewMatch() {
    //generate categoryNumber:
    int min = 2;
    int max = 5;
    int minCategory = 0;
    int maxCategory = 50;
    List<Category> category = new ArrayList<>();
    int randomCategoryNum = ThreadLocalRandom.current().nextInt(min, max + 1);
    for (int i = 0; i < randomCategoryNum; i++) {
      int randomCategory = ThreadLocalRandom.current().nextInt(minCategory, maxCategory + 1);
      if (findCategory(category, randomCategory)) {//duplicate
        i--;
      } else {
        category.add(categoryList.get(randomCategory));
      }
    }
    return category;
  }

  private boolean findCategory(List<Category> category, int index) {
    boolean control = false;
    for (Category c : category) {
      if (c.equals(categoryList.get(index))) {
        control = true;
        break;
      }
    }
    return control;
  }

  private List<String[]> readDataOfPlayers(String file) {
    List<String[]> names = null;
    try {
      names = CsvDataReader.read(file);
    } catch (URISyntaxException use) {
      System.out.println(use);
    } catch (IOException ioe) {
      System.out.println(ioe);
    }
    return names;
  }

  public void quizMenu() {
    int input;
    boolean wasNotCalling = true;
    try {
      do {
        System.out.println("----------------------------------------------------------");
        Scanner inputValue = new Scanner(new InputStreamReader(System.in, "utf-8"));

        System.out.println("---- Please choose: ----");
        System.out.println("1. log in");
        System.out.println("2. register");
        System.out.println("-------------------------");
        System.out.println("3. output by category");
        System.out.println("4. input data of 10000 old players - with drop&create");
        System.out.println("5. input data of category and question - with drop&create");
        System.out.println("-------------------------");
        System.out.println("6. analysis of the game data");
        System.out.println("-------------------------");
        System.out.println("0. exit program");
        input = inputValue.nextInt();
        switch (input) {
          case 1:
            logIn();
            break;
          case 2:
            register();
            break;
          case 3:
            outputByCategory();
            break;
          case 4:
            if (wasNotCalling) {
              List<String[]> names10000 = readDataOfPlayers("10000Names.csv");
              int matchNumber = 100;
              createPlayers(names10000, matchNumber);
              wasNotCalling = false;
            } else {
              System.out.println("was already called");
            }
            break;
          case 5:
            initializePlayer();
            initializeQuestionAndCategoryFromFile();
            break;
          case 6:
            analyseMenu();
            break;
          case 0:
            System.out.println("The program is ended!");
            //close Entity manager factory:
            emFactory.close();
            break;
          default:
            System.out.println("Please enter again!");
            break;
        }
      } while (input != 0);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  private void register() {
    System.out.println("----------------------------------------------------------");
    try {
      Scanner inputValue = new Scanner(new InputStreamReader(System.in, "utf-8"));
      System.out.println("Enter a name:");
      String name = inputValue.next();
      if (getPlayer(name) != null) {
        System.out.println("This name is already known!");
      } else {
        createNewPlayer(name);
        System.out.println("New user was created!");
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

  }

  private void logIn() {
    try {
      Scanner inputValue = new Scanner(new InputStreamReader(System.in, "utf-8"));
      System.out.println("Enter your name:");
      String name = inputValue.next();
      if (getPlayer(name) != null) {
        playerOptions(name);
      } else {
        System.out.println("Name is unknown!");
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  private void playerOptions(String name) {
    this.nameOfActivePlayer = name;
    int input;
    try {
      do {
        System.out.println("----------------------------------------------------------");

        Scanner inputValue = new Scanner(new InputStreamReader(System.in, "utf-8"));

        System.out.println("--- Please choose: ---");
        System.out.println("1. new Match");
        System.out.println("0. log out");
        input = inputValue.nextInt();
        switch (input) {
          case 1:
            Player playerObj = getPlayer(name);
            newMatchWithPersist(playerObj);
            break;
          case 0:
            logOut();
            System.out.println("You are logged out!");
            break;
          default:
            System.out.println("Please enter again!");
            break;
        }
      } while (input != 0);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  private Player getPlayer(String name) {
    for (Player s : playerList) {
      if (s.getName().equals(name)) {
        return s;
      }
    }
    return null;
  }

  private void createNewPlayer(String name) {
    //define entity manager:
    EntityManager em = emFactory.createEntityManager();
    em.getTransaction().begin(); //open transaction

    Player obj = new Player(name);
    playerList.add(obj);
    //persist:
    em.persist(obj);

    em.getTransaction().commit(); //sending to DB
    em.close();
  }

  private void logOut() {
    this.nameOfActivePlayer = "empty";
  }

  private void newMatchWithPersist(Player playerObj) {
    //define entity manager:
    EntityManager em = emFactory.createEntityManager();
    em.getTransaction().begin(); //open transaction
    Match matchObj = new Match(playerObj); //new Match
    //preparation:
    matchObj = matchPreparation(matchObj);
    //playing:
    matchObj.play();
    //save Match:
    saveMatchInUser(matchObj);
    //saveMatchInCategoryAndQuestion(matchObj);
    //Persist:
    em.persist(matchObj);
    em.getTransaction().commit(); //sending to DB
    em.close();
  }

  private void saveMatchInUser(Match obj) {
    for (Player p : playerList) {
      if (p.getName().equals(nameOfActivePlayer)) {
        p.setNewMatch(obj);
        break;
      }
    }
  }

  private void outputByCategory() {
    System.out.println("----------------------------------------------------------");
    try {
      Scanner input = new Scanner(new InputStreamReader(System.in, "utf-8"));

      System.out.println("Please enter a category: ");
      String inputCategory = input.nextLine();
      //Output:
      for (Category c : categoryList) {
        if (c.getName().contains(inputCategory)) {
          c.output();
          break;
        }
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }


  private Match matchPreparation(Match matchObj) {
    System.out.println("########QUIZGAME########");
    List<Category> selectedCategory = selectCategory();
    matchObj.setSelectedCategory(selectedCategory);
    List<Question> selectedQuestion = generateQuestions(selectedCategory);
    //random sort:
    Collections.shuffle(selectedQuestion, new Random());
    //add questions:
    matchObj.setSelectedQuestion(selectedQuestion);
    return matchObj;
  }

  private List<Question> generateQuestions(List<Category> selectedCategory) {
    List<Question> generatedQuestions = new ArrayList<>();
    //adding all Questions:
    for (Category c : selectedCategory) {
      generatedQuestions = addQuestion(c.getQuestionList(), generatedQuestions);
    }
    return generatedQuestions;
  }


  private List<Question> addQuestion
      (List<Question> questions, List<Question> generatedQuestions) {
    int min = 1;
    int max = 5;
    int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
    for (Question q : questions) {
      if (randomNum == 0) {
        return generatedQuestions;
      } else {
        generatedQuestions.add(q);
        randomNum--;
      }
    }
    return generatedQuestions;
  }

    /*
    public int calculateMaxQuestionsPerCategory(List<Category> selectedCategory){
        boolean firstIndex = true;
        int smallestMaxQuestions = 0;
        for(Category c : selectedCategory){
            int maxQuestion = c.getQuestionList().size();
            if(firstIndex){
                smallestMaxQuestions = maxQuestion;
            }else{
                if(smallestMaxQuestions > maxQuestion){
                    smallestMaxQuestions = maxQuestion;
                }
            }
        }
        return smallestMaxQuestions;
    }
    */

  private List<Category> selectCategory() {
    outputAllCategory();
    int minCategoryAreChosen = 2;
    boolean noEndOfInput = true;
    List<Category> selectedCategory = new ArrayList<>();
    do {
      Scanner input = new Scanner(System.in, "utf-8");
      System.out.println("Please enter a category: ");
      if (minCategoryAreChosen <= 0) {
        System.out.println("(q finish the input");
      }
      String inputCategory = input.nextLine();
      if (inputCategory.equals("q") && minCategoryAreChosen <= 0) { //finish input
        noEndOfInput = false;
      }
      Category chosenCategory = getCategory(inputCategory);
      if (chosenCategory == null) {
        System.out.println("Category does not exist!");
        continue;
      }
      if (containsCategory(selectedCategory, inputCategory)) {
        System.out.println("This category has already been entered!");
        continue;
      }
      selectedCategory.add(chosenCategory);
      minCategoryAreChosen--;
    } while (noEndOfInput);
    return selectedCategory;
  }

  private boolean containsCategory(List<Category> selectedCategory, String category) {
    for (Category c : selectedCategory) {
      if (c.getName().equals(category)) {
        return true;
      }
    }
    return false;
  }

  private Category getCategory(String categoryName) {
    for (Category c : categoryList) {
      if (c.getName().equals(categoryName)) {
        return c;
      }
    }
    return null;
  }

  private void outputAllCategory() {
    System.out.println("----------------------------------------------------------");
    int counter = 4;
    for (Category c : categoryList) {
      if (counter >= 0) {
        System.out.print(c.getName() + "      ");
        counter--;
      } else {
        System.out.println(c.getName());
        counter = 4;
      }
    }
    System.out.println();
  }

  private void analyseMenu() {
    int input;
    try {
      do {
        System.out.println("----------------------------------------------------------");
        Scanner inputValue = new Scanner(new InputStreamReader(System.in, "utf-8"));

        System.out.println("---- Please choose: ----");
        System.out.println("1. all players which played in a certain time");
        System.out.println("2. data of one player");
        System.out.println("3. all players with numbers of played games");
        System.out.println("4. popularity of the categories");
        System.out.println("-------------------------");
        System.out.println("0. exit analyse menu");
        System.out.println(" ");
        input = inputValue.nextInt();
        switch (input) {
          case 1:
            playersPlaysInCertainTime();
            break;
          case 2:
            dataOfPlayer();
            break;
          case 3:
            allPlayersWithGameNumbers();
            break;
          case 4:
            categoryPopularity();
            break;
          case 0:
            System.out.println("Go back to - main menu");
            break;
          default:
            System.out.println("Please enter again!");
            break;
        }
      } while (input != 0);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  private void playersPlaysInCertainTime() {
    EntityManager em = emFactory.createEntityManager();
    String startYear = "";
    String startMonth = "";
    String startDate = "";
    String endYear = "";
    String endMonth = "";
    String endDate = "";
    //Date input:
    try {
      System.out.println("----------------------------------------------------------");
      System.out.println("Please input a startDate and endDate:");
      Scanner inputValue = new Scanner(new InputStreamReader(System.in, "utf-8"));
      for (int i = 0; i < 2; i++) {
        if (i == 0) {
          System.out.println("Year(yyyy): ");
          startYear = inputValue.next();
          System.out.println("Month(MM): ");
          startMonth = inputValue.next();
          System.out.println("Day(dd): ");
          startDate = inputValue.next();
        } else {
          System.out.println("Year(yyyy): ");
          endYear = inputValue.next();
          System.out.println("Month(MM): ");
          endMonth = inputValue.next();
          System.out.println("Day(dd): ");
          endDate = inputValue.next();
        }
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try {
      Date timeStart = df.parse(startYear + "-" + startMonth + "-" + startDate + " 00:00:00");
      Date timeEnd = df.parse(endYear + "-" + endMonth + "-" + endDate + " 00:00:00");
      String timeStartAsString = df.format(timeStart);
      String timeEndAsString = df.format(timeEnd);
      try {
        List result = (List<String>) em.createQuery("SELECT distinct p.name\n"
            + "FROM Player p\n"
            + "inner join Match m on p.id = m.playingGamer.id\n"
            + "where m.matchStart between '" + timeStartAsString + "' and\n"
            + " '" + timeEndAsString + "'").getResultList();
        System.out.print("---- Data of all players which played from ");
        System.out.println(timeStartAsString + " to " + timeEndAsString + "----");
        for (Object record : result){
          System.out.println(record);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        em.close();
      }
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  private void dataOfPlayer() {
    String name;
    try {
      System.out.println("----------------------------------------------------------");
      Scanner inputValue = new Scanner(new InputStreamReader(System.in, "utf-8"));
      System.out.println("Please input a name:");
      name = inputValue.next();
      Player player = findPlayer(name);
      outputDataOfPlayer(player);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  private void allPlayersWithGameNumbers() {
    //Version 1:
    EntityManager em = emFactory.createEntityManager();
    try {
      List result = (List<String>) em.createQuery("select p.name, count(m.id) as matchnumber\n"
          + "from Player p \n"
          + "inner join Match m on p.id = m.playingGamer.id\n"
          + "group by p.name\n"
          + "order by count(m.id) DESC").getResultList();
      System.out.println("---- Data of all players with matchNumber ----");
      for (Object record : result){
        Object[] fields = (Object[]) record;
        System.out.println("Player: " + String.format("%-20s %s", fields[0], fields[1]));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      em.close();
    }

      /*
      //Version 2:
      String url = "jdbc:postgresql://localhost:5432/postgres";
      Class.forName("org.postgresql.Driver");
      Connection conn = DriverManager.getConnection(url, "postgres", "DB2x!");
      PreparedStatement ps = conn.prepareStatement("select p.name, count(m.id) as matchnumber\n"
          + "from myschema.Player p \n"
          + "inner join myschema.Match m on p.id = m.playingGamer_id\n"
          + "group by p.name\n"
          + "order by count(m.id) DESC");
      ResultSet rs = ps.executeQuery();
      System.out.println("---- Data of all players with matchNumber ----");
      while (rs.next()){
        System.out.println("Player: " + String.format("%-20s %s" ,
        rs.getString(1), rs.getString(2)));
      }
      ps.close();
      conn.close();
      System.out.println(" ");
    } catch (ClassNotFoundException e) {
      System.err.println("Fehler beim Laden des JDBC-Treibers");
      e.printStackTrace();
    } catch (SQLException e) {
      System.err.println("Fehler bei der Datenbankabfrage");
      e.printStackTrace();
    }
       */
  }

  private void categoryPopularity() {
    EntityManager em = emFactory.createEntityManager();
    try {
      int number = 0;
      List result = (List<String>) em.createQuery("SELECT c.name, count(m_c.id) as callNumber\n"
          + "FROM Category c, Match m \n"
          + "inner join m.selectedCategory m_c on c.id = m_c.id\n"
          + "GROUP BY c.name\n"
          + "order by count(m_c.id) DESC").getResultList();
      System.out.println("---- all category with selectedNumber ----");
      for (Object record : result){
        Object[] fields = (Object[]) record;
        number++;
        System.out.println(number + ".Place: " + String.format("%-20s %s", fields[0], fields[1]));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      em.close();
    }
  }

  private Player findPlayer(String name) {
    boolean found = false;
    Player player = null;
    EntityManager em = emFactory.createEntityManager();
    try {
      List<Player> result = em.createNamedQuery("Player.findByName").
          setParameter("name", name).getResultList();
      player = result.get(0);
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      em.close();
    }
    return player;
  }

  private void outputDataOfPlayer(Player player){
    //all matches:
    System.out.println("---- Data of player " + player.getName() + " ----");
    System.out.println("All matches: ");
    EntityManager em = emFactory.createEntityManager();
    try {
      int matchNumber = 0;
      List result = (List<String>) em.createQuery("SELECT m.id, m.matchStart, m.matchEnd, \n"
          + "m.numberOfCorrectAnswers, size(m.selectedQuestion) as questions \n"
          + "from Player p, Match m \n"
          + "where p.name = '" + player.getName() + "'\n"
          + "and p.id = m.playingGamer.id\n"
          + "group by m.id").getResultList();
      for (Object record : result){
        matchNumber++;
        Object[] fields = (Object[]) record;
        System.out.print(matchNumber + ". match - id: " + fields[0]);
        System.out.print(" startTime: " + fields[1] + " endTime: " + fields[2]);
        //number of correct Answer:
        int correctAnswers = (Integer) fields[3];
        double correctnessInPercent = ((double) correctAnswers / (Integer) fields[4]) * 100;
        System.out.print(" correctAnswers: " + correctAnswers + " from ");
        System.out.print(fields[4] + " questions ");
        System.out.println("- " + correctnessInPercent + "% correctness");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      em.close();
    }
  }

  private int numberOfCorrectAnswers(Match m){
    int correctAnswers = 0;
    for (int i = 0; i < m.getSelectedQuestion().size(); i++){
      if (m.getSelectedQuestion().get(i).getCorrectAnswer() == m.getSelectedAnswers().get(i)){
        correctAnswers++;
      }
    }
    return correctAnswers;
  }
}
