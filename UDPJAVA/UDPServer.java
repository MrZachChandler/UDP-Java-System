import java.io.*;
import java.net.*;

class UDPServer {

   public static void main(String args[]) throws Exception {

      DatagramSocket serverSocket = new DatagramSocket(10050);

      byte[] receiveData = new byte[128];
      byte[] sendData = new byte[128];

      while (true) {
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
         serverSocket.receive(receivePacket);


         // Get IP Address and Port number of Client Machine
         InetAddress IPAddress = receivePacket.getAddress();
         int port = receivePacket.getPort();

         // Get request from datagram
         String clientReq = new String(receivePacket.getData());
         String[] splitRequest = clientReq.split(" ");

         // Get filename and access file
         String fileName = splitRequest[1];
         RandomAccessFile data = new RandomAccessFile(fileName, "r");
         long fileSize =  data.length();
         int dataOffset = 0;
         int sequenceNum = 0;

         // Create and Send Packets
         while (dataOffset != -1) {
            String pseudoHeader = "";
            String infoToSend = "";
            byte[] header;
            byte[] packet;

            // If no packets have been sent, make file header packet
            if (sequenceNum == 0) {
               pseudoHeader = makeFileHeader(sequenceNum, fileSize);
               header = pseudoHeader.getBytes();
               packet = padPacketWithSpaces(header);
            }
            // Else, make packet header packet
            else {
               pseudoHeader = makePacketHeader(sequenceNum);
               header = pseudoHeader.getBytes();
               packet = padPacketWithSpaces(header);
               dataOffset = data.read(packet, header.length, (packet.length - header.length));
            }

            System.out.println("Processing packet " + (sequenceNum));

            infoToSend = calculateCheckSum(packet);

            System.out.println(infoToSend);
            System.out.println("Sending packet " + (sequenceNum) + "\n");

            // If last packet, send 1 null byte
            if (dataOffset == -1) {
               header[header.length - 1] = 0;
               infoToSend = calculateCheckSum(header);
               sendData = infoToSend.getBytes();
               DatagramPacket sendPacket =
                     new DatagramPacket(sendData, sendData.length, IPAddress, port);
               serverSocket.send(sendPacket);
            }
            // Else send packets with the data
            else {
               sendData = infoToSend.getBytes();
               DatagramPacket sendPacket =
                     new DatagramPacket(sendData, sendData.length, IPAddress, port);
               serverSocket.send(sendPacket);
            }
            sequenceNum++;
         }
      }
   }

   public static String calculateCheckSum(byte[] packetInfo) {
      int fullCheckSum = checkSum(packetInfo);
      String message = new String(packetInfo);
      String checkSum = Integer.toString(fullCheckSum);
      message = insertChecksum(packetInfo, checkSum);
      return message;
   }

   public static byte[] padPacketWithSpaces(byte[] header) {
      byte[] paddedHeader = new byte[128];
      for (int offset = 0; offset < paddedHeader.length; offset++) {
         if (offset < header.length) {
            paddedHeader[offset] = header[offset];
         } else {
            paddedHeader[offset] = 32;
         }
      }
      return paddedHeader;
   }

   public static String makeFileHeader(int packetNum , long fileSize) {
      String pseudoHeader = "Packet " + (packetNum) + "\n" + "HTTP/1.0 200 Document Follows\r\n"
            + "Checksum: " + "00000\r\n" + "Content-Type: text/plain\r\n"
            + "Content-Length: " + fileSize + "\r\n\r\n" + "Data";
      return pseudoHeader;
   }

   public static String makePacketHeader(int packetNum) {
      String pseudoHeader = "Packet " + (packetNum) + "\n" + "Checksum: " + "00000\r\n" + "\r\n";
      return pseudoHeader;
   }

   public static String insertChecksum(byte[] message, String checkSum) {
      String packetInfo = new String(message);
      int index = packetInfo.indexOf(":") + 2;
      byte[] temp = checkSum.getBytes();

      switch (checkSum.length()) {
         case 2:
            message[index + 3] = temp[0];
            message[index + 4] = temp[1];
            break;
         case 3:
            message[index + 2] = temp[0];
            message[index + 3] = temp[1];
            message[index + 4] = temp[2];

            break;
         case 4:
            message[index + 1] = temp[0];
            message[index + 2] = temp[1];
            message[index + 3] = temp[2];
            message[index + 4] = temp[3];
            break;
         case 5:
            message[index] = temp[0];
            message[index + 1] = temp[1];
            message[index + 2] = temp[2];
            message[index + 3] = temp[3];
            message[index + 4] = temp[4];
            break;
         default:
            break;
      }
      String info = new String(message);
      return info;
   }

   public static int checkSum(byte[] sendData) {
      int sum = 0;

      for (int i = 0; i < sendData.length; i++) {
         sum += (int) sendData[i];
      }
      return sum;
   }
}
