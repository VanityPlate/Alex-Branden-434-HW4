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
        return new ServerCachedFile(contents);
    }
    
    private void invalidateClients(String fileName) {
        ArrayList<String> clients = fileToClients.get(fileName);
        
        if (clients == null) {
            return;
        }
        
        clients.stream().forEach((client) -> {
            invalidateSingleClient(client);
        });
        
    }
    
    private void invalidateSingleClient(String ip) {
        //TODO: do this
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
    
    public FileContents download(String myIpName, String fileName, char mode) {
        ServerCachedFile serverFile = getFile(fileName);
        
        FileContents result = serverFile.fileContents;
        
        // Wait until we are not changing owner.
        while(serverFile.state == FileState.Ownership_Change) {}
        
        // Request the owner to write back when finished. 
        if (serverFile.currentWriter != null) {
            
            requestWriteBack(serverFile.currentWriter);
            
            while(serverFile.state == FileState.Ownership_Change ||
                    serverFile.state == FileState.Write_Owned) {
                if (serverFile.state == FileState.Write_Owned) {
                    requestWriteBack(serverFile.currentWriter);
                }
            }   
        }
        
        if (mode == 'w') {
            serverFile.state = FileState.Write_Owned;
        } else {
            serverFile.state = FileState.Read_Shared;
        }
        
        removeClientFromFile(myIpName);
        addFileToClient(fileName, myIpName);

        return result;
    }
        
    public boolean upload(String nyIpName, String fileName, FileContents contents){
        invalidateClients(fileName);
        
        ServerCachedFile serverFile = getFile(fileName);
        
        if (serverFile.state == FileState.Not_Shared) {
            return false;
        }
        

        serverFile.currentWriter = null;

        return true;
    }
    
}
