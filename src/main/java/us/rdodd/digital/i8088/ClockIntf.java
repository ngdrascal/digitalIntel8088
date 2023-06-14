package us.rdodd.digital.i8088;

public interface ClockIntf {
   void waitForRisingEdge();

   void waitForFallingEdge();

   Clock addAction(EdgeDirection dir, OnClockEdgeIntf lambda);

   int step(EdgeDirection currentEdge);

   long getClockCounter();

   void setClockCounter(long value);

   void incClockCounter(byte amount);
}
