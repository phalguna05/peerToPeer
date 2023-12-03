import helperFunctions.GetPeerDetails;
import message.Message;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;


public class Server {
    // Port number on which the server will listen
    private static final int sPort = 8000; 

    // HashMap to store peer ID and their corresponding port number
    private static final HashMap<String, String> peerMap = new HashMap<String, String>();

    public static void main(String[] args) throws Exception {
        // Starting the server
        System.out.println("The server is running.");
        ServerSocket listener = new ServerSocket(sPort);

        //listener starts
        try {
            // Continuously listen for client connections
            while(true) {
                // Start a new handler thread for each client
                new Handler(listener.accept()).start();

            }
        } finally {
            // Close the listener socket
            listener.close();
        }
    }


    /**
     * A handler thread class. Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    //Handler thread class to manage individual client requests.
    private static class Handler extends Thread {
        private String peerId; // Peer ID received from the client
        private String portNumber;// Port number of the client
        private String MESSAGE; // Uppercase message to send back to the client
        private Socket connection;// Socket connection to the client
        private ObjectInputStream in; // Input stream from the socket
        private ObjectOutputStream out; // Output stream to the socket
        private int no; //The index number of the client
        private GetPeerDetails peerDetails; // Object to get peer details
        private HashSet<String> downloadedPeers;// Set of peers that have completed downloading
        private int size;// Total number of peers


        public Handler(Socket connection ) {
            this.connection = connection;
            peerDetails = new GetPeerDetails();
            peerDetails.initialize();
            size = peerDetails.getNumberOfPeers();
            downloadedPeers = new HashSet<>();
        }

        public void run() {
            try{
                //Initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());

                Boolean isMsgSent = false;
                try{
                    while(true)
                    {
                        // Receive message from client
                        String[] arr = (String[])in.readObject();
                        peerId = arr[0];
                        if(arr[1].equals("Finished Downloading")){
                            // Add the peer to downloaded peers list
                            downloadedPeers.add(peerId);
                            System.out.println("PeerId "+peerId+" is done with downloading");

                            // Close connection if all peers have finished downloading
                            if(downloadedPeers.size() == size){
                                connection.close();
                                break;
                            }
                        }
                        else {
                            // Update download status for the peer
                            if(peerDetails.getFileStatus(peerId) == 1){
                                downloadedPeers.add(peerId);
                            }
                            portNumber = arr[1];
                            // Store the peer ID and port number
                            peerMap.put(peerId, portNumber);
                            System.out.println("Peer with peerId " + peerId + " is connected. ");

                            //Capitalize all letters in the message
                            MESSAGE = peerId.toUpperCase();

                            
                            /// If all peers are connected, send a message to each peer
                            if (peerMap.size() == size && !isMsgSent) {
                                Thread currThread = Thread.currentThread();
                                currThread.sleep(1000);

                                try {
                                    // Create and send a message to each peer
                                    Message msg = new Message();
                                    msg.convertMessageLength(1);
                                    msg.convertMessageType(8);
                                    msg.addMessagePayload(new byte[0]);
                                    byte[] message = msg.getMessage(1000);

                                    // Send the message to all peers
                                    List<List<String>> linesAsWords = peerDetails.getAllPeers();
                                    for (List<String> list : linesAsWords) {
                                        String peerId = list.get(0);
                                        int remotePort = peerDetails.getPortNumber(peerId);
                                        String remoteAddress = peerDetails.getHostAddress(peerId);
                                        Socket clientSocket = new Socket(remoteAddress, remotePort);
                                        InputStream io = clientSocket.getInputStream();
                                        OutputStream oo = clientSocket.getOutputStream();
                                        oo.write(message);
                                    }
                                    isMsgSent = true;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client ");
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client ");
                }
            }
        }
        //send a message to the output stream
        public void sendMessage(String msg)
        {
            try{
                out.writeObject(msg);
                out.flush();
                System.out.println("Send message: " + msg + " to Client "+no);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }
}