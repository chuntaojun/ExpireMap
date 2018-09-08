package com.executor.org;

/**
 * @author tensor
 */
public class Executor {

    /**
     * 主函数，用于演示
     * @param args
     */
    public static void main(String[] args) {
        ExpireNotifyService notifyService = new ExpireNotifyService();
        ExpireMap expireMap = ExpireMap.newExpireMap(1000 * 6, true, notifyService);
        expireMap.put("1", "test-1");
        expireMap.put("2", "test-2", 1000L * 9);
        System.out.println(expireMap.log());
    }
}