package us.rdodd.digital.i8088;

public interface PinsInternalIntf {
   void setAddrBusPins(int value);

   byte getDataBusPins();

   void setDataBusPins(byte value);

   void setBusStatusPins(BusStatus value);

   void setSegRegPins(byte value);

   byte getIntrPin();

   byte getNmiPin();

   void setLockPin(byte value);

   byte getClkPin();

   byte getResetPin();

   byte getReadyPin();

   void setRdPin(byte value);
}
