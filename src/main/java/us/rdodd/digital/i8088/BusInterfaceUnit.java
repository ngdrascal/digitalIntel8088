package us.rdodd.digital.i8088;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusInterfaceUnit implements BusInterfaceUnitIntf {
   private ClockIntf clock;
   private Registers registers;
   private PinsInternalIntf pins;

   private Logger busLogger;
   private Logger clkLogger;
   private boolean assertLock;
   private byte prefixFlags;

   private final byte LOW = 0;
   private final byte HIGH = 1;

   public BusInterfaceUnit(ClockIntf clock, Registers registers, PinsInternalIntf pins) {
      this.clock = clock;
      this.registers = registers;
      this.pins = pins;

      busLogger = LoggerFactory.getLogger("cpu.bus");
      clkLogger = LoggerFactory.getLogger("cpu.clock");
      assertLock = false;
   }

   @Override
   public byte readCode(int offset) {
      int address = calculateAddress(false, SegmentRegs.CS, offset);
      printBusRequest(address, (byte) 0x00, BusStatus.Code, SegmentRegs.CS);

      return executeBusCycle(BusStatus.Code, address);
   }

   @Override
   public byte readMemoryByte(boolean useSegmentOverride, SegmentRegs segmentReg, int offset) {
      int address = calculateAddress(useSegmentOverride, segmentReg, offset);
      printBusRequest(address, (byte) 0x00, BusStatus.MemRd, segmentReg);

      return executeBusCycle(BusStatus.MemRd, address);
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
      int address = calculateAddress(useSegmentOverride, segmentReg, offset);
      printBusRequest(address, data, BusStatus.MemWr, segmentReg);

      executeBusCycle(BusStatus.MemWr, address, data);
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
      int address = calculateAddress(false, SegmentRegs.None, offset);
      printBusRequest(address, (byte) 0x00, BusStatus.IoRd, SegmentRegs.None);

      return executeBusCycle(BusStatus.IoRd, address);
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
      int address = calculateAddress(false, SegmentRegs.None, offset);
      printBusRequest(address, data, BusStatus.IoWr, SegmentRegs.None);

      executeBusCycle(BusStatus.IoWr, address, data);
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
      assertLock = true;
      executeBusCycle(BusStatus.IntrAck, 0x00000);
      clock.waitForFallingEdge();
      clock.waitForFallingEdge();
      byte readData = executeBusCycle(BusStatus.IntrAck, 0x00000);
      return readData;
   }

   @Override
   public byte sendHalt() {
      clock.waitForRisingEdge();

      pins.setBusStatusPins(BusStatus.Halt);

      return (byte) 0xEE;
   }

   private int calculateAddress(boolean useSegmentOverride, SegmentRegs segmentReg, int offset) {
      int address = 0;

      SegmentRegs localSegmentOverride = SegmentRegs.FromInt(getPrefixFlags() & 0xF0);

      if (useSegmentOverride && localSegmentOverride.value != 0) {
         // Add two additional clocks for segment override
         clock.incClockCounter((byte) 2);

         switch (localSegmentOverride) {
            case CS:
               address = (int) ((registers.CS << 4) + offset);
               break;
            case DS:
               address = (int) ((registers.DS << 4) + offset);
               break;
            case ES:
               address = (int) ((registers.ES << 4) + offset);
               break;
            case SS:
               address = (int) ((registers.SS << 4) + offset);
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
               address = (int) ((registers.CS << 4) + offset);
               break;
            case DS:
               address = (int) ((registers.DS << 4) + offset);
               break;
            case ES:
               address = (int) ((registers.ES << 4) + offset);
               break;
            case SS:
               address = (int) ((registers.SS << 4) + offset);
               break;
         }
      }

      return address;
   }

   private byte executeBusCycle(BusStatus busStatus, int address) {
      return executeBusCycle(busStatus, address, (byte) 0);
   }

   private byte executeBusCycle(BusStatus busStatus, int address, byte writeData) {
      //////////////////////////////////////////
      // Execute the 8088 Maximum Mode Bus Cycle
      //////////////////////////////////////////

      // T0.5 Drive the status pins to begin the cycle on the rising edge of CLK
      // -----------------------------------------------------------------------------
      clock.waitForRisingEdge();
      clkLogger.trace("----T0.5----");

      pins.setBusStatusPins(busStatus);

      // T1
      // -----------------------------------------------------------------------------
      clock.waitForFallingEdge();
      clkLogger.trace("----T1----");

      pins.setAddrBusPins(address);
      pins.setDataBusDirection(DataBusDirection.OUTPUT);

      // T2 - If a read cycle, disable the AD[7:0] buffer
      // If a write cycle, drive data onto the AD[7:0] pins
      // -----------------------------------------------------------------------------

      clock.waitForFallingEdge();
      clkLogger.trace("----T2----");

      // More likely to get POST error 101 with this code
      if (assertLock) {
         pins.setLOCK(LOW);
         assertLock = false;
      } else {
         pins.setLOCK(HIGH);
      }

      if (busStatus.IsReadOperation()) {
         pins.setRD(LOW);
      } else {
         pins.setDataBusPins(writeData);
      }

      // T3 - Sample the READY signal on the falling edge of CLK until it goes high
      // Then set the Status bits to Passive (b111)
      // -----------------------------------------------------------------------------

      do {
         clock.waitForFallingEdge();
         clkLogger.trace("----T3----");
      } while (pins.getREADY() == LOW);

      pins.setBusStatusPins(BusStatus.Pass);

      // T4 - Sample read data on the next falling edge of CLK
      // -----------------------------------------------------------------------------
      clock.waitForFallingEdge();
      clkLogger.trace("----T4----");

      byte result = busStatus.IsReadOperation() ? pins.getDataBusPins() : (byte) 0xEE;

      pins.setRD(HIGH);

      return result;
   }

   private void printBusRequest(int addr, byte data, BusStatus status, SegmentRegs segReg) {
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

      busLogger.trace(String.format("%s %s %s %05X:%s", statusStr, area, segRegStr, addr, dataStr));
   }

   @Override
   public byte getPrefixFlags() {
      return prefixFlags;
   }

   @Override
   public void setPrefixFlags(byte value) {
      prefixFlags = value;
   }

   @Override
   public void orEqPrefixFlags(byte value) {
      prefixFlags += value;
   }
}
