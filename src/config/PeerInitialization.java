package config;

import helperFunctions.GetPeerDetails;
import message.BitField;
import message.Piece;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

// Class responsible for initializing and managing peer configurations
public class PeerInitialization {
        // Configuration properties
        public static int numberOfPreferredNeighbours;
        public static int unchockingInterval;
        public static int optimisticUnchokingInterval;
        public static String fileName;
        public static int fileSize;
        public static int pieceSize;


        public static boolean isFileReceived;
        public static boolean hasFile = false;
        public static boolean isProcessComplete;

        // Bitfield object to manage file pieces
        public static BitField bitFieldObj;
        public static String peerId;

        // Maps and sets to manage peer states and connections
        public static HashMap<String, ArrayList<Integer>> handShakeMap = new HashMap<>();

        public static HashMap<String,Boolean> handShakeRequestMap = new HashMap<>();

        public static HashSet<Integer> interestedNeighbors;

        public static HashSet<Integer> notInterestedNeighbors;

        public static HashSet<Integer> pieceRequestSet;

        public static HashMap<String, Long> downloadRates;
        public static HashSet<String> preferredNeighbors;

        public static HashSet<String> unPreferredNeighbors;

        public static HashMap<String, OutputStream> socketMap;
        public static String optimisticallyUnchokedNeighbor;
        public static HashSet<String> neighborsWhichUnChokedMe;

        public static Boolean isDownloadComplete = false;

        // Constructor for initializing peer configurations
        public PeerInitialization(String peerId){
                isFileReceived = false;
                this.peerId = peerId;
                String fileN = "Common.cfg";

                // Read configuration properties from the Common.cfg file
                try (BufferedReader br = new BufferedReader(new FileReader(fileN))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                                String[] words = line.split("\\s+"); // Split the line into words
                                // Assign values to configuration properties based on file content
                                switch(words[0]){
                                        case "NumberOfPreferredNeighbors":
                                                numberOfPreferredNeighbours = Integer.parseInt(words[1]);
                                                break;
                                        case "UnchokingInterval":
                                                unchockingInterval = Integer.parseInt(words[1]);
                                                break;
                                        case "OptimisticUnchokingInterval":
                                                optimisticUnchokingInterval = Integer.parseInt(words[1]);
                                                break;
                                        case "FileName":
                                                fileName = words[1];
                                                break;
                                        case "FileSize":
                                                fileSize = Integer.parseInt(words[1]);
                                                break;
                                        case "PieceSize":
                                                pieceSize = Integer.parseInt(words[1]);
                                                break;
                                        default:
                                                System.out.println("Unkown value in common.cfg file");
                                }
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                }
                GetPeerDetails peerDetails = new GetPeerDetails();
                // Initialize peer details and various sets and maps for peer management
                peerDetails.initialize();
                interestedNeighbors = new HashSet<>();
                notInterestedNeighbors = new HashSet<>();
                downloadRates = new HashMap<>();
                preferredNeighbors = new HashSet<>();

                pieceRequestSet = new HashSet<>();
                neighborsWhichUnChokedMe = new HashSet<>();
                socketMap = new HashMap<>();
                unPreferredNeighbors = new HashSet<>();
                int fileStatus = peerDetails.getFileStatus(peerId);
                if (fileStatus == 1) {
                        hasFile = true;
                }
                isProcessComplete = false;
                bitFieldObj = new BitField(fileSize,pieceSize,hasFile);
                bitFieldObj.peerID = Integer.parseInt(peerId);
        }

        // Adds a received handshake message to the handshake map for a specific peer ID
        public void addHandshakeToMap(int peerID,ArrayList<Integer> handshakeMessage){
                handShakeMap.put(Integer.toString(peerID),handshakeMessage);
        }

        // Marks that a handshake request has been sent to a specific peer ID
        public void addHandshakeRequest(int peerID){
                handShakeRequestMap.put(Integer.toString(peerID),true);
        }

