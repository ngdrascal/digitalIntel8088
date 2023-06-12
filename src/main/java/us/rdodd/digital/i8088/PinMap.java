package us.rdodd.digital.i8088;

/*
       +------------------------+
    -- |  GND	  VCC           | --
     2 |  A14	  A15           | 39
     3 |  A13	  A16/S3        | 38
     4 |  A12	  A17/S4        | 37
     5 |  A11	  A18/S5        | 36
     6 |  A10	  A19/S6        | 35
     7 |   A9	  SS0           | 34
     8 |   A8	  MN/MX         | --
     9 |  AD7	  RD            | 32
    10 |  AD6	  HOLD  (GT0)   | 31
    11 |  AD5	  HOLDA (GT1)   | 30
    12 |  AD4	  WR    (LOCK)  | 29
    13 |  AD3	  IOM   (S2)    | 28
    14 |  AD2	  DTR   (S1)    | 27
    15 |  AD1	  DEN   (S0)    | 26
    16 |  AD0	  ALE   (QS0)   | 25
    17 |  NMI	  INTA  (QS1)   | 24
    18 | INTR	  TEST          | 23
    19 |  CLK	  READY         | 22
    -- |  GND	  RESET         | 21
       +------------------------+
*/

public interface PinMap {
   final byte GND0 = 1;
   final byte A14 = 2;
   final byte A13 = 3;
   final byte A12 = 4;
   final byte A11 = 5;
   final byte A10 = 6;
   final byte A9 = 7;
   final byte A8 = 8;
   final byte AD7 = 9;
   final byte AD6 = 10;
   final byte AD5 = 11;
   final byte AD4 = 12;
   final byte AD3 = 13;
   final byte AD2 = 14;
   final byte AD1 = 15;
   final byte AD0 = 16;
   final byte NMI = 17;
   final byte INTR = 18;
   final byte CLK = 19;
   final byte GND1 = 20;

   final byte RESET = 21;
   final byte READY = 22;
   final byte TEST = 23;
   final byte QS1 = 24;
   final byte QS0 = 25;
   final byte S0 = 26;
   final byte S1 = 27;
   final byte S2 = 28;
   final byte LOCK = 29;
   final byte RQGT1 = 30;
   final byte RQGT0 = 31;
   final byte RD = 32;
   final byte MNMX = 33;
   final byte SS0 = 34;
   final byte A19 = 35;
   final byte A18 = 36;
   final byte A17 = 37;
   final byte A16 = 38;
   final byte A15 = 39;
   final byte PWR = 40;

   public final byte[] DataPins = { AD0, AD1, AD2, AD3, AD4, AD5, AD6, AD7 };
   public final byte[] AddrPins = { AD0, AD1, AD2, AD3, AD4, AD5, AD6, AD7, A8, A9,
                                    A10, A11, A12, A13, A14, A15, A16, A17, A18, A19 };
};
