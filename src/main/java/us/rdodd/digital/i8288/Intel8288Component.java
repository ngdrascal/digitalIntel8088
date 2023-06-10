package us.rdodd.digital.i8288;

import de.neemann.digital.core.Node;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.ObservableValues;
import de.neemann.digital.core.element.*;
import de.neemann.digital.core.element.PinInfo;
import de.neemann.digital.core.element.PinDescription.Direction;

public class Intel8288Component extends Node implements Element {
   /**
    * The description of the new component
    */
   public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(Intel8288Component.class,
         new PinInfo("IOB", "Input/Output Bus Mode", Direction.input).setPinNumber("1"),
         new PinInfo("~AEN", "Address Enable", Direction.input).setPinNumber("6"),
         new PinInfo("CEN", "Command Enable", Direction.input).setPinNumber("16"),
         new PinInfo("CLK", "Clock", Direction.input).setPinNumber("2"),
         new PinInfo("~S2", "Status Input 2", Direction.input).setPinNumber("18"),
         new PinInfo("~S1", "Status Input 1", Direction.input).setPinNumber("3"),
         new PinInfo("~S0", "Status Input 0", Direction.input).setPinNumber("19")) {
      @Override
      public String getDescription(ElementAttributes elementAttributes) {
         return "Intel 8288 bus controller.";
      }
   }.addAttribute(Keys.ROTATE) // allows
         .addAttribute(Keys.LABEL).addAttribute(Keys.DIP_DEFAULT).addAttribute(Keys.WIDTH).addAttribute(Keys.HEIGHT);

   // Input pins
   private ObservableValue pinIOB;
   private ObservableValue pinCLK;
   private ObservableValue pinS1;
   private ObservableValue pinAEN;
   private ObservableValue pinS0;
   private ObservableValue pinS2;
   private ObservableValue pinCEN;

   // Output pins
   private final ObservableValue pinDTR;
   private final ObservableValue pinALE;
   private final ObservableValue pinMRDC;
   private final ObservableValue pinAMWC;
   private final ObservableValue pinMWTC;
   private final ObservableValue pinMCEPDEN;
   private final ObservableValue pinDEN;
   private final ObservableValue pinINTA;
   private final ObservableValue pinIORC;
   private final ObservableValue pinAIOWC;
   private final ObservableValue pinIOWC;

   private final ObservableValue pinBusT;

   private Intel8288Logic i8288;
   private byte clkPrevious;

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
   public Intel8288Component(ElementAttributes attr) {
      super(true);

      attr.set(Keys.WIDTH, 7);
      attr.set(Keys.HEIGHT, 10);

      pinDTR = new ObservableValue("DT/~R", 1).setDescription("Data Transmit/Receive").setPinNumber("4");
      pinALE = new ObservableValue("ALE", 1).setDescription("Address Latch Enable").setPinNumber("5");
      pinMRDC = new ObservableValue("~MRDC", 1).setDescription("Memory Read Command").setPinNumber("7");
      pinAMWC = new ObservableValue("~AMWC", 1).setDescription("Advanced Memory Write Command").setPinNumber("8");
      pinMWTC = new ObservableValue("~MWTC", 1).setDescription("Memory Write command").setPinNumber("9");
      pinIOWC = new ObservableValue("~IOWC", 1).setDescription("I/O Read Command").setPinNumber("11");
      pinAIOWC = new ObservableValue("~AIOWC", 1).setDescription("Advanced I/O Write Command").setPinNumber("12");
      pinIORC = new ObservableValue("~IORC", 1).setDescription("I/O Read Command").setPinNumber("13");
      pinINTA = new ObservableValue("~INTA", 1).setDescription("Interrupt Acknowledge").setPinNumber("14");
      pinDEN = new ObservableValue("DEN", 1).setDescription("Data Enable").setPinNumber("16");
      pinMCEPDEN = new ObservableValue("MCE/~PDEN", 1).setDescription("Master Cascade Enable/Peripheral Data Enable")
            .setPinNumber("17");

      pinBusT = new ObservableValue("BusT", 4).setDescription("Bus Cycle").setPinNumber("20");

      i8288 = new Intel8288Logic();
   }

   /**
    * This method is called if one of the input values has changed.
    * Here you can read the input values of your component.
    * It is not allowed to write to one of the outputs!!!
    */
   @Override
   public void readInputs() {
      i8288.setIOB((byte) pinIOB.getValue());
      i8288.setS1((byte) pinS1.getValue());
      i8288.setAEN((byte) pinAEN.getValue());
      i8288.setS0((byte) pinS0.getValue());
      i8288.setS2((byte) pinS2.getValue());
      i8288.setCEN((byte) pinCEN.getValue());

      byte clkCurrent = (byte) pinCLK.getValue();
      i8288.setCLK(clkCurrent);
      if (clkCurrent != clkPrevious) {
         i8288.step();
         clkPrevious = clkCurrent;
      }
   }

   /**
    * This method is called if you have to update your output.
    * It is not allowed to read one of the inputs!!!
    */
   @Override
   public void writeOutputs() {
      pinDTR.setValue(i8288.getDTR());
      pinALE.setValue(i8288.getALE());
      pinMRDC.setValue(i8288.getMRDC());
      pinAMWC.setValue(i8288.getAMWC());
      pinMWTC.setValue(i8288.getMWTC());
      pinMCEPDEN.setValue(i8288.getMCEPDEN());
      pinDEN.setValue(i8288.getDEN());
      pinINTA.setValue(i8288.getINTA());
      pinIORC.setValue(i8288.getIORC());
      pinAIOWC.setValue(i8288.getAIOWC());
      pinIOWC.setValue(i8288.getIOWC());

      pinBusT.setValue(i8288.getBusT());
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
      pinIOB = inputs.get(0).addObserverToValue(this).checkBits(1, this);
      pinAEN = inputs.get(1).addObserverToValue(this).checkBits(1, this);
      pinCEN = inputs.get(2).addObserverToValue(this).checkBits(1, this);
      pinCLK = inputs.get(3).addObserverToValue(this).checkBits(1, this);
      pinS2 = inputs.get(4).addObserverToValue(this).checkBits(1, this);
      pinS1 = inputs.get(5).addObserverToValue(this).checkBits(1, this);
      pinS0 = inputs.get(6).addObserverToValue(this).checkBits(1, this);
   }

   /**
    * This method must return the output signals of your component.
    *
    * @return the output signals
    */
   @Override
   public ObservableValues getOutputs() {
      return new ObservableValues(
            pinDTR,
            pinALE,
            pinDEN,
            pinMCEPDEN,
            pinINTA,
            pinMRDC,
            pinAMWC,
            pinMWTC,
            pinIORC,
            pinAIOWC,
            pinIOWC,

            pinBusT);
   }
}
