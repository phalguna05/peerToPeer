import java.net.*;
import java.io.*;
import java.lang.*;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import peerFunctions.ThreadCreation;
import helperFunctions.GetPeerDetails;


public class peerProcess {
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
                String[] arr = {peerId,String.valueOf(portNumber)};
                ThreadCreation curThreadCreator = new ThreadCreation();
                Thread curThread = curThreadCreator.run(peerId);
                sendMessage(arr);
                makeConnections();
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
    void makeConnections(){
        GetPeerDetails peerDetails = new GetPeerDetails();
        peerDetails.initialize();
        int initialPeerId = Integer.parseInt(peerDetails.getInitialPeerId());
        ArrayList<Thread> list = new ArrayList<>();
        int convertedPeerId = Integer.parseInt(peer_id);
        for(int i = initialPeerId;i<convertedPeerId;i++){
            // handshake with peerId and itself
            Thread th = new Thread(new HandleConnections(convertedPeerId,i));
            th.start();

        }
    }

    public static void main(String args[])
    {
        if (args.length == 0) {
            System.out.println("No peer id is passed. Please pass it.");
        }
        else {
            peerProcess peerProcess = new peerProcess();
            peer_id = args[0];
            peerProcess.run(args[0]);
        }
    }
}