package us.rdodd.digital.i8088;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pins implements PinsInternalIntf, PinsExternalIntf {
   private byte[] pinValues;
   private final Logger pinLogger;
   private final Logger nmiLogger;

   private byte readPin(int pinNumber) {
      if (pinNumber < 1 || pinNumber > pinValues.length)
         throw new IndexOutOfBoundsException("pinNumber");

      byte value;
      synchronized (this) {
         value = pinValues[pinNumber];
      }
      return value;
   }

   private void writePin(int pinNumber, byte value) {
      if (pinNumber < 1 || pinNumber > pinValues.length)
         throw new IndexOutOfBoundsException("pinNumber");

      synchronized (this) {
         pinValues[pinNumber] = value;
      }
   }

   public Pins() {
      pinValues = new byte[40];
      for (int i = 0; i < 40; i++) {
         pinValues[i] = 0;
      }
      pinLogger = LoggerFactory.getLogger("cpu.pins");
      nmiLogger = LoggerFactory.getLogger("cpu.pins.nmi");
   }

   @Override
   public void setAddrBusPins(int value) {
      pinLogger.trace("{0}.set(0x{1:X5})", "setAddrBusPins", value);

      for (int pinNumber : PinMap.AddrPins) {
         byte bitValue = (byte) (value & 0x00000001);

         writePin(pinNumber, bitValue);

         value >>= 1;
      }
   }

   @Override
   public byte getDataBusPins() {
      byte data = 0;
      for (int i = 0; i < PinMap.DataPins.length; i++) {
         long pv = readPin(PinMap.DataPins[i]) << i;
         data |= (byte) pv;
      }

      pinLogger.trace("{0}.get(): 0x{1:X2}", "getDataBusPins", data);

      return data;
   }

   @Override
   public void setDataBusPins(byte value) {
      pinLogger.trace("{0}.set(0x{1X2})", "setDataBusPins", value);

      for (int i = 0; i < PinMap.DataPins.length; i++) {
         byte pv = (byte) (value & 0b00000001);
         writePin(PinMap.DataPins[i], pv);

         value >>= 1;
      }
   }

   @Override
   public void setBusStatusPins(BusStatus value)

   {
      pinLogger.trace("{0}.set({1})", "setBusStatusPins", value);

      writePin(PinMap.S0, (byte) ((value.ordinal() & 0x01) >> 0));
      writePin(PinMap.S1, (byte) ((value.ordinal() & 0x02) >> 1));
      writePin(PinMap.S2, (byte) ((value.ordinal() & 0x04) >> 2));
   }

   @Override
   public void setSegRegPins(byte value) {

      pinLogger.trace("{0}.set({1})", "setSegmentRegister", value);

      writePin(PinMap.A16, (byte) ((value & 0x01) >> 0));
      writePin(PinMap.A17, (byte) ((value & 0x02) >> 1));
   }

   @Override
   public byte getIntrPin() {
      byte result = readPin(PinMap.INTR);

      pinLogger.trace("{0}.get(): {1}", "getIntrPin", result);

      return result;
   }

   @Override
   public byte getNmiPin() {

      byte result = readPin(PinMap.NMI);

      nmiLogger.trace("{0}.get(): {1}", "getNmiPin", result);

      return result;
   }

   @Override
   public void setLockPin(byte value) {
      pinLogger.trace("{0}.set({1})", "LockPin", value);

      writePin(PinMap.LOCK, value);
   }

   @Override
   public byte getClkPin() {
      byte result = readPin(PinMap.CLK);
      return result;
   }

   @Override
   public byte getResetPin() {
      byte result = readPin(PinMap.RESET);

      pinLogger.trace("{0}.get(): {1}", "ResetPin", result);

      return result;
   }

   @Override
   public byte getReadyPin() {
      byte result = readPin(PinMap.READY);

      pinLogger.trace("{0}.get(): {1}", "ReadyPin", result);

      return result;
   }

   @Override
   public void setRdPin(byte value) {
      pinLogger.trace("{0}.set({1})", "setRdPin", value);

      writePin(PinMap.RD, value);
   }

   @Override
   public byte getA14() {
      return pinValues[PinMap.A14];
   }

   @Override
   public byte getA13() {
      return pinValues[PinMap.A13];
   }

   @Override
   public byte getA12() {
      return pinValues[PinMap.A12];
   }

   @Override
   public byte getA11() {
      return pinValues[PinMap.A11];
   }

   @Override
   public byte getA10() {
      return pinValues[PinMap.A10];
   }

   @Override
   public byte getA9() {
      return pinValues[PinMap.A9];
   }

   @Override
   public byte getA8() {
      return pinValues[PinMap.A8];
   }

   @Override
   public byte getAD7() {
      return pinValues[PinMap.AD7];
   }

   @Override
   public byte getAD6() {
      return pinValues[PinMap.AD6];
   }

   @Override
   public byte getAD5() {
      return pinValues[PinMap.AD5];
   }

   @Override
   public byte getAD4() {
      return pinValues[PinMap.AD4];
   }

   @Override
   public byte getAD3() {
      return pinValues[PinMap.AD3];
   }

   @Override
   public byte getAD2() {
      return pinValues[PinMap.AD2];
   }

   @Override
   public byte getAD1() {
      return pinValues[PinMap.AD1];
   }

   @Override
   public byte getAD0() {
      return pinValues[PinMap.AD0];
   }

   @Override
   public void setNMI(byte value) {
      pinValues[PinMap.NMI] = value;
   }

   @Override
   public void setINTR(byte value) {
      pinValues[PinMap.INTR] = value;
   }

   @Override
   public void setCLK(byte value) {
      pinValues[PinMap.CLK] = value;
   }

   @Override
   public void setRESET(byte value) {
      pinValues[PinMap.RESET] = value;
   }

   @Override
   public void setREADY(byte value) {
      pinValues[PinMap.READY] = value;
   }

   @Override
   public void setTEST(byte value) {
      pinValues[PinMap.TEST] = value;
   }

   @Override
   public byte getQS1() {
      return pinValues[PinMap.QS1];
   }

   @Override
   public byte getQS0() {
      return pinValues[PinMap.QS0];
   }

   @Override
   public byte getS0() {
      return pinValues[PinMap.S0];
   }

   @Override
   public byte getS1() {
      return pinValues[PinMap.S1];
   }

   @Override
   public byte getS2() {
      return pinValues[PinMap.S2];
   }

   @Override
   public byte getLOCK() {
      return pinValues[PinMap.LOCK];
   }

   @Override
   public byte getRQGT1() {
      return pinValues[PinMap.RQGT1];
   }

   @Override
   public void setRQGT1(byte value) {
      pinValues[PinMap.RQGT1] = value;
   }

   @Override
   public byte getRQGT0() {
      return pinValues[PinMap.RQGT0];
   }

   @Override
   public void setRQGT0(byte value) {
      pinValues[PinMap.RQGT0] = value;
   }

   @Override
   public byte getRD() {
      return pinValues[PinMap.RD];
   }

   @Override
   public void setMNMX(byte value) {
      pinValues[PinMap.MNMX] = value;
   }

   @Override
   public byte getSS0() {
      return pinValues[PinMap.SS0];

   }

   @Override
   public byte getA19() {
      return pinValues[PinMap.A19];
   }

   @Override
   public byte getA18() {
      return pinValues[PinMap.A18];
   }

   @Override
   public byte getA17() {
      return pinValues[PinMap.A17];

   }

   @Override
   public byte getA16() {
      return pinValues[PinMap.A16];

   }

   @Override
   public byte getA15() {
      return pinValues[PinMap.A15];

   }
}
