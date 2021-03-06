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
import java.rmi.server.*;

class ServerCachedFile{
    public FileContents fileContents;
    public String currentWriter = null;
    public FileState state = FileState.Not_Shared;
    
    public ServerCachedFile(FileContents contents){
        this.fileContents = contents;
    }
}

public class DFSServer extends UnicastRemoteObject implements DFSServerInterface{
    public static final String OUR_PORT = Integer.toString(40018);
    Vector<ServerCachedFile> cachedFiles = null;
    HashMap<String, String> clientToFile = new HashMap<String, String>();
    HashMap<String, ArrayList<String>> fileToClients = new HashMap<String, ArrayList<String>>();
    
    public static void main(String[] args) {
        try {
           Naming.rebind("rmi://localhost:" + OUR_PORT + "/dfsserver", new DFSServer());
        } catch (Exception e) {}

    }
    
    public DFSServer()throws RemoteException{
        cachedFiles = new Vector<ServerCachedFile>();
    }
    
    private boolean loadFile(String fileName)throws Exception{
        return true;
    }
    
    private ServerCachedFile getFile(String fileName){
        for(ServerCachedFile file : cachedFiles){
            if(file.fileContents.getName().equals(fileName)){
                return file;
            }
        }
        
        System.out.println("File name: " + fileName + " not found. Attempting to load from disk. ");
        return readFromDisk(fileName);
    }
    
    private ServerCachedFile readFromDisk(String fileName) {
        File file = new File("/tmp/" + fileName);
        FileInputStream fin = null;
        byte[] fileContent = new byte[(int)file.length()];

        try {
            fin = new FileInputStream(file);
            fin.read(fileContent);
            fin.close();
        } catch(Exception e) {
            System.err.println(e);
            return null;
        }

        FileContents contents = new FileContents(fileContent, fileName);
        ServerCachedFile newFile = new ServerCachedFile(contents);
        cachedFiles.add(newFile);
        return newFile;
    }
    
    private void invalidateClients(String fileName, String writerIp) {
        ArrayList<String> clients = fileToClients.get(fileName);
        
        if (clients == null) {
            return;
        }
        
        for(String client : clients) {
            if (!client.equals(writerIp)) {
                invalidateSingleClient(client);
            }
        }
        
    }
    
    private void invalidateSingleClient(String ip) {
        try {
            DFSClientInterface client = (DFSClientInterface) Naming.lookup("rmi://" + ip + ":" + OUR_PORT + "/dfsclient");
            client.invalidate();
        } catch(Exception e) {}
        
    }
    
    private void addClientToFile(String fileName, String clientIp) {
        ArrayList<String> clientsForFile = fileToClients.get(fileName);
        
        if (clientsForFile == null) {
            clientsForFile = new ArrayList<String>();
        }
        
        clientsForFile.add(clientIp);
        
        fileToClients.put(fileName, clientsForFile);
    }
    
    private void requestWriteBack(String clientIp) {
         try {
            System.out.println("Requesting writeback for " + clientIp);

            DFSClientInterface client = (DFSClientInterface) Naming.lookup("rmi://" + clientIp + ":" + OUR_PORT + "/dfsclient");
            client.writeback();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private void addFileToClient(String fileName, String clientIp) {
        clientToFile.put(clientIp, fileName);
        addClientToFile(fileName, clientIp);
    }
    
    private void removeClientFromFile(String clientIp) {
        String fileName = clientToFile.get(clientIp);
        
        if (fileName != null) {
            ArrayList<String> clientsForFile = fileToClients.get(fileName);
            
            clientsForFile.remove(clientIp);
            
            if (clientsForFile.isEmpty()) {
                ServerCachedFile file = getFile(fileName);
                file.state = FileState.Not_Shared;
            }
            
            clientToFile.remove(clientIp);
        }
    }
    
    public synchronized FileContents download(String myIpName, String fileName, char mode) {
        ServerCachedFile serverFile = getFile(fileName);
        
        // Request the owner to write back when finished. 
        if (serverFile.currentWriter != null && mode == 'w') {
            serverFile.state = FileState.Ownership_Change;
            System.out.println("Requesting write back!");
            requestWriteBack(serverFile.currentWriter);
            
            int i = 0;
            
            System.out.println("Write back request completed. ");
            
            // Wait for Ownership_Change state to go away.
            while(serverFile.state == FileState.Ownership_Change) {
                try {
                    Thread.sleep(500);
                    System.out.println("Still waiting. ");
                } catch(Exception e) { }
            }
            
            System.out.println("Write back received. Continuing. ");
        }
        
        FileContents result = serverFile.fileContents;

        // Set mode and current writer if necessary
        if (mode == 'w') {
            serverFile.state = FileState.Write_Owned;
            serverFile.currentWriter = myIpName;
        } else {
            serverFile.state = FileState.Read_Shared;
        }
        
        System.out.println("About to return file: " + fileName);
        
        removeClientFromFile(myIpName);
        addFileToClient(fileName, myIpName);

        return result;
    }
    
    private void writeFile(ServerCachedFile file) {
         String filePath = "/tmp/" + file.fileContents.getName();
        try{
            FileOutputStream stream = new FileOutputStream(filePath);
            stream.write(file.fileContents.get());
            System.out.println("Writing file back to disk!");
            stream.close();
        }catch(Exception e){
            e.printStackTrace();
            return;
        }
    }
        
    public boolean upload(String nyIpName, String fileName, FileContents contents){
        System.out.println("Received file! Name: " + fileName);

        ServerCachedFile serverFile = getFile(fileName);
        
        if (serverFile.state == FileState.Not_Shared) {
            System.out.println("File not shared!");
            return false;
        }
        
        serverFile.fileContents = contents;
        
        writeFile(serverFile);
        
        invalidateClients(fileName, serverFile.currentWriter);

        serverFile.currentWriter = null;
        serverFile.state = FileState.Not_Shared;

        return true;
    }
}
