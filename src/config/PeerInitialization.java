package config;

import helperFunctions.GetPeerDetails;
import message.BitField;
import message.Piece;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class PeerInitialization {
        public static int numberOfPreferredNeighbours;
        public static int unchockingInterval;
        public static int optimisticUnchokingInterval;
        public static String fileName;
        public static int fileSize;
        public static int pieceSize;
        public static boolean isFileReceived;
        public static boolean hasFile = false;
        public static boolean isProcessComplete;

        public static BitField bitFieldObj;
        public static String peerId;

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

        public PeerInitialization(String peerId){
                isFileReceived = false;
                this.peerId = peerId;
                String fileN = "Common.cfg";
                try (BufferedReader br = new BufferedReader(new FileReader(fileN))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                                String[] words = line.split("\\s+"); // Split the line into words
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

        public void addHandshakeToMap(int peerID,ArrayList<Integer> handshakeMessage){
                handShakeMap.put(Integer.toString(peerID),handshakeMessage);
        }

        public void addHandshakeRequest(int peerID){
                handShakeRequestMap.put(Integer.toString(peerID),true);
        }

        public void addInterestedNeighbor(int peerID){
                interestedNeighbors.add(peerID);
        }

        public void addDownloadRates(int peerID, long time, int dataSize){
                long convertedDataSize = dataSize;
                long downloadRate;

                if(time!=0) {
                        downloadRate = convertedDataSize / time;
                }
                else {
                        downloadRate = 1000000;
                }
                if(downloadRates.containsKey(Integer.toString(peerID))){
                        if(downloadRate>downloadRates.get(Integer.toString(peerID))){
                                downloadRates.put(Integer.toString(peerID),downloadRate);
                        }
                }
                else{
                        downloadRates.put(Integer.toString(peerID),downloadRate);
                }

        }

        public void addPreferredNeighbors(int[] arr){
                for(int i=0;i<arr.length;i++){
                        preferredNeighbors.add(Integer.toString(arr[i]));
                }
        }
        public void addUnPreferredNeighbors(int[] arr){
                for(int i=0;i<arr.length;i++){
                        unPreferredNeighbors.add(Integer.toString(arr[i]));
                }
        }

        public int getPieceToRequest(int peerId){
                ArrayList<Integer> remotePeerBitField = bitFieldObj.bitFieldMap.get(Integer.toString(peerId));
                for(int i=0;i<remotePeerBitField.size();i++){
                        int pieceStatus = bitFieldObj.bitField[i] & 0xFF;
                        if(remotePeerBitField.get(i) == 1 && pieceStatus==0 && !pieceRequestSet.contains(i)){
                                pieceRequestSet.add(i);
                                return i;
                        }
                }
                return -1;
        }

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

        public static byte[] getPieceByIndex(int index){
                Piece piece = bitFieldObj.pieces[index];
                return piece.getContent();
        }

        public static void addNotInterestedNeighbor(int peerId){
                notInterestedNeighbors.add(peerId);
        }

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

        public static void addNeighborsWhoUnchokedMe(int peerId){
                neighborsWhichUnChokedMe.add(Integer.toString(peerId));
        }

        public static int getNumberOfPieces(){
                int numberOfPiecesHave = bitFieldObj.numberOfPieces - bitFieldObj.remainingBits;
                return numberOfPiecesHave;
        }

}
