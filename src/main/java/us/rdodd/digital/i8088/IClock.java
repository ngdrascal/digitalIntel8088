package us.rdodd.digital.i8088;

public interface IClock {
   void WaitForRisingEdge();

   void WaitForFallingEdge();

   long getClockCounter();

   void setClockCounter(long value);

   void incClockCounter(byte amount);
}
