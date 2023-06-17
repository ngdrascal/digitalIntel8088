package us.rdodd.digital.i8088;

import de.neemann.digital.core.Node;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.ObservableValues;
import de.neemann.digital.core.element.*;
import de.neemann.digital.core.element.PinDescription.Direction;

public class Intel8088Component extends Node implements Element {

   /**
    * The description of the new component
    */
   public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(
         "Intel-8088",
         Intel8088Component.class,
         new PinInfo("NMI", "Non-maskable Interrupt", Direction.input).setPinNumber("17"),
         new PinInfo("NTR", "Maskable Interrupt", Direction.input).setPinNumber("18"),
         new PinInfo("CLK", "Clock", Direction.input).setPinNumber("19"),
         new PinInfo("RESET", "Reset", Direction.input).setPinNumber("21"),
         new PinInfo("READY", "Ready", Direction.input).setPinNumber("22"),
         new PinInfo("~TEST", "Test", Direction.input).setPinNumber("23"),
         new PinInfo("MN/~MX", "Min/Max mode", Direction.input).setPinNumber("33")) {
      @Override
      public String getDescription(ElementAttributes elementAttributes) {
         return "A cycle accurate Intel 8088 processor.";
      }
   }
         .addAttribute(Keys.ROTATE)
         .addAttribute(Keys.LABEL)
         .addAttribute(Keys.DIP_DEFAULT)
         .addAttribute(Keys.WIDTH)
         .addAttribute(Keys.HEIGHT);

   // Input pins
   private ObservableValue pinNmi;
   private ObservableValue pinIntr;
   private ObservableValue pinClk;
   private ObservableValue pinReset;
   private ObservableValue pinReady;
   private ObservableValue pinTest;

   // Output pins
   private final ObservableValue pinA14;
   private final ObservableValue pinA13;
   private final ObservableValue pinA12;
   private final ObservableValue pinA11;
   private final ObservableValue pinA10;
   private final ObservableValue pinA9;
   private final ObservableValue pinA8;
   private final ObservableValue pinAD7;
   private final ObservableValue pinAD6;
   private final ObservableValue pinAD5;
   private final ObservableValue pinAD4;
   private final ObservableValue pinAD3;
   private final ObservableValue pinAD2;
   private final ObservableValue pinAD1;
   private final ObservableValue pinAD0;

   private final ObservableValue pinQs1;
   private final ObservableValue pinQs0;
   private final ObservableValue pinS0;
   private final ObservableValue pinS1;
   private final ObservableValue pinS2;
   private final ObservableValue pinLock;
   private final ObservableValue pinRqGt1;
   private final ObservableValue pinRqGt0;
   private final ObservableValue pinRd;
   private final ObservableValue pinSs0;

   private final ObservableValue pinA19;
   private final ObservableValue pinA18;
   private final ObservableValue pinA17;
   private final ObservableValue pinA16;
   private final ObservableValue pinA15;

   private PinsExternalIntf devicePins;

   private long clkLastValue = 0;

   private boolean dataBusIsOutput = false;
   private static Intel8088Core intel8088;
   private static Thread i8088Thread;
   private static int threadCount = 0;

