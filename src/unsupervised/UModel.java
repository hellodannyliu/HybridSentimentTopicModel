package unsupervised;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Stuart Blair on 10/05/2017.
 */
public class UModel {

    final int topics;
    final double laplaceSmoothing = 0.001;

    String wmLoc;
    List<String> inputData, words, pos, neg, posIndices, negIndices;
    int[] posCount, negCount;
    double[][] topicPosCount, topicNegCount;
    int totalPos, totalNeg, totalNonUniqueWords;
    double totalNegHits, totalPosHits;

    public UModel(String textData, String wordmapLocation, String posWords, String negWords, int numTopics) {
        this.topics = numTopics;
        System.out.println("Initialising sentiment functionality...");
        wmLoc = wordmapLocation;
        words = new ArrayList<>();
        try {
            inputData = FileUtils.readLines(new File(textData), "UTF-8");
            inputData.remove(0);
            System.out.println("Documents loaded: " + inputData.size());
            pos = FileUtils.readLines(new File(posWords), "UTF-8");
            neg = FileUtils.readLines(new File(negWords), "UTF-8");
        } catch(IOException e) {
            System.err.println("Cannot find positive or negative words!");
            e.printStackTrace();
        }
        loadWordmap();
        countSentiment();
        topicPosCount = new double[topics][words.size()];
        topicNegCount = new double[topics][words.size()];
        getSentimentWordIndices();
    }

    public void countSentiment() {
        posCount = new int[pos.size()];
        negCount = new int[neg.size()];
        for(String s : inputData) {
            String[] temp = s.split(" ");
            totalNonUniqueWords += temp.length;
            for(String str : temp) {
                for (int i = 0; i < pos.size(); i++) {
                    if(str.equals(pos.get(i))) {
                        posCount[i]++;
                    }
                }
                for (int i = 0; i < neg.size(); i++) {
                    if(str.equals(neg.get(i))) {
                        negCount[i]++;
                    }
                }
            }
        }
        for (int i = 0; i < posCount.length; i++) {
            totalPos += posCount[i];
        }
        for (int i = 0; i < negCount.length; i++) {
            totalNeg += negCount[i];
        }
        System.out.println("Total vocabulary size: " + totalNonUniqueWords);
        System.out.println("Positive dictionary size: " + pos.size());
        System.out.println("Positive word counts: " + Arrays.toString(posCount));
        System.out.println("Total positive word count: " + totalPos);
        System.out.println("Negative dictionary size: " + neg.size());
        System.out.println("Negative word counts: " + Arrays.toString(negCount));
        System.out.println("Total negative word count: " + totalNeg);
    }

    public void loadWordmap() {
        List<String> temp = null;
        try {
             temp = FileUtils.readLines(new File(wmLoc), "UTF-8");
        } catch (IOException e) {
            System.err.println("Cannot find wordmap file!");
            e.printStackTrace();
        }
        temp.remove(0);
        String[] objWords = new String[temp.size()];
        for(String s : temp) {
            String[] tempWords = s.split(" ");
            //words.add(tempWords[0]);
            objWords[Integer.parseInt(tempWords[1])] = tempWords[0];
        }
        words = Arrays.asList(objWords);
        System.out.println("Unique vocabulary size: " + words.size());
    }

    public void getSentimentWordIndices() {
        posIndices = new ArrayList<>();
        for(String s : pos) {
            if(!(words.indexOf(s) == -1)) {
                posIndices.add(String.valueOf(words.indexOf(s)));
            }
        }
        negIndices = new ArrayList<>();
        for(String s : neg) {
            if(!(words.indexOf(s) == -1)) {
                negIndices.add(String.valueOf(words.indexOf(s)));
            }
        }
        System.out.println("Found sentiment dictionary indices!");
    }

    public void printWordmap() {
        for(String s : words) {
            System.out.println(s);
        }
    }

