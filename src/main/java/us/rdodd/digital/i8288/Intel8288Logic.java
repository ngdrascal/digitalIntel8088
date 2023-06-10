package us.rdodd.digital.i8288;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Intel8288Logic {

   //   +------------------+
   //  1| IOB          VCC |20
   //  2| CLK           S0 |19
   //  3| S1            S2 |18
   //  4| DR/~R  MCE/!PDEN |17
   //  5| ALE          DEN |16
   //  6| ~AEN         CEN |15
   //  7| ~MRDC      ~INTA |14
   //  8| ~AMWC      ~IORC |13
   //  9| ~MWTC     ~AIOWC |12
   // 10| GND        ~IOWC |11
   //  +-------------------+

   private enum BusCycleStates {
      TNONE, T1LOW, T1HIGH, T2LOW, T2HIGH, T3LOW, T3HIGH, T4LOW, T4HIGH;

      BusCycleStates Next(BusCycleStates current) {
         switch (current) {
            case TNONE:
               return T1LOW;
            case T1LOW:
               return T1HIGH;
            case T1HIGH:
               return T2LOW;
            case T2LOW:
               return T2HIGH;
            case T2HIGH:
               return T3LOW;
            case T3LOW:
               return T3HIGH;
            case T3HIGH:
               return T4LOW;
            case T4LOW:
               return T4HIGH;
            case T4HIGH:
               return TNONE;
            default:
               return TNONE;
         }
      }
   }

   private enum Modes {
      SYSTEM, IO
   }

   private byte clkIn;
   private byte previousStatus;
   private byte status;
   private byte dtrOut;
   private byte aleOut;
   private byte mrdcOut;
   private byte amwcOut;
   private byte mwtcOut;
   private byte iowcOut;
   private byte aiowcOut;
   private byte iorcOut;
   private byte intaOut;
   private byte denOut;
   private byte mceOut;

   private Logger logger;

   private static final byte INTRA = 0;
   private static final byte IORD = 1;
   private static final byte IOWR = 2;
   private static final byte HALT = 3;
   private static final byte CODE = 4;
   private static final byte MRD = 5;
   private static final byte MWR = 6;
   private static final byte PASS = 7;

   private static final byte LOW = 0;
   private static final byte HIGH = 1;

   private BusCycleStates busCycleT;
   private boolean advanceT;

   private byte command;
   private boolean cmdEnabled;
   private boolean addrEnabled;
   private Modes mode;

   public Intel8288Logic() {
      super();

      dtrOut = HIGH;
      aleOut = LOW;
      mrdcOut = HIGH;
      amwcOut = HIGH;
      mwtcOut = HIGH;
      iowcOut = HIGH;
      aiowcOut = HIGH;
      iorcOut = HIGH;
      intaOut = HIGH;
      mceOut = LOW;

      busCycleT = BusCycleStates.TNONE;
      advanceT = false;

      cmdEnabled = true;
      addrEnabled = true;
      mode = Modes.SYSTEM;

      logger = LoggerFactory.getLogger("i8288");
   }

   public void setIOB(byte value) {
      value = clampValue(value);
      mode = value == 0 ? Modes.SYSTEM : Modes.IO;
   }

   public void setCLK(byte value) {
      clkIn = clampValue(value);
   }

   public byte getDTR() {
      return dtrOut;
   }

   public byte getALE() {
      return aleOut;
   }

   public void setAEN(byte value) {
      value = clampValue(value);
      addrEnabled = value == LOW;
   }

   public byte getMRDC() {
      if (isMemCmdEnabled())
         return mrdcOut;
      else
         return HIGH;
   }

   public byte getAMWC() {
      if (isMemCmdEnabled())
         return amwcOut;
      else
         return HIGH;
   }

   public byte getMWTC() {
      if (isMemCmdEnabled())
         return mwtcOut;
      else
         return HIGH;
   }

   public byte getIOWC() {
      if (isIoCmdEnabled())
         return iowcOut;
      else
         return HIGH;
   }

   public byte getAIOWC() {
      if (isIoCmdEnabled())
         return aiowcOut;
      else
         return HIGH;
   }

   public byte getIORC() {
      if (isIoCmdEnabled())
         return iorcOut;
      else
         return HIGH;
   }

   public byte getINTA() {
      if (isIoCmdEnabled())
         return intaOut;
      else
         return HIGH;
   }

   public void setCEN(byte value) {
      value = clampValue(value);
      cmdEnabled = value == HIGH;
   }

   public byte getDEN() {
      if (cmdEnabled)
         return denOut;
      else
         return LOW;
   }

   public byte getMCEPDEN() {
      if (mode == Modes.SYSTEM)
         return mceOut;

      if (cmdEnabled)
         return denOut == 0 ? (byte) 1 : (byte) 0;
      else
         return HIGH;
   }

   public void setS0(byte value) {
      value = clampValue(value);

      if (value == 0)
         status &= 0b110;
      else
         status |= 0b001;
   }

   public void setS1(byte value) {
      value = clampValue(value);

      if (value == 0)
         status &= 0b101;
      else
         status |= 0b010;
   }

   public void setS2(byte value) {
      value = clampValue(value);

      if (value == 0)
         status &= 0b011;
      else
         status |= 0b100;
   }

   public byte getBusT() {
      switch (busCycleT) {
         case T1LOW:
         case T1HIGH:
            return (byte) 1;
         case T2LOW:
         case T2HIGH:
            return (byte) 2;
         case T3LOW:
         case T3HIGH:
            return (byte) 3;
         case T4LOW:
         case T4HIGH:
            return (byte) 4;
         default:
            return (byte) 0;
      }
   }

   private byte clampValue(byte value) {
      if (value > HIGH)
         value = HIGH;
      else if (value < LOW)
         value = LOW;

      return value;
   }

   public void step() {
      logger.trace("step()");

      if (previousStatus == PASS && status != PASS) {
         busCycleT = BusCycleStates.T1LOW;
         command = status;
      } else if (advanceT) {
         busCycleT = busCycleT.Next(busCycleT);
         advanceT = false;
      }
      previousStatus = status;

      switch (busCycleT) {
         case T1LOW:
            executeT1Low();
            advanceT = true;
            break;
         case T1HIGH:
            executeT1High();
            advanceT = true;
            break;
         case T2LOW:
            executeT2Low();
            advanceT = true;
            break;
         case T2HIGH:
            executeT2High();
            advanceT = true;
            break;
         case T3LOW:
            executeT3Low();
            advanceT = true;
            break;
         case T3HIGH:
            executeT3High();
            if (clkIn == HIGH && status == PASS)
               advanceT = true;
            break;
         case T4LOW:
            executeT4Low();
            advanceT = true;
            break;
         case T4HIGH:
            executeT4High();
            advanceT = true;
            break;
         default:
            break;
      }
   }

   private void executeT1Low() {
      aleOut = HIGH;
      if (mode == Modes.SYSTEM)
         mceOut = HIGH;
   }

   private void executeT1High() {
      aleOut = LOW;
   }

   private void executeT2Low() {
      switch (command) {
         case INTRA:
            dtrOut = LOW;
            intaOut = LOW;
            break;
         case IORD:
            dtrOut = LOW;
            iorcOut = LOW;
            break;
         case IOWR:
            denOut = HIGH;
            aiowcOut = LOW;
            break;
         case HALT:
            break;
         case MRD:
         case CODE:
            dtrOut = LOW;
            mrdcOut = LOW;
            break;
         case MWR:
            denOut = HIGH;
            amwcOut = LOW;
            break;
      }

      mceOut = LOW;
   }

   private void executeT2High() {
      if (isReadCommand(command)) {
         denOut = HIGH;
      }
   }

   private void executeT3Low() {
      switch (command) {
         case INTRA:
            break;
         case IORD:
            break;
         case IOWR:
            iowcOut = LOW;
            break;
         case HALT:
            break;
         case MRD:
         case CODE:
            break;
         case MWR:
            mwtcOut = LOW;
            break;
      }
   }

   private void executeT3High() {
   }

   private void executeT4Low() {
      mrdcOut = HIGH;
      amwcOut = HIGH;
      mwtcOut = HIGH;
      iowcOut = HIGH;
      aiowcOut = HIGH;
      iorcOut = HIGH;
      intaOut = HIGH;

      if (isReadCommand(command))
         denOut = LOW;
   }

   private void executeT4High() {
      denOut = LOW;
      dtrOut = HIGH;
   }

   private boolean isReadCommand(byte command) {
      return command == MRD || command == CODE || command == IORD || command == INTRA;
   }

   private boolean isMemCmdEnabled() {
      return cmdEnabled && addrEnabled;
   }

   private boolean isIoCmdEnabled() {
      if (mode == Modes.SYSTEM)
         return cmdEnabled && addrEnabled;
      else
         return cmdEnabled;
   }
}
