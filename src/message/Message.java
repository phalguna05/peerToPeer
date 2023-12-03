package message;

import java.util.ArrayList;

public class Message {
    public static byte[] messageLength;
    public static byte[] messageType;
    public static byte[] messagePayload;

    public static void convertMessageLength(int value) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((value >> 24) & 0xFF);
        bytes[1] = (byte) ((value >> 16) & 0xFF);
        bytes[2] = (byte) ((value >> 8) & 0xFF);
        bytes[3] = (byte) (value & 0xFF);
        messageLength = bytes;
    }

    public static void convertMessageType(int value) {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (value & 0xFF);
        messageType = bytes;
    }

    public static void addMessagePayload(byte[] payload){
        messagePayload = payload;
    }

    public static byte[] getMessage(int peerId){
        byte[] headerBytes = new byte[4];
        headerBytes[0] = (byte) ((peerId >> 24) & 0xFF);
        headerBytes[1] = (byte) ((peerId >> 16) & 0xFF);
        headerBytes[2] = (byte) ((peerId >> 8) & 0xFF);
        headerBytes[3] = (byte) (peerId & 0xFF);
        int totalLength = messageLength.length + messageType.length + messagePayload.length+headerBytes.length;
        byte[] message = new byte[totalLength];
        System.arraycopy(messageLength, 0, message, 0, messageLength.length);
        System.arraycopy(messageType, 0, message, messageLength.length, messageType.length);
        System.arraycopy(messagePayload, 0, message, messageLength.length + messageType.length, messagePayload.length);
        System.arraycopy(headerBytes,0,message,messageLength.length+messageType.length+messagePayload.length,headerBytes.length);
        return message;
    }

    public static int getTypeOfTheMessage(ArrayList<Integer> arr){
        int type = arr.get(4);
        int convertedType = type & 0xFF;
        return convertedType;
    }

    public static ArrayList<Integer> getPayloadFromMessage(ArrayList<Integer> arr){
        ArrayList<Integer> payload = new ArrayList<>();
        for(int i=5;i<arr.size()-4;i++){
            payload.add(arr.get(i));
        }
        return payload;
    }



}
