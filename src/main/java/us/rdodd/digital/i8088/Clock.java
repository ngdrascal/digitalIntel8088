package us.rdodd.digital.i8088;

public class Clock implements IClock {
   private IHostDeviceAdapter _deviceAdapter;
   private IBitLatch _nmiLatched;
   private long clockCounter;
   private int _nmiD;
   private int _directNmi;
   private final AutoResetEvent _resetEvent;

   public Clock(IHostDeviceAdapter deviceAdapter, IBitLatch nmiLatched, AutoResetEvent resetEvent) {
      _deviceAdapter = deviceAdapter;
      _nmiLatched = nmiLatched;
      _resetEvent = resetEvent;

      _nmiD = 1;
      _directNmi = 0;
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
      try {
         while (_deviceAdapter.getClkPin() == 1) {
            _resetEvent.waitOne();
         }

         // Then poll for the first instance where clock is not low
         while (_deviceAdapter.getClkPin() == 0) {
            _resetEvent.waitOne();
         }
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   public void WaitForFallingEdge() {
      if (clockCounter > 0) {
         // Count down clock_Counter here to keep new opcodes from beginning
         // yet still allowing the prefetch queue to fill
         clockCounter--;
      }

      if (_nmiD == 0 && _directNmi != 0) {
         // Latch rising edge of NMI
         _nmiLatched.Set();
      }
      _nmiD = (int) _directNmi;

      try {
         // First ensure clock is at a high level
         while (_deviceAdapter.getClkPin() != 1) {
            _resetEvent.waitOne();
         }

         // Then poll for the first instance where clock is not high
         while (_deviceAdapter.getClkPin() != 0) {
            _resetEvent.waitOne();
         }
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      // Store slightly-delayed version of GPIO6 in a global register
      // GPIO6_raw_data = _deviceAdapter.GPIO6_DR;
      // direct_nmi = (GPIO6_raw_data & 0x00010000);
      _directNmi = _deviceAdapter.getNmiPin();
   }
}