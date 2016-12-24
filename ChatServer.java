import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.ServerSocket;

/* ChatServer class listens on the port for incoming requests.
 	It runs a separate thread for each client maintained as an array of threads.
 	It keeps count of the connected current clients.
*/
public class ChatServer {

  // The server socket.
  private static ServerSocket serverSocket = null;
  // The client socket.
  private static Socket clientSocket = null;
  // Array of threads
  private static final clientThread[] threads = new clientThread[100];
  
  public static void main(String args[]) {

    int portNumber = 2222;
    if (args.length < 1) {
      System.out
          .println("Server started on portnumber: " + portNumber);
    } else {
      portNumber = Integer.valueOf(args[0]).intValue();
    }


    try {
      serverSocket = new ServerSocket(portNumber);
      File file = new File("server");
      file.mkdir(); 
    } catch (IOException e) {
      System.out.println(e);
    }

    /*
     * Create a client socket for each connection, pass it to a new client
     * thread and update count of current clients.
     */
    int currClientsCount = -1;
    while (true) {
      try {
    	  	clientSocket = serverSocket.accept();
              
             currClientsCount++;
             System.out.println("New Client added. Current client count = "+currClientsCount);
            (threads[currClientsCount] = new clientThread(clientSocket, threads, currClientsCount)).start();
            for(int i=0; i<currClientsCount;i++)
            {
            	threads[i].updateClientCount(currClientsCount);
            	
            }
        } catch (IOException e) {
        System.out.println(e);
      }
    }
  }
}

/*
 * The client thread opens the input and the output streams for a particular client.
 * It handles the commands send from the client for broadcast/unicast and blockcast
 * of messages and files.
 */
class clientThread extends Thread {

  private DataInputStream is = null;
  private DataOutputStream dos = null;
  private PrintStream os = null;
  private Socket clientSocket = null;
  private final clientThread[] threads;
  private String clientName = "";
  private int currClientsCount = 0;
 
  public clientThread(Socket clientSocket, clientThread[] threads, int currClientsCount) {
    this.clientSocket = clientSocket;
    this.threads = threads;
    this.currClientsCount= currClientsCount;
     }
  
  public void updateClientCount(int currClientsCount)
  {
	  this.currClientsCount = currClientsCount;
  }
  
  
  
