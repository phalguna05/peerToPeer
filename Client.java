import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import peerFunctions.ThreadCreation;
import helperFunctions.GetPeerDetails;
public class Client {
    Socket requestSocket; //socket connect to the server
    ObjectOutputStream out; //stream write to the socket
    ObjectInputStream in; //stream read from the socket
    String message; //message send to the server
    String MESSAGE; //capitalized message read from the server
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
                int portNum = createThreadForPeer(peerId);
                String[] arr = {peerId,String.valueOf(portNum)};
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
    int createThreadForPeer(String peerId){
        ThreadCreation newThread = new ThreadCreation();
        int portNumber = newThread.run(peerId);
        return portNumber;
    }
    void makeConnections(){
        GetPeerDetails peerDetails = new GetPeerDetails();
        peerDetails.initialize();
        String initialPeerId = peerDetails.getInitialPeerId();
        System.out.println("Initial peer id is "+initialPeerId);

    }
    public static void main(String args[])
    {
        if (args.length == 0) {
            System.out.println("No peer id is passed. Please pass it.");
        }
        else {
            Client client = new Client();
            client.run(args[0]);
        }
    }
}