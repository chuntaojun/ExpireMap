package com.expiremap.lct.client;

import java.util.Observable;
import java.util.Observer;

/**
 * @author tensor
 */
public abstract class ExpireNotify implements Observer {

    @Override
    public void update(Observable o, Object arg) {
        expireEvent(arg);
    }

    /**
     *
     * @param arg
     */
    public abstract void expireEvent(Object arg);
}