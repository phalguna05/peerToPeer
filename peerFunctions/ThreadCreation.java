package peerFunctions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ThreadCreation {
    private static int portNumber;
    public static int run(String id){
        String fileName = "PeerInfo.cfg";
        List<List<String>> linesAsWords = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] words = line.split("\\s+"); // Split the line into words
                List<String> wordsList = new ArrayList<>();
                for (String word : words) {
                    wordsList.add(word);
                }
                if(words[0].equals(id)){
                    portNumber = Integer.parseInt(words[2]);
                }
                linesAsWords.add(wordsList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(portNumber);
        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("Server is listening on port " + portNumber);

            // Listening for connections in a separate thread
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Connection established with client: " + clientSocket.getInetAddress());

                        // Handle client request in a separate thread
                        Thread clientThread = new Thread(() -> {
                            try {
                                InputStream input = clientSocket.getInputStream();
                                OutputStream output = clientSocket.getOutputStream();

                                // Handle client request here

                                // Example: Sending a welcome message to the client
                                String message = "Welcome to the server!";
                                output.write(message.getBytes());

                                clientSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                        clientThread.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return portNumber;
    }

    public static void main(String args[]) {

    }
}
