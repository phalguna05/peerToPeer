package message;

import java.util.ArrayList;

// Class representing a message in a peer-to-peer network
public class Message {
    public static byte[] messageLength;// Byte array to store the length of the message
    public static byte[] messageType;// Byte array to store the type of the message
    public static byte[] messagePayload;// Byte array to store the payload of the message

    // Method to convert message length to a byte array
    public static void convertMessageLength(int value) {
        byte[] bytes = new byte[4];
        // Break the integer value into 4 bytes and store it in the byte array
        bytes[0] = (byte) ((value >> 24) & 0xFF);
        bytes[1] = (byte) ((value >> 16) & 0xFF);
        bytes[2] = (byte) ((value >> 8) & 0xFF);
        bytes[3] = (byte) (value & 0xFF);
        messageLength = bytes;
    }

    // Method to convert message type to a byte array
    public static void convertMessageType(int value) {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (value & 0xFF);
        messageType = bytes;
    }

    // Method to add payload to the message
    public static void addMessagePayload(byte[] payload){
        messagePayload = payload;
    }

    // Method to construct the complete message
    public static byte[] getMessage(int peerId){
        // Construct the header bytes for the peer ID
        byte[] headerBytes = new byte[4];
        headerBytes[0] = (byte) ((peerId >> 24) & 0xFF);
        headerBytes[1] = (byte) ((peerId >> 16) & 0xFF);
        headerBytes[2] = (byte) ((peerId >> 8) & 0xFF);
        headerBytes[3] = (byte) (peerId & 0xFF);

        // Calculate the total length of the message
        int totalLength = messageLength.length + messageType.length + messagePayload.length+headerBytes.length;
        byte[] message = new byte[totalLength];
        // Copy the message length, type, payload, and header into the message
        System.arraycopy(messageLength, 0, message, 0, messageLength.length);
        System.arraycopy(messageType, 0, message, messageLength.length, messageType.length);
        System.arraycopy(messagePayload, 0, message, messageLength.length + messageType.length, messagePayload.length);
        System.arraycopy(headerBytes,0,message,messageLength.length+messageType.length+messagePayload.length,headerBytes.length);
        return message;
    }

    // Method to extract the message type from a byte array
    public static int getTypeOfTheMessage(ArrayList<Integer> arr){
        int type = arr.get(4);
        int convertedType = type & 0xFF;
        return convertedType;
    }

    // Method to extract the payload from a byte array
    public static ArrayList<Integer> getPayloadFromMessage(ArrayList<Integer> arr){
        ArrayList<Integer> payload = new ArrayList<>();
        for(int i=5;i<arr.size()-4;i++){
            payload.add(arr.get(i));
        }
        return payload;
    }



}
