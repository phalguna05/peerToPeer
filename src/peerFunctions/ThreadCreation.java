package peerFunctions;

import config.PeerInitialization;
import helperFunctions.GetPeerDetails;
import logger.CustomLogger;
import message.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class ThreadCreation {
    private static int portNumber;// Port number of the peer 
    private static String id; // Id of the peer
    private static ServerSocket serverSocket;// Server socket for listening incoming connections
    private static Socket clientSocket;// Socket for client connections

    public static PeerInitialization peer;// Object containing peer initialization info
    public static CustomLogger logger;// Logger for logging activities

    // Constructor for ThreadCreation class
    public ThreadCreation(String id, PeerInitialization peerInfo, CustomLogger logger) throws Exception {
        this.id = id;
        this.peer = peerInfo;
        this.logger = logger;
        String fileName = "PeerInfo.cfg";// Configuration file name
        List<List<String>> linesAsWords = new ArrayList<>();// To store lines from config file

        // Read the configuration file and initialize server socket
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

        // Start the thread handler
        ThreadHandler th = new ThreadHandler();
        th.start();

    }

    // Inner class for handling threads
    private static class ThreadHandler extends Thread {
        public Socket connection;// Connection socket
        public InputStream input;// Input stream from the connection
        public OutputStream output;// Output stream to the connection
        public ThreadHandler() {
        }
        @Override
        public void run() {
            // Listening for connections in a separate thread
            try {
                while (true) {
                    // Handle client request here
                    // Accept client connections and process messages
                    connection = serverSocket.accept();
                    input = connection.getInputStream();
                    output = connection.getOutputStream();
                    output.flush();
                    int bytesRead;
                    ArrayList<Integer> arr = new ArrayList<>();// To store received bytes
                    long startTime = System.currentTimeMillis();// Start time for download rate calculation

                    // Read bytes from input stream
                    while ((bytesRead = input.read()) != -1) {
                        arr.add(bytesRead);
                        if (input.available() <= 0) {
                            break;
                        }
                    }

                    // Process received bytes if any
                    if(arr.size()>0){
                    long endTime = System.currentTimeMillis();
                    long elapsedTime = endTime - startTime;
                    Boolean isHandshakeMsg = decodeHeader(arr);
                    if (isHandshakeMsg) {
                        // Process handshake message
                        // Extract peer ID from the handshake message
                        int peerId = ((arr.get(28) & 0xFF) << 24) |
                                ((arr.get(29) & 0xFF) << 16) |
                                ((arr.get(30) & 0xFF) << 8) |
                                (arr.get(31) & 0xFF);
                        logger.tcpDone(peerId, Integer.parseInt(id));

                        // Check if handshake request is present and respond
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
                                logger.tcpRequest(Integer.parseInt(id), peerId);
                                oo.write(bitFieldMsg);

                                peer.socketMap.put(Integer.toString(peerId), oo);
                                // }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // Add handshake to map and start handling connections
                            peer.addHandshakeToMap(peerId, arr);
                            System.out.println("Handshake message is established with " + arr.get(arr.size() - 1));
                            HandleConnections hn = new HandleConnections(Integer.parseInt(id), peerId, peer);
                            hn.start();
                        }
                    } else {
                        // Process other message types
                        Message msg = new Message();
                        int type = msg.getTypeOfTheMessage(arr);
                        int peerId = ((arr.get(arr.size() - 4) & 0xFF) << 24) |
                                ((arr.get(arr.size() - 3) & 0xFF) << 16) |
                                ((arr.get(arr.size() - 2) & 0xFF) << 8) |
                                (arr.get(arr.size() - 1) & 0xFF);
                        switch (type) {
                            case 0:
                                chokeMsgProcess(arr, msg, peerId);
                                break;
                            case 1:
                                unChokeMsgProcess(arr, msg, peerId);
                                break;
                            case 2:
                                interestedMsgProcess(arr, msg, peerId);
                                break;
                            case 3:
                                notInterestedMsgProcess(arr, msg, peerId);
                                break;
                            case 4:
                                System.out.print("Have");
                                break;
                            case 5:
                                bitFieldMsgProcess(arr, msg, elapsedTime, peerId);
                                break;
                            case 6:
                                requestMsgProcess(arr, msg, peerId);
                                break;
                            case 7:
                                pieceMsgProcess(arr, msg, peerId, elapsedTime);
                                break;
                            case 8:
                                calculatePreferredNeighbors();
                                break;
                            default:
                                continue;
                        }


                    }

                    input.close();
                    output.close();
                }}

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    // Close the connection
                    connection.close();
                } catch (IOException ioException) {
                    System.out.println("Disconnect with Client ");
                }
            }

        }

        // Method to process 'not interested' messages
        private static void notInterestedMsgProcess(ArrayList<Integer> arr, Message msg, int peerId) {
            // Mark the peer as not interested in the data
            peer.addNotInterestedNeighbor(peerId);
        }

        // Method to process 'piece' messages
        private static void pieceMsgProcess(ArrayList<Integer> arr, Message msg, int peerId, long time) {
            // Extracting the payload from the received message
            byte[] payload = new byte[arr.size() - 12];
            ArrayList<Integer> index = new ArrayList<>();
            for (int i = 5; i < 9; i++) {
                index.add(arr.get(i));
            }
            int ind = peer.byteArrayToIntBigEndian(index); // Convert byte array to int

            // Updating bitfield object with the received piece
            peer.bitFieldObj.addBitFieldIndex(ind,logger);
            peer.bitFieldObj.pieces[ind].setContent(payload);
            peer.bitFieldObj.pieces[ind].setIsPresent(true);

            // Updating download rates(timings) based on the received piece
            peer.addDownloadRates(peerId, time, arr.size());
            logger.downloading(Integer.parseInt(id),peerId,ind,peer.getNumberOfPieces());

            // Requesting the next piece if the peer is unchoked
            if(peer.neighborsWhichUnChokedMe.contains(Integer.toString(peerId))) {
                int indVal = peer.getPieceToRequest(peerId);// Determine next piece to request
                if (indVal != -1) {
                    // Preparing the request message for the next piece
                    byte[] bytes = new byte[4];
                    bytes[0] = (byte) ((indVal >> 24) & 0xFF);
                    bytes[1] = (byte) ((indVal >> 16) & 0xFF);
                    bytes[2] = (byte) ((indVal >> 8) & 0xFF);
                    bytes[3] = (byte) (indVal & 0xFF);
                    msg.convertMessageLength(5);
                    msg.convertMessageType(6);
                    msg.addMessagePayload(bytes);
                    byte[] requestMsg = msg.getMessage(Integer.parseInt(id));

                    // Sending the request message to the peer
                    try {
//                    if(peer.socketMap.containsKey(Integer.toString(peerId))){
//                        OutputStream oo = peer.socketMap.get(Integer.toString(peerId));
//                        oo.write(requestMsg);
//
//                    }
//                    else {
                        GetPeerDetails pd = new GetPeerDetails();
                        String remoteAddress = pd.getHostAddress(Integer.toString(peerId));
                        int remotePort = pd.getPortNumber(Integer.toString(peerId));
                        clientSocket = new Socket(remoteAddress, remotePort);

                        InputStream io = clientSocket.getInputStream();
                        OutputStream oo = clientSocket.getOutputStream();
                        oo.write(requestMsg);

                        // Storing the output stream for future use
                        peer.socketMap.put(Integer.toString(peerId), oo);
                        //}
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // If no more pieces are needed, send 'not interested' message
                    msg.convertMessageLength(1);
                    msg.convertMessageType(3);
                    msg.addMessagePayload(new byte[0]);
                    byte[] notInterestedMsg = msg.getMessage(Integer.parseInt(id));

                    // Sending the 'not interested' message to the peer
                    try {
                        GetPeerDetails pd = new GetPeerDetails();
                        String remoteAddress = pd.getHostAddress(Integer.toString(peerId));
                        int remotePort = pd.getPortNumber(Integer.toString(peerId));
                        clientSocket = new Socket(remoteAddress, remotePort);
                        InputStream io = clientSocket.getInputStream();
                        OutputStream oo = clientSocket.getOutputStream();
                        oo.write(notInterestedMsg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        // Method to process 'request' messages
        private static void requestMsgProcess(ArrayList<Integer> arr, Message msg, int peerId) {
            ArrayList<Integer> bytes = new ArrayList<>();
            // Extract the index of the requested piece from the message
            for (int i = 5; i < 9; i++) {
                bytes.add(arr.get(i));
            }
            int value = peer.byteArrayToIntBigEndian(bytes);// Convert bytes to integer

            // Prepare the byte array for the piece index
            byte[] byteArr = new byte[4];
            byteArr[0] = (byte) ((value >> 24) & 0xFF);
            byteArr[1] = (byte) ((value >> 16) & 0xFF);
            byteArr[2] = (byte) ((value >> 8) & 0xFF);
            byteArr[3] = (byte) (value & 0xFF);

            // Retrieve the content of the requested piece
            byte[] content = peer.getPieceByIndex(value);

            // Prepare the payload for the 'piece' message
            byte[] payload = new byte[4 + content.length];
            System.arraycopy(byteArr, 0, payload, 0, byteArr.length);

            // Copy array2 into the result array, starting from the end of array1
            System.arraycopy(content, 0, payload, byteArr.length, content.length);

            // Set message properties for sending the piece
            msg.convertMessageLength(1 + payload.length);
            msg.convertMessageType(7);
            msg.addMessagePayload(payload);
            byte[] pieceMessage = msg.getMessage(Integer.parseInt(id));

            // Send the piece message to the requesting peer
            try {
                GetPeerDetails pd = new GetPeerDetails();
                String remoteAddress = pd.getHostAddress(Integer.toString(peerId));
                int remotePort = pd.getPortNumber(Integer.toString(peerId));
                clientSocket = new Socket(remoteAddress, remotePort);
                InputStream io = clientSocket.getInputStream();
                OutputStream oo = clientSocket.getOutputStream();
                oo.write(pieceMessage);

                // Store output stream for future communication with the peer
                peer.socketMap.put(Integer.toString(peerId), oo);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        // Method to process 'unchoke' messages
        private static void unChokeMsgProcess(ArrayList<Integer> arr, Message msg, int peerId) {
            // Log the unchoking event
            logger.unchoking(Integer.parseInt(id), peerId);

            // Update list of neighbors who have unchoked this peer
            peer.addNeighborsWhoUnchokedMe(peerId);

            // Determine next piece to request
            int ind = peer.getPieceToRequest(peerId);

            if (ind != -1) {
                // Prepare the 'request' message for the next piece
                byte[] bytes = new byte[4];
                bytes[0] = (byte) ((ind >> 24) & 0xFF);
                bytes[1] = (byte) ((ind >> 16) & 0xFF);
                bytes[2] = (byte) ((ind >> 8) & 0xFF);
                bytes[3] = (byte) (ind & 0xFF);
                msg.convertMessageLength(5);
                msg.convertMessageType(6);
                msg.addMessagePayload(bytes);
                byte[] requestMsg = msg.getMessage(Integer.parseInt(id));

                // Send the 'request' message to the unchoking peer
                try {
                    GetPeerDetails pd = new GetPeerDetails();
                    String remoteAddress = pd.getHostAddress(Integer.toString(peerId));
                    int remotePort = pd.getPortNumber(Integer.toString(peerId));
                    clientSocket = new Socket(remoteAddress, remotePort);
                    InputStream io = clientSocket.getInputStream();
                    OutputStream oo = clientSocket.getOutputStream();
                    oo.write(requestMsg);

                    // Store output stream for future communication with the peer
                    peer.socketMap.put(Integer.toString(peerId), oo);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Send 'not interested' message if no more pieces are needed
                msg.convertMessageLength(1);
                msg.convertMessageType(3);
                msg.addMessagePayload(new byte[0]);
                byte[] notInterestedMsg = msg.getMessage(Integer.parseInt(id));
                try {
                    GetPeerDetails pd = new GetPeerDetails();
                    String remoteAddress = pd.getHostAddress(Integer.toString(peerId));
                    int remotePort = pd.getPortNumber(Integer.toString(peerId));
                    clientSocket = new Socket(remoteAddress, remotePort);
                    InputStream io = clientSocket.getInputStream();
                    OutputStream oo = clientSocket.getOutputStream();
                    oo.write(notInterestedMsg);

                    // Store output stream for future communication with the peer
                    peer.socketMap.put(Integer.toString(peerId), oo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Method to process 'choke' messages
        private static void chokeMsgProcess(ArrayList<Integer> arr, Message msg, int peerId) {
            // Log the choking event
            logger.choking(Integer.parseInt(id), peerId);
            // Remove the peer from the list of neighbors that have unchoked this peer
            peer.neighborsWhichUnChokedMe.remove(Integer.toString(peerId));
        }

        // Method to calculate and schedule tasks for finding preferred and optimistically unchoked neighbors
        private static void calculatePreferredNeighbors() {
            Timer timer = new Timer();
            // Schedule tasks to find preferred neighbors and optimistically unchoked neighbors
            timer.scheduleAtFixedRate(new FindPreferredNeighbors(), 0, peer.unchockingInterval * 1000);
            timer.scheduleAtFixedRate(new FindOptimisticallyUnchokedNeighbor(), 0, peer.optimisticUnchokingInterval * 1000);
        }

        // Method to process 'interested' messages
        private static void interestedMsgProcess(ArrayList<Integer> arr, Message msg, int peerId) {
            // Log the interested event
            logger.interested(Integer.parseInt(id), peerId);
            // Add the peer to the list of interested neighbors
            peer.addInterestedNeighbor(peerId);
        }

        // Method to process 'bitfield' messages
        private static void bitFieldMsgProcess(ArrayList<Integer> arr, Message msg, Long time, int peerId) {
            // Extract the payload from the message
            ArrayList<Integer> payload = msg.getPayloadFromMessage(arr);

            // Update download rates based on the bitfield message
            peer.addDownloadRates(peerId, time, arr.size());

            // Check if there are any interesting pieces and update bitfield map
            Boolean isThereInterestingPieces = peer.bitFieldObj.addBitFieldToMap(Integer.toString(peerId), payload);

            // If bitfield message has not been sent to this peer, prepare and send it
            if (!peer.bitFieldObj.bitFieldSentSet.contains(Integer.toString(peerId))) {
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
                    peer.socketMap.put(Integer.toString(peerId), oo);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // If there are interesting pieces, send an 'interested' message
            if (isThereInterestingPieces) {
                Message interestedMsg = new Message();
                interestedMsg.convertMessageLength(1);
                interestedMsg.convertMessageType(2);
                interestedMsg.addMessagePayload(new byte[0]);
                byte[] interested = interestedMsg.getMessage(Integer.parseInt(id));
                try {
                    GetPeerDetails pd = new GetPeerDetails();
                    String remoteAddress = pd.getHostAddress(Integer.toString(peerId));
                    int remotePort = pd.getPortNumber(Integer.toString(peerId));
                    clientSocket = new Socket(remoteAddress, remotePort);
                    InputStream io = clientSocket.getInputStream();
                    OutputStream oo = clientSocket.getOutputStream();
                    oo.write(interested);
                    peer.socketMap.put(Integer.toString(peerId), oo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        // Method to decode the header of a message
        private static Boolean decodeHeader(ArrayList<Integer> arr) {
            // Check if the handshake header (first 18 bytes) is correct
            if (arr.size() == 32) {
                for (int i = 18; i < 29; i++) {
                    if (arr.get(i) != 0) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }

        }

        // TimerTask to find and manage preferred neighbors based on download rates
        static class FindPreferredNeighbors extends TimerTask {
            @Override
            public void run() {
                // This code is executed at each scheduled interval
                // Retrieve download rates for all peers
                HashMap<String, Long> downloadRates = peer.downloadRates;

                // Select top peers as preferred neighbors based on download rates
                HashMap<String, Long> preferredNeighborValues = downloadRates.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(peer.numberOfPreferredNeighbours)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                HashMap::new
                        ));

                // Prepare unchoke message for preferred neighbors
                Message msg = new Message();
                msg.convertMessageLength(5);
                msg.convertMessageType(1);
                msg.addMessagePayload(new byte[0]);
                byte[] unChokeMessage = msg.getMessage(Integer.parseInt(id));

                // Iterate over preferred neighbors to send unchoke messages
                int[] arrayOfPeers = new int[peer.numberOfPreferredNeighbours];
                final int[] index = {0};
                preferredNeighborValues.forEach((key, value) ->
                        {
                            arrayOfPeers[index[0]] = Integer.parseInt(key);
                            index[0] = index[0] + 1;
                            try {
                                // Establish connection and send unchoke message to each preferred neighbor
                                GetPeerDetails pd = new GetPeerDetails();
                                String remoteAddress = pd.getHostAddress(key);
                                int remotePort = pd.getPortNumber(key);
                                clientSocket = new Socket(remoteAddress, remotePort);
                                InputStream io = clientSocket.getInputStream();
                                OutputStream oo = clientSocket.getOutputStream();
                                oo.write(unChokeMessage);

                                // Store output stream for future communication
                                peer.socketMap.put(key, oo);
                                //}
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );

                // Prepare choke message for non-preferred neighbors
                msg.convertMessageType(0);// 0 indicates 'choke' message type
                byte[] chokeMessage = msg.getMessage(Integer.parseInt(id));
                peer.addPreferredNeighbors(arrayOfPeers);
                logger.changeOfPreferredNeighbors(Integer.parseInt(id),arrayOfPeers);

                // Identify and choke non-preferred neighbors
                int[] unPreferredNeighbors = new int[peer.downloadRates.size() - arrayOfPeers.length];
                final int[] unInd = {0};
                downloadRates.forEach((key, value) -> {
                    if (!preferredNeighborValues.containsKey(key)) {
                        unPreferredNeighbors[unInd[0]] = Integer.parseInt(key);
                        unInd[0] = unInd[0] + 1;
                        try {
//                            if(peer.socketMap.containsKey(key)){
//                                System.out.println(peer.socketMap);
//                                OutputStream oo = peer.socketMap.get(key);
//                                oo.write(unChokeMessage);
//
//                            }
//                            else {

                            // Send choke message to non-preferred neighbors
                            GetPeerDetails pd = new GetPeerDetails();
                            String remoteAddress = pd.getHostAddress(key);
                            int remotePort = pd.getPortNumber(key);
                            clientSocket = new Socket(remoteAddress, remotePort);
                            InputStream io = clientSocket.getInputStream();
                            OutputStream oo = clientSocket.getOutputStream();
                            oo.write(chokeMessage);
                            
                            // Store output stream for future communication
                            peer.socketMap.put(key, oo);
                            //}
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                // Update the list of non-preferred neighbors
                peer.addUnPreferredNeighbors(unPreferredNeighbors);
            }
        }

        // TimerTask to find and manage an optimistically unchoked neighbor
        static class FindOptimisticallyUnchokedNeighbor extends TimerTask {
            @Override
            public void run() {
                // This code is executed at each scheduled interval
                // Select a random neighbor as optimistically unchoked neighbor
                peer.optimisticallyUnchokedNeighbor = peer.getRandomValue();

                // Prepare unchoke message for the selected neighbor
                Message msg = new Message();
                msg.convertMessageLength(5);
                msg.convertMessageType(1);// 1 indicates 'unchoke' message type
                msg.addMessagePayload(new byte[0]);
                byte[] unChokeMessage = msg.getMessage(Integer.parseInt(id));


                if(peer.optimisticallyUnchokedNeighbor!=null) {
                    // Log change of optimistically unchoked neighbor
                    logger.changeOfOptimisticallyUnchokedNeighbor(Integer.parseInt(id), Integer.parseInt(peer.optimisticallyUnchokedNeighbor));
                    try {
                        // Establish connection and send unchoke message to the selected neighbor
                        GetPeerDetails pd = new GetPeerDetails();
                        String remoteAddress = pd.getHostAddress(peer.optimisticallyUnchokedNeighbor);
                        int remotePort = pd.getPortNumber(peer.optimisticallyUnchokedNeighbor);
                        clientSocket = new Socket(remoteAddress, remotePort);
                        InputStream io = clientSocket.getInputStream();
                        OutputStream oo = clientSocket.getOutputStream();
                        oo.write(unChokeMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }
}