        // Adds a peer ID to the set of interested neighbors
        public void addInterestedNeighbor(int peerID){
                interestedNeighbors.add(peerID);
        }

        // Calculates and updates the download rate for a specific peer ID
        public void addDownloadRates(int peerID, long time, int dataSize){
                long convertedDataSize = dataSize;
                long downloadRate;
                // Calculate download rate, handling division by zero
                if(time!=0) {
                        downloadRate = convertedDataSize / time;
                }
                else {
                        downloadRate = 1000000;// Arbitrary high value if time is zero
                }

                // Update download rate if it's higher than the existing rate
                if(downloadRates.containsKey(Integer.toString(peerID))){
                        if(downloadRate>downloadRates.get(Integer.toString(peerID))){
                                downloadRates.put(Integer.toString(peerID),downloadRate);
                        }
                }
                else{
                        downloadRates.put(Integer.toString(peerID),downloadRate);
                }

        }

        // Adds a set of peer IDs to the set of preferred neighbors
        public void addPreferredNeighbors(int[] arr){
                for(int i=0;i<arr.length;i++){
                        preferredNeighbors.add(Integer.toString(arr[i]));
                }
        }

        // Adds a set of peer IDs to the set of unpreferred neighbors
        public void addUnPreferredNeighbors(int[] arr){
                for(int i=0;i<arr.length;i++){
                        unPreferredNeighbors.add(Integer.toString(arr[i]));
                }
        }

        // Determines which piece to request next from a specific peer
        public int getPieceToRequest(int peerId){
                ArrayList<Integer> remotePeerBitField = bitFieldObj.bitFieldMap.get(Integer.toString(peerId));
                for(int i=0;i<remotePeerBitField.size();i++){
                        int pieceStatus = bitFieldObj.bitField[i] & 0xFF;
                        if(remotePeerBitField.get(i) == 1 && pieceStatus==0 && !pieceRequestSet.contains(i)){
                                pieceRequestSet.add(i);
                                return i;// Return index of the piece to request
                        }
                }
                return -1;// Return -1 if no suitable piece found
        }

        // Converts a byte array (big endian) to an integer
        public static int byteArrayToIntBigEndian(ArrayList<Integer> byteArray) {
                if (byteArray.size()!= 4) {
                        throw new IllegalArgumentException("Byte array must have exactly 4 bytes");
                }

                int value = 0;
                for (int i = 0; i < 4; i++) {
                        value = (value << 8) | (byteArray.get(i) & 0xFF);
                }

                return value;
        }

        // Retrieves the content of a piece by its index
        public static byte[] getPieceByIndex(int index){
                Piece piece = bitFieldObj.pieces[index];
                return piece.getContent();
        }

        // Adds a peer ID to the set of not interested neighbors
        public static void addNotInterestedNeighbor(int peerId){
                notInterestedNeighbors.add(peerId);
        }
        
        // Gets a random value (peer ID) from the set of unpreferred neighbors
        public static <T> T getRandomValue() {
                // Convert the HashSet to an array
                T[] array = (T[]) unPreferredNeighbors.toArray();
                // Check if the HashSet is not empty
                if (array.length > 0) {
                        // Generate a random index
                        int randomIndex = new Random().nextInt(array.length);

                        // Return the random value from the array
                        return array[randomIndex];
                } else {
                        // Return null or throw an exception, depending on your use case
                        return null;
                }
        }

        // Adds a peer ID to the set of neighbors who have unchoked this peer
        public static void addNeighborsWhoUnchokedMe(int peerId){
                neighborsWhichUnChokedMe.add(Integer.toString(peerId));
        }

        // Adds a peer ID to the set of neighbors who have unchoked this peer
        public static int getNumberOfPieces(){
                int numberOfPiecesHave = bitFieldObj.numberOfPieces - bitFieldObj.remainingBits;
                return numberOfPiecesHave;
        }

}
