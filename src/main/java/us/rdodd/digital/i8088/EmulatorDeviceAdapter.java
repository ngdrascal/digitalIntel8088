package us.rdodd.digital.i8088;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmulatorDeviceAdapter implements IHostDeviceAdapter {
   private final IEmulatorDevice _device;
   private final Logger _pinLogger;
   private final Logger _nmiLogger;

   public EmulatorDeviceAdapter(IEmulatorDevice device) {
      _device = device;
      _pinLogger = LoggerFactory.getLogger("cpu.pins");
      _nmiLogger = LoggerFactory.getLogger("cpu.pins.nmi");
   }

   @Override
   public void setAddrBusPins(int value) {
      _pinLogger.trace("{0}.set(0x{1:X5})", "setAddrBusPins", value);

      for (int pinNumber : IPinMap.AddrPins) {
         byte bitValue = (byte) (value & 0x00000001);

         _device.Write(pinNumber, bitValue);

         value >>= 1;
      }
   }

   @Override
   public byte getDataBusPins() {
      byte data = 0;
      for (int i = 0; i < IPinMap.DataPins.length; i++) {
         long pv = _device.Read(IPinMap.DataPins[i]) << i;
         data |= (byte) pv;
      }

      _pinLogger.trace("{0}.get(): 0x{1:X2}", "getDataBusPins", data);

      return data;
   }

   @Override
   public void setDataBusPins(byte value) {
      _pinLogger.trace("{0}.set(0x{1X2})", "setDataBusPins", value);

      for (int i = 0; i < IPinMap.DataPins.length; i++) {
         byte pv = (byte) (value & 0b00000001);
         _device.Write(IPinMap.DataPins[i], pv);

         value >>= 1;
      }
   }

   @Override
   public void setBusStatusPins(BusStatus value)

   {
      _pinLogger.trace("{0}.set({1})", "setBusStatusPins", value);

      _device.Write(IPinMap.S0, (byte) ((value.ordinal() & 0x01) >> 0));
      _device.Write(IPinMap.S1, (byte) ((value.ordinal() & 0x02) >> 1));
      _device.Write(IPinMap.S2, (byte) ((value.ordinal() & 0x04) >> 2));
   }

   @Override
   public void setSegmentRegister(byte value) {

      _pinLogger.trace("{0}.set({1})", "setSegmentRegister", value);

      _device.Write(IPinMap.A16, (byte) ((value & 0x01) >> 0));
      _device.Write(IPinMap.A17, (byte) ((value & 0x02) >> 1));
   }

   @Override
   public byte getIntrPin() {
      byte result = _device.Read(IPinMap.INTR);

      _pinLogger.trace("{0}.get(): {1}", "getIntrPin", result);

      return result;
   }

   @Override
   public byte getNmiPin() {

      byte result = _device.Read(IPinMap.NMI);

      _nmiLogger.trace("{0}.get(): {1}", "getNmiPin", result);

      return result;
   }

   @Override
   public void setLockPin(byte value) {
      _pinLogger.trace("{0}.set({1})", "LockPin", value);

      _device.Write(IPinMap.LOCK, value);
   }

   @Override
   public byte getClkPin() {
      byte result = _device.Read(IPinMap.CLK);
      return result;
   }

   @Override
   public byte getResetPin() {
      byte result = _device.Read(IPinMap.RESET);

      _pinLogger.trace("{0}.get(): {1}", "ResetPin", result);

      return result;
   }

   @Override
   public byte getReadyPin() {
      byte result = _device.Read(IPinMap.READY);

      _pinLogger.trace("{0}.get(): {1}", "ReadyPin", result);

      return result;
   }

   @Override
   public void setRdPin(byte value) {
      _pinLogger.trace("{0}.set({1})", "setRdPin", value);

      _device.Write(IPinMap.RD, value);
   }
}
