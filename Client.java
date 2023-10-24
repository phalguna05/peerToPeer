import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.lang.*;
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

import peerFunctions.ThreadCreation;
import helperFunctions.GetPeerDetails;
import peerFunctions.Handshake;
public class Client {
    Socket requestSocket; //socket connect to the server
    ObjectOutputStream out; //stream write to the socket
    ObjectInputStream in; //stream read from the socket
    ObjectOutputStream pToPOut;
    ObjectInputStream pToPIn;
    String message; //message send to the server
    String MESSAGE; //capitalized message read from the server
    public static String peer_id;
    private static int portNumber;
    private static Thread serverThread;
    public void Client() {}
    void run(String peerId)
    {
        try{
        //create a socket to connect to the server
            requestSocket = new Socket("localhost", 8000);
            System.out.println("Connected to localhost in port 8000");
            //initialize inputStream and outputStream
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());
            //get Input from standard input
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while(true)
            {
                //Send the sentence to the server
                serverThread = run(peerId);
                String[] arr = {peerId,String.valueOf(portNumber)};
                sendMessage(arr);
                makeConnections(curThread);
                //Receive the upperCase sentence from the server
                MESSAGE = (String)in.readObject();
                //show the message to the user
                System.out.println("Receive message: " + MESSAGE);
            }
        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch ( ClassNotFoundException e ) {
            System.err.println("Class not found");
        }
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
//Close connections
            try{
                in.close();
                out.close();
                requestSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }
    //send a message to the output stream
    void sendMessage(String[] msg)
    {
        try{
//stream write the message
            out.writeObject(msg);
            out.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
    //main method
    Thread createThreadForPeer(String peerId){
        ThreadCreation newThread = new ThreadCreation();
        Thread currThread = newThread.run(peerId);
        return currThread;
    }
    void makeConnections(Thread currentThread){
        GetPeerDetails peerDetails = new GetPeerDetails();
        peerDetails.initialize();
        int initialPeerId = Integer.parseInt(peerDetails.getInitialPeerId());
        int convertedPeerId = Integer.parseInt(peer_id);
        for(int i = initialPeerId;i<convertedPeerId;i++){
            // handshake with peerId and itself.
            Handshake newHandshake = new Handshake();
            newHandshake.sendHandshakeMessage(pToPOut,convertedPeerId,i);


        }


    }
    public static Thread run(String id){
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
        try {
            ServerSocket clientSocket = new ServerSocket(portNumber);
            System.out.println("Server is listening on port " + portNumber);
            pToPOut = new ObjectOutputStream(clientSocket.getOutputStream());
            pToPOut.flush();
            pToPIn = new ObjectInputStream(clientSocket.getInputStream());
            // Listening for connections in a separate thread

            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        String peerMsg = (String)in.readObject();
                        System.out.println("Message received in "+peer_id+" and msg is "+peerMsg);
                        Socket temSocket = clientSocket.accept();
                        System.out.println("Connection established with client: " + temSocket.getInetAddress());

                        // Handle client request in a separate thread
                        Thread clientThread = new Thread(() -> {
                            try {
                                InputStream input = temSocket.getInputStream();
                                OutputStream output = temSocket.getOutputStream();

                                // Handle client request here

                                // Example: Sending a welcome message to the client
                                String message = "Welcome to the server!";
                                output.write(message.getBytes());

                                temSocket.close();
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
            return thread;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Thread();

    }
    public static void main(String args[])
    {
        if (args.length == 0) {
            System.out.println("No peer id is passed. Please pass it.");
        }
        else {
            Client client = new Client();
            peer_id = args[0];
            client.run(args[0]);
        }
    }
}