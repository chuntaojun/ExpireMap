package com.hashexpiremap.lct;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * 带有过期清除功能的Map
 *
 * @author tensor
 */
public class ExpireMap<K, V, T extends Long> extends Observable {

    /**
     * 存储真正的数据信息
     */
    protected Hashtable expireTable = new Hashtable<>();
    /**
     * 默认过期时间为 6 Minutes
     */
    private static long DEFAULT_EXPIRE_TIME = 1000 * 60 * 6;
    /**
     * 缓存过期时间
     */
    private HashMap<String, Long> cacheMap = new HashMap<>();

    private ScheduledExecutorService scanService;

    private boolean openExpire;
    private boolean openNotify;

    /**
     * @param openExpire
     * @param openNotify
     * @param observer
     */
    private ExpireMap(boolean openExpire, boolean openNotify, Observer observer) {
        this.openExpire = openExpire;
        this.openNotify = openNotify;
        if (openExpire) {
            startExpireScan();
        }
        if (this.openNotify && observer != null) {
            this.addObserver(observer);
        }
    }

    /**
     * @return
     */
    public static ExpireMap newExpireMap() {
        return new ExpireMap<>(true, false, null);
    }

    /**
     * 自定义过期时间的构造方法
     *
     * @param defaultExpireTime {单位为毫秒}
     * @return
     */
    public static ExpireMap newExpireMap(long defaultExpireTime) {
        DEFAULT_EXPIRE_TIME = defaultExpireTime;
        return new ExpireMap<>(true, false, null);
    }

    /**
     * 自行决定是否开启定时扫描线程实现过期扫描任务，若设置为true，
     * 则{@link ExpireMap.ExpireKeyScan#isExpire(String)}会
     * 覆盖外部类{@link ExpireMap#isExpire(Object)}
     *
     * @param defaultExpireTime {单位为毫秒}
     * @param openExpire
     * @return
     */
    public static ExpireMap newExpireMap(long defaultExpireTime, boolean openExpire) {
        DEFAULT_EXPIRE_TIME = defaultExpireTime;
        return new ExpireMap<>(openExpire, false, null);
    }

    /**
     * 自行决定是否开启定时扫描线程实现过期扫描任务，若设置为true，
     * 则{@link ExpireMap.ExpireKeyScan#isExpire(String)}会
     * 覆盖外部类{@link ExpireMap#isExpire(Object)};同时设置观察者对象{@link Observer observer}
     *
     * @param defaultExpireTime {单位为毫秒}
     * @param openExpire
     * @param observer
     * @return
     */
    public static ExpireMap newExpireMap(long defaultExpireTime, boolean openExpire, Observer observer) {
        DEFAULT_EXPIRE_TIME = defaultExpireTime;
        return new ExpireMap<>(openExpire, true, observer);
    }

    /**
     * 开启线程执行过期扫描任务
     */
    public void startExpireScan() {
        scanService = newSingleThreadScheduledExecutor();
        scanService.scheduleAtFixedRate(new ExpireMap.ExpireKeyScan(), DEFAULT_EXPIRE_TIME, DEFAULT_EXPIRE_TIME + 10,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 采用预设过期时间存储
     *
     * @param k
     * @param v
     */
    public void put(K k, V v) {
        if (k instanceof String) {
            System.out.println("K is :[" + k + "], V is [" + v + "]");
            this.expireTable.put(k, v);
            this.cacheMap.put((String) k, System.currentTimeMillis() + DEFAULT_EXPIRE_TIME);
        } else {
            throw new RuntimeException("Key must be java.lang.String");
        }
    }

    /**
     * 采取自定义的缓存时间存储{key-value}数据， 时间数据 {@link Long t}
     *
     * @param k
     * @param v
     * @param t
     */
    public void put(K k, V v, T t) {
        if (k instanceof String) {
            System.out.println("K is :[" + k + "], V is [" + v + "], T is [" + t + "]");
            this.expireTable.put(k, v);
            this.cacheMap.put((String) k, System.currentTimeMillis() + (long) t);
        } else {
            throw new RuntimeException("Key must be java.lang.String");
        }
    }

    /**
     * 根据Key获取对应的值，如果Key已过期或者Key在cacheMap中不存在，则抛出过期异常
     *
     * @param k
     * @return
     */
    public V get(K k) {
        if (isExpire(k) || this.cacheMap.get(k) == null) {
            throw new RuntimeException("This key had expire");
        }
        return (V) expireTable.get(k);
    }

    public String log() {
        return this.expireTable.toString();
    }

    /**
     * 如果{@link boolean openExpire}为true，则采用线程定时扫描的方式覆盖该方法
     *
     * @param k
     * @return
     */
    public boolean isExpire(K k) {
        if (!this.openExpire) {
            if (this.cacheMap.get(k) < System.currentTimeMillis()) {
                this.cacheMap.remove(k);
                this.expireTable.remove(k);
                if (openNotify) {
                    this.setChanged();
                    this.notifyObservers(k);
                }
                return true;
            }
            return false;
        }
        throw new RuntimeException("This function had coverd by ExpireMap.ExpireKeyScan");
    }

    public static long GET_DEFAULT_EXPIRE_TIME() {
        return DEFAULT_EXPIRE_TIME;
    }

    private class ExpireKeyScan implements Runnable {

        private boolean isExpire(String k) {
            if (cacheMap.get(k) < System.currentTimeMillis()) {
                if (openNotify) {
                    setChanged();
                    notifyObservers(k);
                }
                return true;
            }
            return false;
        }

        /**
         * 开启一个定时扫描线程，定期扫描{@link HashMap
         * cacheMap}，利用函数{@link ExpireKeyScan#isExpire(String)}扫描出
         * 已过期的Key去remove在{@link java.util.Hashtable expireTable}、{@link HashMap
         * cacheMap}对应的值
         *
         * @return {@link Long cout} 过期的key的数目
         */
        @Override
        public void run() {
            for (Iterator<Map.Entry<String, Long>> iterator = cacheMap.entrySet().iterator(); iterator.hasNext(); iterator.next()) {
                Map.Entry<String, Long> entry = iterator.next();
                if (isExpire(entry.getKey())) {
                    iterator.remove();
                }
            }
        }
    }

}