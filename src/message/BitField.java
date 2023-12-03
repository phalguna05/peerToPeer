package message;

import logger.CustomLogger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
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
    public BitField(int fileSize,int pieceSize, Boolean hasFile) throws IOException {
        Properties properties = new Properties();
        numberOfPieces = fileSize / pieceSize;
        bitField = new byte[numberOfPieces];
        bitFieldMap = new HashMap<>();
        bitFieldSentSet = new HashSet<>();
        unavailableBits = new HashSet<>();
        pieces = new Piece[numberOfPieces];
        for(int i=0;i<numberOfPieces;i++){
            pieces[i] = new Piece(i);
        }
        if(hasFile) {
            File fi = new File("tree.jpg");
            byte[] fileContent = Files.readAllBytes(fi.toPath());
            int startInd = 0;
            int endInd = pieceSize;
            for(int i = 0;i<numberOfPieces;i++){
                byte[] portionArray = Arrays.copyOfRange(fileContent, startInd, endInd);
                pieces[i].setContent(portionArray);
                startInd = endInd;
                endInd = endInd+pieceSize;
            }
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
        for(int i=0;i<numberOfPieces;i++){
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

    public void addBitFieldIndex(int ind, CustomLogger logger, Socket connection){
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
