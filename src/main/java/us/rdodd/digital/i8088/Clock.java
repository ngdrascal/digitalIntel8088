package us.rdodd.digital.i8088;

public class Clock implements IClock {
   private PinsInternalIntf pins;
   private IBitLatch nmiLatch;
   private long clockCounter;
   private byte lastNmi;
   private byte currentNmi;

   public Clock(PinsInternalIntf pins, IBitLatch nmiLatch) {
      this.pins = pins;
      this.nmiLatch = nmiLatch;

      lastNmi = 1;
      currentNmi = 0;
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

   public void WaitForRisingEdge() {
      // First ensure clock is at a low level
      while (pins.getClkPin() == 1) {
      }

      // Then poll for the first instance where clock is not low
      while (pins.getClkPin() == 0) {
      }
   }

   public void WaitForFallingEdge() {
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
