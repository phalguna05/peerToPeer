import helperFunctions.GetPeerDetails;
import message.Message;

import java.net.*;
import java.io.*;
import java.util.List;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.HashMap;
public class Server {
    private static final int sPort = 8000; //The server will be listening on this port number
    private static final HashMap<String, String> peerMap = new HashMap<String, String>();
    public static void main(String[] args) throws Exception {
        System.out.println("The server is running.");
        ServerSocket listener = new ServerSocket(sPort);
        //listener starts
        try {
            while(true) {
                new Handler(listener.accept()).start();

            }
        } finally {
            listener.close();
        }
    }
    /**
     * A handler thread class. Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    private static class Handler extends Thread {
        private String peerId; //message received from the client
        private String portNumber;
        private String MESSAGE; //uppercase message send to the client
        private Socket connection;
        private ObjectInputStream in; //stream read from the socket
        private ObjectOutputStream out; //stream write to the socket
        private int no; //The index number of the client
        private GetPeerDetails peerDetails;
        private HashSet<String> downloadedPeers;
        private int size;
        public Handler(Socket connection ) {
            this.connection = connection;
            peerDetails = new GetPeerDetails();
            peerDetails.initialize();
            size = peerDetails.getNumberOfPeers();
            downloadedPeers = new HashSet<>();
        }
        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());

                Boolean isMsgSent = false;
                try{
                    while(true)
                    {
                        //receive the message sent from the client
                        String[] arr = (String[])in.readObject();
                        peerId = arr[0];
                        if(arr[1].equals("Finished Downloading")){
                            downloadedPeers.add(peerId);
                            System.out.println("PeerId "+peerId+" is done with downloading");
                            if(downloadedPeers.size() == size){
                                connection.close();
                                break;
                            }
                        }
                        else {
                            if(peerDetails.getFileStatus(peerId) == 1){
                                downloadedPeers.add(peerId);
                            }
                            portNumber = arr[1];
                            //show the message to the user
                            peerMap.put(peerId, portNumber);
                            System.out.println("Peer with peerId " + peerId + " is connected. ");
                            //Capitalize all letters in the message
                            MESSAGE = peerId.toUpperCase();
                            //send MESSAGE back to the client
                            //sendMessage(MESSAGE);
                            if (peerMap.size() == size && !isMsgSent) {
                                Thread currThread = Thread.currentThread();
                                currThread.sleep(1000);
                                try {
                                    Message msg = new Message();
                                    msg.convertMessageLength(1);
                                    msg.convertMessageType(8);
                                    msg.addMessagePayload(new byte[0]);
                                    byte[] message = msg.getMessage(1000);
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