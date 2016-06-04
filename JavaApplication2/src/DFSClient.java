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
    private String serverIp;
    private ClientCachedFile myFile = new ClientCachedFile(null, FileState.Invalid, null);
    private ServerInterface myServer = null; 
    
    public boolean writeback(){
        return true;
    }
    
    public boolean invalidate(){
        return true;
    }
    
    //returns a boolean for whether or not the current file
    //is cached
    private boolean isCached(String fileName){        
        return myFile.getFileState() != FileState.Invalid && myFile.getName().equals(fileName);
    }
    
    private void runClient(){
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
                if(fileMode.length() > 0 && fileMode.charAt(0) == 'r' || fileMode.charAt(0) == 'w'){
                    mode = fileMode.charAt(0);
                    break;
                }
                else{
                    System.err.println("Incorrect mode!");
                }
            }
        
            //Checking if file cached
            if(!isCached(fileName)){
                this.myFile.setMode(mode);
                if(mode == 'r'){
                    fileContents = 
                    this.myFile.setFileState(FileState.Read_Shared);
                }
            }
            else{
            }
        }
    }
    
    private DFSClient(String serverIp){
        //setting serverIp
        this.serverIp = serverIp;
        this.runClient();

    }
    
    public static void main(String args[]) throws Exception{
        //Error checking
        if(args.length < 1){
            System.err.println("usage: java DFSClient serverIp");
            return;
        }
        else{
            new DFSClient(args[0]);
        }
    }
}
