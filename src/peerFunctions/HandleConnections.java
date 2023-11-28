package peerFunctions;

import config.PeerInitialization;
import helperFunctions.GetPeerDetails;
import peerFunctions.Handshake;
import peerFunctions.ThreadCreation;

import java.io.*;
import java.net.Socket;


public class HandleConnections extends Thread{
    private int ownId;
    private int remoteId;
    private int remotePort;
    private InputStream io;
    private OutputStream oo;
    private Socket clientSocket;
    private static PeerInitialization peer;


    public HandleConnections(int peerId, int remId, PeerInitialization peer) {
        ownId = peerId;
        remoteId = remId;
        this.peer = peer;
        GetPeerDetails peerDetails = new GetPeerDetails();
        peerDetails.initialize();
        String convertedRemoteId = String.valueOf(remoteId);
        remotePort = peerDetails.getPortNumber(convertedRemoteId);
        String remoteAddress = peerDetails.getHostAddress(convertedRemoteId);
        try {
            clientSocket = new Socket(remoteAddress, remotePort);
            io = clientSocket.getInputStream();
            oo = clientSocket.getOutputStream();
        }catch(IOException e) {
            e.printStackTrace();
        }

    }

//    public void init(){
//        System.out.println("In thread");
//        Thread runner = new Thread(this);
//        runner.start();
//    }

    public void run() {
        try {
            Handshake newHandshake = new Handshake();
            byte[] header = newHandshake.createHeader(ownId);
            peer.addHandshakeRequest(remoteId);
            oo.write(header);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}