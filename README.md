## HashExpireMap



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

-[ ] Add a permanent storage put method
-[ ] Use red and black trees instead of linked lists
-[ ] Optimize code structure
