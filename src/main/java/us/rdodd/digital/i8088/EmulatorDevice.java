package us.rdodd.digital.i8088;

public class EmulatorDevice implements IEmulatorDevice {
   private byte[] _pinValues;

   public EmulatorDevice() {
      _pinValues = new byte[40];
      for (int i = 0; i < 40; i++) {
         _pinValues[i] = 0;
      }
   }

   @Override
   public byte Read(int pinNumber) {
      if (pinNumber < 1 || pinNumber > _pinValues.length)
         throw new IndexOutOfBoundsException("pinNumber");

      byte value;
      synchronized (this) {
         value = _pinValues[pinNumber];
      }
      return value;
   }

   @Override
   public void Write(int pinNumber, byte value) {
      if (pinNumber < 1 || pinNumber > _pinValues.length)
         throw new IndexOutOfBoundsException("pinNumber");

      synchronized (this) {
         _pinValues[pinNumber] = value;
      }
   }
}
