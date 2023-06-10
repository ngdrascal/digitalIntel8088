package us.rdodd.digital.i8088;

public class NmiLatch implements IBitLatch {
   private boolean _value;

   public void Set() {
      _value = true;
   }

   public void Clear() {
      _value = false;
   }

   public boolean IsSet() {
      return _value;
   }

   public boolean IsClear() {
      return !_value;
   }
}
