package us.rdodd.digital.i8088;

public interface PinsInternalIntf {
   void setAddrBusPins(int value);

   byte getDataBusPins();

   void setDataBusPins(byte value);

   void setBusStatusPins(BusStatus value);

   void setSegRegPins(byte value);

   byte getINTR();

   byte getNMI();

   void setLockPin(byte value);

   void setQueueStatusPins(byte value);

   byte getClkPin();

   byte getResetPin();

   byte getReadyPin();

   void setRdPin(byte value);

   void setSS0Pin(byte value);
}
