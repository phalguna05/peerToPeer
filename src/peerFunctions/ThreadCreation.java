package peerFunctions;

import config.PeerInitialization;
import helperFunctions.GetPeerDetails;
import logger.CustomLogger;
import message.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
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
    private static int portNumber;
    private static String id;
    private static ServerSocket serverSocket;
    private static Socket clientSocket;

    public static PeerInitialization peer;
    public static CustomLogger logger;

    public ThreadCreation(String id, PeerInitialization peerInfo, CustomLogger logger) throws Exception {
        this.id = id;
        this.peer = peerInfo;
        this.logger = logger;
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
                    long startTime = System.currentTimeMillis();
                    while ((bytesRead = input.read()) != -1) {
                        arr.add(bytesRead);
                        if (input.available() <= 0) {
                            break;
                        }
                    }
                    if(arr.size()>0){
                    long endTime = System.currentTimeMillis();
                    long elapsedTime = endTime - startTime;
                    Boolean isHandshakeMsg = decodeHeader(arr);
                    if (isHandshakeMsg) {
                        int peerId = ((arr.get(28) & 0xFF) << 24) |
                                ((arr.get(29) & 0xFF) << 16) |
                                ((arr.get(30) & 0xFF) << 8) |
                                (arr.get(31) & 0xFF);
                        logger.tcpDone(peerId, Integer.parseInt(id));
                        if (peer.handShakeRequestMap.containsKey(Integer.toString(peerId))) {
                            Message msg = new Message();
                            msg.convertMessageLength(peer.bitFieldObj.numberOfPieces + 1);
                            msg.convertMessageType(5);
                            msg.addMessagePayload(peer.bitFieldObj.bitField);
                            byte[] bitFieldMsg = msg.getMessage(Integer.parseInt(id));

                            try {
//                                if(peer.socketMap.containsKey(Integer.toString(peerId))){
//                                    OutputStream oo = peer.socketMap.get(Integer.toString(peerId));
//                                    oo.write(bitFieldMsg);
//                                }
//                                else {
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
                            peer.addHandshakeToMap(peerId, arr);
                            System.out.println("Handshake message is established with " + arr.get(arr.size() - 1));
                            HandleConnections hn = new HandleConnections(Integer.parseInt(id), peerId, peer);
                            hn.start();
                        }
                    } else {
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

                    connection.close();
                } catch (IOException ioException) {
                    System.out.println("Disconnect with Client ");
                }
            }

        }
        private static void notInterestedMsgProcess(ArrayList<Integer> arr, Message msg, int peerId) {
            peer.addNotInterestedNeighbor(peerId);
        }
        private static void pieceMsgProcess(ArrayList<Integer> arr, Message msg, int peerId, long time) {
            byte[] payload = new byte[arr.size() - 12];
            ArrayList<Integer> index = new ArrayList<>();
            for (int i = 5; i < 9; i++) {
                index.add(arr.get(i));
            }
            int ind = peer.byteArrayToIntBigEndian(index);

            peer.bitFieldObj.addBitFieldIndex(ind,logger);
            peer.bitFieldObj.pieces[ind].setContent(payload);
            peer.bitFieldObj.pieces[ind].setIsPresent(true);
            peer.addDownloadRates(peerId, time, arr.size());
            logger.downloading(Integer.parseInt(id),peerId,ind,peer.getNumberOfPieces());
            if(peer.neighborsWhichUnChokedMe.contains(Integer.toString(peerId))) {
                int indVal = peer.getPieceToRequest(peerId);
                if (indVal != -1) {
                    byte[] bytes = new byte[4];
                    bytes[0] = (byte) ((indVal >> 24) & 0xFF);
                    bytes[1] = (byte) ((indVal >> 16) & 0xFF);
                    bytes[2] = (byte) ((indVal >> 8) & 0xFF);
                    bytes[3] = (byte) (indVal & 0xFF);
                    msg.convertMessageLength(5);
                    msg.convertMessageType(6);
                    msg.addMessagePayload(bytes);
                    byte[] requestMsg = msg.getMessage(Integer.parseInt(id));
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

                        peer.socketMap.put(Integer.toString(peerId), oo);
                        //}
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
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
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        private static void requestMsgProcess(ArrayList<Integer> arr, Message msg, int peerId) {
            ArrayList<Integer> bytes = new ArrayList<>();
            for (int i = 5; i < 9; i++) {
                bytes.add(arr.get(i));
            }
            int value = peer.byteArrayToIntBigEndian(bytes);
            byte[] byteArr = new byte[4];
            byteArr[0] = (byte) ((value >> 24) & 0xFF);
            byteArr[1] = (byte) ((value >> 16) & 0xFF);
            byteArr[2] = (byte) ((value >> 8) & 0xFF);
            byteArr[3] = (byte) (value & 0xFF);
            byte[] content = peer.getPieceByIndex(value);
            byte[] payload = new byte[4 + content.length];
            System.arraycopy(byteArr, 0, payload, 0, byteArr.length);

            // Copy array2 into the result array, starting from the end of array1
            System.arraycopy(content, 0, payload, byteArr.length, content.length);

            msg.convertMessageLength(1 + payload.length);
            msg.convertMessageType(7);
            msg.addMessagePayload(payload);
            byte[] pieceMessage = msg.getMessage(Integer.parseInt(id));
            try {
                GetPeerDetails pd = new GetPeerDetails();
                String remoteAddress = pd.getHostAddress(Integer.toString(peerId));
                int remotePort = pd.getPortNumber(Integer.toString(peerId));
                clientSocket = new Socket(remoteAddress, remotePort);
                InputStream io = clientSocket.getInputStream();
                OutputStream oo = clientSocket.getOutputStream();
                oo.write(pieceMessage);

                peer.socketMap.put(Integer.toString(peerId), oo);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        private static void unChokeMsgProcess(ArrayList<Integer> arr, Message msg, int peerId) {
            logger.unchoking(Integer.parseInt(id), peerId);
            peer.addNeighborsWhoUnchokedMe(peerId);
            int ind = peer.getPieceToRequest(peerId);
            if (ind != -1) {
                byte[] bytes = new byte[4];
                bytes[0] = (byte) ((ind >> 24) & 0xFF);
                bytes[1] = (byte) ((ind >> 16) & 0xFF);
                bytes[2] = (byte) ((ind >> 8) & 0xFF);
                bytes[3] = (byte) (ind & 0xFF);
                msg.convertMessageLength(5);
                msg.convertMessageType(6);
                msg.addMessagePayload(bytes);
                byte[] requestMsg = msg.getMessage(Integer.parseInt(id));
                try {
                    GetPeerDetails pd = new GetPeerDetails();
                    String remoteAddress = pd.getHostAddress(Integer.toString(peerId));
                    int remotePort = pd.getPortNumber(Integer.toString(peerId));
                    clientSocket = new Socket(remoteAddress, remotePort);
                    InputStream io = clientSocket.getInputStream();
                    OutputStream oo = clientSocket.getOutputStream();
                    oo.write(requestMsg);

                    peer.socketMap.put(Integer.toString(peerId), oo);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
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

                    peer.socketMap.put(Integer.toString(peerId), oo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        private static void chokeMsgProcess(ArrayList<Integer> arr, Message msg, int peerId) {
            logger.choking(Integer.parseInt(id), peerId);
            peer.neighborsWhichUnChokedMe.remove(Integer.toString(peerId));
        }
        private static void calculatePreferredNeighbors() {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new FindPreferredNeighbors(), 0, peer.unchockingInterval * 1000);
            timer.scheduleAtFixedRate(new FindOptimisticallyUnchokedNeighbor(), 0, peer.optimisticUnchokingInterval * 1000);
        }
        private static void interestedMsgProcess(ArrayList<Integer> arr, Message msg, int peerId) {
            logger.interested(Integer.parseInt(id), peerId);
            peer.addInterestedNeighbor(peerId);
        }
        private static void bitFieldMsgProcess(ArrayList<Integer> arr, Message msg, Long time, int peerId) {
            ArrayList<Integer> payload = msg.getPayloadFromMessage(arr);
            peer.addDownloadRates(peerId, time, arr.size());
            Boolean isThereInterestingPieces = peer.bitFieldObj.addBitFieldToMap(Integer.toString(peerId), payload);
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
        private static Boolean decodeHeader(ArrayList<Integer> arr) {
            // Assuming the handshake header is the first 18 bytes
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
        static class FindPreferredNeighbors extends TimerTask {
            @Override
            public void run() {
                // Code to be executed at each scheduled interval
                HashMap<String, Long> downloadRates = peer.downloadRates;
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
                Message msg = new Message();
                msg.convertMessageLength(5);
                msg.convertMessageType(1);
                msg.addMessagePayload(new byte[0]);
                byte[] unChokeMessage = msg.getMessage(Integer.parseInt(id));
                int[] arrayOfPeers = new int[peer.numberOfPreferredNeighbours];
                final int[] index = {0};
                preferredNeighborValues.forEach((key, value) ->
                        {
                            arrayOfPeers[index[0]] = Integer.parseInt(key);
                            index[0] = index[0] + 1;
                            try {
                                GetPeerDetails pd = new GetPeerDetails();
                                String remoteAddress = pd.getHostAddress(key);
                                int remotePort = pd.getPortNumber(key);
                                clientSocket = new Socket(remoteAddress, remotePort);
                                InputStream io = clientSocket.getInputStream();
                                OutputStream oo = clientSocket.getOutputStream();
                                oo.write(unChokeMessage);

                                peer.socketMap.put(key, oo);
                                //}
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );
                msg.convertMessageType(0);
                byte[] chokeMessage = msg.getMessage(Integer.parseInt(id));
                peer.addPreferredNeighbors(arrayOfPeers);
                logger.changeOfPreferredNeighbors(Integer.parseInt(id),arrayOfPeers);
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
                            GetPeerDetails pd = new GetPeerDetails();
                            String remoteAddress = pd.getHostAddress(key);
                            int remotePort = pd.getPortNumber(key);
                            clientSocket = new Socket(remoteAddress, remotePort);
                            InputStream io = clientSocket.getInputStream();
                            OutputStream oo = clientSocket.getOutputStream();
                            oo.write(chokeMessage);

                            peer.socketMap.put(key, oo);
                            //}
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                peer.addUnPreferredNeighbors(unPreferredNeighbors);
            }
        }
        static class FindOptimisticallyUnchokedNeighbor extends TimerTask {
            @Override
            public void run() {
                // Code to be executed at each scheduled interval
                peer.optimisticallyUnchokedNeighbor = peer.getRandomValue();
                Message msg = new Message();
                msg.convertMessageLength(5);
                msg.convertMessageType(1);
                msg.addMessagePayload(new byte[0]);
                byte[] unChokeMessage = msg.getMessage(Integer.parseInt(id));
                if(peer.optimisticallyUnchokedNeighbor!=null) {
                    logger.changeOfOptimisticallyUnchokedNeighbor(Integer.parseInt(id), Integer.parseInt(peer.optimisticallyUnchokedNeighbor));
                    try {
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




