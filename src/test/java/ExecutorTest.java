import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author tensor
 */
public class ExecutorTest {

    private static HashMap< Integer, Integer > map = new HashMap<>(2);

    /**
     * 主函数，用于演示
     */
    @Test
    public void test() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for (int i = 1; i <= 100000; i++) {
                int result = i;
                new Thread(() -> map.put(result, result), "ftf" + i).start();
            }
        });

        t1.start();

        ConcurrentHashMap concurrentHashMap;


        //让主线程睡眠5秒，保证线程1和线程2执行完毕
        Thread.sleep(5000);
        for (int i= 1; i <= 100000; i++) {
            //检测数据是否发生丢失
            Integer value = map.get(i);
            if (value==null) {
                System.out.println(i + "数据丢失");
            }
        }

        System.out.println("end...");
    }
}