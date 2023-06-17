package us.rdodd.digital.i8088;

public enum BusStatus {
   IntrAck((byte) 0),
   IoRd((byte) 1),
   IoWr((byte) 2),
   Halt((byte) 3),
   Code((byte) 4),
   MemRd((byte) 5),
   MemWr((byte) 6),
   Pass((byte) 7);

   private byte value;

   private BusStatus(byte value) {
      this.value = value;
   }

   public static BusStatus valueOf(byte s0, byte s1, byte s2) {
      byte value = 0;
      value = s0 == 0 ? (byte) 0 : (byte) 1;
      value = s1 == 0 ? (byte) (value & 0b101) : (byte) (value | 0b010);
      value = s2 == 0 ? (byte) (value & 0b011) : (byte) (value | 0b100);
      return BusStatus.values()[value];
   }

   public boolean IsMemoryOperation() {
      return this.value == BusStatus.Code.value ||
            this.value == BusStatus.MemRd.value ||
            this.value == BusStatus.MemWr.value;
   }

   public boolean IsReadOperation() {
      return this.value == BusStatus.Code.value ||
            this.value == BusStatus.MemRd.value ||
            this.value == BusStatus.IoRd.value ||
            this.value == BusStatus.IntrAck.value;
   }

   public static boolean isReadOperation(byte s0, byte s1, byte s2) {
      return valueOf(s0, s1, s2).IsReadOperation();
   }

   public boolean isWriteOperation() {
      return this.value == BusStatus.MemWr.value ||
            this.value == BusStatus.IoWr.value;
   }

   public static boolean isWriteOperation(byte s0, byte s1, byte s2) {
      return valueOf(s0, s1, s2).isWriteOperation();
   }
}
