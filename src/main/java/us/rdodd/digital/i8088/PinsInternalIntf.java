package us.rdodd.digital.i8088;

public interface PinsInternalIntf {
   void setAddrBusPins(int value);

   void setDataBusDirection(DataBusDirection busDir);

   byte getDataBusPins();

   void setDataBusPins(byte value);

   void setBusStatusPins(BusStatus value);

   void setSegRegPins(byte value);

   byte getINTR();

   byte getNMI();

   void setLOCK(byte value);

   void setQueueStatusPins(byte value);

   byte getCLK();

   byte getRESET();

   byte getREADY();

   void setRD(byte value);

   void setSS0(byte value);
}
