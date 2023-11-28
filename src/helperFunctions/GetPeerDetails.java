package helperFunctions;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetPeerDetails {
    private static List<List<String>> linesAsWords = new ArrayList<>();
    public static void main(String args[]){}
    public void initialize(){
            String fileName = "PeerInfo.cfg";
            try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] words = line.split("\\s+"); // Split the line into words
                    List<String> wordsList = new ArrayList<>();
                    for (String word : words) {
                        wordsList.add(word);
                    }
                    linesAsWords.add(wordsList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
    public int getPortNumber(String peerId){
        for (List<String> list : linesAsWords) {

            if(list.get(0).equals(peerId)){
                return Integer.parseInt(list.get(2));
            }
        }
        return 0;
    }

    public String getHostAddress(String peerId){
        for (List<String> list : linesAsWords) {
            if(list.get(0).equals(peerId)){
                return list.get(1);
            }
        }
        return "None";
    }

    public String getInitialPeerId(){
        if(linesAsWords.size()>0){
            return linesAsWords.get(0).get(0);
        }
        return "None";
    }
    public int getFileStatus(String peerId){
        for (List<String> list : linesAsWords) {
            if(list.get(0).equals(peerId)){
                return Integer.parseInt(list.get(2));
            }
        }
        return 0;
    }
}