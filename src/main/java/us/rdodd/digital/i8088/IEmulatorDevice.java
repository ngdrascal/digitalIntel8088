package us.rdodd.digital.i8088;

public interface IEmulatorDevice
{
    byte Read(int pinNumber);
    void Write(int pinNumber, byte value);
}
