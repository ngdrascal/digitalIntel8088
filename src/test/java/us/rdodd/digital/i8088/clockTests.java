package us.rdodd.digital.i8088;

import org.junit.Test;

import junit.framework.TestCase;

public class clockTests extends TestCase {

   @Test
   public void testClock01() {
      // Arrange:
      Pins pins = new Pins();
      NmiLatch nmiLatch = new NmiLatch();
      ClockIntf clock = new Clock(pins, nmiLatch);

      // Act:
      int actionCnt = clock.step(EdgeDirection.RISING);

      // Assert:
      assertEquals("actionCount", 0, actionCnt);
   }

   @Test
   public void testClock02() {
      // Arrange:
      Pins pins = new Pins();
      NmiLatch nmiLatch = new NmiLatch();
      ClockIntf clock = new Clock(pins, nmiLatch);
      clock.addAction(EdgeDirection.FALLING, (clk) -> {
         System.out.println("Falling: 0");
      });

      // Act:
      int actionCnt = clock.step(EdgeDirection.RISING);

      // Assert:
      assertEquals("actionCount", 1, actionCnt);
   }

   @Test
   public void testClock03() {
      // Arrange:
      Pins pins = new Pins();
      NmiLatch nmiLatch = new NmiLatch();
      ClockIntf clock = new Clock(pins, nmiLatch);
      clock.addAction(EdgeDirection.FALLING, (clk) -> {
         System.out.println("Falling: 0");
      }).addAction(EdgeDirection.RISING, (clk) -> {
         System.out.println("Rising: 1");
      }).addAction(EdgeDirection.FALLING, (clk) -> {
         System.out.println("Falling: 2");
      });

      // Act:
      int actionCnt = clock.step(EdgeDirection.FALLING);
      actionCnt = clock.step(EdgeDirection.RISING);
      actionCnt = clock.step(EdgeDirection.FALLING);
      actionCnt = clock.step(EdgeDirection.RISING);

      // Assert:
      assertEquals("actionCount", 0, actionCnt);
   }
}