   /**
    * Creates a component.
    * The constructor is able to access the components attributes and has
    * to create the components output signals, which are instances of the
    * {@link ObservableValue} class.
    * As soon as the constructor is called you have to expect a call to the
    * getOutputs() method.
    *
    * @param attr the attributes which are editable in the components properties
    *             dialog
    */
   public Intel8088Component(ElementAttributes attr) {
      super(true);

      attr.set(Keys.WIDTH, 7);
      attr.set(Keys.HEIGHT, 20);

      pinA14 = new ObservableValue("A14", 1).setDescription("Address bit 14").setPinNumber("2");
      pinA13 = new ObservableValue("A13", 1).setDescription("Address bit 13").setPinNumber("3");
      pinA12 = new ObservableValue("A12", 1).setDescription("Address bit 12").setPinNumber("4");
      pinA11 = new ObservableValue("A11", 1).setDescription("Address bit 11").setPinNumber("5");
      pinA10 = new ObservableValue("A10", 1).setDescription("Address bit 10").setPinNumber("6");
      pinA9 = new ObservableValue("A9", 1).setDescription("Address bit 9").setPinNumber("7");
      pinA8 = new ObservableValue("A8", 1).setDescription("Address bit 8").setPinNumber("8");
      pinAD7 = new ObservableValue("AD7", 1).setDescription("Address/Data bit 7").setPinNumber("9").setBidirectional();
      pinAD6 = new ObservableValue("AD6", 1).setDescription("Address/Data bit 6").setPinNumber("10").setBidirectional();
      pinAD5 = new ObservableValue("AD5", 1).setDescription("Address/Data bit 5").setPinNumber("11").setBidirectional();
      pinAD4 = new ObservableValue("AD4", 1).setDescription("Address/Data bit 4").setPinNumber("12").setBidirectional();
      pinAD3 = new ObservableValue("AD3", 1).setDescription("Address/Data bit 3").setPinNumber("13").setBidirectional();
      pinAD2 = new ObservableValue("AD2", 1).setDescription("Address/Data bit 2").setPinNumber("14").setBidirectional();
      pinAD1 = new ObservableValue("AD1", 1).setDescription("Address/Data bit 1").setPinNumber("15").setBidirectional();
      pinAD0 = new ObservableValue("AD0", 1).setDescription("Address/Data bit 0").setPinNumber("16").setBidirectional();

      pinQs1 = new ObservableValue("QS1", 1).setDescription("Queue Status 1").setPinNumber("24");
      pinQs0 = new ObservableValue("QS0", 1).setDescription("Queue Status 0").setPinNumber("25");
      pinS0 = new ObservableValue("S0", 1).setDescription("Pin S0").setPinNumber("26");
      pinS1 = new ObservableValue("S1", 1).setDescription("Pin S1").setPinNumber("27");
      pinS2 = new ObservableValue("S2", 1).setDescription("Pin S2").setPinNumber("28");
      pinLock = new ObservableValue("~Lock", 1).setDescription("Lock").setPinNumber("29");
      pinRqGt1 = new ObservableValue("~RQ/~GT1", 1).setDescription("Rqst/Grnt 1").setPinNumber("30").setBidirectional();
      pinRqGt0 = new ObservableValue("~RQ/~GT0", 1).setDescription("Rqst/Grnt 0").setPinNumber("31").setBidirectional();
      pinRd = new ObservableValue("~RD", 1).setDescription("Read").setPinNumber("32");
      pinSs0 = new ObservableValue("~SS0", 1).setDescription("Status 0").setPinNumber("34");

      pinA19 = new ObservableValue("A19", 1).setDescription("Address bit 19").setPinNumber("35");
      pinA18 = new ObservableValue("A18", 1).setDescription("Address bit 18").setPinNumber("36");
      pinA17 = new ObservableValue("A17", 1).setDescription("Address bit 17").setPinNumber("37");
      pinA16 = new ObservableValue("A16", 1).setDescription("Address bit 16").setPinNumber("38");
      pinA15 = new ObservableValue("A15", 1).setDescription("Address bit 15").setPinNumber("39");
      Pins pins = new Pins();
      devicePins = pins;

      BitLatchIntf nmiLatch = new NmiLatch();
      PinsInternalIntf internalPins = pins;
      ClockIntf clock = new Clock(internalPins, nmiLatch);

      Registers registers = new Registers();
      BusInterfaceUnitIntf biu = new BusInterfaceUnit(clock, registers, internalPins);

      if (intel8088 != null)
         intel8088 = null;
      intel8088 = new Intel8088Core(clock, registers, biu, nmiLatch, internalPins);

      if (i8088Thread != null) {
         i8088Thread.interrupt();
         i8088Thread = null;
      }
      i8088Thread = new Thread(intel8088, "i8088-" + threadCount);
      threadCount++;

      i8088Thread.start();
   }

   /**
    * This method is called if one of the input values has changed.
    * Here you can read the input values of your component.
    * It is not allowed to write to one of the outputs!!!
    */
   @Override
   public void readInputs() {
      devicePins.setNMI((byte) pinNmi.getValue());
      devicePins.setINTR((byte) pinIntr.getValue());
      devicePins.setRESET((byte) pinReset.getValue());
      devicePins.setREADY((byte) pinReady.getValue());
      devicePins.setTEST((byte) pinTest.getValue());

      if (true) {
         devicePins.setAD7((byte) pinAD7.getValue());
         devicePins.setAD6((byte) pinAD6.getValue());
         devicePins.setAD5((byte) pinAD5.getValue());
         devicePins.setAD4((byte) pinAD4.getValue());
         devicePins.setAD3((byte) pinAD3.getValue());
         devicePins.setAD2((byte) pinAD2.getValue());
         devicePins.setAD1((byte) pinAD1.getValue());
         devicePins.setAD0((byte) pinAD0.getValue());
      }

      // set the clock last because it advances the simulation
      byte clkCurValue = (byte) pinClk.getValue();
      if (clkCurValue != clkLastValue) {
         devicePins.setCLK(clkCurValue);
         clkLastValue = clkCurValue;

         // intel8088.step();
      }
   }

