package com.expiremap.lct.client;

import java.util.Observable;
import java.util.Observer;

/**
 * 集成这个抽象类即可获取过期通知时间信息
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