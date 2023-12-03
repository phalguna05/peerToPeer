package peerFunctions;

import config.PeerInitialization;
import helperFunctions.GetPeerDetails;
import peerFunctions.Handshake;
import peerFunctions.ThreadCreation;

import java.io.*;
import java.net.Socket;

// This class handles connections between peers in a peer-to-peer network
public class HandleConnections extends Thread{
    private int ownId;// ID of this peer
    private int remoteId;// ID of the remote peer to connect to
    private int remotePort;// Port number of the remote peer
    private InputStream io;// Input stream for reading data from the remote peer
    private OutputStream oo;// Output stream for writing data to the remote peer
    private Socket clientSocket;// Socket for network communication with the remote peer
    private static PeerInitialization peer;// Initialization details of the peer

    // Constructor to initialize the connection handling
    public HandleConnections(int peerId, int remId, PeerInitialization peer) {
        ownId = peerId;
        remoteId = remId;
        this.peer = peer;

        // Retrieve details about the remote peer
        GetPeerDetails peerDetails = new GetPeerDetails();
        peerDetails.initialize();
        String convertedRemoteId = String.valueOf(remoteId);
        remotePort = peerDetails.getPortNumber(convertedRemoteId);
        String remoteAddress = peerDetails.getHostAddress(convertedRemoteId);

        try {
            // Establish a socket connection to the remote peer
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
            // Create and send a handshake to the remote peer
            Handshake newHandshake = new Handshake();
            byte[] header = newHandshake.createHeader(ownId);
            peer.addHandshakeRequest(remoteId);// Register the handshake request
            oo.write(header);// Write the handshake header to the output stream
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}