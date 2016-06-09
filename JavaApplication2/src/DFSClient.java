/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Alex
 */
import java.io.*;
import java.util.*;
import java.rmi.*;
import java.net.*;
import java.rmi.server.UnicastRemoteObject;



public class DFSClient extends UnicastRemoteObject implements DFSClientInterface{
    private static String username = System.getProperty("user.name");
    private String serverIp;
    private ClientCachedFile myFile = new ClientCachedFile(null, FileState.Invalid, null);
    private DFSServerInterface myServer = null; 
    private String myIp;
    
    public boolean writeback(){
        this.myFile.setFileState(FileState.Release_Ownership);
        
        System.out.println("Received write back request!");
        return true;
    }
    
    public boolean invalidate(){
        System.out.println("Invalidating!");
        this.myFile.setFileState(FileState.Invalid);
        return true;
    }
    
    //returns a boolean for whether or not the current file
    //is cached
    private boolean isCached(String fileName){        
        return myFile.getFileState() != FileState.Invalid && myFile.getName().equals(fileName);
    }
    
    private void runEmacs(String fileName){
        Runtime runtime = Runtime.getRuntime( );
        System.out.println("Username is " + username);
        ProcessBuilder processBuilder = new ProcessBuilder("emacs", "/tmp/" + username + ".txt");
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        
        try{
            System.out.println("Trying to exec emacs");
            
            Process process = processBuilder.start();

            process.waitFor();
            
            File file = new File("/tmp/" + username + ".txt");
            byte[] fileContent = new byte[(int)file.length()];

            FileInputStream fin = new FileInputStream(file);
            fin.read(fileContent);
            fin.close();
            myFile.setContents(fileContent);            
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private void writeFile(String fileName){
        String filePath = "/tmp/" + username + ".txt";
        try{
            
            String command = "chmod u+w /tmp/" + username + ".txt";
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            FileOutputStream stream = new FileOutputStream(filePath, false);
            stream.write(this.myFile.get());
            stream.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private void setFileMode(boolean writable){
        String number = null;
        if(writable){
           this.myFile.setMode('w');
            number = "600";
        }
        else{
            this.myFile.setMode('r');
            number = "400";
        }
        
        String command = "chmod " + number + " /tmp/" + username + ".txt";
        Runtime runtime = Runtime.getRuntime( );
        try{
            Process process = runtime.exec( command );
            process.waitFor();
        }catch(Exception e){
            e.printStackTrace();
            return;
        }
    }
    
    public void runClient(){
        while(true){
            //Getting User Input
            Scanner getInput = new Scanner(System.in);
            System.out.println("File Client: Next file to open");
            System.out.print("File name:");
            String fileName = getInput.nextLine();

            //Checking Input
            String fileMode;
            char mode;
            while(true){
                System.out.println("How(r/w)");
                fileMode = getInput.next();
                if(fileMode.length() > 0 && fileMode.charAt(0) == 'r' || 
                        fileMode.charAt(0) == 'w'){
                    mode = fileMode.charAt(0);
                    break;
                }
                else{
                    System.err.println("Incorrect mode!");
                }
            }
            
            //Checking if file cached
            if(!isCached(fileName)){
                
                System.out.println("File is not cached!");
                
                //checking if current cached file is write owned
                //if true writing currently cached file up to the server
                if(this.myFile.getOwnership()){
                    try {
                        this.myServer.upload(this.myIp, this.myFile.getName(), this.myFile);
                        System.out.println("Uploading file!");
                    } catch(Exception e) {
                        System.out.println("Upload exception!");
                    }
                }
                
                //getting file from server
                FileContents fileContents = null;
                
                try {
                    fileContents =  this.myServer.download(this.myIp, 
                        fileName, mode);
                    System.out.println("Got file contents!");
                } catch(Exception e) {}
                
                //if it is read only
                if(mode == 'r'){
                    this.myFile = new ClientCachedFile(fileContents.get(), 
                        FileState.Read_Shared, fileContents.getName());
                    this.myFile.setOwnership(false);
                    this.writeFile(this.myFile.getName());
                    this.setFileMode(false);
                }
                //if it is writalble
                else{
                    this.myFile = new ClientCachedFile(fileContents.get(), 
                        FileState.Write_Owned, fileContents.getName());
                    this.myFile.setOwnership(true);
                    this.writeFile(this.myFile.getName());
                    this.setFileMode(true);
                }
            }
            //if the file is cached only download in case that you are changing
            //from read to write
            else{
                if(!this.myFile.getOwnership()){
                    if(mode != 'r'){
                        FileContents fileContents = null;
                        
                        try {
                            fileContents = this.myServer.download(this.myIp, 
                            fileName, mode);
                        } catch(Exception e) {
                            
                        }
                        
               
                        this.myFile = new ClientCachedFile(fileContents.get(), 
                            FileState.Write_Owned, fileContents.getName());
                        this.myFile.setOwnership(true);
                        this.writeFile(this.myFile.getName());
                        this.setFileMode(true);
                    }
                }
            }
            //Now running emacs
            runEmacs(this.myFile.getName());
            
            //Doing Final Check if owner of the file writing back to the server
            if(this.myFile.getFileState() == FileState.Release_Ownership){
                releaseOwnership();
            }
        }
    }
    
    private void releaseOwnership() {
        try {
            this.myServer.upload(this.myIp, this.myFile.getName(), this.myFile); 
            this.myFile.setFileState(FileState.Read_Shared);
            this.myFile.setMode('r');
            this.myFile.setOwnership(false);
        } catch(Exception e) {}
    }
    
    private DFSClient(String myIp, String serverIp)throws Exception{
        //setting serverIp and myIp
        this.serverIp = serverIp;
        this.myIp = myIp;
        try{
        this.myServer = (DFSServerInterface) Naming.lookup("rmi://" +
            this.serverIp + ":" + DFSServer.OUR_PORT + "/dfsserver"); 
        }catch(Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    public static void main(String args[]) throws Exception{
        //Error checking
        if(args.length < 2){
            System.err.println("usage: java DFSClient yourIp serverIp");
            return;
        }
        else{
            DFSClient dfsclient = new DFSClient(args[0], args[1]);
            //Registering Self with RMI
            try{
                Naming.rebind("rmi://localhost:" + DFSServer.OUR_PORT 
                        + "/dfsclient", dfsclient);
            }catch(Exception e){
                e.printStackTrace();
                System.exit(1);
            }
            dfsclient.runClient();
        }
    }
}