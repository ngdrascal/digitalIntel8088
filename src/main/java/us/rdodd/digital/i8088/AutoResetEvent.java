package us.rdodd.digital.i8088;

public class AutoResetEvent
{
   private volatile boolean isSet = false;
   private final Object monitor = new Object();

    public AutoResetEvent(boolean isSet)
    {
        this.isSet = isSet;
    }

    public void waitOne() throws InterruptedException
    {
        synchronized (monitor) {
            while (!isSet) {
                monitor.wait();
            }
            isSet = false;
        }
    }

    public void waitOne(long timeout) throws InterruptedException
    {
        synchronized (monitor) {
            long t = System.currentTimeMillis();
            while (!isSet) {
                monitor.wait(timeout);
                // Check for timeout
                if (System.currentTimeMillis() - t >= timeout)
                    break;
            }
            isSet = false;
        }
    }

    public void set()
    {
        synchronized (monitor) {
            isSet = true;
            monitor.notify();
        }
    }

    public void reset()
    {
        isSet = false;
    }
}
