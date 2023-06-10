package us.rdodd.digital.i8288;

import de.neemann.digital.core.Model;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.element.ElementAttributes;

import junit.framework.TestCase;

import static de.neemann.digital.core.ObservableValues.ovs;

import org.junit.Test;

public class i8288Tests extends TestCase {
   private final byte LOW = 0;
   private final byte HIGH = 1;

   private final byte MODE_SYSTEM = 0;
   private final byte MODE_IO = 1;

   private static final byte CMD_PASS = 7;

   // inputs
   private ObservableValue pinIOB = new ObservableValue("IOB", 1);
   private ObservableValue pinCLK = new ObservableValue("CLK", 1);
   private ObservableValue pinS0 = new ObservableValue("S0", 1);
   private ObservableValue pinAEN = new ObservableValue("AEN", 1);
   private ObservableValue pinCEN = new ObservableValue("CEN", 1);
   private ObservableValue pinS1 = new ObservableValue("S1", 1);
   private ObservableValue pinS2 = new ObservableValue("S2", 1);

   // outputs
   private ObservableValue pinDTR;
   private ObservableValue pinALE;
   private ObservableValue pinDEN;
   private ObservableValue pinMCEPDEN;
   private ObservableValue pinINTA;
   private ObservableValue pinMRDC;
   private ObservableValue pinAMWC;
   private ObservableValue pinMWTC;
   private ObservableValue pinIORC;
   private ObservableValue pinAIOWC;
   private ObservableValue pinIOWC;

   private final int TTIDX_IOB = 0;
   private final int TTIDX_AEN = TTIDX_IOB + 1;
   private final int TTIDX_CEN = TTIDX_AEN + 1;
   private final int TTIDX_STATUS = TTIDX_CEN + 1;
   private final int TTIDX_DTR = TTIDX_STATUS + 1;
   private final int TTIDX_ALE = TTIDX_DTR + 1;
   private final int TTIDX_DEN = TTIDX_ALE + 1;
   private final int TTIDX_MCE = TTIDX_DEN + 1;
   private final int TTIDX_MRDC = TTIDX_MCE + 1;
   private final int TTIDX_MWTC = TTIDX_MRDC + 1;
   private final int TTIDX_AMWC = TTIDX_MWTC + 1;
   private final int TTIDX_IORC = TTIDX_AMWC + 1;
   private final int TTIDX_IOWC = TTIDX_IORC + 1;
   private final int TTIDX_AIOWC = TTIDX_IOWC + 1;
   private final int TTIDX_INTA = TTIDX_AIOWC + 1;

   private Model initModel(byte mode) throws NodeException {
      Model model = new Model();
      Intel8288Component i8288Comp = model.add(new Intel8288Component(new ElementAttributes()));

      i8288Comp.setInputs(ovs(pinIOB, pinAEN, pinCEN, pinCLK, pinS2, pinS1, pinS0));

      pinDTR = i8288Comp.getOutputs().get(0);
      pinALE = i8288Comp.getOutputs().get(1);
      pinDEN = i8288Comp.getOutputs().get(2);
      pinMCEPDEN = i8288Comp.getOutputs().get(3);
      pinINTA = i8288Comp.getOutputs().get(4);
      pinMRDC = i8288Comp.getOutputs().get(5);
      pinAMWC = i8288Comp.getOutputs().get(6);
      pinMWTC = i8288Comp.getOutputs().get(7);
      pinIORC = i8288Comp.getOutputs().get(8);
      pinAIOWC = i8288Comp.getOutputs().get(9);
      pinIOWC = i8288Comp.getOutputs().get(10);

      model.init();

      // put device into a know state
      pinIOB.setValue(mode); // 0 = System, 1 = IO
      pinAEN.setValue(LOW); // Address-Enable: Active LOW
      pinCEN.setValue(HIGH); // Command-Enable: Active HIGH

      setStatus(CMD_PASS);
      pinCLK.setValue(LOW);
      model.doStep();
      pinCLK.setValue(HIGH);
      model.doStep();

      return model;
   }

   private void runTest(Model model, byte[][] table) throws NodeException {
      for (int i = 0; i < 10; i++) {
         System.out.print(i);
         
         // ACT:
         pinAEN.setValue(table[i][TTIDX_AEN]);
         pinCEN.setValue(table[i][TTIDX_CEN]);
         setStatus(table[i][TTIDX_STATUS]);
         pinCLK.setValue(i % 2);
         model.doStep();

         // ASSERT:
         assertEquals("DTR", table[i][TTIDX_DTR], pinDTR.getValue());
         assertEquals("ALE", table[i][TTIDX_ALE], pinALE.getValue());
         assertEquals("DEN", table[i][TTIDX_DEN], pinDEN.getValue());
         assertEquals("MCE", table[i][TTIDX_MCE], pinMCEPDEN.getValue());
         assertEquals("MRDC", table[i][TTIDX_MRDC], pinMRDC.getValue());
         assertEquals("MWTC", table[i][TTIDX_MWTC], pinMWTC.getValue());
         assertEquals("AMWC", table[i][TTIDX_AMWC], pinAMWC.getValue());
         assertEquals("IORC", table[i][TTIDX_IORC], pinIORC.getValue());
         assertEquals("IOWC", table[i][TTIDX_IOWC], pinIOWC.getValue());
         assertEquals("AIOWC", table[i][TTIDX_AIOWC], pinAIOWC.getValue());
         assertEquals("INTA", table[i][TTIDX_INTA], pinINTA.getValue());
      }

      System.out.println();
   }

