package us.rdodd.digital.i8088;

public interface IBusInterfaceUnit {
   byte ReadCode(int offset);

   byte ReadMemoryByte(boolean useSegmentOverride, SegmentRegs segmentReg, int offset);

   int ReadMemoryWord(boolean useSegmentOverride, SegmentRegs segmentReg, int offset);

   void WriteMemoryByte(boolean useSegmentOverride, SegmentRegs segmentReg, int offset, byte data);

   void WriteMemoryWord(boolean useSegmentOverride, SegmentRegs segmentReg, int offset, int data);

   byte ReadIoByte(int offset);

   int ReadIoWord(int offset);

   void WriteIoByte(int offset, byte data);

   void WriteIoWord(int offset, int data);

   byte InterruptAck();

   byte SendHalt();

   byte getPrefixFlags();

   void setPrefixFlags(byte value);

   void orEqPrefixFlags(byte value);
}
