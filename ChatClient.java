import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.temporal.IsoFields;

public class ChatClient implements Runnable {

  // The client socket
  private static Socket clientSocket = null;
  // The output stream
  private static PrintStream os = null;
  // The input stream
  private static DataInputStream is = null;

  private static BufferedReader inputLine = null;
  private static boolean closed = false;
  
  
  public static boolean isFileExist(String filename)
  {
	 
	  File varTmpDir = new File(filename);
	  boolean exists = varTmpDir.exists();
	  return exists;
  }
  
  public static void main(String[] args) {
	
	// Initialize default port and host
    int portNumber = 2222;
    String host = "localhost";

    if (args.length < 2) {
      System.out.println("Client running on portNumber =" + portNumber);
    } else {
      host = args[0];
      portNumber = Integer.valueOf(args[1]).intValue();
    }

    /*
     * Create input and output streams on the socket.
     */
    try {
      clientSocket = new Socket(host, portNumber);
      inputLine = new BufferedReader(new InputStreamReader(System.in));
      os = new PrintStream(clientSocket.getOutputStream());
      is = new DataInputStream(clientSocket.getInputStream());
    } catch (UnknownHostException e) {
      System.err.println("Unkown Host : " + host);
    } catch (IOException e) {
      System.err.println("Connection not successful to the host :"+ host);
    }

    /*
     * Write data to the socket 
     */
    if (clientSocket != null) {
      try {

        /* Create a thread to read from the server. */
        new Thread(new ChatClient()).start();
        while (!closed) {
          String line=inputLine.readLine();
          String[] splitted= line.split("\\s+");      
          
          // Check if its a file transfer or a message being sent from C
		  if(splitted.length > 1 &&splitted[1].equals("file") && isFileExist(splitted[2])){
			  line += " " + new File(splitted[2]).length();
			  os.println(line.trim());
			  sendFile(clientSocket,splitted[2]);
		  }
		  else
		  {
			  // For all other cases send the input to the server
	          os.println(line.trim());
		  }
        }
        /*
         * Close the output stream, close the input stream, close the socket.
         */
        os.close();
        is.close();
        clientSocket.close();
      } catch (IOException e) {
        System.err.println("IOException:  " + e);
      }
    }
  }

	public static void sendFile(Socket s,String file) throws IOException {
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		FileInputStream fis = new FileInputStream(file);
		int filesize = (int) fis.getChannel().size();
		System.out.println("*** File Sent ***");
		byte[] buffer = new byte[filesize];
		
		fis.read(buffer);
		dos.write(buffer);fis.close();	
	}
	
	public void saveFile(Socket clientSock,String folderPath, String filename, int filesize) throws IOException {
		DataInputStream dis = new DataInputStream(clientSock.getInputStream());
		FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir")+"/"+folderPath+"/"+filename);
		byte[] buffer = new byte[4096];
		
		int read = 0;
		int totalRead = 0;
		int remaining = filesize;
		while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
			totalRead += read;
			remaining -= read;
			fos.write(buffer, 0, read);
		}
	
		
		fos.close();
	  }
  /*
   * Thread to read from server
   * 
   */
  @SuppressWarnings("deprecation")
public void run() {
    /*
     * Keep on reading from the socket till we receive "Bye" from the
     * server. Once we received that then we want to break.
     */
    String responseLine;
    try {
      while ((responseLine = is.readLine()) != null) {
    		
        if(responseLine.startsWith("download")){
        	int filesize = Integer.parseInt(responseLine.split(" ")[4]);
        	saveFile(clientSocket,responseLine.split(" ")[1],responseLine.split(" ")[2], filesize);
        	System.out.println("*** File Received: "+ responseLine.split(" ")[2]+" from "+ responseLine.split(" ")[3]+"***");
        }
        else {
        	System.out.println(responseLine);
	        if (responseLine.indexOf("*** Bye") != -1)
	          break;
        }
      }
    } catch (IOException e) {
      System.err.println("IOException run:  " + e);
    }
  }
}
