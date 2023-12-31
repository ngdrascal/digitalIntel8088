package us.rdodd.digital.i8088;

// import java.util.concurrent.BlockingQueue;
// import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pins implements PinsInternalIntf, PinsExternalIntf {
   private byte[] pinValues;
   private DataBusDirection busDir = DataBusDirection.HIGHZ;
   // private BlockingQueue<Byte> queue;
   private final Logger pinLogger;
   private final Logger dataBusLogger;
   // private final Logger nmiLogger;

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

      // queue = new LinkedBlockingQueue<>(2);

      pinLogger = LoggerFactory.getLogger("i8088.pins");
      // nmiLogger = LoggerFactory.getLogger("i8088.pins.nmi");
      dataBusLogger = LoggerFactory.getLogger("i8088.pins.databus");
   }

   @Override
   public void setAddrBusPins(int value) {
      pinLogger.trace(String.format("%s.set(0x%05X)", "AddrBusPins", value));

      for (int pinNumber : PinMap.AddrPins) {
         byte bitValue = (byte) (value & 0x00000001);

         writePin(pinNumber, bitValue);

         value >>= 1;
      }
   }

   @Override
   public DataBusDirection getDataBusDirection() {
      return busDir;
   }

   @Override
   public void setDataBusDirection(DataBusDirection busDir) {
      pinLogger.trace("setDataBusDirection({})", busDir);
      this.busDir = busDir;
   }

   @Override
   public byte getDataBusPins() {
      byte data = 0;
      for (int i = 0; i < PinMap.DataPins.length; i++) {
         long pv = readPin(PinMap.DataPins[i]) << i;
         data |= (byte) pv;
      }

      pinLogger.trace(String.format("%s.get(): 0x%02X", "DataBusPins", data));

      return data;
   }

   @Override
   public void setDataBusPins(byte value) {
      pinLogger.trace(String.format("%s.set(0x%02X)", "DataBusPins", value));

      for (int i = 0; i < PinMap.DataPins.length; i++) {
         byte pv = (byte) (value & 0b00000001);
         writePin(PinMap.DataPins[i], pv);

         value >>= 1;
      }
   }

   @Override
   public void setBusStatusPins(BusStatus value)

   {
      // pinLogger.trace(String.format("%s.set(%d)", "BusStatusPins", value));

      writePin(PinMap.S0, (byte) ((value.ordinal() & 0x01) >> 0));
      writePin(PinMap.S1, (byte) ((value.ordinal() & 0x02) >> 1));
      writePin(PinMap.S2, (byte) ((value.ordinal() & 0x04) >> 2));
   }

   @Override
   public void setSegRegPins(byte value) {
      // pinLogger.trace(String.format("%s.set(%d)", "SegReg", value));

      writePin(PinMap.A16, (byte) ((value & 0x01) >> 0));
      writePin(PinMap.A17, (byte) ((value & 0x02) >> 1));
   }

   @Override
   public byte getINTR() {
      byte result = readPin(PinMap.INTR);

      // pinLogger.trace(String.format("%s.get(): %d", "INTR", result));

      return result;
   }

   @Override
   public byte getNMI() {
      byte result = readPin(PinMap.NMI);

      // nmiLogger.trace(String.format("%s.get(): %d", "NMI", result));

      return result;
   }

   @Override
   public void setLOCK(byte value) {
      // pinLogger.trace(String.format("%s.set(%d)", "LOCK", value));

      writePin(PinMap.LOCK, value);
   }

   @Override
   public void setQueueStatusPins(byte value) {
      if (value < 0 || value > 3)
         throw new IllegalArgumentException("value out of range 0..3");

      writePin(PinMap.QS1, value);
      writePin(PinMap.QS1, value);
   }

   @Override
   public byte getCLK() {
      return readPin(PinMap.CLK);

      // byte result = 0;
      // try {
      //    result = queue.take();
      // } catch (InterruptedException e) {
      //    e.printStackTrace();
      // }
      // return result;
   }

   @Override
   public byte getRESET() {
      byte result = readPin(PinMap.RESET);

      // pinLogger.trace(String.format("%s.get(): $d", "RESET", result));

      return result;
   }

   @Override
   public byte getREADY() {
      byte result = readPin(PinMap.READY);

      // pinLogger.trace(String.format("%s.get(): $d", "READY", result));

      return result;
   }

   @Override
   public void setRD(byte value) {
      pinLogger.trace(String.format("%s.set(%d)", "RD", value));

      writePin(PinMap.RD, value);
   }

   @Override
   public byte getA14() {
      return readPin(PinMap.A14);
   }

   @Override
   public byte getA13() {
      return readPin(PinMap.A13);
   }

   @Override
   public byte getA12() {
      return readPin(PinMap.A12);
   }

   @Override
   public byte getA11() {
      return readPin(PinMap.A11);
   }

   @Override
   public byte getA10() {
      return readPin(PinMap.A10);
   }

   @Override
   public byte getA9() {
      return readPin(PinMap.A9);
   }

   @Override
   public byte getA8() {
      return readPin(PinMap.A8);
   }

   @Override
   public byte getAD7() {
      byte value = readPin(PinMap.AD7);
      dataBusLogger.trace(String.format("Pins.get%s(): 0x%02X", "AD7", value));
      return value;
   }

   @Override
   public void setAD7(byte value) {
      writePin(PinMap.AD7, value);
   }

   @Override
   public byte getAD6() {
      byte value = readPin(PinMap.AD6);
      dataBusLogger.trace(String.format("Pins.get%s(): 0x%02X", "AD6", value));
      return value;
   }

   @Override
   public void setAD6(byte value) {
      writePin(PinMap.AD6, value);
   }

   @Override
   public byte getAD5() {
      byte value = readPin(PinMap.AD5);
      dataBusLogger.trace(String.format("Pins.get%s(): 0x%02X", "AD5", value));
      return value;
   }

   @Override
   public void setAD5(byte value) {
      writePin(PinMap.AD5, value);
   }

   @Override
   public byte getAD4() {
      byte value = readPin(PinMap.AD4);
      dataBusLogger.trace(String.format("Pins.get%s(): 0x%02X", "AD4", value));
      return value;
   }

   @Override
   public void setAD4(byte value) {
      writePin(PinMap.AD4, value);
   }

   @Override
   public byte getAD3() {
      byte value = readPin(PinMap.AD3);
      dataBusLogger.trace(String.format("Pins.get%s(): 0x%02X", "AD3", value));
      return value;
   }

   @Override
   public void setAD3(byte value) {
      writePin(PinMap.AD3, value);
   }

   @Override
   public byte getAD2() {
      byte value = readPin(PinMap.AD2);
      dataBusLogger.trace(String.format("Pins.get%s(): 0x%02X", "AD2", value));
      return value;
   }

   @Override
   public void setAD2(byte value) {
      writePin(PinMap.AD2, value);
   }

   @Override
   public byte getAD1() {
      byte value = readPin(PinMap.AD1);
      dataBusLogger.trace(String.format("Pins.get%s(): 0x%02X", "AD1", value));
      return value;
   }

   @Override
   public void setAD1(byte value) {
      writePin(PinMap.AD1, value);
   }

   @Override
   public byte getAD0() {
      byte value = readPin(PinMap.AD0);
      dataBusLogger.trace(String.format("Pins.get%s(): 0x%02X", "AD0", value));
      return value;
   }

   @Override
   public void setAD0(byte value) {
      writePin(PinMap.AD0, value);
   }

   @Override
   public void setNMI(byte value) {
      writePin(PinMap.NMI, value);
   }

   @Override
   public void setINTR(byte value) {
      writePin(PinMap.INTR, value);
   }

   @Override
   public void setCLK(byte value) {
      writePin(PinMap.CLK, value);

      // try {
      //    queue.put(value);
      // } catch (InterruptedException e) {
      //    e.printStackTrace();
      // }
   }

   @Override
   public void setRESET(byte value) {
      writePin(PinMap.RESET, value);
   }

   @Override
   public void setREADY(byte value) {
      writePin(PinMap.READY, value);
   }

   @Override
   public void setTEST(byte value) {
      writePin(PinMap.TEST, value);
   }

   @Override
   public byte getQS1() {
      return readPin(PinMap.QS1);
   }

   @Override
   public byte getQS0() {
      return readPin(PinMap.QS0);
   }

   @Override
   public byte getS0() {
      return readPin(PinMap.S0);
   }

   @Override
   public byte getS1() {
      return readPin(PinMap.S1);
   }

   @Override
   public byte getS2() {
      return readPin(PinMap.S2);
   }

   @Override
   public byte getLOCK() {
      return readPin(PinMap.LOCK);
   }

   @Override
   public byte getRQGT1() {
      return readPin(PinMap.RQGT1);
   }

   @Override
   public void setRQGT1(byte value) {
      writePin(PinMap.RQGT1, value);
   }

   @Override
   public byte getRQGT0() {
      return readPin(PinMap.RQGT0);
   }

   @Override
   public void setRQGT0(byte value) {
      writePin(PinMap.RQGT0, value);
   }

   @Override
   public byte getRD() {
      return readPin(PinMap.RD);
   }

   @Override
   public void setMNMX(byte value) {
      writePin(PinMap.MNMX, value);
   }

   @Override
   public byte getSS0() {
      return readPin(PinMap.SS0);
   }

   @Override
   public void setSS0(byte value) {
      writePin(PinMap.SS0, value);
   }

   @Override
   public byte getA19() {
      return readPin(PinMap.A19);
   }

   @Override
   public byte getA18() {
      return readPin(PinMap.A18);
   }

   @Override
   public byte getA17() {
      return readPin(PinMap.A17);
   }

   @Override
   public byte getA16() {
      return readPin(PinMap.A16);
   }

   @Override
   public byte getA15() {
      return readPin(PinMap.A15);
   }
}