   @Test
   public void testMemReadSysMode() throws NodeException {
      // Arrange:
      Model model = initModel(MODE_SYSTEM);

      byte[][] truthTable = {
            //         S
            //         T                             A
            //         A              M  M  A  I  I  I  I
            //I  A  C  T  D  A  D  M  R  W  M  O  O  O  N
            //O  E  E  U  T  L  E  C  D  T  W  R  W  W  T
            //B  N  N  S  R  E  N  E  C  C  C  C  C  C  A
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup Low
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup HIGH
            { 0, 0, 1, 5, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 LOW
            { 0, 0, 1, 5, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 HIGH
            { 0, 0, 1, 5, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1 }, // T2 LOW
            { 0, 0, 1, 5, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1 }, // T2 HIGH
            { 0, 0, 1, 7, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1 }, // T3 LOW
            { 0, 0, 1, 7, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1 }, // T3 HIGH
            { 0, 0, 1, 7, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // T4 LOW
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 } //  T4 HIGH
      };

      // Act & Assert:
      runTest(model, truthTable);
   }

   @Test
   public void testCodeReadSysMode() throws NodeException {
      // Arrange:
      Model model = initModel(MODE_SYSTEM);

      byte[][] truthTable = {
            //         S
            //         T                             A
            //         A              M  M  A  I  I  I  I
            //I  A  C  T  D  A  D  M  R  W  M  O  O  O  N
            //O  E  E  U  T  L  E  C  D  T  W  R  W  W  T
            //B  N  N  S  R  E  N  E  C  C  C  C  C  C  A
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup Low
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup HIGH
            { 0, 0, 1, 4, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 LOW
            { 0, 0, 1, 4, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 HIGH
            { 0, 0, 1, 4, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1 }, // T2 LOW
            { 0, 0, 1, 4, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1 }, // T2 HIGH
            { 0, 0, 1, 7, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1 }, // T3 LOW
            { 0, 0, 1, 7, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1 }, // T3 HIGH
            { 0, 0, 1, 7, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // T4 LOW
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 } //  T4 HIGH
      };

      // Act & Assert:
      runTest(model, truthTable);
   }

   @Test
   public void testMemWriteSysMode() throws NodeException {
      // Arrange:
      Model model = initModel(MODE_SYSTEM);

      byte[][] truthTable = {
            //         S
            //         T                             A
            //         A              M  M  A  I  I  I  I
            //I  A  C  T  D  A  D  M  R  W  M  O  O  O  N
            //O  E  E  U  T  L  E  C  D  T  W  R  W  W  T
            //B  N  N  S  R  E  N  E  C  C  C  C  C  C  A
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup LOW
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup HIGH
            { 0, 0, 1, 6, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 LOW
            { 0, 0, 1, 6, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 HIGH
            { 0, 0, 1, 6, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1 }, // T2 LOW
            { 0, 0, 1, 6, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1 }, // T2 HIGH
            { 0, 0, 1, 7, 1, 0, 1, 0, 1, 0, 0, 1, 1, 1, 1 }, // T3 LOW
            { 0, 0, 1, 7, 1, 0, 1, 0, 1, 0, 0, 1, 1, 1, 1 }, // T3 HIGH
            { 0, 0, 1, 7, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1 }, // T4 LOW
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 } //  T4 HIGH
      };

      // Act & Assert:
      runTest(model, truthTable);
   }

   @Test
   public void testIoReadSysMode() throws NodeException {
      // Arrange:
      Model model = initModel(MODE_SYSTEM);

      byte[][] truthTable = {
            //         S
            //         T                             A
            //         A              M  M  A  I  I  I  I
            //I  A  C  T  D  A  D  M  R  W  M  O  O  O  N
            //O  E  E  U  T  L  E  C  D  T  W  R  W  W  T
            //B  N  N  S  R  E  N  E  C  C  C  C  C  C  A
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup Low
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup HIGH
            { 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 LOW
            { 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 HIGH
            { 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 1 }, // T2 LOW
            { 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1 }, // T2 HIGH
            { 0, 0, 1, 7, 0, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1 }, // T3 LOW
            { 0, 0, 1, 7, 0, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1 }, // T3 HIGH
            { 0, 0, 1, 7, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // T4 LOW
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 } //  T4 HIGH
      };

      // Act & Assert:
      runTest(model, truthTable);
   }

   @Test
   public void testIoReadIoMode() throws NodeException {
      // Arrange:
      Model model = initModel(MODE_IO);

      byte[][] truthTable = {
            //         S
            //         T                             A
            //         A           P  M  M  A  I  I  I  I
            //I  A  C  T  D  A  D  D  R  W  M  O  O  O  N
            //O  E  E  U  T  L  E  E  D  T  W  R  W  W  T
            //B  N  N  S  R  E  N  N  C  C  C  C  C  C  A
            { 0, 0, 1, 7, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // Warmup Low
            { 0, 0, 1, 7, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // Warmup HIGH
            { 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 LOW
            { 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 HIGH
            { 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1 }, // T2 LOW
            { 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1 }, // T2 HIGH
            { 0, 0, 1, 7, 0, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1 }, // T3 LOW
            { 0, 0, 1, 7, 0, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1 }, // T3 HIGH
            { 0, 0, 1, 7, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T4 LOW
            { 0, 0, 1, 7, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 } //  T4 HIGH
      };

      // Act & Assert:
      runTest(model, truthTable);
   }

   @Test
   public void testIoWriteSysMode() throws NodeException {
      // Arrange:
      Model model = initModel(MODE_SYSTEM);

      byte[][] truthTable = {
            //         S
            //         T                             A
            //         A              M  M  A  I  I  I  I
            //I  A  C  T  D  A  D  M  R  W  M  O  O  O  N
            //O  E  E  U  T  L  E  C  D  T  W  R  W  W  T
            //B  N  N  S  R  E  N  E  C  C  C  C  C  C  A
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup Low
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup HIGH
            { 0, 0, 1, 2, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 LOW
            { 0, 0, 1, 2, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 HIGH
            { 0, 0, 1, 2, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1 }, // T2 LOW
            { 0, 0, 1, 2, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1 }, // T2 HIGH
            { 0, 0, 1, 7, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1 }, // T3 LOW
            { 0, 0, 1, 7, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1 }, // T3 HIGH
            { 0, 0, 1, 7, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1 }, // T4 LOW
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 } //  T4 HIGH
      };

      // Act & Assert:
      runTest(model, truthTable);
   }

   @Test
   public void testIoWriteIoMode() throws NodeException {
      // Arrange:
      Model model = initModel(MODE_IO);

      byte[][] truthTable = {
            //         S
            //         T                             A
            //         A           P  M  M  A  I  I  I  I
            //I  A  C  T  D  A  D  D  R  W  M  O  O  O  N
            //O  E  E  U  T  L  E  E  D  T  W  R  W  W  T
            //B  N  N  S  R  E  N  N  C  C  C  C  C  C  A
            { 0, 0, 1, 7, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // Warmup Low
            { 0, 0, 1, 7, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // Warmup HIGH
            { 0, 0, 1, 2, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 LOW
            { 0, 0, 1, 2, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 HIGH
            { 0, 0, 1, 2, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1 }, // T2 LOW
            { 0, 0, 1, 2, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1 }, // T2 HIGH
            { 0, 0, 1, 7, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1 }, // T3 LOW
            { 0, 0, 1, 7, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1 }, // T3 HIGH
            { 0, 0, 1, 7, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1 }, // T4 LOW
            { 0, 0, 1, 7, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 } //  T4 HIGH
      };

      // Act & Assert:
      runTest(model, truthTable);
   }

   @Test
   public void testIntrAckSysMode() throws NodeException {
      // Arrange:
      Model model = initModel(MODE_SYSTEM);

      byte[][] truthTable = {
            //         S
            //         T                             A
            //         A              M  M  A  I  I  I  I
            //I  A  C  T  D  A  D  M  R  W  M  O  O  O  N
            //O  E  E  U  T  L  E  C  D  T  W  R  W  W  T
            //B  N  N  S  R  E  N  E  C  C  C  C  C  C  A
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup Low
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // Warmup HIGH
            { 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 LOW
            { 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 }, // T1 HIGH
            { 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0 }, // T2 LOW
            { 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0 }, // T2 HIGH
            { 0, 0, 1, 7, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0 }, // T3 LOW
            { 0, 0, 1, 7, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0 }, // T3 HIGH
            { 0, 0, 1, 7, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }, // T4 LOW
            { 0, 0, 1, 7, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 } //  T4 HIGH
      };

      // Act & Assert:
      runTest(model, truthTable);
   }

   private void setStatus(byte value) {
      pinS0.setValue((byte) value & 0b00000001);
      pinS1.setValue((byte) (value & 0b00000010) >> 1);
      pinS2.setValue((byte) (value & 0b00000100) >> 2);
   }
}
