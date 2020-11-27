/*
 * WordGenerator.java
 * Author : 박찬형
 * Created Date : 2020-11-23
 */
package util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WordGenerator {
    private List<String> words;

    public WordGenerator(){
        words = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        try {
            FileReader fileReader = new FileReader("files/word.dat");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while((line = bufferedReader.readLine()) != null){
                sb.append(line);
            }
            String[] wordList = sb.toString().split("#");
            words.addAll(Arrays.asList(wordList));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String generateRandomWord(){
        return words.get(ThreadLocalRandom.current().nextInt(0, words.size()));
    }
}
