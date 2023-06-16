package us.rdodd.digital.i8088;

import org.junit.Test;

import de.neemann.digital.core.Model;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.element.ElementAttributes;
import junit.framework.TestCase;

import static de.neemann.digital.core.ObservableValues.ovs;

public class i8088CoreTests extends TestCase {
   private final byte LOW = 0;
   private final byte HIGH = 1;

   // Input pins
   private ObservableValue pinNMI = new ObservableValue("NMI", 1);;
   private ObservableValue pinINTR = new ObservableValue("INTR", 1);;
   private ObservableValue pinCLK = new ObservableValue("CLK", 1);;
   private ObservableValue pinRESET = new ObservableValue("RESET", 1);;
   private ObservableValue pinREADY = new ObservableValue("READY", 1);;
   private ObservableValue pinTEST = new ObservableValue("TEST", 1);;

   // Output pins
   private ObservableValue pinA14;
   private ObservableValue pinA13;
   private ObservableValue pinA12;
   private ObservableValue pinA11;
   private ObservableValue pinA10;
   private ObservableValue pinA9;
   private ObservableValue pinA8;
   private ObservableValue pinAD7;
   private ObservableValue pinAD6;
   private ObservableValue pinAD5;
   private ObservableValue pinAD4;
   private ObservableValue pinAD3;
   private ObservableValue pinAD2;
   private ObservableValue pinAD1;
   private ObservableValue pinAD0;

   // private ObservableValue pinQs1;
   // private ObservableValue pinQs0;
   private ObservableValue pinS0;
   private ObservableValue pinS1;
   private ObservableValue pinS2;
   // private ObservableValue pinLock;
   // private ObservableValue pinRqGt1;
   // private ObservableValue pinRqGt0;
   // private ObservableValue pinRd;
   // private ObservableValue pinSs0;

   private ObservableValue pinA19;
   private ObservableValue pinA18;
   private ObservableValue pinA17;
   private ObservableValue pinA16;
   private ObservableValue pinA15;

   private Model initModel() throws NodeException {
      Model model = new Model();
      Intel8088Component i8088Comp = model.add(new Intel8088Component(new ElementAttributes()));

      i8088Comp.setInputs(ovs(pinNMI, pinINTR, pinCLK, pinRESET, pinREADY, pinTEST));

      pinA19 = i8088Comp.getOutputs().get(0);
      pinA18 = i8088Comp.getOutputs().get(1);
      pinA17 = i8088Comp.getOutputs().get(2);
      pinA16 = i8088Comp.getOutputs().get(3);
      pinA15 = i8088Comp.getOutputs().get(4);
      pinA14 = i8088Comp.getOutputs().get(5);
      pinA13 = i8088Comp.getOutputs().get(6);
      pinA12 = i8088Comp.getOutputs().get(7);
      pinA11 = i8088Comp.getOutputs().get(8);
      pinA10 = i8088Comp.getOutputs().get(9);
      pinA9 = i8088Comp.getOutputs().get(10);
      pinA8 = i8088Comp.getOutputs().get(11);
      pinAD7 = i8088Comp.getOutputs().get(12);
      pinAD6 = i8088Comp.getOutputs().get(13);
      pinAD5 = i8088Comp.getOutputs().get(14);
      pinAD4 = i8088Comp.getOutputs().get(15);
      pinAD3 = i8088Comp.getOutputs().get(16);
      pinAD2 = i8088Comp.getOutputs().get(17);
      pinAD1 = i8088Comp.getOutputs().get(18);
      pinAD0 = i8088Comp.getOutputs().get(19);
      // pinQs1 = i8088Comp.getOutputs().get(20);
      // pinQs0 = i8088Comp.getOutputs().get(21);
      pinS2 = i8088Comp.getOutputs().get(22);
      pinS1 = i8088Comp.getOutputs().get(23);
      pinS0 = i8088Comp.getOutputs().get(24);
      // pinLock = i8088Comp.getOutputs().get(25);
      // pinRqGt1 = i8088Comp.getOutputs().get(26);
      // pinRqGt0 = i8088Comp.getOutputs().get(27);
      // pinRd = i8088Comp.getOutputs().get(28);
      // pinSs0 = i8088Comp.getOutputs().get(29);

      model.init();
      return model;
   }

   @Test
   public void testReset() throws NodeException {
      // Arrange:
      Model model = initModel();

      // Act:
      pinRESET.setValue(HIGH); // Active HIGH

      executeClockCycle(model, 4);

      pinRESET.setValue(LOW);

      executeClockCycle(model, 107);

      // Assert:
      assertEquals("S2", HIGH, pinS0.getValue());
      assertEquals("S1", HIGH, pinS1.getValue());
      assertEquals("S1", HIGH, pinS2.getValue());

      assertEquals("address", 0xFFFF0, addrBusValue());

      try {
         Thread.sleep(5000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   private void executeClockCycle(Model model, int count) {
      for (int i = 0; i < count; i++) {
         pinCLK.setValue(LOW);
         model.doStep();
         pinCLK.setValue(HIGH);
         model.doStep();
      }
   }

   private int addrBusValue() {
      int address = 0;
      address |= pinA19.getValue() << 19;
      address |= pinA18.getValue() << 18;
      address |= pinA17.getValue() << 17;
      address |= pinA16.getValue() << 16;
      address |= pinA15.getValue() << 15;
      address |= pinA14.getValue() << 14;
      address |= pinA13.getValue() << 13;
      address |= pinA12.getValue() << 12;
      address |= pinA11.getValue() << 11;
      address |= pinA10.getValue() << 10;
      address |= pinA9.getValue() << 9;
      address |= pinA8.getValue() << 8;
      address |= pinAD7.getValue() << 7;
      address |= pinAD6.getValue() << 6;
      address |= pinAD5.getValue() << 5;
      address |= pinAD4.getValue() << 4;
      address |= pinAD3.getValue() << 3;
      address |= pinAD2.getValue() << 2;
      address |= pinAD1.getValue() << 1;
      address |= pinAD0.getValue() << 0;

      return address;
   }
}
