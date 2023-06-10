package us.rdodd.digital.i8088;

public class Registers {
   public int Flags;
   public int AX;
   public int BX;
   public int CX;
   public int DX;
   public int SI;
   public int DI;
   public int IP;
   public int SP;
   public int BP;
   public int CS;
   public int DS;
   public int ES;
   public int SS;

   public Registers() {
      Flags = 0xF000;
   }
}
