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

public class FileContents implements Serializable{
    private byte[] contents; //file contents
    private String fileName; //the file name
    public FileContents(byte[] contents, String fileName){
        this.fileName = fileName;
        this.contents= contents;
    }
    public void print() throws IOException {
        System.out.println("FileContents = " + contents);
    }
    public byte[] get(){
        return contents;
    }
    
    public String getName(){
        return this.fileName;
    }
}
