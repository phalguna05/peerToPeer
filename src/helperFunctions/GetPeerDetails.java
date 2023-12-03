package helperFunctions;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// This class is responsible for reading and storing details of peers from a configuration file
public class GetPeerDetails {
    private static List<List<String>> linesAsWords = new ArrayList<>();
    public static void main(String args[]){}

    // Method to read and initialize peer details from the configuration file
    public void initialize(){
            String fileName = "PeerInfo.cfg";

            // Check if details are already loaded
            if(!(linesAsWords.size()>0)) {
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
    }

    // Method to get the port number of a specific peer
    public int getPortNumber(String peerId){
        for (List<String> list : linesAsWords) {
            if(list.get(0).equals(peerId)){
                return Integer.parseInt(list.get(2));
            }
        }
        return 0;
    }

    // Method to get all peer details
    public List<List<String>> getAllPeers(){
        return linesAsWords;
    }

    // Method to get the host address of a specific peer
    public String getHostAddress(String peerId){
        for (List<String> list : linesAsWords) {
            if(list.get(0).equals(peerId)){
                return list.get(1);
            }
        }
        return "None";
    }

    // Method to get the ID of the initial peer
    public String getInitialPeerId(){
        if(linesAsWords.size()>0){
            return linesAsWords.get(0).get(0);
        }
        return "None";
    }

    // Method to get the file status (has file or not) of a specific peer
    public int getFileStatus(String peerId){
        for (List<String> list : linesAsWords) {
            if(list.get(0).equals(peerId)){
                return Integer.parseInt(list.get(3));
            }
        }
        return 0;
    }

    // Method to get the total number of peers
    public int getNumberOfPeers(){
        return linesAsWords.size();
    }

}