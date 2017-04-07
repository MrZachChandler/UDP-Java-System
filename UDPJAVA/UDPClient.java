import java.io.*;
import java.net.*;
import java.util.*;

class UDPClient {

   public static void main(String args[]) throws Exception {

      DatagramSocket clientSocket = new DatagramSocket(10049);

      InetAddress IPAddress = InetAddress.getByName("Zachs-MacBook-Pro.local");

      byte[] sendData = new byte[128];
      String message = "";
      String dataContent = "";
      int endOfMessage = 1;
      int iteration = 1;

      // Send HTTP Request Message
      sendData = httpReqMessage().getBytes();
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 10050);
      clientSocket.send(sendPacket);

      // Get the name to save the file as
      String savedAs = saveName();

      // Get probability packet is corrupted
      int probability = getProbability();

      // Receive message from server
      while (endOfMessage != 0) {

         // Receive Datagram
         byte[] receiveData = new byte[128];
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
         clientSocket.receive(receivePacket);

         // Get Data
         byte[] receivedMessage = receivePacket.getData();

         // Pass through Gremlin function
         receivedMessage = gremlin(receivedMessage, probability);

         // Validate the checksum
         errorDetected(receivedMessage);



         // Check if this is the last packet
         int i = 0;
         while (endOfMessage != 0 && i < receivedMessage.length - 1) {
            endOfMessage = receivedMessage[i];
            i++;
         }

         // Reassemble Message
         if (iteration > 1 && endOfMessage != 0) {
            message = new String(receivedMessage);
            String noHeader = removeHeader(message);
            dataContent = dataContent.concat(noHeader);
         }
         iteration++;
      }

      System.out.println("\nFULL MESSAGE READS: \n" + dataContent);

      clientSocket.close();

      System.out.println("\nSaving file...");
      writeFile(dataContent, savedAs);
   }

   public static String removeHeader(String packetInfo) {
      String data = packetInfo.substring(packetInfo.indexOf(":") + 11);
      return data;
   }

   public static String httpReqMessage() throws IOException {

      BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

      System.out.print("Please type the name of the data file you would like to view: ");

      String sentence = inFromUser.readLine();
      System.out.println();

      return "GET " + sentence + ".html HTTP/1.0";
   }

   public static String saveName() throws IOException {
      String name = "";
      BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

      System.out.print("What would you like to save this file as: ");

      name = input.readLine();
      System.out.println();
      return name;
   }

   public static void writeFile(String output, String fileName) {
      try {
         PrintWriter writer = new PrintWriter(fileName, "UTF-8");
         writer.println(output);
         writer.close();
      } catch (IOException e) {
         System.out.println("Failed to write file with error: " + e);
      }
      System.out.println("Successfully saved file: " + fileName);
   }

   public static byte[] zeroCheckSum(byte[] message) {
      String info = new String(message);
      int index = info.indexOf(":") + 1;

      for (int i = index + 1; i < index + 6; i++) {
         message[i] = 48;
      }
      return message;
   }

   public static int getProbability() {
      System.out.println("Enter a probability for error: ");
      Scanner in = new Scanner(System.in);
      double prob1 = in.nextDouble();
      System.out.println();
      while (prob1 < 0 || prob1 > 1) {
         System.out.println("Enter a probability between 0-1: ");
         prob1 = in.nextDouble();
      }
      in.close();
      return (int) (prob1 * 100);
   }

   public static String getCheckSumSent(byte[] input) {
      String checkSum = "";

      byte[] byteCheckSum = new byte[5];
      String info = new String(input);
      int index = info.indexOf(":") + 1;
      int j = 0;
      for (int i = index + 1; i < index + 6; i++) {
         byteCheckSum[j] = input[i];
         j++;
      }
      checkSum = new String(byteCheckSum);

      boolean leadingZeros = true;
      while (leadingZeros) {
         leadingZeros = checkSum.startsWith("0");
         if (leadingZeros) {
            checkSum = checkSum.substring(1);
         }
      }
      return checkSum;
   }

   public static byte[] gremlin(byte[] array, int prob) {

      Random r = new Random();

      // 0-9
      int randint = java.lang.Math.abs(r.nextInt()) % 100;
      int randint2 = java.lang.Math.abs(r.nextInt()) % 100;

      int byte1 = java.lang.Math.abs(r.nextInt()) % 128;
      int byte2 = java.lang.Math.abs(r.nextInt()) % 128;
      int byte3 = java.lang.Math.abs(r.nextInt()) % 128;

      if (randint <= prob && prob != 0) {
         if (randint2 == 0) {
         } else if (randint2 <= 40) {
            array[byte1] = 35;
         } else if (randint2 > 40 || randint2 < 80) {
            array[byte1] = 35;
            array[byte2] = 35;
         } else if (randint2 >= 80) {
            array[byte1] = 35;
            array[byte2] = 35;
            array[byte3] = 35;
         }
      }
      return array;
   }

   public static int checkSum(byte[] data) {
      int sum = 0;

      for (int i = 0; i < data.length; i++) {
         sum += (int) data[i];
      }
      return sum;
   }

   public static boolean errorDetected(byte[] receiveData) {
      int checkSum;
      boolean errorExists = false;
      String ogMessage = new String(receiveData);
      String sumIn = getCheckSumSent(receiveData);
      byte[] pseudoHeader = zeroCheckSum(receiveData);
      checkSum = checkSum(pseudoHeader);

      if (sumIn.equals(Integer.toString(checkSum))) {
         System.out.println("\n" + ogMessage);
      } else {
         errorExists = true;
         String packetInfo = new String(receiveData);
         System.out.println("\nAn error was detected in the following packet: ");
         System.out.println(ogMessage);
         System.out.println("\nTime Out!");
      }
      return errorExists;
   }
}
