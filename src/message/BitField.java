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

public class BitField {
    public int numberOfPieces;

    public int peerID;
    public Piece[] pieces;
    public static byte[] bitField;
    public static HashMap<String,ArrayList<Integer>> bitFieldMap;
    public static HashSet<String> bitFieldSentSet;

    public static HashSet<Integer> unavailableBits;

    public static int remainingBits;
    public BitField(int fileSize,int pieceSize, Boolean hasFile) {
        Properties properties = new Properties();
        numberOfPieces = (int) Math.ceil(fileSize / pieceSize);
        //Creating piece objects
        bitField = new byte[numberOfPieces];
        bitFieldMap = new HashMap<>();
        bitFieldSentSet = new HashSet<>();
        unavailableBits = new HashSet<>();
        pieces = new Piece[numberOfPieces];
        for(int i=0;i<numberOfPieces;i++){
            pieces[i] = new Piece(i);
        }
        if(hasFile){
            setAllElementsToOne(bitField);
            for(int i=0;i<numberOfPieces;i++){
                pieces[i].setIsPresent(true);
            }
            remainingBits = 0;
        }
        else{
            for(int i=0;i<numberOfPieces;i++){
                unavailableBits.add(i);
            }
            remainingBits = numberOfPieces;
        }
    }

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

    public void addPeerToBitSet(String peerId){
        bitFieldSentSet.add(peerId);
    }

    public static void setAllElementsToOne(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = 1;
        }
    }

    public void addBitFieldIndex(int ind, CustomLogger logger){
        bitField[ind] = 1;
        unavailableBits.remove(Integer.toString(ind));
        remainingBits = remainingBits - 1;
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
