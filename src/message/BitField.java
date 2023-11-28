package message;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class BitField {
    public int numberOfPieces;
    private Piece[] pieces;
    public static byte[] bitField;
    public static HashMap<String,ArrayList<Integer>> bitFieldMap;
    public static HashSet<String> bitFieldSentSet;
    public BitField(int fileSize,int pieceSize, Boolean hasFile) {
        Properties properties = new Properties();
        numberOfPieces = (int) Math.ceil(fileSize / pieceSize);
        //Creating piece objects
        numberOfPieces = (int) Math.ceil(numberOfPieces/8);

        pieces = new Piece[numberOfPieces];
        //initializing each Piece object with content and piece index
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = new Piece();
        }
        bitField = new byte[numberOfPieces];
        if(hasFile){
            setAllElementsToOne(bitField);
        }
    }

    public void addBitFieldToMap(String remotePeerId, ArrayList<Integer> remotePeerBitField){
        bitFieldMap.put(remotePeerId,remotePeerBitField);
    }

    public void addPeerToBitSet(String peerId){
        bitFieldSentSet.add(peerId);
    }

    public static void setAllElementsToOne(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = 1;
        }
    }
}
