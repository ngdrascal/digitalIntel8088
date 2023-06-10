package us.rdodd.digital.i8088;

public interface IBitLatch {
   void Set();

   void Clear();

   boolean IsSet();

   boolean IsClear();
}