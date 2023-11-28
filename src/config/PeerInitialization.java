package config;

import helperFunctions.GetPeerDetails;
import message.BitField;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                int fileStatus = peerDetails.getFileStatus(peerId);
                if (fileStatus == 1) {
                        hasFile = true;
                }
                isProcessComplete = false;
                bitFieldObj = new BitField(fileSize,pieceSize,hasFile);

        }

        public void addHandshakeToMap(int peerID,ArrayList<Integer> handshakeMessage){

                handShakeMap.put(Integer.toString(peerID),handshakeMessage);
        }

        public void addHandshakeRequest(int peerID){
                handShakeRequestMap.put(Integer.toString(peerID),true);
        }

}
