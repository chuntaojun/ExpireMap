import com.expiremap.lct.client.ExpireNotify;
import com.expiremap.lct.map.ConcurrentExpireMap;
import org.junit.Test;

/**
 * @author tensor
 */
public class ExecutorTest {


    /**
     * 主函数，用于演示
     */
    @Test
    public void test() throws InterruptedException {
        ConcurrentExpireMap<String, Integer, Long> map = ConcurrentExpireMap.newExpireMap(1000, new MyObserver());

        Thread t1 = new Thread(() -> {
            for (int i = 1; i <= 100; i++) {
                int result = i;
                map.put(String.valueOf(result), result);
            }
        });

        t1.start();

        //让主线程睡眠5秒，保证线程1和线程2执行完毕
        Thread.sleep(5000);

        try {
            for (int i= 1; i <= 100; i++) {
                //检测数据是否发生丢失
                Integer value = map.get(String.valueOf(i));
                if (value==null) {
                    System.out.println(i + "数据丢失");
                }
            }

            System.out.println("end...");
        } catch (Exception e) {}

    }

    public static class MyObserver extends ExpireNotify {

        @Override
        public void expireEvent(Object arg) {
            System.out.println(String.valueOf(arg));
        }
    }
}