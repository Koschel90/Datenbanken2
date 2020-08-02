package de.hda.fbi.db2.stud;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.hda.fbi.db2.stud.entity.QuizGame;
import de.hda.fbi.db2.tools.CsvDataReader;


public class Main {
    public static void main(String[] args) {

        //Main main = new Main();

        try {
            //Read default csv
            final List<String[]> defaultCsvLines = CsvDataReader.read();
            //main.printDataQuestions(default1
            //CsvLines);
            //main.printDataCategories(defaultCsvLines);

            QuizGame quiz = new QuizGame(defaultCsvLines);
            quiz.quizMenu();

            //Read (if available) additional csv-files and default csv-file
            List<String> availableFiles = CsvDataReader.getAvailableFiles();
            for (String availableFile : availableFiles) {
                final List<String[]> additionalCsvLines = CsvDataReader.read(availableFile);
                //main.printDataQuestions(additionalCsvLines);
                //main.printDataCategories(additionalCsvLines);
            }
        } catch (URISyntaxException use) {
            System.out.println(use);
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }




    public void printDataQuestions(List<String[]> data) {

        for (int i = 0; i < data.size(); i++) {

            for (int j = 0; j < data.get(i).length; j++) {
                System.out.print(data.get(i)[j]);
            }
            System.out.print('\n');
        }
        int numberOfQuestions = data.size() - 1;
        System.out.println('\n' + "Anzahl Fragen: " + numberOfQuestions);
    }

    public void printDataCategories(List<String[]> data) {

        List<String> catList = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {

            for (int j = 7; j < data.get(i).length; j++) {

                catList.add(data.get(i)[7]);
            }
        }
        List<String> newList = catList.stream().distinct().collect(Collectors.toList());
        int numberOfCategories = newList.size() - 1;
        System.out.println("Anzahl der Kategorien: " + numberOfCategories);
    }

    public String getGreeting() {
        return "app should have a greeting";
    }

}
