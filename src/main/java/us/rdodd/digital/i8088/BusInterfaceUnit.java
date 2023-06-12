package us.rdodd.digital.i8088;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusInterfaceUnit implements BusInterfaceUnitIntf {
   private ClockIntf _clock;
   private Registers _registers;
   private PinsInternalIntf _deviceAdapter;

   private Logger _busLogger;
   private Logger _clkLogger;
   private boolean _assertLock;
   private byte _prefixFlags;

   public BusInterfaceUnit(ClockIntf clock,
         Registers registers,
         PinsInternalIntf deviceAdapter) {
      _clock = clock;
      _registers = registers;
      _deviceAdapter = deviceAdapter;

      _busLogger = LoggerFactory.getLogger("cpu.bus");
      _clkLogger = LoggerFactory.getLogger("cpu.clock");
      _assertLock = false;
   }

   @Override
   public byte readCode(int offset) {
      int address = CalculateAddress(false, SegmentRegs.CS, offset);
      PrintBusRequest(address, (byte) 0x00, BusStatus.Code, SegmentRegs.CS);

      return ExecuteBusCycle(BusStatus.Code, address);
   }

   @Override
   public byte readMemoryByte(boolean useSegmentOverride, SegmentRegs segmentReg, int offset) {
      int address = CalculateAddress(useSegmentOverride, segmentReg, offset);
      PrintBusRequest(address, (byte) 0x00, BusStatus.MemRd, segmentReg);

      return ExecuteBusCycle(BusStatus.MemRd, address);
   }

   @Override
   public int readMemoryWord(boolean useSegmentOverride, SegmentRegs segmentReg, int offset) {
      byte dataLo = readMemoryByte(useSegmentOverride, segmentReg, offset);

      offset++; // 16-bit value allows for wrapping within a segment

      byte dataHi = readMemoryByte(useSegmentOverride, segmentReg, offset);

      int readData = (int) ((dataHi << 8) | dataLo);
      return readData;
   }

   @Override
   public void writeMemoryByte(boolean useSegmentOverride, SegmentRegs segmentReg, int offset, byte data) {
      int address = CalculateAddress(useSegmentOverride, segmentReg, offset);
      PrintBusRequest(address, data, BusStatus.MemWr, segmentReg);

      ExecuteBusCycle(BusStatus.MemWr, address, data);
   }

   @Override
   public void writeMemoryWord(boolean useSegmentOverride, SegmentRegs segmentReg, int offset, int data) {
      byte dataLo = (byte) (data & 0x00FF);
      writeMemoryByte(useSegmentOverride, segmentReg, offset, dataLo);

      offset++; // 16-bit value allows for wrapping within a segment

      byte dataHi = (byte) (data >> 8);
      writeMemoryByte(useSegmentOverride, segmentReg, offset, dataHi);
   }

   @Override
   public byte readIoByte(int offset) {
      int address = CalculateAddress(false, SegmentRegs.None, offset);
      PrintBusRequest(address, (byte) 0x00, BusStatus.IoRd, SegmentRegs.None);

      return ExecuteBusCycle(BusStatus.IoRd, address);
   }

   @Override
   public int readIoWord(int offset) {
      byte dataLo = readIoByte(offset);

      offset++; // 16-bit value allows for wrapping within a segment

      byte dataHi = readIoByte(offset);

      int readData = (int) ((dataHi << 8) | dataLo);
      return readData;
   }

   @Override
   public void writeIoByte(int offset, byte data) {
      int address = CalculateAddress(false, SegmentRegs.None, offset);
      PrintBusRequest(address, data, BusStatus.IoWr, SegmentRegs.None);

      ExecuteBusCycle(BusStatus.IoWr, address, data);
   }

   @Override
   public void writeIoWord(int offset, int data) {
      byte dataLo = (byte) (data & 0x00FF);
      writeIoByte(offset, dataLo);

      offset++; // 16-bit value allows for wrapping within a segment

      byte dataHi = (byte) (data >> 8);
      writeIoByte(offset, dataHi);
   }

   @Override
   public byte interruptAck() {
      _assertLock = true;
      ExecuteBusCycle(BusStatus.IntrAck, 0x00000);
      _clock.waitForFallingEdge();
      _clock.waitForFallingEdge();
      byte readData = ExecuteBusCycle(BusStatus.IntrAck, 0x00000);
      return readData;
   }

   @Override
   public byte sendHalt() {
      _clock.waitForRisingEdge();

      _deviceAdapter.setBusStatusPins(BusStatus.Halt);

      return (byte) 0xEE;
   }

   private int CalculateAddress(boolean useSegmentOverride, SegmentRegs segmentReg, int offset) {
      int address = 0;

      SegmentRegs localSegmentOverride = SegmentRegs.FromInt(getPrefixFlags() & 0xF0);

      if (useSegmentOverride && localSegmentOverride.value != 0) {
         // Add two additional clocks for segment override
         _clock.incClockCounter((byte) 2);

         switch (localSegmentOverride) {
            case CS:
               address = (int) ((_registers.CS << 4) + offset);
               break;
            case DS:
               address = (int) ((_registers.DS << 4) + offset);
               break;
            case ES:
               address = (int) ((_registers.ES << 4) + offset);
               break;
            case SS:
               address = (int) ((_registers.SS << 4) + offset);
               break;
            case None:
               break;
         }
         ;
      } else {
         switch (segmentReg) {
            case None:
               address = offset;
               break;
            case CS:
               address = (int) ((_registers.CS << 4) + offset);
               break;
            case DS:
               address = (int) ((_registers.DS << 4) + offset);
               break;
            case ES:
               address = (int) ((_registers.ES << 4) + offset);
               break;
            case SS:
               address = (int) ((_registers.SS << 4) + offset);
               break;
         }
      }

      return address;
   }

   private byte ExecuteBusCycle(BusStatus busStatus, int address) {
      return ExecuteBusCycle(busStatus, address, (byte) 0);
   }

   private byte ExecuteBusCycle(BusStatus busStatus, int address, byte writeData) {
      //////////////////////////////////////////
      // Execute the 8088 Maximum Mode Bus Cycle
      //////////////////////////////////////////

      // T0.5 Drive the status pins to begin the cycle on the rising edge of CLK
      // -----------------------------------------------------------------------------
      _clock.waitForRisingEdge();
      _clkLogger.trace("----T0.5----");

      _deviceAdapter.setBusStatusPins(busStatus);

      // T1
      // -----------------------------------------------------------------------------
      _clock.waitForFallingEdge();
      _clkLogger.trace("----T1----");

      _deviceAdapter.setAddrBusPins(address);

      // T2 - If a read cycle, disable the AD[7:0] buffer
      // If a write cycle, drive data onto the AD[7:0] pins
      // -----------------------------------------------------------------------------

      _clock.waitForFallingEdge();
      _clkLogger.trace("----T2----");

      // More likely to get POST error 101 with this code
      if (_assertLock) {
         _deviceAdapter.setLockPin((byte) 0x0);
         _assertLock = false;
      } else {
         _deviceAdapter.setLockPin((byte) 0x1);
      }

      if (busStatus.IsReadOperation()) {
         _deviceAdapter.setRdPin((byte) 0);
      } else {
         _deviceAdapter.setDataBusPins(writeData);
      }

      // T3 - Sample the READY signal on the falling edge of CLK until it goes high
      // Then set the Status bits to Passive (b111)
      // -----------------------------------------------------------------------------

      do {
         _clock.waitForFallingEdge();
         _clkLogger.trace("----T3----");
      } while (_deviceAdapter.getReadyPin() == 0);

      _deviceAdapter.setBusStatusPins(BusStatus.Pass);

      // T4 - Sample read data on the next falling edge of CLK
      // -----------------------------------------------------------------------------
      _clock.waitForFallingEdge();
      _clkLogger.trace("----T4----");

      byte result = busStatus.IsReadOperation() ? _deviceAdapter.getDataBusPins() : (byte) 0xEE;

      _deviceAdapter.setRdPin((byte) 1);

      return result;
   }

   private void PrintBusRequest(int addr, byte data, BusStatus status, SegmentRegs segReg) {
      String statusStr;

      switch (status) {
         case Code:
            statusStr = "C";
            break;
         case MemRd:
            statusStr = "R";
            break;
         case MemWr:
            statusStr = "W";
            break;
         case IoRd:
            statusStr = "I";
            break;
         case IoWr:
            statusStr = "O";
            break;
         case IntrAck:
            statusStr = "A";
            break;
         case Halt:
            statusStr = "H";
            break;
         case Pass:
            statusStr = "P";
            break;
         default:
            statusStr = "?";
      }

      char area = '-';

      String segRegStr = segReg == SegmentRegs.None ? "--" : segReg.toString();

      String dataStr = status == BusStatus.MemWr || status == BusStatus.IoWr ? String.format("X2", data) : "--";

      _busLogger.trace("{0} {1} {2} {3:X5}:{4}", statusStr, area, segRegStr, addr, dataStr);
   }

   @Override
   public byte getPrefixFlags() {
      return _prefixFlags;
   }

   @Override
   public void setPrefixFlags(byte value) {
      _prefixFlags = value;
   }

   @Override
   public void orEqPrefixFlags(byte value) {
      _prefixFlags += value;
   }
}
