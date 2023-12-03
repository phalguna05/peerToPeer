package message;

import logger.CustomLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.*;
import java.net.Socket;


// Class representing the bitfield of a peer in a peer-to-peer network
public class BitField {
    public int numberOfPieces;// Total number of pieces of the file

    public int peerID;// ID of this peer
    public Piece[] pieces;// Array of Piece objects representing each piece of the file
    public static byte[] bitField;// Bitfield representing which pieces this peer has
    public static HashMap<String,ArrayList<Integer>> bitFieldMap;// Map of bitfields of other peers
    public static HashSet<String> bitFieldSentSet;// Set of peers to whom the bitfield has been sent

    public static HashSet<Integer> unavailableBits;// Set of indices of pieces not yet downloaded

    public static int remainingBits;// Number of pieces remaining to be downloaded

    // Constructor to initialize the bitfield
    public BitField(int fileSize,int pieceSize, Boolean hasFile) {
        Properties properties = new Properties();
        numberOfPieces = (int) Math.ceil(fileSize / pieceSize);// Calculate the total number of pieces
        //Creating piece objects
        bitField = new byte[numberOfPieces];
        bitFieldMap = new HashMap<>();
        bitFieldSentSet = new HashSet<>();
        unavailableBits = new HashSet<>();
        pieces = new Piece[numberOfPieces];

        // Initialize each piece
        for(int i=0;i<numberOfPieces;i++){
            pieces[i] = new Piece(i);
        }

        if(hasFile){
            // If the file is already present, mark all pieces as available
            setAllElementsToOne(bitField);
            for(int i=0;i<numberOfPieces;i++){
                pieces[i].setIsPresent(true);
            }
            remainingBits = 0;
        }
        else{
            // If the file is not present, mark all pieces as unavailable
            for(int i=0;i<numberOfPieces;i++){
                unavailableBits.add(i);
            }
            remainingBits = numberOfPieces;
        }
    }

    // Method to update the bitfield map with information from a remote peer
    public Boolean addBitFieldToMap(String remotePeerId, ArrayList<Integer> remotePeerBitField){
        bitFieldMap.put(remotePeerId,remotePeerBitField);
        for(int i=0;i<remotePeerBitField.size();i++){
            int pieceStatus = bitField[i] & 0xFF;
            if(remotePeerBitField.get(i) == 1 && pieceStatus==0){
                return true;
            }
        }
        return false;

    }

    // Method to mark that the bitfield has been sent to a particular peer
    public void addPeerToBitSet(String peerId){
        bitFieldSentSet.add(peerId);
    }

    // Helper method to set all elements of a byte array to one
    public static void setAllElementsToOne(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = 1;
        }
    }

    // Method to mark a piece as having been downloaded
    public void addBitFieldIndex(int ind, CustomLogger logger){
        bitField[ind] = 1;
        unavailableBits.remove(Integer.toString(ind));
        remainingBits = remainingBits - 1;
        
        // If all pieces have been downloaded, notify the server
        if(remainingBits==0){
            try {
                Socket clientSocket = new Socket("localhost", 8000);
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                String[] payload = {Integer.toString(peerID),"Finished Downloading"};
                logger.downloaded(peerID);
                out.writeObject(payload);
            }catch (IOException e) {
                System.err.println("Connection refused. You need to initiate a server first.");
            }


        }
    }
}
