import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.ArrayList;

import config.PeerInitialization;
import logger.CustomLogger;
import peerFunctions.HandleConnections;
import peerFunctions.ThreadCreation;
import helperFunctions.GetPeerDetails;


class peerProcess {
    Socket requestSocket; // Sockets for connecting to the server and peer-to-peer communication
    ObjectOutputStream out; // Output stream to write to the socket
    ObjectInputStream in; // Input stream to read from the socket
    ObjectOutputStream pToPOut;
    ObjectInputStream pToPIn;
    String message; // Message to send to the server
    String MESSAGE; // Capitalized message received from the server
    public static String peer_id;// ID of the peer
    private static int portNumber;// Port number for the peer
    private static Thread serverThread; // Thread for running the server
    public static PeerInitialization peerInfo;// Object containing peer info
    public static CustomLogger logger;// Logger for logging activities

    // Run method to start the peer process
    void run(String peerId) {
        try {
            // Initialize logger
            logger = new CustomLogger();
            logger.initialize(peerId);

            //create a socket to connect to the server
            requestSocket = new Socket("localhost", 8000);
            System.out.println("Connected to localhost in port 8000");

            // Initialize input and output streams for communication
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

            // Initialize peer information
            peerInfo = new PeerInitialization(peerId);
            
            //get Input from standard input
            String[] arr = {peerId, String.valueOf(portNumber)};

            // Create and start threads for peer communication
            new ThreadCreation(peerId,peerInfo,logger);
            sendMessage(arr);
            makeConnections(logger);

            //Receive the upperCase sentence from the server
            MESSAGE = (String) in.readObject();

            //show the message to the user
            System.out.println("Receive message: " + MESSAGE);

        } catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found");
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
        //Close connections
            try {
                in.close();
                out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    // Method to send a message to the server
    void sendMessage(String[] msg) {
        try {
            //stream write the message
            out.writeObject(msg);
            out.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    

    // Method to establish connections with other peers
    void makeConnections(CustomLogger logger) {
        GetPeerDetails peerDetails = new GetPeerDetails();
        peerDetails.initialize();
        int initialPeerId = Integer.parseInt(peerDetails.getInitialPeerId());
        ArrayList<Thread> list = new ArrayList<>();
        int convertedPeerId = Integer.parseInt(peer_id);

        for (int i = initialPeerId; i < convertedPeerId; i++) {
            // handshake with peerId and itself
            Thread th = new Thread(new HandleConnections(convertedPeerId, i, peerInfo));
            th.start();
            logger.tcpRequest(convertedPeerId,i);
            peerInfo.addHandshakeRequest(convertedPeerId);

        }
    }

//    void initialize(String peerId) throws IOException {
//        try {
//            System.out.println("Reading data from Common.cfg");
//            List<String> linesFromFile = Files.readAllLines(Path.of("Common.cfg"));
//            PeerInitialization peerInit = new PeerInitialization();
//            for (String line : linesFromFile) {
//                String[] arr = line.split("\\s+");
//
//                if (arr[0].equalsIgnoreCase("NumberOfPreferredNeighbors")) {
//                    peerInit.numberOfPreferredNeighbours = Integer.parseInt(arr[1]);
//                } else if (arr[0].equalsIgnoreCase("UnchokingInterval")) {
//                    peerInit.unchockingInterval = Integer.parseInt(arr[1]);
//                } else if (arr[0].equalsIgnoreCase("OptimisticUnchokingInterval")) {
//                    peerInit.optimisticUnchokingInterval = Integer.parseInt(arr[1]);
//                } else if (arr[0].equalsIgnoreCase("FileName")) {
//                    peerInit.fileName = arr[1];
//                } else if (arr[0].equalsIgnoreCase("FileSize")) {
//                    peerInit.fileSize = Integer.parseInt(arr[1]);
//                } else if (arr[0].equalsIgnoreCase("PieceSize")) {
//                    peerInit.pieceSize = Integer.parseInt(arr[1]);
//                }
//            }
//            GetPeerDetails peerDetails = new GetPeerDetails();
//            peerDetails.initialize();
//            int fileStatus = peerDetails.getFileStatus(peerId);
//            if (fileStatus == 0) {
//                peerInit.isFileReceived = true;
//            }
//            peerInit.isProcessComplete = false;
//        } catch (IOException e) {
//            throw e;
//        }

    //}

    // Main method to start the peer process
    public static void main(String args[]) {
        if (args.length == 0) {
            System.out.println("No peer id is passed. Please pass it.");
        } else {
            peerProcess peerProcess = new peerProcess();
            peer_id = args[0];
            peerProcess.run(args[0]);
        }
    }


}