    public void assignTopicSentimentWeights(String topicAssignmentFile, int nGram) {
        String iteration = topicAssignmentFile.substring(0, topicAssignmentFile.lastIndexOf("."));
        for (int i = 0; i < topicPosCount.length; i++) {
            for (int j = 0; j < topicPosCount[i].length; j++) {
                topicPosCount[i][j] = 0;
                topicNegCount[i][j] = 0;
            }
        }
        List<String> documentTopics = null;
        try {
            documentTopics = FileUtils.readLines(new File(topicAssignmentFile), "UTF-8");
        } catch (IOException e) {
            System.err.println("Cannot find topic assignment file!");
            e.printStackTrace();
        }
        for(String s : documentTopics) {
            String[] wordTopicPair = s.split(" ");
            String[] docWords = new String[wordTopicPair.length];
            String[] topics = new String[wordTopicPair.length];
            for (int i = 0; i < wordTopicPair.length; i++) {
                String[] temp = wordTopicPair[i].split(":");
                docWords[i] = temp[0];
                topics[i] = temp[1];
            }
            for (int i = 0; i < wordTopicPair.length; i++) {
                int[] grams = new int[nGram];
                if(i + nGram < wordTopicPair.length) {
                    int index = 0;
                    for (int j = i; j < i + nGram; j++) {
                        grams[index] = Integer.parseInt(docWords[j]);
                        index++;
                    }
                }
                for(int integer : grams) {
                    if(posIndices.contains(String.valueOf(integer))) {
                        topicPosCount[Integer.parseInt(topics[i])][Integer.parseInt(docWords[i])]++;
                    } else if(negIndices.contains(String.valueOf(integer))) {
                        topicNegCount[Integer.parseInt(topics[i])][Integer.parseInt(docWords[i])]++;
                    } else {
//                        topicPosCount[Integer.parseInt(topics[i])][Integer.parseInt(docWords[i])] += 0;
//                        topicNegCount[Integer.parseInt(topics[i])][Integer.parseInt(docWords[i])] += 0;
                    }
                }
            }
        }
        try {
            for (int i = 0; i < topicPosCount.length; i++) {
                StringBuilder ps = new StringBuilder();
                StringBuilder ns = new StringBuilder();
                for (int j = 0; j < topicPosCount[i].length; j++) {
                    ps.append(String.valueOf(topicPosCount[i][j]) + ",");
                    ns.append(String.valueOf(topicNegCount[i][j]) + ",");
                    //FileUtils.writeStringToFile(new File(iteration + ".passign"), String.valueOf(topicPosCount[i][j]) + ",", "UTF-8", true);
                    //FileUtils.writeStringToFile(new File(iteration + ".nassign"), String.valueOf(topicNegCount[i][j]) + ",", "UTF-8", true);
                }
                FileUtils.writeStringToFile(new File(iteration + ".passign"), ps.toString() + "\n", "UTF-8", true);
                FileUtils.writeStringToFile(new File(iteration + ".nassign"), ns.toString() + "\n", "UTF-8", true);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void classifySentiment(String topicAssignmentFile, boolean meanSo) {
        String iteration = topicAssignmentFile.substring(0, topicAssignmentFile.lastIndexOf("."));

        totalPosHits = setTotalPosHits();
        totalNegHits = setTotalNegHits();

        List<String> documentTopics = null;
        try {
            documentTopics = FileUtils.readLines(new File(topicAssignmentFile), "UTF-8");
        } catch (IOException e) {
            System.err.println("Cannot find topic assignment file!");
            e.printStackTrace();
        }
        for (String s : documentTopics) {
            String[] wordTopicPair = s.split(" ");
            String[] docWords = new String[wordTopicPair.length];
            String[] topics = new String[wordTopicPair.length];
            for (int i = 0; i < wordTopicPair.length; i++) {
                String[] temp = wordTopicPair[i].split(":");
                docWords[i] = temp[0];
                topics[i] = temp[1];
            }
            double overallSo = 0.0;
            for (int i = 0; i < wordTopicPair.length; i++) {
                overallSo += Math.log(((topicPosCount[Integer.parseInt(topics[i])][Integer.parseInt(docWords[i])] + laplaceSmoothing) * (totalNegHits + laplaceSmoothing)) / ((topicNegCount[Integer.parseInt(topics[i])][Integer.parseInt(docWords[i])] + laplaceSmoothing) * (totalPosHits + laplaceSmoothing))) * Math.log(2);
            }
            double finalSo;
            if(meanSo == true) {
                finalSo = overallSo/wordTopicPair.length;
            } else {
                finalSo = overallSo;
            }
            try {
                FileUtils.writeStringToFile(new File(iteration + ".sassign"), String.valueOf(finalSo) + "\n","UTF-8",true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        getAccuracy(iteration + ".sassign", 1000);
    }

    public double setTotalNegHits() {
        double total = 0;
            for(String s : negIndices) {
                for (int i = 0; i < topicNegCount.length; i++) {
                    if(Integer.parseInt(s) <= words.size()) {
                        total += topicNegCount[i][Integer.parseInt(s)];
                    }
                }
        }
        return total;
    }

    public double setTotalPosHits() {
        double total = 0;
        for(String s : posIndices) {
            for (int i = 0; i < topicPosCount.length; i++) {
                if(Integer.parseInt(s) <= words.size()) {
                    total += topicPosCount[i][Integer.parseInt(s)];
                }
            }
        }
        return total;
    }

    public void getAccuracy(String sentiAssignFile, int numPerClass) {
        double correct = 0.0;
        List<String> sentimentAssignments = null;
        try {
            sentimentAssignments = FileUtils.readLines(new File(sentiAssignFile), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < numPerClass; i++) {
            if(Double.parseDouble(sentimentAssignments.get(i)) > 0.0) {
                correct++;
            }
        }
        for (int i = numPerClass; i < numPerClass*2; i++) {
            if(Double.parseDouble(sentimentAssignments.get(i)) < 0.0) {
                correct++;
            }
        }
        double totalAcc = (correct/(Double.valueOf(numPerClass)*2.0))*100.0;
        try {
            FileUtils.writeStringToFile(new File("C:/data/movies/model.acc"), String.valueOf(totalAcc) + "\n","UTF-8", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
