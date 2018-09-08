package com.executor.org;

import java.util.Observable;

/**
 * 观察者，当ExpireMap有key过期时会通知该观察者
 * @author tensor
 */
public class ExpireNotifyService extends AbstractExpireNotify {

    @Override
    public void update(Observable o, Object arg) {
        System.out.println("过期Key为 : [" + arg + "]");
    }

}