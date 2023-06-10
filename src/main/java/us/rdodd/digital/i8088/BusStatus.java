package us.rdodd.digital.i8088;

public enum BusStatus {
   IntrAck((byte)0),
   IoRd((byte)1),
   IoWr((byte)2),
   Halt((byte)3),
   Code((byte)4),
   MemRd((byte)5),
   MemWr((byte)6),
   Pass((byte)7);

   private byte value;

   BusStatus(byte value) {
      this.value = value;
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
}
