import helperFunctions.GetPeerDetails;
import peerFunctions.Handshake;

import java.io.*;
import java.net.Socket;


public class HandleConnections implements Runnable{
    private int ownId;
    private int remoteId;
    private int remotePort;
    private InputStream io;
    private OutputStream oo;
    private Socket clientSocket;


    public HandleConnections(int peerId, int remId) {
        ownId = peerId;
        remoteId = remId;
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

            oo.write(header);
            while (true) {
                try {
                    int bytesRead;
                    boolean isHandshakeReceived = false;
                    bytesRead = io.read();

//                    String arr = Arrays.toString(peerMsg);

                    System.out.println("Received Msg from peer is "+bytesRead);
                    if(bytesRead!=0){
                        break;
                    }

                } catch(IOException e) {
                    e.printStackTrace();
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}