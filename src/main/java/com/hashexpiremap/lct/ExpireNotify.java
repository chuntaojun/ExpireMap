package com.hashexpiremap.lct;

import java.util.Observable;
import java.util.Observer;

/**
 * @author tensor
 */
public class ExpireNotify implements Observer {

    @Override
    public void update(Observable o, Object arg) {
        System.out.println("The expired key is : [" + arg + "]");
    }
}