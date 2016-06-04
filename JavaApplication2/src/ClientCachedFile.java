/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Alex
 */
public class ClientCachedFile extends FileContents {
    private FileState state;
    private char accessMode;
    private boolean ownership;
    
    public ClientCachedFile(byte[] bytes, FileState state, String fileName) {
        super(bytes, fileName);
        this.state = state;
        this.ownership = false;
    }
    
    public FileState getFileState(){
        return state;
    }
    
    public void setFileState(FileState state){
        this.state = state;
    }
    
    public char getMode(){
        return this.accessMode;
    }
    
    public void setMode(char mode){
        this.accessMode = mode;
    }
    
    public boolean getOwnership(){
        return this.ownership;
    }
    
    public void setOwnership(boolean ownership){
        this.ownership = ownership;
    }
}
