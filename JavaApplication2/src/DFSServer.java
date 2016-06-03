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
    public HashMap<String, FileState> clients = new HashMap<String, FileState>();
    public String currentWriter = null;
    
    public ServerCachedFile(FileContents contents){
        this.fileContents = contents;
    }
}

public class DFSServer extends UnicastRemoteObject implements DFSServerInterface{
    
    Vector<ServerCachedFile> cachedFiles = null;
    
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
        return null;
    }
    
    public FileContents download(String myIpName,String fileName, FileState mode){
        ServerCachedFile serverFile = getFile(fileName);
        if(serverFile == null){
            File file = new File("/tmp/" + fileName);
            FileInputStream fin = null;
            byte[] fileContent = new byte[(int)file.length()];
           
            try{
                fin = new FileInputStream(file);
                fin.read(fileContent);
                fin.close();
           }catch(Exception e){
                   System.err.println(e);
                   return null;
            }
            FileContents contents = new FileContents(fileContent, fileName);
            serverFile = new ServerCachedFile(contents);
        }
        switch(mode){
            case Read_Shared:
                
            case Write_Owned:
        }
    }
        
    public boolean upload(String nyIpName, String fileName, FileContents contents){
        return true;
    }
    
}
