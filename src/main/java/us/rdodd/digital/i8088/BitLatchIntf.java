package us.rdodd.digital.i8088;

public interface BitLatchIntf {
   void Set();

   void Clear();

   boolean IsSet();

   boolean IsClear();
}