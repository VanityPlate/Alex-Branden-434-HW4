/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Alex
 */
import java.util.*;
import java.rmi.*;

public interface DFSClientInterface extends Remote{
    public boolean invalidate();
    public boolean writeback();
}
