package peerFunctions;

import config.PeerInitialization;
import helperFunctions.GetPeerDetails;
import message.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThreadCreation {
    private static int portNumber;
    private static String id;
    private static ServerSocket serverSocket;
    private static Socket clientSocket;
    public static PeerInitialization peer;

    public ThreadCreation(String id, PeerInitialization peerInfo) throws Exception {
        this.id = id;
        this.peer = peerInfo;
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
                if (words[0].equals(id)) {
                    portNumber = Integer.parseInt(words[2]);
                }
                linesAsWords.add(wordsList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverSocket = new ServerSocket(portNumber);
        System.out.println("Server is listening on port " + portNumber);
        ThreadHandler th = new ThreadHandler();
        th.start();

    }

        private static class ThreadHandler extends Thread {
            public Socket connection;
            public InputStream input;
            public OutputStream output;

            public ThreadHandler() {

            }
            @Override
            public void run() {
                    // Listening for connections in a separate thread
                    try {
                        while (true) {
                            // Handle client request here
                            connection = serverSocket.accept();
                            input = connection.getInputStream();
                            output = connection.getOutputStream();
                            output.flush();
                            int bytesRead;
                            ArrayList<Integer> arr = new ArrayList<>();
                            while ((bytesRead = input.read()) != -1) {
                                arr.add(bytesRead);
                                if(input.available()<=0){
                                    break;
                                }
                            }
                            Boolean isHandshakeMsg = decodeHeader(arr);
                            if(isHandshakeMsg) {
                                int peerId = ((arr.get(28) & 0xFF) << 24) |
                                        ((arr.get(29) & 0xFF) << 16) |
                                        ((arr.get(30) & 0xFF) << 8) |
                                        (arr.get(31) & 0xFF);
                                if (peer.handShakeRequestMap.containsKey(Integer.toString(peerId))) {
                                    Message msg = new Message();
                                    msg.convertMessageLength(peer.bitFieldObj.numberOfPieces + 1);
                                    msg.convertMessageType(5);
                                    msg.addMessagePayload(peer.bitFieldObj.bitField);
                                    byte[] bitFieldMsg = msg.getMessage(Integer.parseInt(id));

                                    try {
                                        GetPeerDetails pd = new GetPeerDetails();
                                        String remoteAddress = pd.getHostAddress(Integer.toString(peerId));
                                        int remotePort = pd.getPortNumber(Integer.toString(peerId));
                                        clientSocket = new Socket(remoteAddress, remotePort);
                                        InputStream io = clientSocket.getInputStream();
                                        OutputStream oo = clientSocket.getOutputStream();
                                        oo.write(bitFieldMsg);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    peer.addHandshakeToMap(peerId, arr);
                                    System.out.println("Handshake message is established with " + arr.get(arr.size() - 1));
                                    HandleConnections hn = new HandleConnections(Integer.parseInt(id), peerId, peer);
                                    hn.start();
                                }
                            }
                            else{
                                System.out.println("Received bitfield msg "+arr);
                                int peerId = ((arr.get(arr.size()-4) & 0xFF) << 24) |
                                        ((arr.get(arr.size()-3) & 0xFF) << 16) |
                                        ((arr.get(arr.size()-2) & 0xFF) << 8) |
                                        (arr.get(arr.size()-1) & 0xFF);
                                peer.bitFieldObj.addBitFieldToMap(Integer.toString(peerId),arr);
                                if(!peer.bitFieldObj.bitFieldSentSet.contains(Integer.toString(peerId))){
                                    Message msg = new Message();
                                    msg.convertMessageLength(peer.bitFieldObj.numberOfPieces + 1);
                                    msg.convertMessageType(5);
                                    msg.addMessagePayload(peer.bitFieldObj.bitField);
                                    byte[] bitFieldMsg = msg.getMessage(Integer.parseInt(id));
                                    peer.bitFieldObj.addPeerToBitSet(Integer.toString(peerId));
                                    try {
                                        GetPeerDetails pd = new GetPeerDetails();
                                        String remoteAddress = pd.getHostAddress(Integer.toString(peerId));
                                        int remotePort = pd.getPortNumber(Integer.toString(peerId));
                                        clientSocket = new Socket(remoteAddress, remotePort);
                                        InputStream io = clientSocket.getInputStream();
                                        OutputStream oo = clientSocket.getOutputStream();
                                        oo.write(bitFieldMsg);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }

                            }
                            input.close();
                            output.close();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                finally{
                    try{

                        connection.close();
                    }catch(IOException ioException){
                        System.out.println("Disconnect with Client ");
                    }
                }

            }
        }

    private static Boolean decodeHeader(ArrayList<Integer> arr) {
        // Assuming the handshake header is the first 18 bytes
        if(arr.size() == 32){
            for(int i = 18;i<29;i++){
                if(arr.get(i)!=0){
                    return false;
                }
            }
            return true;
        }
        else{
            return false;
        }

    }
}
