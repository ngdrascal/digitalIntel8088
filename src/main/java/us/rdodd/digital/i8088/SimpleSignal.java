package us.rdodd.digital.i8088;

public class SimpleSignal implements ISimpleSignal {
   private final AutoResetEvent event;

   public SimpleSignal(boolean initialState) {
      event = new AutoResetEvent(initialState);
   }

   public void Signal() {
      event.set();
   }

   public void Wait() {
      try {
         event.waitOne();
      } catch (InterruptedException ie) {
         ie.printStackTrace();
      }
   }

}