   /**
    * This method is called if you have to update your output.
    * It is not allowed to read one of the inputs!!!
    */
   @Override
   public void writeOutputs() {
      pinA19.setValue(devicePins.getA19());
      pinA18.setValue(devicePins.getA18());
      pinA17.setValue(devicePins.getA17());
      pinA16.setValue(devicePins.getA16());
      pinA15.setValue(devicePins.getA15());
      pinA14.setValue(devicePins.getA15());
      pinA13.setValue(devicePins.getA13());
      pinA12.setValue(devicePins.getA12());
      pinA11.setValue(devicePins.getA11());
      pinA10.setValue(devicePins.getA10());
      pinA9.setValue(devicePins.getA9());
      pinA8.setValue(devicePins.getA8());

      if (dataBusIsOutput) {
         pinAD7.setValue(devicePins.getAD7());
         pinAD6.setValue(devicePins.getAD6());
         pinAD5.setValue(devicePins.getAD5());
         pinAD4.setValue(devicePins.getAD4());
         pinAD3.setValue(devicePins.getAD3());
         pinAD2.setValue(devicePins.getAD2());
         pinAD1.setValue(devicePins.getAD1());
         pinAD0.setValue(devicePins.getAD0());
      } else {
         pinAD7.setToHighZ();
         pinAD6.setToHighZ();
         pinAD5.setToHighZ();
         pinAD4.setToHighZ();
         pinAD3.setToHighZ();
         pinAD2.setToHighZ();
         pinAD1.setToHighZ();
         pinAD0.setToHighZ();
      }
      pinQs1.setValue(devicePins.getQS1());
      pinQs0.setValue(devicePins.getQS0());

      pinS2.setValue(devicePins.getS2());
      pinS1.setValue(devicePins.getS1());
      pinS0.setValue(devicePins.getS0());

      pinLock.setValue(devicePins.getLOCK());
      pinRqGt1.setValue(devicePins.getRQGT1());
      pinRqGt0.setValue(devicePins.getRQGT0());
      pinRd.setValue(devicePins.getRD());
      pinSs0.setValue(devicePins.getSS0());
   }

   /**
    * This method is called to register the input signals which are
    * connected to your components inputs. The order is the same as given in
    * the {@link ElementTypeDescription}.
    * You can store the instances, make some checks and so on.
    * IMPORTANT: If it's necessary that your component is called if the input
    * changes, you have to call the addObserverToValue method on that input.
    * If a combinatorial component is implemented you have to add the observer
    * to all inputs. If your component only reacts on a clock signal you only
    * need to add the observer to the clock signal.
    *
    * @param inputs the list of <code>ObservableValue</code>s to use
    * @throws NodeException NodeException
    */
   @Override
   public void setInputs(ObservableValues inputs) throws NodeException {
      pinNmi = inputs.get(0).addObserverToValue(this).checkBits(1, this);
      pinIntr = inputs.get(1).addObserverToValue(this).checkBits(1, this);
      pinClk = inputs.get(2).addObserverToValue(this).checkBits(1, this);
      pinReset = inputs.get(3).addObserverToValue(this).checkBits(1, this);
      pinReady = inputs.get(4).addObserverToValue(this).checkBits(1, this);
      pinTest = inputs.get(5).addObserverToValue(this).checkBits(1, this);
   }

   /**
    * This method must return the output signals of your component.
    *
    * @return the output signals
    */
   @Override
   public ObservableValues getOutputs() {
      return new ObservableValues(
            pinA19, pinA18, pinA17, pinA16, pinA15, pinA14, pinA13, pinA12, pinA11, pinA10,
            pinA9, pinA8, pinAD7, pinAD6, pinAD5, pinAD4, pinAD3, pinAD2, pinAD1, pinAD0,
            pinQs1, pinQs0, pinS2, pinS1, pinS0, pinLock, pinRqGt1, pinRqGt0, pinRd, pinSs0);
   }
}
