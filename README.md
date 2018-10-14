## HashExpireMap


> English

#### How to use

> HashExpireMap(long initialDelay, long period, TimeUnit timeUnit)

```java
HashExpireMap hashExpireMap = new HashExpireMap(3, 2, TimeUnit.SECONDS);
hashExpireMap.put("1", "1", "1s");          // 1 seconds
hashExpireMap.put("2", "2", "2m");          // 2 minutes
hashExpireMap.put("3", "3", "2h");          // 2 hours
```

#### how to achieve

Compared to the HashMap that comes with JDK8, the HashExpireMap data structure changes from <K, V> to <K, V, T extend Long>, and has an effective storage period. At the same time, a timing is enabled in HashExpireMap. Thread, which is used to periodically scan the ratio of the effective storage period of all data elements in the HashExpireMap to the current system time. If the discovery is out of date, the observer mode is used to broadcast the expiration notification to all observers.

#### Next optimization

- [ ] Add a permanent storage put method
- [ ] Use red and black trees instead of linked lists
- [ ] Optimize code structure



> 中文

#### 如何使用

> HashExpireMap(long initialDelay, long period, TimeUnit timeUnit)

```java
HashExpireMap hashExpireMap = new HashExpireMap(3, 2, TimeUnit.SECONDS);
hashExpireMap.put("1", "1", "1s");          // 1 seconds
hashExpireMap.put("2", "2", "2m");          // 2 minutes
hashExpireMap.put("3", "3", "2h");          // 2 hours
```

#### 如何实现

与JDK8附带的HashMap相比，HashExpireMap数据结构从<K，V>变为<K，V，T延伸长>，并且具有有效的存储周期。 同时，在HashExpireMap中启用了计时。 Thread，用于定期扫描HashExpireMap中所有数据元素的有效存储周期与当前系统时间的比率。 如果发现已过期，则使用观察者模式向所有观察者广播过期通知。