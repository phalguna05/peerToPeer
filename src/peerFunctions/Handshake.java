package peerFunctions;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.lang.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Handshake{
    private static String handshakeHeader = "P2PFILESHARINGPROJ";
    public byte[] createHeader(int peerID){
        byte[] headerBytes = new byte[32];
        byte[] headerStringBytes = handshakeHeader.getBytes(StandardCharsets.UTF_8);

        // Copying the handshake header
        System.arraycopy(headerStringBytes, 0, headerBytes, 0, headerStringBytes.length);

        // Adding zero bits after the handshake header
        Arrays.fill(headerBytes, 18, 28, (byte) 0);

        // Adding the 4-byte peer ID at the end
        headerBytes[28] = (byte) ((peerID >> 24) & 0xFF);
        headerBytes[29] = (byte) ((peerID >> 16) & 0xFF);
        headerBytes[30] = (byte) ((peerID >> 8) & 0xFF);
        headerBytes[31] = (byte) (peerID & 0xFF);
        return headerBytes;
    }

    public void sendHandshakeMessage(ObjectOutputStream out,int peerID,int remoteID){
        byte[] header = createHeader(peerID);
        try{
            out.writeObject(header);
            out.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }


    public static void main(String[] args){}


}