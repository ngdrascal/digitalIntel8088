package us.rdodd.digital.i8088;

import java.util.LinkedList;
import java.util.Queue;

public class Clock implements ClockIntf {
   private class Action {
      public Action(EdgeDirection edge, OnClockEdgeIntf lambda) {
         this.edge = edge;
         this.lambda = lambda;
      }

      public OnClockEdgeIntf lambda;

      public EdgeDirection edge;
   }

   private PinsInternalIntf pins;
   private BitLatchIntf nmiLatch;
   private long clockCounter;
   private byte lastNmi;
   private byte currentNmi;
   private EdgeDirection lastEdgeDir;
   private Queue<Action> actionQueue;

   public Clock(PinsInternalIntf pins, BitLatchIntf nmiLatch) {
      this.pins = pins;
      this.nmiLatch = nmiLatch;

      lastNmi = 1;
      currentNmi = 0;
      lastEdgeDir = EdgeDirection.RISING;
      actionQueue = new LinkedList<Action>();
   }

   public Clock addAction(EdgeDirection edgeDir, OnClockEdgeIntf lambda) {
      Action action = new Action(edgeDir, lambda);
      actionQueue.add(action);
      return this;
   }

   public int step(EdgeDirection edgeDir) {
      if (edgeDir == lastEdgeDir)
         return actionQueue.size();

      lastEdgeDir = edgeDir;
      if (edgeDir == EdgeDirection.FALLING) { // falling edge
         if (clockCounter > 0)
            // Count down clock_Counter here to keep new opcodes from beginning
            // yet still allowing the prefetch queue to fill
            clockCounter--;

         if (lastNmi == 0 && currentNmi != 0)
            // Latch rising edge of NMI
            nmiLatch.Set();

         lastNmi = currentNmi;

         // Store slightly-delayed version of GPIO6 in a global register
         // GPIO6_raw_data = _deviceAdapter.GPIO6_DR;
         // direct_nmi = (GPIO6_raw_data & 0x00010000);
         currentNmi = pins.getNmiPin();

         if (actionQueue.isEmpty())
            return 0;

         if (actionQueue.peek().edge == EdgeDirection.FALLING) {
            Action action = actionQueue.poll();
            action.lambda.execute(this);
         }
         return actionQueue.size();
      } else { // rising edge
         if (actionQueue.isEmpty())
            return 0;

         if (actionQueue.peek().edge == EdgeDirection.RISING) {
            Action action = actionQueue.poll();
            action.lambda.execute(this);
         }
         return actionQueue.size();
      }
   }

   public long getClockCounter() {
      return clockCounter;
   }

   public void setClockCounter(long value) {
      clockCounter = value;
   }

   public void incClockCounter(byte amount) {
      clockCounter += amount;
   }

   public void waitForRisingEdge() {
      // First ensure clock is at a low level
      while (pins.getClkPin() == 1) {
      }

      // Then poll for the first instance where clock is not low
      while (pins.getClkPin() == 0) {
      }
   }

   public void waitForFallingEdge() {
      if (clockCounter > 0)
         // Count down clock_Counter here to keep new opcodes from beginning
         // yet still allowing the prefetch queue to fill
         clockCounter--;

      if (lastNmi == 0 && currentNmi != 0)
         // Latch rising edge of NMI
         nmiLatch.Set();

      lastNmi = currentNmi;

      // First ensure clock is at a high level
      while (pins.getClkPin() != 1) {
      }

      // Then poll for the first instance where clock is not high
      while (pins.getClkPin() != 0) {
      }

      // Store slightly-delayed version of GPIO6 in a global register
      // GPIO6_raw_data = _deviceAdapter.GPIO6_DR;
      // direct_nmi = (GPIO6_raw_data & 0x00010000);
      currentNmi = pins.getNmiPin();
   }
}
