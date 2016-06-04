/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Alex
 */
import java.rmi.*;
import java.util.*;

public interface DFSServerInterface extends Remote{
    public FileContents download(String myIpName,String filename, char mode);
    public boolean upload(String nyIpName, String fileName, FileContents contents);
}
