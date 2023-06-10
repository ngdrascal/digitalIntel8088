package us.rdodd.digital.i8088;

public interface IHostDeviceAdapter {
   void setAddrBusPins(int value);

   byte getDataBusPins();

   void setDataBusPins(byte value);

   void setBusStatusPins(BusStatus value);

   void setSegmentRegister(byte value);

   byte getIntrPin();

   byte getNmiPin();

   void setLockPin(byte value);

   byte getClkPin();

   byte getResetPin();

   byte getReadyPin();

   void setRdPin(byte value);
}