  public void saveFile(Socket clientSock,DataOutputStream dos, String sender, String filename, int filesize) throws IOException {
	DataInputStream dis = new DataInputStream(clientSock.getInputStream());	
	FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir")+"/server/"+"servercopy"+filename);
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
  public void sendFile(DataOutputStream dos,String file) throws IOException {
		FileInputStream fis = new FileInputStream(System.getProperty("user.dir")+"/server/"+"servercopy"+file);
		
		int filesize = (int) fis.getChannel().size();
		byte[] buffer = new byte[filesize];
		
		fis.read(buffer);
		dos.write(buffer);
		
		fis.close();

	}
  
  public boolean isValidCommand(String name)
  {
	  for(int i=0; i <= currClientsCount;i++)
	  {		  
		  if(threads[i].clientName.equals(name))
		  {
			 return true;
		  }
	  }
	  
	  return false;
  }
  
  public boolean isFileExist(String filename)
  {
	 
	  File varTmpDir = new File(filename);
	  boolean exists = varTmpDir.exists();
	  return exists;
  }
  
  public void run() {
	  
	  
	  clientThread[] threads = this.threads;
    try {

      is = new DataInputStream(clientSocket.getInputStream());
      dos=new DataOutputStream(clientSocket.getOutputStream());
      os = new PrintStream(clientSocket.getOutputStream());
      os.println("Enter a one word username:");
      String name = is.readLine();
      this.clientName=name;
      os.println("Welcome " + name
          + "!\n Enter commands as follows:\n"
          + "To broadcast a messagae : broadcast msg <text> \n"
          + "To unicast a message: unicast msg <text> username \n"
          + "to blockcast a message: blockcast msg <text> username \n"
          + "To broadcast a file: broadcast file <filename> \n"
          + "To unicast a file: unicast file <filename> username \n"
          + "To leave: /quit");
      for (int i = 0; i <= currClientsCount; i++) {
        if (threads[i] != null && threads[i] != this) {
          threads[i].os.println("*** username: " + this.clientName
              + " entered the chat room ***");
        }
      }
      // Create a directory for the client
      
      File file = new File(this.clientName);
      file.mkdir(); 
           
      while (true) {
		String line = is.readLine();
        if(line == null){
        	break;
        }
               
        else if (line.startsWith("/quit")) {
        	break;
        }
        String[] splitted= line.split("\\s+");
		String clientName= null;
        String msg="";
        for(int k=2;k<splitted.length-1;k++){
    		msg+=splitted[k]+" ";
    	}
        if (line.startsWith("blockcast msg") && isValidCommand(splitted[splitted.length-1])) {
        	clientName= splitted[splitted.length-1];
    		for (int j = 0; j <= currClientsCount; j++) {
  	          if (threads[j]!=null && !threads[j].clientName.equals(clientName)) {
  	        	  threads[j].os.println("@" + this.clientName + ":"+msg);
  	          }
  	        }
        }
        else if (line.startsWith("unicast msg")&& isValidCommand(splitted[splitted.length-1])) {
        	clientName= splitted[splitted.length-1];
	  		for (int j = 0; j <= currClientsCount; j++) {
	          if (threads[j]!=null && threads[j].clientName.equals(clientName)) {
	        	  threads[j].os.println("@" + this.clientName + ":" + msg);
	          }
	        }
	  		this.os.println("@"+ this.clientName+":" + msg);
         }
        
        else if (line.startsWith("broadcast msg")) {
	        for (int i = 0; i <= currClientsCount; i++) {
	          if (threads[i] != null) {
	        	  threads[i].os.println("@" + this.clientName + ":" + msg+splitted[splitted.length-1]);
	          }
	        }
        }
        else if (line.startsWith("broadcast file") && isFileExist(splitted[2])) {
        	String filename = splitted[2];
        	int filesize= Integer.parseInt(splitted[3]);
        	saveFile(clientSocket,threads[0].dos, this.clientName, filename, filesize);
        	for (int i = 0; i <= currClientsCount; i++) {
        		
  	          if (threads[i] != null && !threads[i].clientName.equals(this.clientName)) {
  	        	threads[i].os.println("download "+threads[i].clientName+" "+ filename +" "+this.clientName + " "+ filesize);
  	        	sendFile(threads[i].dos,splitted[2]);
  	        	System.out.println("*** File "+ filename+ " sent to "+ threads[i].clientName+" ***");
  	        	}
  	          
  	        }
        	
        	this.os.println("*** File sent successfully ***");
        	//Delete temporary file in server directory
        	Path path = FileSystems.getDefault().getPath(System.getProperty("user.dir")+"/server", "servercopy"+filename);   	
        	Files.deleteIfExists(path);
        	
        }
        else if (line.startsWith("unicast file") && isValidCommand(splitted[3]) && isFileExist(splitted[2])) {
        	String filename = splitted[2];
        	int filesize= Integer.parseInt(splitted[4]);
        	saveFile(clientSocket,threads[0].dos, this.clientName, filename, filesize);
        	for (int i = 0; i <= currClientsCount; i++) {
        		if (threads[i] != null && threads[i].clientName.equals(splitted[3])) {
  	        	threads[i].os.println("download "+threads[i].clientName+" "+ filename+" "+this.clientName +" "+ filesize);
  	        	sendFile(threads[i].dos,splitted[2]);
  	        	System.out.println("*** File "+ filename+ " sent to "+ threads[i].clientName+" ***");
  	          }
  	        }
        	this.os.println("*** File sent successfully ***");
        	

        	//Delete temporary file in server directory
        	Path path = FileSystems.getDefault().getPath(System.getProperty("user.dir")+"/server", "servercopy"+filename);   	
        	Files.deleteIfExists(path);
        	
        }
        else{
        	this.os.println("Invalid command line.");
        }
      }
      
      /*
       * Handle connection close and clean up.
       */
      
      for (int i = 0; i <= currClientsCount; i++) {
        if (threads[i] != null && threads[i] != this) {
          threads[i].os.println("*** User " + name + " has left the chat room !!! ***");
          
        }
      }
      os.println("*** Bye " + name + " ***");

      for (int i = 0; i <= currClientsCount; i++) {
        if (threads[i] == this) {
          threads[i] = null;
        }
      }

      /*
       * Close the output stream, close the input stream, close the socket.
       */
      is.close();
      os.close();
      clientSocket.close();
    } catch (IOException e) {
    }
  }


}
