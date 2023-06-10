package us.rdodd.digital.i8088;

public enum SegmentRegs {
   ES(0x10),
   CS(0x20),
   SS(0x40),
   DS(0x80),
   None(0x99);

   public final int value;

   SegmentRegs(int value) {
      this.value = value;
   }

   static SegmentRegs FromInt(int value)
   {
      switch(value){
         case 0x10: return SegmentRegs.ES;
         case 0x20: return SegmentRegs.CS;
         case 0x40: return SegmentRegs.SS;
         case 0x80: return SegmentRegs.DS;
         default:
            return SegmentRegs.None;
      }
   }
}
