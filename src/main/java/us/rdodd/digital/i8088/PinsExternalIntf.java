package us.rdodd.digital.i8088;

public interface PinsExternalIntf {
   byte getA14();

   byte getA13();

   byte getA12();

   byte getA11();

   byte getA10();

   byte getA9();

   byte getA8();

   byte getAD7();

   void setAD7(byte value);

   byte getAD6();

   void setAD6(byte value);

   byte getAD5();

   void setAD5(byte value);

   byte getAD4();

   void setAD4(byte value);

   byte getAD3();

   void setAD3(byte value);

   byte getAD2();

   void setAD2(byte value);

   byte getAD1();

   void setAD1(byte value);

   byte getAD0();

   void setAD0(byte value);

   void setNMI(byte value);

   void setINTR(byte value);

   void setCLK(byte value);

   void setRESET(byte value);

   void setREADY(byte value);

   void setTEST(byte value);

   byte getQS1();

   byte getQS0();

   byte getS0();

   byte getS1();

   byte getS2();

   byte getLOCK();

   byte getRQGT1();

   void setRQGT1(byte value);

   byte getRQGT0();

   void setRQGT0(byte value);

   byte getRD();

   void setMNMX(byte value);

   byte getSS0();

   byte getA19();

   byte getA18();

   byte getA17();

   byte getA16();

   byte getA15();
}
