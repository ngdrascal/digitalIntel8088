package us.rdodd.digital.i8088;

public interface IBusInterfaceUnit {
   byte readCode(int offset);

   byte readMemoryByte(boolean useSegmentOverride, SegmentRegs segmentReg, int offset);

   int readMemoryWord(boolean useSegmentOverride, SegmentRegs segmentReg, int offset);

   void writeMemoryByte(boolean useSegmentOverride, SegmentRegs segmentReg, int offset, byte data);

   void writeMemoryWord(boolean useSegmentOverride, SegmentRegs segmentReg, int offset, int data);

   byte readIoByte(int offset);

   int readIoWord(int offset);

   void writeIoByte(int offset, byte data);

   void writeIoWord(int offset, int data);

   byte interruptAck();

   byte sendHalt();

   byte getPrefixFlags();

   void setPrefixFlags(byte value);

   void orEqPrefixFlags(byte value);
}
