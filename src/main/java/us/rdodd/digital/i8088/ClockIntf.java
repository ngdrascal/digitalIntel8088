package us.rdodd.digital.i8088;

public interface ClockIntf {
   void waitForRisingEdge();

   void waitForFallingEdge();

   long getClockCounter();

   void setClockCounter(long value);

   void incClockCounter(byte amount);
}
