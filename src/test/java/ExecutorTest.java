import com.hashexpiremap.lct.ExpireNotify;
import com.hashexpiremap.lct.HashExpireMap;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author tensor
 */
public class ExecutorTest {

    /**
     * 主函数，用于演示
     */
    public void test() {
        HashExpireMap hashExpireMap = new HashExpireMap(3, 2, TimeUnit.SECONDS);
        hashExpireMap.put("1", "1", "1s");
        hashExpireMap.put("2", "2", "2s");
        hashExpireMap.addObserver(new ExpireNotify());
    }
}