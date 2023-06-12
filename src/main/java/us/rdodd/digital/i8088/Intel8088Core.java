package us.rdodd.digital.i8088;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Intel8088Core {
   private final byte LOW = 0;
   private final byte HIGH = 1;

   // --------------------------------------------------------------------------------------------------
   // --------------------------------------------------------------------------------------------------

   private boolean SegmentOverridableTrue = true;
   private boolean SegmentOverridableFalse = false;

   private byte REG_AL = 0x0;

   private String[] _opNames = {
         // 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D OE 0F
         /* 00 */ "ADD", "ADD", "ADD", "ADD", "ADD", "ADD", "PUSH ES", "POP ES", "OR", "OR", "OR", "OR", "OR", "OR",
         "PUSH CS", "POP CS", // 00
         /* 10 */ "ADC", "ADC", "ADC", "ADC", "ADC", "ADC", "PUSH SS", "POP SS", "SBB", "SBB", "SBB", "SBB", "SBB",
         "SBB", "PUSH DS", "POP DS", // 01
         /* 20 */ "AND", "AND", "AND", "AND", "AND", "AND", "ES:", "DAA", "SUB", "SUB", "SUB", "SUB", "SUB", "SUB",
         "CS:", "DAS", // 02
         /* 30 */ "XOR", "XOR", "XOR", "XOR", "XOR", "XOR", "SS:", "AAA", "CMP", "CMP", "CMP", "CMP", "CMP", "CMP",
         "DS:", "AAS", // 03
         /* 40 */ "INC AX", "INC CX", "INC DX", "INC BX", "INC SP", "INC BP", "INC SI", "INC DI", "DEC AX", "DEC CX",
         "DEC DX", "DEC BX", "DEC SP", "DEC BP", "DEC SI", "DEC DI", // 04
         /* 50 */ "PUSH AX", "PUSH CX", "PUSH DX", "PUSH BX", "PUSH SP", "PUSH BP", "PUSH SI", "PUSH DI", "POP AX",
         "POP CX", "POP DX", "POP BX", "POP SP", "POP BP", "POP SI", "POP DI", // 05
         /* 60 */ "PUSHA", "POPA", "", "", "", "", "", "", "PUSH", "IMUL", "PUSH", "IMUL", "INS", "INS", "OUTS", "OUTS", // 06
         /* 70 */ "J0", "JN0", "JC", "JNC", "JE", "JNE", "JBE", "JA", "JS", "JNS", "JPE", "JPO", "JL", "JGE", "JLE",
         "JG", // 07
         /* 80 */ "A/S8", "A/S16", "", "", "TEST", "TEST", "XCHG", "XCHG", "MOV", "MOV", "MOV", "MOV", "MOV", "LEA",
         "MOV", "POP", // 08
         /* 90 */ "NOP", "XCHG AX,CX", "XCHG AX,DX", "XCHG AX,BX", "XCHG AX,SP", "XCHG AX,BP", "XCHG AX,SI",
         "XCHG AX,DI", "CBW", "CWD", "CALLF", "WAIT", "PUSHF", "POPF", "SAHF", "LAHF", // 09
         /* A0 */ "MOV", "MOV", "MOV", "MOV", "MOVS", "MOVS", "CMPS", "CMPS", "TEST", "TEST", "STOS", "STOS", "LODS",
         "LODS", "SCAS", "SCAS", // 0A
         /* B0 */ "MOV AL", "MOV CL", "MOV DL", "MOV BL", "MOV AH", "MOV CH", "MOV DH", "MOV BH", "MOV AX", "MOV CX",
         "MOV DX", "MOV BX", "MOV SP", "MOV BP", "MOV SI", "MOV DI", // 0B
         /* C0 */ "", "", "RETN", "RETN", "LDS", "LES", "MOV", "MOV", "ENTER", "LEAVE", "RETF", "RETF", "INT3", "INT",
         "INT0", "IRET", // 0C
         /* D0 */ "R/S8", "R/S16", "R/S8", "R/S16", "", "", "", "XLAT", "", "", "", "", "", "", "", "", // 0D
         /* E0 */ "LPNZ", "LPZ", "LOOP", "JECXZ", "IN", "IN", "OUT", "OUT", "CALL", "JMP", "JMPF", "JMP", "IN", "IN",
         "OUT", "OUT", // 0E
         /* F0 */ "LOCK", "", "REPNZ", "REP", "HLT", "", "", "", "CLC", "STC", "CLI", "STI", "CLD", "STD", "", "" // 0F
   };

   // --------------------------------------------------------------------------------------------------
   // --------------------------------------------------------------------------------------------------

   // private int clockCounter;
   private byte prefetch_queue_count;
   private byte pfq_byte_A;
   private byte pfq_byte_B;
   private byte pfq_byte_C;
   private byte pfq_byte_D;

   private boolean _lastInstrSetPrefix;
   private boolean _pauseInterrupts;
   private byte incDec; // bool
   private byte with_carry; // bool
   private byte _eaIsRegister; // bool
   private byte _wordOperation; // bool
   private byte _opCodeFirstByte;
   // private byte _opCodeSecondByte;
   private byte _regFieldTable;
   private byte _rmFieldTable;
   private byte _regField;
   private byte _modField;
   private byte _rmField;
   private SegmentRegs _eaSegment;

   private int _pfqInAddress;

   private int temp16;
   private int _eaAddress;

   // Pre-calculated 8-bit parity array
   private byte[] Parity_Array = {
         4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4,
         0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0,
         0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0,
         4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4,
         0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4,
         0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4,
         4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4
   };

   private ClockIntf _clock;
   private Registers _registers;
   private BusInterfaceUnitIntf _biu;
   private BitLatchIntf _nmiLatched;
   private PinsInternalIntf pins;
   private Logger _instLogger;

   public Intel8088Core(ClockIntf clock,
         Registers registers,
         BusInterfaceUnitIntf biu,
         BitLatchIntf nmiLatched,
         PinsInternalIntf pins) {
      _clock = clock;
      _registers = registers;
      _biu = biu;
      _nmiLatched = nmiLatched;
      this.pins = pins;

      // _logger = LoggerFactory.getLogger("cpu");
      _instLogger = LoggerFactory.getLogger("cpu.instruction");

      pins.setBusStatusPins(BusStatus.Pass);
   }

   // #define flag_o ( (_registers.Flags & 0x0800)==0 ? 0 : 1 )
   private byte Flag_o() {
      return (byte) ((_registers.Flags & 0x0800) == 0 ? 0 : 1);
   }

   // #define flag_d ( (_registers.Flags & 0x0400)==0 ? 0 : 1 )
   private byte Flag_d() {
      return (byte) ((_registers.Flags & 0x0400) == 0 ? 0 : 1);
   }

   // #define flag_i ( (_registers.Flags & 0x0200)==0 ? 0 : 1 )
   private byte Flag_i() {
      return (byte) ((_registers.Flags & 0x0200) == 0 ? 0 : 1);
   }

   // #define flag_t ( (_registers.Flags & 0x0100)==0 ? 0 : 1 )
   private byte Flag_t() {
      return (byte) ((_registers.Flags & 0x0100) == 0 ? 0 : 1);
   }

   // #define flag_s ( (_registers.Flags & 0x0080)==0 ? 0 : 1 )
   private byte Flag_s() {
      return (byte) ((_registers.Flags & 0x0080) == 0 ? 0 : 1);
   }

   // #define Flag_z ( (_registers.Flags & 0x0040)==0 ? 0 : 1 )
   private byte Flag_z() {
      return (byte) ((_registers.Flags & 0x0040) == 0 ? 0 : 1);
   }

   // #define Flag_a ( (_registers.Flags & 0x0010)==0 ? 0 : 1 )
   private byte Flag_a() {
      return (byte) ((_registers.Flags & 0x0010) == 0 ? 0 : 1);
   }

   // #define Flag_p ( (_registers.Flags & 0x0004)==0 ? 0 : 1 )
   private byte Flag_p() {
      return (byte) ((_registers.Flags & 0x0004) == 0 ? 0 : 1);
   }

   // #define flag_c ( (_registers.Flags & 0x0001)==0 ? 0 : 1 )
   private byte Flag_c() {
      return (byte) ((_registers.Flags & 0x0001) == 0 ? 0 : 1);
   }

   // #define prefix_lock ( (prefixFlags & 0x01)==0 ? 0 : 1 )
   // private byte Prefix_lock()
   // {
   // return (byte)((prefixFlags & 0x01) == 0 ? 0 : 1);
   // }

   // // #define prefix_seg_override_es ( (prefixFlags & 0x10)==0 ? 0 : 1 )
   // private byte prefix_seg_override_es()
   // {
   // return (byte)((prefixFlags & 0x10) == 0 ? 0 : 1);
   // }
   //
   // // #define prefix_seg_override_cs ( (prefixFlags & 0x20)==0 ? 0 : 1 )
   // private byte prefix_seg_override_cs()
   // {
   // return (byte)((prefixFlags & 0x20) == 0 ? 0 : 1);
   // }
   //
   // // #define prefix_seg_override_ss ( (prefixFlags & 0x40)==0 ? 0 : 1 )
   // private byte prefix_seg_override_ss()
   // {
   // return (byte)((prefixFlags & 0x40) == 0 ? 0 : 1);
   // }
   //
   // // #define prefix_seg_override_ds ( (prefixFlags & 0x80)==0 ? 0 : 1 )
   // private byte prefix_seg_override_ds()
   // {
   // return (byte)((prefixFlags & 0x80) == 0 ? 0 : 1);
   // }

   /*
    * //
    * -----------------------------------------------------------------------------
    * ---------------------
    * //
    * -----------------------------------------------------------------------------
    * ---------------------
    * //
    * // Begin 8088 Bus Interface Unit
    * //
    * //
    * -----------------------------------------------------------------------------
    * ---------------------
    * //
    * -----------------------------------------------------------------------------
    * ---------------------
    * 
    * // -------------------------------------------------
    * // Wait for the CLK rising edge
    * // -------------------------------------------------
    * private void Wait_for_CLK_rising_edge()
    * {
    * // First ensure clock is at a low level
    * while (_deviceAdapter.ClkPin != 0 && !_token.IsCancellationRequested) { }
    * 
    * // Then poll for the first instance where clock is not low
    * do { } while (_deviceAdapter.ClkPin == 0 && !_token.IsCancellationRequested);
    * }
    * 
    * // -------------------------------------------------
    * // Wait for the CLK falling edge and sample signals
    * // -------------------------------------------------
    * private void _clock.WaitForFallingEdge()
    * {
    * if (clockCounter > 0)
    * {
    * // Count down clockCounter here to keep new opcodes from beginning yet
    * allowing
    * // prefetch queue to fill
    * clockCounter--;
    * }
    * 
    * if (nmi_d == 0 && direct_nmi != 0)
    * {
    * // Latch rising edge of NMI
    * nmiLatched = true;
    * }
    * nmi_d = (int)direct_nmi;
    * 
    * // First ensure clock is at a high level
    * while (_deviceAdapter.ClkPin == 0 && !_token.IsCancellationRequested) { }
    * 
    * // Then poll for the first instance where clock is not high
    * do { } while (_deviceAdapter.ClkPin != 0 && !_token.IsCancellationRequested);
    * 
    * direct_nmi = _deviceAdapter.NmiPin;
    * }
    * 
    * // -------------------------------------------------
    * // Initiate a 8088 Bus Cycle
    * // -------------------------------------------------
    * private byte BIU_Bus_Cycle(BiuOperations biu_operation, uint localAddress,
    * byte localData)
    * {
    * PrintBusRequest(localAddress, localData, biu_operation, _eaSegment);
    * 
    * var readCycle = biu_operation.IsReadCycle();
    * 
    * // For HALT, just drive the status pins on th e next rising edge of CLK, then
    * exit
    * if (biu_operation == BiuOperations.SendHalt)
    * {
    * Wait_for_CLK_rising_edge();
    * 
    * _deviceAdapter.BusStatusPins = BusStatus.Halt;
    * 
    * return 0xEE;
    * }
    * 
    * //////////////////////////////////////////
    * // Execute the 8088 Maximum Mode Bus Cycle
    * //////////////////////////////////////////
    * 
    * // T0.5 Drive the status pins to begin the cycle on the rising edge of CLK
    * //
    * -----------------------------------------------------------------------------
    * Wait_for_CLK_rising_edge();
    * _clkLogger.LogTrace("----T0.5----");
    * 
    * _deviceAdapter.BusStatusPins = biu_operation.BusStatusOf();
    * 
    * // T1
    * //
    * -----------------------------------------------------------------------------
    * _clock.WaitForFallingEdge();
    * _clkLogger.LogTrace("----T1----");
    * 
    * _deviceAdapter.AddrBusPins = localAddress;
    * 
    * 
    * // T2 - If a read cycle, disable the AD[7:0] buffer
    * // If a write cycle, drive data onto the AD[7:0] pins
    * //
    * -----------------------------------------------------------------------------
    * 
    * _clock.WaitForFallingEdge();
    * _clkLogger.LogTrace("----T2----");
    * 
    * // More likely to get POST error 101 with this code
    * if (assert_lock == 1)
    * {
    * _deviceAdapter.LockPin = 0x0;
    * assert_lock = 0;
    * }
    * else
    * {
    * _deviceAdapter.LockPin = 0x1;
    * }
    * 
    * if (readCycle)
    * _deviceAdapter.RdPin = 0;
    * else
    * _deviceAdapter.DataBusPins = localData;
    * 
    * _deviceAdapter.SegmentRegister = _eaSegment;
    * 
    * 
    * // T3 - Sample the READY signal on the falling edge of CLK until it goes high
    * // Then set the Status bits to Passive (b111)
    * //
    * -----------------------------------------------------------------------------
    * 
    * // Poll the slightly delayed sample of READY signal directly from the GPIO
    * do
    * {
    * _clock.WaitForFallingEdge();
    * _clkLogger.LogTrace("----T3----");
    * }
    * while (_deviceAdapter.ReadyPin == 0 && !_token.IsCancellationRequested);
    * 
    * _deviceAdapter.BusStatusPins = BusStatus.Pass;
    * 
    * 
    * // T4 - Sample read data on the next falling edge of CLK
    * //
    * -----------------------------------------------------------------------------
    * _clock.WaitForFallingEdge();
    * _clkLogger.LogTrace("----T4----");
    * 
    * byte result;
    * 
    * if (!readCycle)
    * {
    * result = 0xEE;
    * }
    * else
    * {
    * var data = _deviceAdapter.DataBusPins;
    * result = data;
    * }
    * 
    * _deviceAdapter.RdPin = 1;
    * 
    * // _busLogger.LogTrace("<--- {0}(): {1:X2}", nameof(BIU_Bus_Cycle), result);
    * return result;
    * }
    * 
    * private void PrintBusRequest(uint addr, byte data, BiuOperations operation,
    * byte segReg)
    * {
    * var status = operation switch
    * {
    * BiuOperations.InterruptAck => 'A',
    * BiuOperations.IoReadByte => 'I',
    * BiuOperations.IoReadWord => 'I',
    * BiuOperations.IoWriteByte => '0',
    * BiuOperations.IoWriteWord => '0',
    * BiuOperations.SendHalt => 'H',
    * BiuOperations.CodeReadByte => 'C',
    * BiuOperations.MemReadByte => 'R',
    * BiuOperations.MemReadWord => 'R',
    * BiuOperations.MemWriteByte => 'W',
    * BiuOperations.MemWriteWord => 'W',
    * _ => throw new ArgumentOutOfRangeException(nameof(operation), operation,
    * null)
    * };
    * 
    * const char area = '-';
    * string segRegStr;
    * if (operation is BiuOperations.IoReadByte or BiuOperations.IoWriteByte or
    * BiuOperations.IoReadWord or BiuOperations.IoWriteWord)
    * {
    * segRegStr = "--";
    * } else
    * {
    * segRegStr = segReg switch
    * {
    * SEGMENT_CS => "CS",
    * SEGMENT_DS => "DS",
    * SEGMENT_ES => "ES",
    * SEGMENT_SS => "SS",
    * _ => "--"
    * };
    * }
    * var dataStr = operation is BiuOperations.MemWriteByte or
    * BiuOperations.MemWriteWord or
    * BiuOperations.IoWriteByte or BiuOperations.IoWriteWord
    * ? data.ToString("X2") : "--";
    * 
    * _busLogger.LogTrace("{0} {1} {2} {3:X5}:{4}", status, area, segRegStr, addr,
    * dataStr);
    * }
    * 
    * //
    * -----------------------------------------------------------------------------
    * ---------------------
    * //
    * -----------------------------------------------------------------------------
    * ---------------------
    * //
    * // End 8088 Bus Interface Unit
    * //
    * //
    * -----------------------------------------------------------------------------
    * ---------------------
    * //
    * -----------------------------------------------------------------------------
    * ---------------------
    * 
    * 
    * // ------------------------------------------------------
    * // Calculate full address allowing for segment override
    * // ------------------------------------------------------
    * private uint CalculateFullAddress(boolean useSegmentOverride, byte segment,
    * int address)
    * {
    * uint biuFullAddress = 0;
    * 
    * var localSegmentOverride = (byte)(prefixFlags & 0xF0);
    * 
    * if (useSegmentOverride && localSegmentOverride != 0)
    * {
    * clockCounter = (byte)(clockCounter + 2); // Add two additional clocks for
    * segment override
    * biuFullAddress = localSegmentOverride switch
    * {
    * SEGMENT_ES => (uint)((_registers.ES << 4) + address),
    * SEGMENT_SS => (uint)((_registers.SS << 4) + address),
    * SEGMENT_DS => (uint)((_registers.DS << 4) + address),
    * SEGMENT_CS => (uint)((_registers.CS << 4) + address),
    * _ => biuFullAddress
    * };
    * }
    * else
    * {
    * biuFullAddress = segment switch
    * {
    * SEGMENT_00 => address,
    * SEGMENT_ES => (uint)((_registers.ES << 4) + address),
    * SEGMENT_SS => (uint)((_registers.SS << 4) + address),
    * SEGMENT_DS => (uint)((_registers.DS << 4) + address),
    * SEGMENT_CS => (uint)((_registers.CS << 4) + address),
    * _ => biuFullAddress
    * };
    * }
    * 
    * return biuFullAddress;
    * }
    * 
    * // ------------------------------------------------------
    * // Initiate Bus Interface cycles
    * // ------------------------------------------------------
    * private int Biu_Operation(BiuOperations biuOperation,
    * boolean useSegmentOverride,
    * int segment,
    * int address,
    * int writeData)
    * {
    * int readData;
    * uint biuFullAddress;
    * 
    * if (biuOperation == BiuOperations.InterruptAck)
    * {
    * assert_lock = 1;
    * _ = BIU_Bus_Cycle(biuOperation, 0x00000, 0x00);
    * _clock.WaitForFallingEdge();
    * _clock.WaitForFallingEdge();
    * readData = BIU_Bus_Cycle(biuOperation, 0x00000, 0x00);
    * }
    * else if ((byte)biuOperation > 0x0F) // 16 bit operation?
    * {
    * // Word cycle
    * biuFullAddress = CalculateFullAddress(useSegmentOverride, (byte)segment,
    * address);
    * var lowerByte = (byte)(0x00ff & writeData);
    * var readDataLower = BIU_Bus_Cycle(biuOperation, biuFullAddress, lowerByte);
    * 
    * address++; // 16-bit value allows for wrapping within a segment
    * 
    * biuFullAddress = CalculateFullAddress(useSegmentOverride, (byte)segment,
    * address);
    * var upperByte = (byte)(writeData >> 8);
    * var readDataUpper = BIU_Bus_Cycle(biuOperation, biuFullAddress, upperByte);
    * 
    * readData = (int)((readDataUpper << 8) | readDataLower);
    * }
    * else // 8 bit operation
    * {
    * biuFullAddress = CalculateFullAddress(useSegmentOverride, (byte)segment,
    * address);
    * readData = BIU_Bus_Cycle(biuOperation, biuFullAddress, (byte)(writeData &
    * 0x00FF));
    * }
    * 
    * return readData;
    * }
    */

   // ------------------------------------------------------
   // Add a byte to the prefetch queue
   // ------------------------------------------------------
   private void PfqAddByte() {
      if (prefetch_queue_count > 3)
         return; // Prefetch queue limited to four bytes

      // var localByte = (byte)Biu_Operation(BiuOperations.CodeReadByte,
      // SEGMENT_OVERRIDABLE_FALSE, SEGMENT_CS, _pfqInAddress, 0x00);
      byte localByte = _biu.readCode(_pfqInAddress);

      _pfqInAddress++;

      switch (prefetch_queue_count) {
         case 0:
            pfq_byte_A = localByte;
            break;
         case 1:
            pfq_byte_B = localByte;
            break;
         case 2:
            pfq_byte_C = localByte;
            break;
         case 3:
            pfq_byte_D = localByte;
            break;
      }

      prefetch_queue_count++;
   }

   // ------------------------------------------------------
   // Fetch a byte from the prefetch queue
   // ------------------------------------------------------
   private byte PfqFetchByte() {
      if (prefetch_queue_count == 0)
         PfqAddByte(); // Prefetch queue empty, so must fill at least one byte in the queue

      byte pfq_top_byte = pfq_byte_A;
      pfq_byte_A = pfq_byte_B;
      pfq_byte_B = pfq_byte_C;
      pfq_byte_C = pfq_byte_D;
      pfq_byte_D = 0x00;
      prefetch_queue_count--;

      _registers.IP++;
      return pfq_top_byte;
   }

   // ------------------------------------------------------
   // Fetch a word from the prefetch queue
   // ------------------------------------------------------
   private int PfqFetchWord() {
      int local_temp = PfqFetchByte();
      local_temp = (int) ((PfqFetchByte() << 8) | local_temp);
      return local_temp;
   }

   // --------------------------------------------------------------------------------------------------
   // --------------------------------------------------------------------------------------------------
   //
   // Begin 8086 Execution Unit
   //
   // --------------------------------------------------------------------------------------------------
   // --------------------------------------------------------------------------------------------------

   // private void ResetSequence() {
   //    _logger.trace("--> Reset sequence");

   //    // Stay here until RESET is de-asserted
   //    while (pins.getResetPin() != 0) {
   //       _clock.WaitForRisingEdge();
   //       _clock.WaitForFallingEdge();
   //    }

   //    _clock.WaitForFallingEdge(); // Wait 7 clocks total before beginning to fetch instructions
   //    _clock.WaitForFallingEdge();
   //    _clock.WaitForFallingEdge();

   //    pins.setBusStatusPins(BusStatus.Pass);

   //    _clock.WaitForFallingEdge();
   //    _clock.WaitForFallingEdge();
   //    _clock.WaitForFallingEdge();
   //    _clock.WaitForFallingEdge();
   //    _clock.WaitForFallingEdge();
   //    _clock.WaitForFallingEdge();
   //    _clock.WaitForFallingEdge();

   //    _nmiLatched.Clear(); // Debounce NMI

   //    _clock.setClockCounter(10); // Debounce prefixes and cycle counter
   //    _lastInstrSetPrefix = false;
   //    _pauseInterrupts = false;

   //    _registers.Flags = 0x0000; // Reset registers
   //    _registers.ES = 0;
   //    _registers.SS = 0;
   //    _registers.DS = 0;
   //    _registers.CS = 0xFFFF;
   //    _registers.IP = 0;

   //    _pfqInAddress = 0;
   //    prefetch_queue_count = 0;

   //    _clock.WaitForFallingEdge();

   //    _logger.trace("<-- Reset sequence");
   // }

   // ------------------------------------------------------
   // Displacement and sign helpers
   // ------------------------------------------------------
   private int SignExtendedByte(int localData) {
      if ((0x0080 & localData) != 0)
         return (int) (0xFF00 | localData);
      return (int) (0x00FF & localData);
   }

   // ------------------------------------------------------
   // Read from CPU registers
   // ------------------------------------------------------
   private int FetchRegister(int local_regSel) {
      switch (local_regSel) {
         case 0x00:
            return (int) (0x00FF & _registers.AX);
         case 0x01:
            return (int) (0x00FF & _registers.CX);
         case 0x02:
            return (int) (0x00FF & _registers.DX);
         case 0x03:
            return (int) (0x00FF & _registers.BX);
         case 0x04:
            return (int) (_registers.AX >> 8);
         case 0x05:
            return (int) (_registers.CX >> 8);
         case 0x06:
            return (int) (_registers.DX >> 8);
         case 0x07:
            return (int) (_registers.BX >> 8);
         case 0x08:
            return _registers.AX;
         case 0x09:
            return _registers.CX;
         case 0x0A:
            return _registers.DX;
         case 0x0B:
            return _registers.BX;
         case 0x0C:
            return _registers.SP;
         case 0x0D:
            return _registers.BP;
         case 0x0E:
            return _registers.SI;
         case 0x0F:
            return _registers.DI;
         default:
            return 0xDEAD;
      }
   }

   // ------------------------------------------------------
   // Read from CPU segment registers
   // ------------------------------------------------------
   private int FetchSegReg(byte localRegSel) {
      localRegSel &= 0x3;
      switch (localRegSel) {
         case 0x00:
            return _registers.ES;
         case 0x01:
            return _registers.CS;
         case 0x02:
            return _registers.SS;
         case 0x03:
            return _registers.DS;
         default:
            return 0xDEAD;
      }
   }

   // ------------------------------------------------------
   // Write to CPU segment registers
   // ------------------------------------------------------
   private void WriteSegReg(byte localRegSel, int localData) {
      localRegSel &= 0x3;
      switch (localRegSel) {
         case 0x00:
            _registers.ES = localData;
            break; // ES
         case 0x01:
            _registers.CS = localData;
            break; // CS
         case 0x02:
            _registers.SS = localData;
            break; // SS
         case 0x03:
            _registers.DS = localData;
            break; // DS
      }

      _pauseInterrupts = true; // Don't allow interrupt until after next instruction
   }

   // ------------------------------------------------------
   // Write to CPU registers
   // ------------------------------------------------------
   private void WriteRegister(int localRegSel, int localData) {
      switch (localRegSel) {
         case 0x00:
            _registers.AX = (int) ((0xFF00 & _registers.AX) | (localData & 0xFF));
            break; // AL
         case 0x01:
            _registers.CX = (int) ((0xFF00 & _registers.CX) | (localData & 0xFF));
            break; // CL
         case 0x02:
            _registers.DX = (int) ((0xFF00 & _registers.DX) | (localData & 0xFF));
            break; // DL
         case 0x03:
            _registers.BX = (int) ((0xFF00 & _registers.BX) | (localData & 0xFF));
            break; // BL
         case 0x04:
            _registers.AX = (int) ((0x00FF & _registers.AX) | localData << 8);
            break; // AH
         case 0x05:
            _registers.CX = (int) ((0x00FF & _registers.CX) | localData << 8);
            break; // CH
         case 0x06:
            _registers.DX = (int) ((0x00FF & _registers.DX) | localData << 8);
            break; // DH
         case 0x07:
            _registers.BX = (int) ((0x00FF & _registers.BX) | localData << 8);
            break; // BH
         case 0x08:
            _registers.AX = localData;
            break; // AX
         case 0x09:
            _registers.CX = localData;
            break; // CX
         case 0x0A:
            _registers.DX = localData;
            break; // DX
         case 0x0B:
            _registers.BX = localData;
            break; // BX
         case 0x0C:
            _registers.SP = localData;
            break; // SP
         case 0x0D:
            _registers.BP = localData;
            break; // BP
         case 0x0E:
            _registers.SI = localData;
            break; // SI
         case 0x0F:
            _registers.DI = localData;
            break; // DI
      }
   }

   // ------------------------------------------------------
   // Calculate the Effective Address
   // ------------------------------------------------------
   private void CalculateEa() {
      /*
       * Register Table
       * REG w=0 w=1 REG w=0 w=1
       * 000 AL AX 100 AH SP
       * 001 CL CX 101 CH BP
       * 010 DL DX 110 DH SI
       * 011 BL BX 111 BH DI
       * 
       * R/M Table 1 (Mod = 00)
       * 000: [BX+SI]
       * 001: [BX+DI]
       * 010: [BP+SI]
       * 011: [BP+DI]
       * 100: [SI]
       * 101: [DI]
       * 110: direct addressing
       * 111: [BX]
       * 
       * 
       * R/M Table 2 (Mod = 01) Add DISP to register specified:
       * 000: [BX+SI]
       * 001: [BX+DI]
       * 010: [BP+SI]
       * 011: [BP+DI]
       * 100: [SI]
       * 101: [DI]
       * 110: [BP]
       * 111: [BX]
       */

      _wordOperation = (byte) (_opCodeFirstByte & 0x01); // Isolate the R/W bit from the opcode
      byte opCodeSecondByte = PfqFetchByte(); // Fetch the MOD/REG/RM byte from the prefetch queue

      _regField = (byte) ((0x38 & opCodeSecondByte) >> 3); // Opcode REG field
      _modField = (byte) (opCodeSecondByte >> 6); // Opcode MOD field
      _rmField = (byte) (0x07 & opCodeSecondByte); // Opcode R/M field

      _regFieldTable = (byte) (_wordOperation << 3 | _regField); // concatenate W and MOD=11 field bits
      _rmFieldTable = (byte) (_wordOperation << 3 | _rmField); // concatenate W and R/M field bits

      _eaIsRegister = 0; // Default

      if (_modField == 3) // Two register instruction; use REG table
         _eaIsRegister = 1;
      else if (_modField == 0) // Use R/M Table-1 for R/M operand
      {
         switch (_rmField) {
            case 0x00:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.BX + _registers.SI);
               _clock.incClockCounter((byte) 7);
               break;
            case 0x01:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.BX + _registers.DI);
               _clock.incClockCounter((byte) 8);
               break;
            case 0x02:
               _eaSegment = SegmentRegs.SS;
               _eaAddress = (int) (_registers.BP + _registers.SI);
               _clock.incClockCounter((byte) 8);
               break;
            case 0x03:
               _eaSegment = SegmentRegs.SS;
               _eaAddress = (int) (_registers.BP + _registers.DI);
               _clock.incClockCounter((byte) 7);
               break;
            case 0x04:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = _registers.SI;
               _clock.incClockCounter((byte) 5);
               break;
            case 0x05:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = _registers.DI;
               _clock.incClockCounter((byte) 5);
               break;
            case 0x06:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = PfqFetchWord();
               _clock.incClockCounter((byte) 6);
               break;
            case 0x07:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = _registers.BX;
               _clock.incClockCounter((byte) 5);
               break;
         }
      } else if (_modField == 1) // Use R/M Table-2 with 8-bit displacement
      {
         switch (_rmField) {
            case 0x00:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.BX + _registers.SI + SignExtendedByte(PfqFetchByte()));
               _clock.incClockCounter((byte) 11);
               break;
            case 0x01:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.BX + _registers.DI + SignExtendedByte(PfqFetchByte()));
               _clock.incClockCounter((byte) 12);
               break;
            case 0x02:
               _eaSegment = SegmentRegs.SS;
               _eaAddress = (int) (_registers.BP + _registers.SI + SignExtendedByte(PfqFetchByte()));
               _clock.incClockCounter((byte) 12);
               break;
            case 0x03:
               _eaSegment = SegmentRegs.SS;
               _eaAddress = (int) (_registers.BP + _registers.DI + SignExtendedByte(PfqFetchByte()));
               _clock.incClockCounter((byte) 11);
               break;
            case 0x04:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.SI + SignExtendedByte(PfqFetchByte()));
               _clock.incClockCounter((byte) 9);
               break;
            case 0x05:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.DI + SignExtendedByte(PfqFetchByte()));
               _clock.incClockCounter((byte) 9);
               break;
            case 0x06:
               _eaSegment = SegmentRegs.SS;
               _eaAddress = (int) (_registers.BP + SignExtendedByte(PfqFetchByte()));
               _clock.incClockCounter((byte) 9);
               break;
            case 0x07:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.BX + SignExtendedByte(PfqFetchByte()));
               _clock.incClockCounter((byte) 9);
               break;
         }
      } else if (_modField == 2) // Use R/M Table 2 with 16-bit displacement
      {
         switch (_rmField) {
            case 0x00:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.BX + _registers.SI + PfqFetchWord());
               _clock.incClockCounter((byte) 11);
               break;
            case 0x01:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.BX + _registers.DI + PfqFetchWord());
               _clock.incClockCounter((byte) 12);
               break;
            case 0x02:
               _eaSegment = SegmentRegs.SS;
               _eaAddress = (int) (_registers.BP + _registers.SI + PfqFetchWord());
               _clock.incClockCounter((byte) 12);
               break;
            case 0x03:
               _eaSegment = SegmentRegs.SS;
               _eaAddress = (int) (_registers.BP + _registers.DI + PfqFetchWord());
               _clock.incClockCounter((byte) 11);
               break;
            case 0x04:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.SI + PfqFetchWord());
               _clock.incClockCounter((byte) 9);
               break;
            case 0x05:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.DI + PfqFetchWord());
               _clock.incClockCounter((byte) 9);
               break;
            case 0x06:
               _eaSegment = SegmentRegs.SS;
               _eaAddress = (int) (_registers.BP + PfqFetchWord());
               _clock.incClockCounter((byte) 9);
               break;
            case 0x07:
               _eaSegment = SegmentRegs.DS;
               _eaAddress = (int) (_registers.BX + PfqFetchWord());
               _clock.incClockCounter((byte) 9);
               break;
         }
      }
   }

   // ------------------------------------------------------
   // Fetch the data from the Effective Address
   // ------------------------------------------------------
   private int FetchEa() {
      int local_data = 0;
      if (_eaIsRegister == 1) {
         local_data = FetchRegister(_rmFieldTable);
      } else if (_wordOperation == 0) {
         // local_data = Biu_Operation(BiuOperations.MemReadByte,
         // SEGMENT_OVERRIDABLE_TRUE, _eaSegment, _eaAddress, 0x00);
         local_data = _biu.readMemoryByte(SegmentOverridableTrue, _eaSegment, _eaAddress);
      } else if (_wordOperation == 1) {
         // local_data = Biu_Operation(BiuOperations.MemReadWord,
         // SEGMENT_OVERRIDABLE_TRUE, _eaSegment, _eaAddress, 0x00);
         local_data = _biu.readMemoryWord(SegmentOverridableTrue, _eaSegment, _eaAddress);
      }

      return local_data;
   }

   // ------------------------------------------------------
   // Write data back to the Effective Address
   // ------------------------------------------------------
   private void WriteBackEa(int local_data) {
      if (_eaIsRegister == 1) {
         WriteRegister(_rmFieldTable, local_data);
      } else if (_wordOperation == 0) {
         // Biu_Operation(BiuOperations.MemWriteByte, SEGMENT_OVERRIDABLE_TRUE,
         // _eaSegment, _eaAddress, local_data);
         _biu.writeMemoryByte(SegmentOverridableTrue, _eaSegment, _eaAddress, (byte) local_data);
      } else if (_wordOperation == 1) {
         // Biu_Operation(BiuOperations.MemWriteWord, SEGMENT_OVERRIDABLE_TRUE,
         // _eaSegment, _eaAddress, local_data);
         _biu.writeMemoryWord(SegmentOverridableTrue, _eaSegment, _eaAddress, local_data);
      }
   }

   // ------------------------------------------------------
   // Interrupt Processing
   // ------------------------------------------------------
   private void Interrupt_Handler(byte local_intr_type) {
      _clock.incClockCounter((byte) 71);

      int pushAddr = (_registers.CS << 4) + _registers.IP;
      _instLogger.debug("     pushing return addr: 0x{0:X5}", pushAddr);

      // Push the Flags and set bits [15:12] to F like 8088 does.
      Push((int) (_registers.Flags | 0xF000));

      // Clear the IF and TF Flags
      _registers.Flags &= 0xFCFF;

      // Push the Code Segment
      Push(_registers.CS);

      // Shift Interrupt type left 2 bits (*4) then
      int local_address = (int) (local_intr_type << 2);

      // Fetch the CS (offset +2 from Interrupt vector base)
      // var new_cs = Biu_Operation(BiuOperations.MemReadWord,
      // SEGMENT_OVERRIDABLE_FALSE, SEGMENT_00, (int)(local_address + 2), 0x00);
      int new_cs = _biu.readMemoryWord(SegmentOverridableFalse, SegmentRegs.None, (int) (local_address + 2));

      // Push the IP
      Push(_registers.IP);

      // Fetch the IP at the Interrupt vector base
      // _registers.IP = Biu_Operation(BiuOperations.MemReadWord,
      // SEGMENT_OVERRIDABLE_FALSE, SEGMENT_00, local_address, 0x00);
      _registers.IP = _biu.readMemoryWord(SegmentOverridableFalse, SegmentRegs.None, local_address);

      _registers.CS = new_cs;

      int popAddr = (_registers.CS << 4) + _registers.IP;
      _instLogger.debug("     interrupt addr: 0x{0:X5}", popAddr);

      _pfqInAddress = _registers.IP;
      prefetch_queue_count = 0;
      _biu.setPrefixFlags((byte) 0);
   }

   // ------------------------------------------------------
   // Interrupt sources
   // ------------------------------------------------------

   // DIV0 Handler - Interrupt Type 0 - Division by Zero
   private void Div0Handler() {
      _clock.incClockCounter((byte) 1);
      Interrupt_Handler((byte) 0x0);
   }

   // TRAP Handler - Interrupt Type 1 - TRAP (Single Step)
   private void TrapHandler() {
      _clock.incClockCounter((byte) 1);
      Interrupt_Handler((byte) 0x1);
   }

   // NMI Handler - Interrupt Type 2
   private void NmiHandler() {
      _clock.incClockCounter((byte) 1);
      _nmiLatched.Clear();
      Interrupt_Handler((byte) 0x2);
   }

   private void IntrHandler() {
      // External Interrupt Processing - INTR type fetched from i8259
      // var local_intr_type = (byte)Biu_Operation(BiuOperations.InterruptAck,
      // SEGMENT_OVERRIDABLE_FALSE, SEGMENT_00, 0x00000, 0x00);
      byte local_intr_type = _biu.interruptAck();
      Interrupt_Handler(local_intr_type);
   }

   // ------------------------------------------------------
   // Set Flags S, Z, P for byte data:
   // ------------------------------------------------------
   private void Set_Flags_Byte_SZP(byte localData) {
      // Zero out Flags: S, Z, P
      _registers.Flags = (int) (_registers.Flags & 0xFF3B);

      // Set S Flag
      if ((localData & 0x80) > 0)
         _registers.Flags = (int) (_registers.Flags | 0x0080);

      // Set Z Flag
      if (localData == 0x00)
         _registers.Flags = (int) (_registers.Flags | 0x0040);

      // Set P Flag using array table lookup
      _registers.Flags = (int) (_registers.Flags | Parity_Array[localData]);
   }

   // ------------------------------------------------------
   // Set Flags S, Z, P for word data:
   // ------------------------------------------------------
   private void Set_Flags_Word_SZP(int local_data) {
      // Zero out Flags: S, Z, P
      _registers.Flags = (int) (_registers.Flags & 0xFF3B);

      // Set S Flag
      if ((local_data & 0x8000) > 0)
         _registers.Flags = (int) (_registers.Flags | 0x0080);

      // Set Z Flag
      if (local_data == 0x0000)
         _registers.Flags = (int) (_registers.Flags | 0x0040);

      // Set P Flag using array table lookup
      _registers.Flags = (int) (_registers.Flags | Parity_Array[(0xFF & local_data)]);
   }

   // ------------------------------------------------------
   // Addition for bytes
   // ------------------------------------------------------
   private byte AddBytes(byte local_data1, byte local_data2) {
      byte local_nibble_results;
      int local_byte_results;

      byte local_cf = (Flag_c());

      if (incDec == 1)
         _registers.Flags = (int) (_registers.Flags & 0xF7EF); // Zero out Flags: O, A
      else
         _registers.Flags = (int) (_registers.Flags & 0xF7EE); // Zero out Flags: O, A ,C

      if (with_carry == 1) {
         local_nibble_results = (byte) ((0x0F & local_data1) + (0x0F & local_data2) + local_cf); // Perform the nibble
                                                                                                 // math
         local_byte_results = (int) (local_data1 + local_data2 + local_cf); // Perform the byte math
      } else {
         local_nibble_results = (byte) ((0x0F & local_data1) + (0x0F & local_data2)); // Perform the nibble math
         local_byte_results = (int) (local_data1 + local_data2); // Perform the byte math
      }

      if (local_nibble_results > 0x0F)
         _registers.Flags = (int) (_registers.Flags | 0x0010); // Set A Flag
      if (incDec == 0 && (local_byte_results > 0xFF))
         _registers.Flags = (int) (_registers.Flags | 0x0001); // Set C Flag if not INC or DEC opcodes

      incDec = 0; // Debounce incDec

      int operand0 = (int) (local_data1 & 0x0080);
      int operand1 = (int) (local_data2 & 0x0080);
      int result = (int) (local_byte_results & 0x0080);
      if (operand0 == 0 && operand1 == 0 && result != 0)
         _registers.Flags = (int) (_registers.Flags | 0x0800); // Set O Flag
      else if (operand0 != 0 && operand1 != 0 && result == 0)
         _registers.Flags = (int) (_registers.Flags | 0x0800);

      Set_Flags_Byte_SZP((byte) local_byte_results); // Set S,Z,P Flags
      with_carry = 0;

      return (byte) local_byte_results;
   }

   // ------------------------------------------------------
   // Addition for words
   // ------------------------------------------------------
   private int AddWords(int local_data1, int local_data2) {
      byte local_nibble_results;
      int local_word_results;
      int local_cf = Flag_c();

      if (incDec == 1)
         _registers.Flags = (int) (_registers.Flags & 0xF7EF); // Zero out Flags: O, A
      else
         _registers.Flags = (int) (_registers.Flags & 0xF7EE); // Zero out Flags: O, A ,C

      if (with_carry == 1) {
         local_nibble_results = (byte) ((0x0F & local_data1) + (0x0F & local_data2) + local_cf); // Perform the nibble
                                                                                                 // math
         local_word_results = (int) (local_data1 + local_data2 + local_cf); // Perform the word math
      } else {
         local_nibble_results = (byte) ((0x0F & local_data1) + (0x0F & local_data2)); // Perform the nibble math
         local_word_results = (int) (local_data1 + local_data2); // Perform the word math
      }

      if (local_nibble_results > 0x0F)
         _registers.Flags = (int) (_registers.Flags | 0x0010); // Set A Flag
      if (incDec == 0 && (local_word_results > 0xFFFF))
         _registers.Flags = (int) (_registers.Flags | 0x0001); // Set C Flag if not INC or DEC opcodes
      incDec = 0; // Debounce incDec

      int operand0 = (int) (local_data1 & 0x8000);
      int operand1 = (int) (local_data2 & 0x8000);
      int result = (int) (local_word_results & 0x8000);
      if (operand0 == 0 && operand1 == 0 && result != 0)
         _registers.Flags = (int) (_registers.Flags | 0x0800); // Set O Flag
      else if (operand0 != 0 && operand1 != 0 && result == 0)
         _registers.Flags = (int) (_registers.Flags | 0x0800);

      Set_Flags_Word_SZP((int) local_word_results); // Set S,Z,P Flags
      with_carry = 0;

      return (int) local_word_results;
   }

   // ------------------------------------------------------
   // Subtraction for bytes
   // ------------------------------------------------------
   private byte SubBytes(byte local_data1, byte local_data2) {
      byte local_nibble_results;
      int local_byte_results;
      int local_cf = Flag_c();

      if (incDec == 1)
         _registers.Flags = (int) (_registers.Flags & 0xF7EF); // Zero out Flags: O, A
      else
         _registers.Flags = (int) (_registers.Flags & 0xF7EE); // Zero out Flags: O, A ,C

      if (with_carry == 1) {
         local_nibble_results = (byte) ((0x0F & local_data1) - (0x0F & local_data2) - local_cf); // Perform the nibble
                                                                                                 // math
         local_byte_results = (int) (local_data1 - local_data2 - local_cf); // Perform the byte math
      } else {
         local_nibble_results = (byte) ((0x0F & local_data1) - (0x0F & local_data2)); // Perform the nibble math
         local_byte_results = (int) (local_data1 - local_data2); // Perform the byte math
      }

      if (local_nibble_results > 0x0F)
         _registers.Flags = (int) (_registers.Flags | 0x0010); // Set A Flag
      if (incDec == 0 && (local_byte_results > 0xFF))
         _registers.Flags = (int) (_registers.Flags | 0x0001); // Set C Flag if not INC or DEC opcodes

      incDec = 0; // Debounce incDec

      int operand0 = (int) (local_data1 & 0x0080);
      int operand1 = (int) (local_data2 & 0x0080);
      int result = (int) (local_byte_results & 0x0080);
      if (operand0 == 0 && operand1 != 0 && result != 0)
         _registers.Flags = (int) (_registers.Flags | 0x0800); // Set O Flag
      else if (operand0 != 0 && operand1 == 0 && result == 0)
         _registers.Flags = (int) (_registers.Flags | 0x0800);

      Set_Flags_Byte_SZP((byte) local_byte_results); // Set S,Z,P Flags
      with_carry = 0;

      return (byte) local_byte_results;
   }

   // ------------------------------------------------------
   // Subtraction for words
   // ------------------------------------------------------
   private int SubWords(int local_data1, int local_data2) {
      byte local_nibble_results;
      int local_word_results;
      int local_cf = (Flag_c());

      if (incDec == 1)
         _registers.Flags = (int) (_registers.Flags & 0xF7EF); // Zero out Flags: O, A
      else
         _registers.Flags = (int) (_registers.Flags & 0xF7EE); // Zero out Flags: O, A ,C

      if (with_carry == 1) {
         local_nibble_results = (byte) ((0x0F & local_data1) - (0x0F & local_data2) - local_cf); // Perform the nibble
                                                                                                 // math
         local_word_results = (int) (local_data1 - local_data2 - local_cf); // Perform the word math
      } else {
         local_nibble_results = (byte) ((0x0F & local_data1) - (0x0F & local_data2)); // Perform the nibble math
         local_word_results = (int) (local_data1 - local_data2); // Perform the word math
      }

      if (local_nibble_results > 0x0F)
         _registers.Flags = (int) (_registers.Flags | 0x0010); // Set A Flag
      if (incDec == 0 && (local_word_results > 0xFFFF))
         _registers.Flags = (int) (_registers.Flags | 0x0001); // Set C Flag if not INC or DEC opcodes

      incDec = 0; // Debounce incDec

      int operand0 = (int) (local_data1 & 0x8000);
      int operand1 = (int) (local_data2 & 0x8000);
      int result = (int) (local_word_results & 0x8000);
      if (operand0 == 0 && operand1 != 0 && result != 0)
         _registers.Flags = (int) (_registers.Flags | 0x0800); // Set O Flag
      else if (operand0 != 0 && operand1 == 0 && result == 0)
         _registers.Flags = (int) (_registers.Flags | 0x0800);

      Set_Flags_Word_SZP((int) local_word_results); // Set S,Z,P Flags
      with_carry = 0;

      return (int) local_word_results;
   }
   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   // Stack Opcodes
   ////////////////////////////////////////////////////////////////////////////
   ////////////////////////////////////////////////////////////////////////////

   // =========================================================================
   // Stack - PUSH
   // =========================================================================
   private void Push(int localData) {
      _registers.SP -= 2;

      int addr = (_registers.SS << 4) + _registers.SP;
      _instLogger.trace("     pushing to {0:X5}:{1:X4}", addr, localData);

      // Biu_Operation(BiuOperations.MemWriteWord, SEGMENT_OVERRIDABLE_FALSE,
      // SEGMENT_SS, _registers.SP, localData);
      _biu.writeMemoryWord(SegmentOverridableFalse, SegmentRegs.SS, _registers.SP, localData);
   }

   // -------------------------------------------------------------------------
   // 0x06 - PUSH ES
   // -------------------------------------------------------------------------
   private void OpCode_0x06() {
      _clock.incClockCounter((byte) 14);
      Push(_registers.ES);
   }

   // -------------------------------------------------------------------------
   // 0x0E - PUSH CS
   // -------------------------------------------------------------------------
   private void OpCode_0x0E() {
      _clock.incClockCounter((byte) 14);
      Push(_registers.CS);
   }

   // -------------------------------------------------------------------------
   // 0x16 - PUSH SS
   // -------------------------------------------------------------------------
   private void OpCode_0x16() {
      _clock.incClockCounter((byte) 14);
      Push(_registers.SS);
   }

   // -------------------------------------------------------------------------
   // 0x1E - PUSH DS
   // -------------------------------------------------------------------------
   private void OpCode_0x1E() {
      _clock.incClockCounter((byte) 14);
      Push(_registers.DS);
   }

   // -------------------------------------------------------------------------
   // 0x9C - PUSHF - Push Flags
   // -------------------------------------------------------------------------
   private void OpCode_0x9C() {
      _clock.incClockCounter((byte) 14);
      Push((byte) (0xF000 | _registers.Flags));
   }

   // -------------------------------------------------------------------------
   // 0x50 - PUSH AX
   // -------------------------------------------------------------------------
   private void OpCode_0x50() {
      _clock.incClockCounter((byte) 15);
      Push(_registers.AX);
   }

   // -------------------------------------------------------------------------
   // 0x51 - PUSH CX
   // -------------------------------------------------------------------------
   private void OpCode_0x51() {
      _clock.incClockCounter((byte) 15);
      Push(_registers.CX);
   }

   // -------------------------------------------------------------------------
   // 0x52 - PUSH DX
   // -------------------------------------------------------------------------
   private void OpCode_0x52() {
      _clock.incClockCounter((byte) 15);
      Push(_registers.DX);
   }

   // -------------------------------------------------------------------------
   // 0x53 - PUSH BX
   // -------------------------------------------------------------------------
   private void OpCode_0x53() {
      _clock.incClockCounter((byte) 15);
      Push(_registers.BX);
   }

   // -------------------------------------------------------------------------
   // 0x54 - PUSH the new SP
   // -------------------------------------------------------------------------
   private void OpCode_0x54() {
      _clock.incClockCounter((byte) 15);
      Push((byte) (_registers.SP - 2));
   }

   // -------------------------------------------------------------------------
   // 0x55 - PUSH BP
   // -------------------------------------------------------------------------
   private void OpCode_0x55() {
      _clock.incClockCounter((byte) 15);
      Push(_registers.BP);
   }

   // -------------------------------------------------------------------------
   // 0x56 - PUSH SI
   // -------------------------------------------------------------------------
   private void OpCode_0x56() {
      _clock.incClockCounter((byte) 15);
      Push(_registers.SI);
   }

   // -------------------------------------------------------------------------
   // 0x57 - PUSH DI
   // -------------------------------------------------------------------------
   private void OpCode_0x57() {
      _clock.incClockCounter((byte) 15);
      Push(_registers.DI);
   }

   // =========================================================================
   // Stack - POP
   // =========================================================================

   private int Pop() {
      // var localData = Biu_Operation(BiuOperations.MemReadWord,
      // SEGMENT_OVERRIDABLE_FALSE, SEGMENT_SS, _registers.SP, 0x00);
      int localData = _biu.readMemoryWord(SegmentOverridableFalse, SegmentRegs.SS, _registers.SP);
      int addr = (_registers.SS << 4) + _registers.SP;
      _instLogger.trace("     popping from {0:X5}:{1:X4}", addr, localData);
      _registers.SP += 2;
      return localData;
   }

   // -------------------------------------------------------------------------
   // 0x07 - POP ES - Set prefix so no interrupt on next instruction
   // -------------------------------------------------------------------------
   private void OpCode_0x07() {
      _clock.incClockCounter((byte) 8);
      _registers.ES = Pop();
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0x0F - POP CS - Set prefix so no interrupt on next instruction
   // -------------------------------------------------------------------------
   private void OpCode_0x0F() {
      _clock.incClockCounter((byte) 8);
      _registers.CS = Pop();
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0x17 - POP SS - Set prefix so no interrupt on next instruction
   // -------------------------------------------------------------------------
   private void OpCode_0x17() {
      _clock.incClockCounter((byte) 8);
      _registers.SS = Pop();
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0x1F - POP DS - Set prefix so no interrupt on next instruction
   // -------------------------------------------------------------------------
   private void OpCode_0x1F() {
      _clock.incClockCounter((byte) 8);
      _registers.DS = Pop();
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0x9D - POPF - POP Flags
   // -------------------------------------------------------------------------
   private void OpCode_0x9D() {
      _clock.incClockCounter((byte) 8);
      _registers.Flags = (int) (0xF000 | (0x0FD5 & Pop()));
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0x58 - POP AX
   // -------------------------------------------------------------------------
   private void OpCode_0x58() {
      _clock.incClockCounter((byte) 8);
      _registers.AX = Pop();
   }

   // -------------------------------------------------------------------------
   // 0x59 - POP CX
   // -------------------------------------------------------------------------
   private void OpCode_0x59() {
      _clock.incClockCounter((byte) 8);
      _registers.CX = Pop();
   }

   // -------------------------------------------------------------------------
   // 0x5A - POP DX
   // -------------------------------------------------------------------------
   private void OpCode_0x5A() {
      _clock.incClockCounter((byte) 8);
      _registers.DX = Pop();
   }

   // -------------------------------------------------------------------------
   // 0x5B - POP BX
   // -------------------------------------------------------------------------
   private void OpCode_0x5B() {
      _clock.incClockCounter((byte) 8);
      _registers.BX = Pop();
   }

   // -------------------------------------------------------------------------
   // 0x5C - POP SP
   // -------------------------------------------------------------------------
   private void OpCode_0x5C() {
      _clock.incClockCounter((byte) 8);
      _registers.SP = Pop();
   }

   // -------------------------------------------------------------------------
   // 0x5D - POP BP
   // -------------------------------------------------------------------------
   private void OpCode_0x5D() {
      _clock.incClockCounter((byte) 8);
      _registers.BP = Pop();
   }

   // -------------------------------------------------------------------------
   // 0x5E - POP SI
   // -------------------------------------------------------------------------
   private void OpCode_0x5E() {
      _clock.incClockCounter((byte) 8);
      _registers.SI = Pop();
   }

   // -------------------------------------------------------------------------
   // 0x5F - POP DI
   // -------------------------------------------------------------------------
   private void OpCode_0x5F() {
      _clock.incClockCounter((byte) 8);
      _registers.DI = Pop();
   }

   // -------------------------------------------------------------------------
   // 0x8F - POP REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x8F() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 8);
      else
         _clock.incClockCounter((byte) 17);
      WriteBackEa(Pop());
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   // IO Opcodes
   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   // -------------------------------------------------------------------------
   // 0xEC - IN - ac,DX - Variable Port - Byte
   // -------------------------------------------------------------------------
   private void OpCode_0xEC() {
      _clock.incClockCounter((byte) 8);

      // Fetch the IO byte data at address DX
      // var localData = Biu_Operation(BiuOperations.IoReadByte,
      // SEGMENT_OVERRIDABLE_FALSE, SEGMENT_00, _registers.DX, 0x00);
      byte localData = _biu.readIoByte(_registers.DX);
      _registers.AX = (int) ((_registers.AX & 0xFF00) | localData);
   }

   // -------------------------------------------------------------------------
   // 0xE4 0xpp - IN - ac,Opcode Port - Byte
   // -------------------------------------------------------------------------
   private void OpCode_0xE4() {
      _clock.incClockCounter((byte) 10);

      // Fetch the IO byte data at address described in next opcode
      // var localData = Biu_Operation(BiuOperations.IoReadByte,
      // SEGMENT_OVERRIDABLE_FALSE, SEGMENT_00, (int)(0xFF & PfqFetchByte()), 0x00);
      int portAddr = (int) (0xFF & PfqFetchByte());
      byte localData = _biu.readIoByte(portAddr);
      _registers.AX = (int) ((_registers.AX & 0xFF00) | localData);
   }

   // -------------------------------------------------------------------------
   // 0xED - IN - ac,DX - Variable Port - Word
   // -------------------------------------------------------------------------
   private void OpCode_0xED() {
      _clock.incClockCounter((byte) 12);

      // _registers.AX = Biu_Operation(BiuOperations.IoReadWord,
      // SEGMENT_OVERRIDABLE_FALSE, SEGMENT_00, _registers.DX, 0x00);
      _registers.AX = _biu.readIoWord(_registers.DX);
   }

   // -------------------------------------------------------------------------
   // 0xE5 0xpp - IN - ac,Opcode Port - Word
   // -------------------------------------------------------------------------
   private void OpCode_0xE5() {
      _clock.incClockCounter((byte) 14);

      // _registers.AX = Biu_Operation(BiuOperations.IoReadWord,
      // SEGMENT_OVERRIDABLE_FALSE, SEGMENT_00, (int)(0xFF & PfqFetchByte()), 0x00);
      int portAddr = (int) (0xFF & PfqFetchByte());
      _registers.AX = _biu.readIoWord(portAddr);
   }

   // -------------------------------------------------------------------------
   // 0xEE - OUT - DX - Variable Port - Byte
   // -------------------------------------------------------------------------
   private void OpCode_0xEE() {
      _clock.incClockCounter((byte) 8);

      // Biu_Operation(BiuOperations.IoWriteByte, SEGMENT_OVERRIDABLE_FALSE,
      // SEGMENT_00, _registers.DX, _registers.AX);
      _biu.writeIoByte(_registers.DX, (byte) (_registers.AX & 0x00FF));
   }

   // -------------------------------------------------------------------------
   // 0xEF - OUT - DX - Variable Port - Word
   // -------------------------------------------------------------------------
   private void OpCode_0xEF() {
      _clock.incClockCounter((byte) 12);

      // Biu_Operation(BiuOperations.IoWriteWord, SEGMENT_OVERRIDABLE_FALSE,
      // SEGMENT_00, _registers.DX, _registers.AX);
      byte registerAl = (byte) (_registers.AX & 0x00FF);
      _biu.writeIoWord(_registers.DX, registerAl);
   }

   // -------------------------------------------------------------------------
   // 0xE6 0xpp - OUT - Opcode Port - Byte
   // -------------------------------------------------------------------------
   private void OpCode_0xE6() {
      _clock.incClockCounter((byte) 10);

      // Biu_Operation(BiuOperations.IoWriteByte, SEGMENT_OVERRIDABLE_FALSE,
      // SEGMENT_00, (int)(0xFF & PfqFetchByte()), _registers.AX);
      int portAddr = (int) (0xFF & PfqFetchByte());
      byte registerAl = (byte) (_registers.AX & 0x00FF);
      _biu.writeIoByte(portAddr, registerAl);
   }

   // -------------------------------------------------------------------------
   // 0xE7 0xpp - OUT - Opcode Port - Word
   // -------------------------------------------------------------------------
   private void OpCode_0xE7() {
      _clock.incClockCounter((byte) 14);

      // Biu_Operation(BiuOperations.IoWriteWord, SEGMENT_OVERRIDABLE_FALSE,
      // SEGMENT_00, (int)(0xFF & PfqFetchByte()), _registers.AX);
      _biu.writeIoWord((int) (0xFF & PfqFetchByte()), _registers.AX);
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   // Jumps Opcodes
   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   private void Jump_Not_Taken8() {
      _clock.incClockCounter((byte) 3);
      PfqFetchByte();
   }

   private void Jump_Taken8() {
      _clock.incClockCounter((byte) 9);
      int localDisplacement = PfqFetchByte();
      localDisplacement = SignExtendedByte(localDisplacement);
      _registers.IP += localDisplacement;

      int addr = (_registers.CS << 4) + _registers.IP;
      _instLogger.debug("     jmp rel-8 offset: 0x{0:X2} to addr: 0x{1:X5}", localDisplacement, addr);

      _pfqInAddress = _registers.IP;
      prefetch_queue_count = 0;
   }

   private void Jump_Taken16() {
      _clock.incClockCounter((byte) 7);
      int localDisplacement = PfqFetchWord();
      _registers.IP += localDisplacement;

      int addr = (_registers.CS << 4) + _registers.IP;
      _instLogger.debug("     jmp rel-16 offset: 0x{0:X4} to addr: 0x{1:X5}", localDisplacement, addr);

      _pfqInAddress = _registers.IP;
      prefetch_queue_count = 0;
   }

   private void Jump_Taken32() {
      _clock.incClockCounter((byte) 7);
      int localNewIp = PfqFetchWord();
      int localNewCs = PfqFetchWord();

      _registers.CS = localNewCs;
      _registers.IP = localNewIp;

      int addr = (_registers.CS << 4) + _registers.IP;
      _instLogger.debug("     jmp to addr: 0x{1:X5}", addr);

      _pfqInAddress = _registers.IP;
      prefetch_queue_count = 0;
   }

   // -------------------------------------------------------------------------
   // [0x70 0xdd] - JO - Jump on Overflow
   // -------------------------------------------------------------------------
   private void OpCode_0x70() {
      if (Flag_o() == 1)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x71 0xdd] - JNO - Jump on Not Overflow
   // -------------------------------------------------------------------------
   private void OpCode_0x71() {
      if (Flag_o() == 0)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x72 0xdd] - JB - Jump on Below
   // -------------------------------------------------------------------------
   private void OpCode_0x72() {
      if (Flag_c() == 1)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x73 0xdd] - JNB - Jump on Not Below
   // -------------------------------------------------------------------------
   private void OpCode_0x73() {
      if (Flag_c() == 0)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x74 0xdd] - JZ - Jump on Zero
   // -------------------------------------------------------------------------
   private void OpCode_0x74() {
      if (Flag_z() == 1)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x75 0xdd] - JNZ - Jump on Not Zero
   // -------------------------------------------------------------------------
   private void OpCode_0x75() {
      if (Flag_z() == 0)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x76 0xdd] - JNA - Jump on Not Above
   // -------------------------------------------------------------------------
   private void OpCode_0x76() {
      if ((Flag_z() == 1) || (Flag_c() == 1))
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x77 0xdd] - JA - Jump on Above
   // -------------------------------------------------------------------------
   private void OpCode_0x77() {
      if ((Flag_z() == 0) && (Flag_c() == 0))
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x78 0xdd] - JS - Jump on Sign
   // -------------------------------------------------------------------------
   private void OpCode_0x78() {
      if (Flag_s() == 1)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x79 0xdd] - JNS - Jump on Not Sign
   // -------------------------------------------------------------------------
   private void OpCode_0x79() {
      if (Flag_s() == 0)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x7A 0xdd] - JP - Jump on Parity
   // -------------------------------------------------------------------------
   private void OpCode_0x7A() {
      if (Flag_p() == 1)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x7B 0xdd] - JNP - Jump on Not Parity
   // -------------------------------------------------------------------------
   private void OpCode_0x7B() {
      if (Flag_p() == 0)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x7C 0xdd] - JL - Jump on Less
   // -------------------------------------------------------------------------
   private void OpCode_0x7C() {
      if (Flag_s() != Flag_o())
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x7D 0xdd] - JNL - Jump on Not Less
   // -------------------------------------------------------------------------
   private void OpCode_0x7D() {
      if (Flag_s() == Flag_o())
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x7E 0xdd] - JLE - Jump on Less or Equal
   // -------------------------------------------------------------------------
   private void OpCode_0x7E() {
      if (Flag_s() != Flag_o() || Flag_z() == 1)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0x7F 0xdd] - JNLE - Jump on Not Less or Equal
   // -------------------------------------------------------------------------
   private void OpCode_0x7F() {
      if (Flag_s() == Flag_o() && Flag_z() == 0)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0xE3 0xdd] - JCXZ - Jump if CX Zero
   // -------------------------------------------------------------------------
   private void OpCode_0xE3() {
      _clock.incClockCounter((byte) 0);
      if (_registers.CX == 0)
         Jump_Taken8();
      else
         Jump_Not_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0xEB 0xdd] - JMP - Disp8
   // -------------------------------------------------------------------------
   private void OpCode_0xEB() {
      Jump_Taken8();
   }

   // -------------------------------------------------------------------------
   // [0xE9 0xLO 0xHI] - JMP - Disp16
   // -------------------------------------------------------------------------
   private void OpCode_0xE9() {
      Jump_Taken16();
   }

   // -------------------------------------------------------------------------
   // [0xEA 0xLO 0xHI 0xCSLO 0xCSHI] - JMP - Inter-segment
   // -------------------------------------------------------------------------
   private void OpCode_0xEA() {
      Jump_Taken32();
   }

   // -------------------------------------------------------------------------
   // [0xE8 0xLO 0xHI] - CALL Intra-segment
   // -------------------------------------------------------------------------
   private void OpCode_0xE8() {
      _clock.incClockCounter((byte) 8);
      Push((int) (_registers.IP + 2));
      Jump_Taken16();
   }

   // -------------------------------------------------------------------------
   // 0x9A 0xLO 0xHI 0xCSLO 0xCSHI] - CALL Inter-segment
   // -------------------------------------------------------------------------
   private void OpCode_0x9A() {
      _clock.incClockCounter((byte) 21);
      Push(_registers.CS);
      Push((int) (_registers.IP + 4));
      Jump_Taken32();
   }

   // -------------------------------------------------------------------------
   // 0xC3 - Return - Intra-Segment
   // -------------------------------------------------------------------------
   private void OpCode_0xC3() {
      _clock.incClockCounter((byte) 20);
      _registers.IP = Pop();
      _pfqInAddress = _registers.IP;

      int addr = (_registers.CS << 4) + _registers.IP;
      _instLogger.debug("     return to addr: 0x{1:X5}", addr);

      prefetch_queue_count = 0;
   }

   // -------------------------------------------------------------------------
   // 0xCB - Return - Inter-Segment
   // -------------------------------------------------------------------------
   private void OpCode_0xCB() {
      _clock.incClockCounter((byte) 34);
      _registers.IP = Pop();
      _registers.CS = Pop();

      int addr = (_registers.CS << 4) + _registers.IP;
      _instLogger.debug("     return to addr: 0x{1:X5}", addr);

      _pfqInAddress = _registers.IP;
      prefetch_queue_count = 0;
   }

   // -------------------------------------------------------------------------
   // [0xC2 0xLO 0xHI] - Return - Intra-Segment and Add Immediate to Stack Pointer
   // -------------------------------------------------------------------------
   private void OpCode_0xC2() {
      _clock.incClockCounter((byte) 24);
      int tempIp = Pop();
      _registers.SP += PfqFetchWord();
      _registers.IP = tempIp;

      int addr = (_registers.CS << 4) + _registers.IP;
      _instLogger.debug("     return to addr: 0x{1:X5}", addr);

      _pfqInAddress = _registers.IP;
      prefetch_queue_count = 0;
   }

   // -------------------------------------------------------------------------
   // [0xCA 0xLO 0xHI]- Return - Inter-Segment and Add Immediate to Stack Pointer
   // -------------------------------------------------------------------------
   private void OpCode_0xCA() {
      _clock.incClockCounter((byte) 33);
      int newIp = Pop();
      int newCs = Pop();
      _registers.SP += PfqFetchWord();
      _registers.IP = newIp;
      _registers.CS = newCs;

      int addr = (_registers.CS << 4) + _registers.IP;
      _instLogger.debug("     return to addr: 0x{1:X5}", addr);

      _pfqInAddress = _registers.IP;
      prefetch_queue_count = 0;
   }

   // -------------------------------------------------------------------------
   // 0xCF - IRET - Return from Interrupt
   // -------------------------------------------------------------------------
   private void OpCode_0xCF() {
      _clock.incClockCounter((byte) 44);
      _registers.IP = Pop();
      _registers.CS = Pop();

      int retAddr = (_registers.CS << 4) + _registers.IP;
      _instLogger.debug("     return to addr: 0x{0:X5}", retAddr);

      _registers.Flags = (int) (0x0FD5 & Pop());

      _biu.setPrefixFlags((byte) 0);
      _pfqInAddress = _registers.IP;
      prefetch_queue_count = 0;

      _pauseInterrupts = true;
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   // Logic Opcodes
   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   // ------------------------------------------------------
   // Boolean - OR
   // ------------------------------------------------------
   private int BooleanOr(int localDataA, int localDataB) {
      int localResults = (int) (localDataA | localDataB);
      if (_wordOperation == 1)
         Set_Flags_Word_SZP(localResults);
      else
         Set_Flags_Byte_SZP((byte) localResults);
      _registers.Flags = (int) (_registers.Flags & 0xF7FE); // Zero out Flags: C, O
      return localResults;
   }

   // -------------------------------------------------------------------------
   // 0x08 OR - REG8/MEM8,REG8
   // -------------------------------------------------------------------------
   private void OpCode_0x08() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 16);
      WriteBackEa(BooleanOr(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x09 OR - REG16/MEM16,REG16
   // -------------------------------------------------------------------------
   private void OpCode_0x09() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 24);
      WriteBackEa(BooleanOr(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x0A OR - REG8,REG8/MEM8
   // -------------------------------------------------------------------------
   private void OpCode_0x0A() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 9);
      WriteRegister(_regFieldTable, BooleanOr(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x0B OR - REG16,REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x0B() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 13);
      WriteRegister(_regFieldTable, BooleanOr(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x0C OR - AL,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0x0C() {
      _clock.incClockCounter((byte) 4);
      _wordOperation = 0;
      WriteRegister(REG_AL, BooleanOr(_registers.AX, PfqFetchByte()));
   }

   // -------------------------------------------------------------------------
   // 0x0D OR - AX,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0x0D() {
      _clock.incClockCounter((byte) 4);
      _wordOperation = 1;
      _registers.AX = BooleanOr(_registers.AX, PfqFetchWord());
   }

   // ------------------------------------------------------
   // Boolean - AND
   // ------------------------------------------------------
   private int BooleanAnd(int localDataA, int localDataB) {
      int localResults = (int) (localDataA & localDataB);

      if (_wordOperation == 1)
         Set_Flags_Word_SZP(localResults);
      else
         Set_Flags_Byte_SZP((byte) localResults);
      _registers.Flags = (int) (_registers.Flags & 0xF7FE); // Zero out Flags: C, O
      return localResults;
   }

   // -------------------------------------------------------------------------
   // 0x20 AND - REG8/MEM8,REG8
   // -------------------------------------------------------------------------
   private void OpCode_0x20() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 16);
      WriteBackEa(BooleanAnd(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x21 AND - REG16/MEM16,REG16
   // -------------------------------------------------------------------------
   private void OpCode_0x21() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 24);
      WriteBackEa(BooleanAnd(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x22 AND - REG8,REG8/MEM8
   // -------------------------------------------------------------------------
   private void OpCode_0x22() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 9);
      WriteRegister(_regFieldTable, BooleanAnd(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x23 AND - REG16,REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x23() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 13);
      WriteRegister(_regFieldTable, BooleanAnd(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x24 AND - AL,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0x24() {
      _clock.incClockCounter((byte) 4);
      _wordOperation = 0;
      WriteRegister(REG_AL, BooleanAnd(_registers.AX, PfqFetchByte()));
   }

   // -------------------------------------------------------------------------
   // 0x25 AND - AX,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0x25() {
      _clock.incClockCounter((byte) 4);
      _wordOperation = 1;
      _registers.AX = BooleanAnd(_registers.AX, PfqFetchWord());
   }

   // ------------------------------------------------------
   // Boolean - XOR
   // ------------------------------------------------------

   private int BooleanXor(int localDataA, int localDataB) {
      int localResults = (int) (localDataA ^ localDataB);
      if (_wordOperation == 1)
         Set_Flags_Word_SZP(localResults);
      else
         Set_Flags_Byte_SZP((byte) localResults);
      _registers.Flags = (int) (_registers.Flags & 0xF7FE); // Zero out Flags: C, O
      return localResults;
   }

   // -------------------------------------------------------------------------
   // 0x30 XOR - REG8/MEM8,REG8
   // -------------------------------------------------------------------------
   private void OpCode_0x30() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 16);
      WriteBackEa(BooleanXor(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x31 XOR - REG16/MEM16,REG16
   // -------------------------------------------------------------------------
   private void OpCode_0x31() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 24);
      WriteBackEa(BooleanXor(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x32 XOR - REG8,REG8/MEM8
   // -------------------------------------------------------------------------
   private void OpCode_0x32() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 9);
      WriteRegister(_regFieldTable, BooleanXor(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x33 XOR - REG16,REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x33() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 13);
      WriteRegister(_regFieldTable, BooleanXor(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // 0x34 XOR - AL,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0x34() {
      _clock.incClockCounter((byte) 4);
      _wordOperation = 0;
      WriteRegister(REG_AL, BooleanXor(_registers.AX, PfqFetchByte()));
   }

   // -------------------------------------------------------------------------
   // 0x35 XOR - AX,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0x35() {
      _clock.incClockCounter((byte) 4);
      _wordOperation = 1;
      _registers.AX = BooleanXor(_registers.AX, PfqFetchWord());
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   // Math Opcodes
   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   // -------------------------------------------------------------------------
   // 0x00 - ADD REG8/MEM8,REG8
   // -------------------------------------------------------------------------
   private void OpCode_0x00() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 16);
      WriteBackEa(AddBytes((byte) FetchEa(), (byte) FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x01 - ADD REG16/MEM16,REG16
   // -------------------------------------------------------------------------
   private void OpCode_0x01() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 24);
      WriteBackEa(AddWords(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x02 - ADD REG8,REG8/MEM8
   // -------------------------------------------------------------------------
   private void OpCode_0x02() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 9);
      WriteRegister(_regFieldTable, AddBytes((byte) FetchEa(), (byte) FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x03 - ADD REG16,REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x03() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 13);
      WriteRegister(_regFieldTable, AddWords(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x04 - ADD AL , IMMED8
   // -------------------------------------------------------------------------
   private void OpCode_0x04() {
      _clock.incClockCounter((byte) 4);
      WriteRegister(REG_AL, AddBytes((byte) _registers.AX, PfqFetchByte()));
   }

   // -------------------------------------------------------------------------
   // # 0x05 - ADD AX , IMMED16
   // -------------------------------------------------------------------------
   private void OpCode_0x05() {
      _clock.incClockCounter((byte) 4);
      _registers.AX = AddWords(_registers.AX, PfqFetchWord());
   }

   // -------------------------------------------------------------------------
   // # 0x10 - ADC REG8/MEM8,REG8
   // -------------------------------------------------------------------------
   private void OpCode_0x10() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 16);
      with_carry = 1;
      WriteBackEa(AddBytes((byte) FetchEa(), (byte) FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x11 - ADC REG16/MEM16,REG16
   // -------------------------------------------------------------------------
   private void OpCode_0x11() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 24);
      with_carry = 1;
      WriteBackEa(AddWords(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x12 - ADC REG8,REG8/MEM8
   // -------------------------------------------------------------------------
   private void OpCode_0x12() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 9);
      with_carry = 1;
      WriteRegister(_regFieldTable, AddBytes((byte) FetchEa(), (byte) FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x13 - ADC REG16,REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x13() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 13);
      with_carry = 1;
      WriteRegister(_regFieldTable, AddWords(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x14 - ADC AL , IMMED8
   // -------------------------------------------------------------------------
   private void OpCode_0x14() {
      _clock.incClockCounter((byte) 4);
      with_carry = 1;
      WriteRegister(REG_AL, AddBytes((byte) _registers.AX, PfqFetchByte()));
   }

   // -------------------------------------------------------------------------
   // # 0x15 - ADC AX , IMMED16
   // -------------------------------------------------------------------------
   private void OpCode_0x15() {
      _clock.incClockCounter((byte) 4);
      with_carry = 1;
      _registers.AX = AddWords(_registers.AX, PfqFetchWord());
   }

   // -------------------------------------------------------------------------
   // # 0x18 - SBB REG8/MEM8,REG8
   // -------------------------------------------------------------------------
   private void OpCode_0x18() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 16);
      with_carry = 1;
      WriteBackEa(SubBytes((byte) FetchEa(), (byte) FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x19 - SBB REG16/MEM16,REG16
   // -------------------------------------------------------------------------
   private void OpCode_0x19() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 24);
      with_carry = 1;
      WriteBackEa(SubWords(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x1A - SBB REG8,REG8/MEM8
   // -------------------------------------------------------------------------
   private void OpCode_0x1A() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 9);
      with_carry = 1;
      WriteRegister(_regFieldTable, SubBytes((byte) FetchRegister(_regFieldTable), (byte) FetchEa()));
   }

   // -------------------------------------------------------------------------
   // # 0x1B - SBB REG16,REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x1B() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 13);
      with_carry = 1;
      WriteRegister(_regFieldTable, SubWords(FetchRegister(_regFieldTable), FetchEa()));
   }

   // -------------------------------------------------------------------------
   // # 0x1C - SBB AL , IMMED8
   // -------------------------------------------------------------------------
   private void OpCode_0x1C() {
      _clock.incClockCounter((byte) 4);
      with_carry = 1;
      WriteRegister(REG_AL, SubBytes((byte) _registers.AX, PfqFetchByte()));
   }

   // -------------------------------------------------------------------------
   // # 0x1D - SBB AX , IMMED16
   // -------------------------------------------------------------------------
   private void OpCode_0x1D() {
      _clock.incClockCounter((byte) 4);
      with_carry = 1;
      _registers.AX = SubWords(_registers.AX, PfqFetchWord());
   }

   // -------------------------------------------------------------------------
   // # 0x28 - SUB REG8/MEM8,REG8
   // -------------------------------------------------------------------------
   private void OpCode_0x28() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 16);
      WriteBackEa(SubBytes((byte) FetchEa(), (byte) FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x29 - SUB REG16/MEM16,REG16
   // -------------------------------------------------------------------------
   private void OpCode_0x29() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 24);
      WriteBackEa(SubWords(FetchEa(), FetchRegister(_regFieldTable)));
   }

   // -------------------------------------------------------------------------
   // # 0x2A - SUB REG8,REG8/MEM8
   // -------------------------------------------------------------------------
   private void OpCode_0x2A() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 9);
      WriteRegister(_regFieldTable, SubBytes((byte) FetchRegister(_regFieldTable), (byte) FetchEa()));
   }

   // -------------------------------------------------------------------------
   // # 0x2B - SUB REG16,REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x2B() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 13);
      WriteRegister(_regFieldTable, SubWords(FetchRegister(_regFieldTable), FetchEa()));
   }

   // -------------------------------------------------------------------------
   // # 0x2C - SUB AL , IMMED8
   // -------------------------------------------------------------------------
   private void OpCode_0x2C() {
      _clock.incClockCounter((byte) 4);
      WriteRegister(REG_AL, SubBytes((byte) _registers.AX, PfqFetchByte()));
   }

   // -------------------------------------------------------------------------
   // # 0x2D - SUB AX , IMMED16
   // -------------------------------------------------------------------------
   private void OpCode_0x2D() {
      _clock.incClockCounter((byte) 4);
      _registers.AX = SubWords(_registers.AX, PfqFetchWord());
   }

   // -------------------------------------------------------------------------
   // # 0x38 - CMP REG8/MEM8,REG8
   // -------------------------------------------------------------------------
   private void OpCode_0x38() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 9);
      SubBytes((byte) FetchEa(), (byte) FetchRegister(_regFieldTable));
   }

   // -------------------------------------------------------------------------
   // # 0x39 - CMP REG16/MEM16,REG16
   // -------------------------------------------------------------------------
   private void OpCode_0x39() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 13);
      SubWords(FetchEa(), FetchRegister(_regFieldTable));
   }

   // -------------------------------------------------------------------------
   // # 0x3A - CMP REG8,REG8/MEM8
   // -------------------------------------------------------------------------
   private void OpCode_0x3A() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 9);
      SubBytes((byte) FetchRegister(_regFieldTable), (byte) FetchEa());
   }

   // -------------------------------------------------------------------------
   // # 0x3B - CMP REG16,REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x3B() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 13);
      SubWords(FetchRegister(_regFieldTable), FetchEa());
   }

   // -------------------------------------------------------------------------
   // # 0x3C - CMP AL , IMMED8
   // -------------------------------------------------------------------------
   private void OpCode_0x3C() {
      _clock.incClockCounter((byte) 4);
      SubBytes((byte) _registers.AX, PfqFetchByte());
   }

   // -------------------------------------------------------------------------
   // # 0x3D - CMP AX , IMMED16
   // -------------------------------------------------------------------------
   private void OpCode_0x3D() {
      _clock.incClockCounter((byte) 4);
      SubWords(_registers.AX, PfqFetchWord());
   }

   // ------------------------------------------------------
   // INC Registers
   // ------------------------------------------------------

   // -------------------------------------------------------------------------
   // # 0x40 - INC AX
   // -------------------------------------------------------------------------
   private void OpCode_0x40() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.AX = AddWords(_registers.AX, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x41 - INC CX
   // -------------------------------------------------------------------------
   private void OpCode_0x41() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.CX = AddWords(_registers.CX, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x42 - INC DX
   // -------------------------------------------------------------------------
   private void OpCode_0x42() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.DX = AddWords(_registers.DX, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x43 - INC BX
   // -------------------------------------------------------------------------
   private void OpCode_0x43() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.BX = AddWords(_registers.BX, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x44 - INC SP
   // -------------------------------------------------------------------------
   private void OpCode_0x44() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.SP = AddWords(_registers.SP, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x45 - INC BP
   // -------------------------------------------------------------------------
   private void OpCode_0x45() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.BP = AddWords(_registers.BP, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x46 - INC SI
   // -------------------------------------------------------------------------
   private void OpCode_0x46() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.SI = AddWords(_registers.SI, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x47 - INC DI
   // -------------------------------------------------------------------------
   private void OpCode_0x47() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.DI = AddWords(_registers.DI, 0x1);
   }

   // ------------------------------------------------------
   // DEC Registers
   // ------------------------------------------------------

   // -------------------------------------------------------------------------
   // # 0x48 - DEC AX
   // -------------------------------------------------------------------------
   private void OpCode_0x48() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.AX = SubWords(_registers.AX, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x49 - DEC CX
   // -------------------------------------------------------------------------
   private void OpCode_0x49() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.CX = SubWords(_registers.CX, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x4A - DEC DX
   // -------------------------------------------------------------------------
   private void OpCode_0x4A() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.DX = SubWords(_registers.DX, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x4B - DEC BX
   // -------------------------------------------------------------------------
   private void OpCode_0x4B() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.BX = SubWords(_registers.BX, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x4C - DEC SP
   // -------------------------------------------------------------------------
   private void OpCode_0x4C() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.SP = SubWords(_registers.SP, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x4D - DEC BP
   // -------------------------------------------------------------------------
   private void OpCode_0x4D() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.BP = SubWords(_registers.BP, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x4E - DEC SI
   // -------------------------------------------------------------------------
   private void OpCode_0x4E() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.SI = SubWords(_registers.SI, 0x1);
   }

   // -------------------------------------------------------------------------
   // # 0x4F - DEC DI
   // -------------------------------------------------------------------------
   private void OpCode_0x4F() {
      _clock.incClockCounter((byte) 2);
      incDec = 1;
      _registers.DI = SubWords(_registers.DI, 0x1);
   }

   // -------------------------------------------------------------------------
   // 0xFE Opcodes
   // -------------------------------------------------------------------------
   private void OpCode_0xFE() {
      CalculateEa();
      switch (_regField) {
         case 0x0:
            _wordOperation = 0;
            incDec = 1;
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 3);
            else
               _clock.incClockCounter((byte) 15);
            WriteBackEa(AddBytes((byte) FetchEa(), (byte) 0x1));
            break; // # 0xFE REG[0] - INC REG8/MEM8
         case 0x1:
            _wordOperation = 0;
            incDec = 1;
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 3);
            else
               _clock.incClockCounter((byte) 15);
            WriteBackEa(SubBytes((byte) FetchEa(), (byte) 0x1));
            break; // # 0xFE REG[1] - DEC REG8/MEM8
         case 0x2:
            _clock.incClockCounter((byte) 21);
            _wordOperation = 1;
            Push(_registers.IP);
            _registers.IP = FetchEa();
            _pfqInAddress = _registers.IP;
            prefetch_queue_count = 0;
            break; // # 0xFF REG[2] - CALL REG16/MEM16 Intra-segment ** Repeated **
         case 0x3:
            _clock.incClockCounter((byte) 37);
            _wordOperation = 1;
            Push(_registers.CS);
            Push(_registers.IP);
            _registers.IP = FetchEa();
            prefetch_queue_count = 0;
            _eaAddress += 2;
            _registers.CS = FetchEa();
            break; // # 0xFF REG[3] - CALL MEM16 Inter-segment ** Repeated **
         case 0x4:
            _clock.incClockCounter((byte) 18);
            _wordOperation = 1;
            _registers.IP = FetchEa();
            _pfqInAddress = _registers.IP;
            prefetch_queue_count = 0;
            break; // # 0xFF REG[4] - JMP REG16/MEM16 Intra-segment ** Repeated **
         case 0x5:
            _clock.incClockCounter((byte) 24);
            _wordOperation = 1;
            _registers.IP = FetchEa();
            prefetch_queue_count = 0;
            _eaAddress += 2;
            _registers.CS = FetchEa();
            break; // # 0xFF REG[5] - JMP MEM16 Inter-segment ** Repeated **
         case 0x6:
            _clock.incClockCounter((byte) 16);
            _wordOperation = 1;
            Push(FetchEa());
            break; // # 0xFF REG[6] - PUSH MEM16 ** Repeated **
         case 0x7:
            _clock.incClockCounter((byte) 16);
            _wordOperation = 1;
            Push(FetchEa());
            break; // # 0xFF REG[7] - PUSH MEM16 ** Repeated **
      }
   }

   // -------------------------------------------------------------------------
   // 0xFF Opcodes
   // -------------------------------------------------------------------------
   private void OpCode_0xFF() {
      CalculateEa();
      switch (_regField) {
         case 0x0:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 3);
            else
               _clock.incClockCounter((byte) 23);
            incDec = 1;
            WriteBackEa(AddWords(FetchEa(), 0x1));
            break; // # 0xFF REG[0] - INC MEM16
         case 0x1:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 3);
            else
               _clock.incClockCounter((byte) 23);
            incDec = 1;
            WriteBackEa(SubWords(FetchEa(), 0x1));
            break; // # 0xFF REG[1] - DEC MEM16
         case 0x2:
            _clock.incClockCounter((byte) 29);
            Push(_registers.IP);
            _registers.IP = FetchEa();
            _pfqInAddress = _registers.IP;
            prefetch_queue_count = 0;
            break; // # 0xFF REG[2] - CALL REG16/MEM16 Intra-segment
         case 0x3:
            _clock.incClockCounter((byte) 53);
            Push(_registers.CS);
            Push(_registers.IP);
            _registers.IP = FetchEa();
            _eaAddress += 2;
            _registers.CS = FetchEa();
            _pfqInAddress = _registers.IP;
            prefetch_queue_count = 0;
            break; // # 0xFF REG[3] - CALL MEM16 Inter-segment
         case 0x4:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 11);
            else
               _clock.incClockCounter((byte) 22);
            _registers.IP = FetchEa();
            _pfqInAddress = _registers.IP;
            prefetch_queue_count = 0;
            break; // # 0xFF REG[4] - JMP REG16/MEM16 Intra-segment
         case 0x5:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 11);
            else
               _clock.incClockCounter((byte) 24);
            _registers.IP = FetchEa();
            _eaAddress += 2;
            _registers.CS = FetchEa();
            _pfqInAddress = _registers.IP;
            prefetch_queue_count = 0;
            break; // # 0xFF REG[5] - JMP MEM16 Inter-segment
         case 0x6:
            _clock.incClockCounter((byte) 24);
            Push(FetchEa());
            break; // # 0xFF REG[6] - PUSH MEM16
         case 0x7:
            _clock.incClockCounter((byte) 24);
            Push(FetchEa());
            break; // # 0xFF REG[7] - PUSH MEM16 ** Repeated **
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   // Misc Opcodes
   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   // -------------------------------------------------------------------------
   // 0x90 - NOP
   // -------------------------------------------------------------------------
   private void OpCode_0x90() {
      _clock.setClockCounter((byte) 3);
   }

   // =========================================================================
   // Set Flags
   // =========================================================================

   // -------------------------------------------------------------------------
   // 0xF8 - CLC - Clear Carry Flag
   // -------------------------------------------------------------------------
   private void OpCode_0xF8() {
      _clock.setClockCounter((byte) 2);
      _registers.Flags &= 0xFFFE;
   }

   // -------------------------------------------------------------------------
   // 0xF5 - CMC - Complement Carry Flag
   // -------------------------------------------------------------------------
   private void OpCode_0xF5() {
      _clock.setClockCounter((byte) 2);
      _registers.Flags ^= 0x0001;
   }

   // -------------------------------------------------------------------------
   // 0xF9 - STC - Set Carry Flag
   // -------------------------------------------------------------------------
   private void OpCode_0xF9() {
      _clock.setClockCounter((byte) 2);
      _registers.Flags |= 0x0001;
   }

   // -------------------------------------------------------------------------
   // 0xFC - CLD - Clear Direction Flag
   // -------------------------------------------------------------------------
   private void OpCode_0xFC() {
      _clock.setClockCounter((byte) 2);
      _registers.Flags &= 0xFBFF;
   }

   // -------------------------------------------------------------------------
   // 0xFD - STD - Set Direction Flag
   // -------------------------------------------------------------------------
   private void OpCode_0xFD() {
      _clock.setClockCounter((byte) 2);
      _registers.Flags |= 0x0400;
   }

   // -------------------------------------------------------------------------
   // 0xFA - CLI - Clear Interrupt Flag
   // -------------------------------------------------------------------------
   private void OpCode_0xFA() {
      _clock.setClockCounter((byte) 2);
      _registers.Flags &= 0xFDFF;
   }

   // -------------------------------------------------------------------------
   // 0xFB - STI - Set Interrupt Flag
   // -------------------------------------------------------------------------
   private void OpCode_0xFB() {
      _clock.setClockCounter((byte) 2);
      _registers.Flags |= 0x0200;
      _lastInstrSetPrefix = true;
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0x9F - LAHF - Load 8080 Flags into AH
   // -------------------------------------------------------------------------
   private void OpCode_0x9F() {
      _clock.setClockCounter((byte) 4);
      _registers.AX = (int) ((_registers.Flags << 8) | (_registers.AX & 0x00FF));
      _registers.AX = (int) (_registers.AX | 0x0200);
   }

   // -------------------------------------------------------------------------
   // 0x9E - SAHF - Store AH into 8080 Flags
   // -------------------------------------------------------------------------
   private void OpCode_0x9E() {
      _clock.setClockCounter((byte) 4);
      _registers.Flags = (int) ((_registers.Flags & 0xFF00) | ((_registers.AX & 0xD500) >> 8));
   }

   // =========================================================================
   // Set Prefixes
   // =========================================================================

   // -------------------------------------------------------------------------
   // 0xF0 - LOCK Prefix
   // -------------------------------------------------------------------------
   private void OpCode_0xF0() {
      _clock.setClockCounter((byte) 2);
      _biu.orEqPrefixFlags((byte) 0x01);
      _lastInstrSetPrefix = true;
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0xF2 - REPNZ Prefix
   // -------------------------------------------------------------------------
   private void OpCode_0xF2() {
      _clock.setClockCounter((byte) 2);
      _biu.orEqPrefixFlags((byte) 0x02);
      _lastInstrSetPrefix = true;
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0xF3 - REPZ Prefix
   // -------------------------------------------------------------------------
   private void OpCode_0xF3() {
      _clock.setClockCounter((byte) 2);
      _biu.orEqPrefixFlags((byte) 0x04);
      _lastInstrSetPrefix = true;
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0x26 - Segment Override Prefix - ES
   // -------------------------------------------------------------------------
   private void OpCode_0x26() {
      _clock.setClockCounter((byte) 2);
      _biu.orEqPrefixFlags((byte) 0x10);
      _lastInstrSetPrefix = true;
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0x2E - Segment Override Prefix - CS
   // -------------------------------------------------------------------------
   private void OpCode_0x2E() {
      _clock.setClockCounter((byte) 2);
      _biu.orEqPrefixFlags((byte) 0x20);
      _lastInstrSetPrefix = true;
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0x36 - Segment Override Prefix - SS
   // -------------------------------------------------------------------------
   private void OpCode_0x36() {
      _clock.setClockCounter((byte) 2);
      _biu.orEqPrefixFlags((byte) 0x40);
      _lastInstrSetPrefix = true;
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0x3E - Segment Override Prefix - DS
   // -------------------------------------------------------------------------
   private void OpCode_0x3E() {
      _clock.setClockCounter((byte) 2);
      _biu.orEqPrefixFlags((byte) 0x80);
      _lastInstrSetPrefix = true;
      _pauseInterrupts = true;
   }

   // -------------------------------------------------------------------------
   // 0x98 - CBW - Sign extend AL into AH
   // -------------------------------------------------------------------------
   private void OpCode_0x98() {
      _clock.setClockCounter((byte) 2);
      _registers.AX = SignExtendedByte(_registers.AX);
   }

   // -------------------------------------------------------------------------
   // 0x99 - CWD - Sign extend AX into DX
   // -------------------------------------------------------------------------
   private void OpCode_0x99() {
      _clock.setClockCounter((byte) 5);
      _registers.DX = (0x8000 & _registers.AX) > 0 ? (int) 0xFFFF : (int) 0x0000;
   }

   // =========================================================================
   // Interrupts
   // =========================================================================

   // -------------------------------------------------------------------------
   // 0xCC - INT - Interrupt Type 3 - Breakpoint
   // -------------------------------------------------------------------------
   private void OpCode_0xCC() {
      _clock.incClockCounter((byte) 1);
      Interrupt_Handler((byte) 0x3);
   }

   // -------------------------------------------------------------------------
   // 0xCE - INTO - Interrupt Type 4 - (Interrupt on Overflow)
   // -------------------------------------------------------------------------
   private void OpCode_0xCE() {
      if (Flag_o() == 0) {
         _clock.incClockCounter((byte) 4);
      } else {
         _clock.incClockCounter((byte) 2);
         Interrupt_Handler((byte) 0x4);
      }
   }

   // -------------------------------------------------------------------------
   // [0xCD 0xnn] - Interrupt Type specified in second byte
   // -------------------------------------------------------------------------
   private void OpCode_0xCD() {
      byte intNum = PfqFetchByte();
      Interrupt_Handler(intNum);
   }

   // -------------------------------------------------------------------------
   // 0x80 Opcodes ** Duplicated on 0x82 **
   // -------------------------------------------------------------------------
   private void OpCode_0x80() {
      CalculateEa();
      switch (_regField) {
         case 0x0:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 17);
            WriteBackEa(AddBytes((byte) FetchEa(), PfqFetchByte()));
            break; // # 0x80 REG[0] - ADD REG8/MEM8 , IMMED8
         case 0x1:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 17);
            WriteBackEa(BooleanOr(FetchEa(), PfqFetchByte()));
            break; // # 0x80 REG[1] - OR REG8/MEM8 , IMMED8
         case 0x2:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 17);
            with_carry = 1;
            WriteBackEa(AddBytes((byte) FetchEa(), PfqFetchByte()));
            break; // # 0x80 REG[2] - ADC REG8/MEM8 , IMMED8
         case 0x3:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 17);
            with_carry = 1;
            WriteBackEa(SubBytes((byte) FetchEa(), PfqFetchByte()));
            break; // # 0x80 REG[3] - SBB REG8/MEM8 , IMMED8
         case 0x4:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 17);
            WriteBackEa(BooleanAnd(FetchEa(), PfqFetchByte()));
            break; // # 0x80 REG[4] - AND REG8/MEM8 , IMMED8
         case 0x5:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 17);
            WriteBackEa(SubBytes((byte) FetchEa(), PfqFetchByte()));
            break; // # 0x80 REG[5] - SUB REG8/MEM8 , IMMED8
         case 0x6:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 17);
            WriteBackEa(BooleanXor(FetchEa(), PfqFetchByte()));
            break; // # 0x80 REG[6] - XOR REG8/MEM8 , IMMED8
         case 0x7:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 3);
            else
               _clock.incClockCounter((byte) 10);
            SubBytes((byte) FetchEa(), PfqFetchByte());
            break; // # 0x80 REG[7] - CMP REG8/MEM8 , IMMED8
      }
   }

   // -------------------------------------------------------------------------
   // 0x81 Opcodes
   // -------------------------------------------------------------------------
   private void OpCode_0x81() {
      CalculateEa();
      switch (_regField) {
         case 0x0:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            WriteBackEa(AddWords(FetchEa(), PfqFetchWord()));
            break; // # 0x81 REG[0] - ADD REG16/MEM16 , IMMED16
         case 0x1:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            WriteBackEa(BooleanOr(FetchEa(), PfqFetchWord()));
            break; // # 0x81 REG[1] - OR REG16/MEM16 , IMMED16
         case 0x2:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            with_carry = 1;
            WriteBackEa(AddWords(FetchEa(), PfqFetchWord()));
            break; // # 0x81 REG[2] - ADC REG16/MEM16 , IMMED16
         case 0x3:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            with_carry = 1;
            WriteBackEa(SubWords(FetchEa(), PfqFetchWord()));
            break; // # 0x81 REG[3] - SBB REG16/MEM16 , IMMED16
         case 0x4:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            WriteBackEa(BooleanAnd(FetchEa(), PfqFetchWord()));
            break; // # 0x81 REG[4] - AND REG16/MEM16 , IMMED16
         case 0x5:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            WriteBackEa(SubWords(FetchEa(), PfqFetchWord()));
            break; // # 0x81 REG[5] - SUB REG16/MEM16 , IMMED16
         case 0x6:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            WriteBackEa(BooleanXor(FetchEa(), PfqFetchWord()));
            break; // # 0x81 REG[6] - XOR REG16/MEM16 , IMMED16
         case 0x7:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 14);
            SubWords(FetchEa(), PfqFetchWord());
            break; // # 0x81 REG[7] - CMP REG16/MEM16 , IMMED16
      }
   }

   // -------------------------------------------------------------------------
   // 0x83 Opcodes
   // -------------------------------------------------------------------------
   private void OpCode_0x83() {
      CalculateEa();
      switch (_regField) {
         case 0x0:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            WriteBackEa(AddWords(FetchEa(), SignExtendedByte(PfqFetchByte())));
            break; // # 0x83 REG[0] - ADD REG16/MEM16 , IMMED8-Sign_Extended
         case 0x1:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            WriteBackEa(BooleanOr(FetchEa(), SignExtendedByte(PfqFetchByte())));
            break; // # 0x83 REG[1] - OR REG16/MEM16 , IMMED8-Sign_Extended
         case 0x2:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            with_carry = 1;
            WriteBackEa(AddWords(FetchEa(), SignExtendedByte(PfqFetchByte())));
            break; // # 0x83 REG[2] - ADC REG16/MEM16 , IMMED8-Sign_Extended
         case 0x3:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            with_carry = 1;
            WriteBackEa(SubWords(FetchEa(), SignExtendedByte(PfqFetchByte())));
            break; // # 0x83 REG[3] - SBB REG16/MEM16 , IMMED8-Sign_Extended
         case 0x4:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            WriteBackEa(BooleanAnd(FetchEa(), SignExtendedByte(PfqFetchByte())));
            break; // # 0x83 REG[4] - AND REG16/MEM16 , IMMED8-Sign_Extended
         case 0x5:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            WriteBackEa(SubWords(FetchEa(), SignExtendedByte(PfqFetchByte())));
            break; // # 0x83 REG[5] - SUB REG16/MEM16 , IMMED8-Sign_Extended
         case 0x6:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 23);
            WriteBackEa(BooleanXor(FetchEa(), SignExtendedByte(PfqFetchByte())));
            break; // # 0x83 REG[6] - XOR REG16/MEM16 , IMMED8-Sign_Extended
         case 0x7:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 4);
            else
               _clock.incClockCounter((byte) 14);
            SubWords(FetchEa(), SignExtendedByte(PfqFetchByte()));
            break; // # 0x83 REG[7] - CMP REG16/MEM16 , IMMED8-Sign_Extended
      }
   }

   // =========================================================================
   // TEST Opcodes
   // =========================================================================

   // -------------------------------------------------------------------------
   // 0xA8 TEST - AL,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0xA8() {
      _clock.incClockCounter((byte) 4);
      _wordOperation = 0;
      BooleanAnd(_registers.AX, PfqFetchByte());
   }

   // -------------------------------------------------------------------------
   // 0xA9 TEST - AX,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0xA9() {
      _clock.incClockCounter((byte) 4);
      _wordOperation = 1;
      BooleanAnd(_registers.AX, PfqFetchWord());
   }

   // -------------------------------------------------------------------------
   // 0x84 TEST - REG8/MEM8 , REG8
   // -------------------------------------------------------------------------
   private void OpCode_0x84() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 9);
      BooleanAnd(FetchEa(), FetchRegister(_regFieldTable));
   }

   // -------------------------------------------------------------------------
   // 0x85 TEST - REG16/MEM16,REG16
   // -------------------------------------------------------------------------
   private void OpCode_0x85() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 3);
      else
         _clock.incClockCounter((byte) 13);
      BooleanAnd(FetchEa(), FetchRegister(_regFieldTable));
   }

   // -------------------------------------------------------------------------
   // 0xF6 Opcodes
   // -------------------------------------------------------------------------

   private void OpCode_0xF6() {
      CalculateEa();
      switch (_regField) {
         case 0x0:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 5);
            else
               _clock.incClockCounter((byte) 11);
            BooleanAnd(FetchEa(), PfqFetchByte());
            break; // # 0xF6 REG[0] - TEST REG8/MEM8,IMM8
         case 0x1:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 5);
            else
               _clock.incClockCounter((byte) 11);
            BooleanAnd(FetchEa(), PfqFetchByte());
            break; // # 0xF6 REG[1] - TEST REG8/MEM8,IMM8 ** Duplicate **
         case 0x2:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 3);
            else
               _clock.incClockCounter((byte) 16);
            WriteBackEa((int) (0xFF ^ FetchEa()));
            break; // # 0xF6 REG[2] - NOT REG8/MEM8
         case 0x3:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 3);
            else
               _clock.incClockCounter((byte) 16);
            WriteBackEa(SubBytes((byte) 0x00, (byte) FetchEa()));
            break; // # 0xF6 REG[3] - NEG REG8/MEM8

         case 0x4:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 65);
            else
               _clock.incClockCounter((byte) 69); // # 0xF6 REG[4] - MUL incR(MEM)8
            _registers.AX = (int) (FetchEa() * (_registers.AX & 0x00FF));
            if ((_registers.AX & 0xFF00) != 0) {
               _registers.Flags |= 0x0801;
            } // Set O,C flags
            else {
               _registers.Flags &= 0xF7FE; // Clear O,C flags
            }

            _registers.Flags &= 0xFFBF; // Clear zero bit to appear like an 8086
            break;

         case 0x5:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 79);
            else
               _clock.incClockCounter((byte) 85); // # 0xF6 REG[5] - IMUL incR(MEM)8
            _registers.AX = (int) ((short) FetchEa() * (short) (_registers.AX & 0x00FF));
            short tempAx = (short) _registers.AX;
            if (tempAx > 255) {
               _registers.Flags |= 0x0801;
            } // Set O,C flags
            else if (tempAx <= -256) {
               _registers.Flags |= 0x0801;
            } // Set O,C flags
            else {
               _registers.Flags &= 0xF7FE;
            } // Clear O,C flags

            _registers.Flags &= 0xFFBF; // Clear zero bit to appear like an 8086
            break;

         case 0x6:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 75);
            else
               _clock.incClockCounter((byte) 81); // # 0xF6 REG[6] - DIV incR(MEM)8

            byte localDivr = (byte) FetchEa();
            if (localDivr == 0)
               Div0Handler();
            else {
               byte localQuo = (byte) (_registers.AX / localDivr);
               byte localRem = (byte) (_registers.AX % localDivr);
               _registers.AX = (int) (localRem << 8);
               WriteRegister(REG_AL, localQuo);
            }
            break;
         case 0x7:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 97);
            else
               _clock.incClockCounter((byte) 1104); // # 0xF6 REG[7] - IDIV incR(MEM)8

            short signedLocalDivr = (short) FetchEa();
            if (signedLocalDivr == 0)
               Div0Handler();
            else {
               short signedLocalQuo = (short) ((short) _registers.AX / signedLocalDivr);
               short signedLocalRem = (short) ((short) _registers.AX % signedLocalDivr);
               _registers.AX = (int) (signedLocalRem << 8);
               WriteRegister(REG_AL, (int) signedLocalQuo);
            }

            break;
      }
   }

   // -------------------------------------------------------------------------
   // 0xF7 Opcodes
   // -------------------------------------------------------------------------
   private void OpCode_0xF7() {
      int localData;

      CalculateEa();
      switch (_regField) {
         case 0x0:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 5);
            else
               _clock.incClockCounter((byte) 15);
            BooleanAnd(FetchEa(), PfqFetchWord());
            break; // # 0xF7 REG[0] - TEST REG16/MEM16 , IMM16
         case 0x1:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 5);
            else
               _clock.incClockCounter((byte) 15);
            BooleanAnd(FetchEa(), PfqFetchWord());
            break; // # 0xF7 REG[1] - TEST REG16/MEM16 , IMM16 ** Duplicate **
         case 0x2:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 3);
            else
               _clock.incClockCounter((byte) 24);
            WriteBackEa((int) (0xFFFF ^ FetchEa()));
            break; // # 0xF7 REG[2] - NOT REG16/MEM16
         case 0x3:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 3);
            else
               _clock.incClockCounter((byte) 24);
            WriteBackEa(SubWords(0x00, FetchEa()));
            break; // # 0xF7 REG[3] - NEG REG16/MEM16

         case 0x4:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 110);
            else
               _clock.incClockCounter((byte) 121); // # 0xF7 REG[4] - MUL incRE(MEM1)6
            localData = (int) (FetchEa() * _registers.AX);
            _registers.DX = (int) (localData >> 16);
            _registers.AX = (int) (localData & 0x0000FFFF);
            if (_registers.DX != 0) {
               _registers.Flags |= 0x0801;
            } // Set O,C flags
            else {
               _registers.Flags &= 0xF7FE;
            } // Clear O,C flags

            // Set_Flags_Word_SZP(_registers.AX);
            _registers.Flags &= 0xFFBF; // temp clear zero bit
            break;

         case 0x5:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 131);
            else
               _clock.incClockCounter((byte) 137); // # 0xF7 REG[5] - IMUL incRE(MEM1)6
            int fetchEaPositive;
            int fetchAxPositive;

            int localFetchEa = FetchEa();
            if ((0x8000 & localFetchEa) > 0)
               fetchEaPositive = (int) ((localFetchEa ^ 0xffff) + 1);
            else
               fetchEaPositive = localFetchEa;

            if ((0x8000 & _registers.AX) > 0)
               fetchAxPositive = (int) ((_registers.AX ^ 0xffff) + 1);
            else
               fetchAxPositive = _registers.AX;

            int positiveResult = fetchEaPositive * fetchAxPositive;
            if (positiveResult > 0xFFFF)
               _registers.Flags |= 0x0801; // Set O,C flags
            else
               _registers.Flags &= 0xF7FE; // Clear O,C flags

            localData = (int) ((short) localFetchEa * (short) _registers.AX);
            _registers.DX = (int) (localData >> 16);
            _registers.AX = (int) (localData & 0x0000FFFF);

            _registers.Flags &= 0xFFBF; // temp clear zero bit
            break;
         case 0x6:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 143);
            else
               _clock.incClockCounter((byte) 157); // # 0xF7 REG[6] - DIV incRE(MEM1)6
            int localNumr = (int) ((_registers.DX << 16) | _registers.AX);
            int localDivr = FetchEa();
            if (localDivr == 0)
               Div0Handler();
            else {
               _registers.AX = (int) (localNumr / localDivr);
               _registers.DX = (int) (localNumr % localDivr);
            }

            break;
         case 0x7:
            if (_eaIsRegister == 1)
               _clock.incClockCounter((byte) 175);
            else
               _clock.incClockCounter((byte) 181); // # 0xF7 REG[7] - IDIV incRE(MEM1)6
            int signedLocalNumr = (int) (_registers.DX << 16 | _registers.AX);
            int signedLocalDivr = FetchEa();
            if (signedLocalDivr == 0)
               Div0Handler();
            else {
               _registers.AX = (int) (signedLocalNumr / signedLocalDivr);
               _registers.DX = (int) (signedLocalNumr % signedLocalDivr);
            }

            break;
      }
   }

   // -------------------------------------------------------------------------
   // 0x27 - DAA - Decimal Adjust for Addition
   // -------------------------------------------------------------------------
   private void OpCode_0x27() {
      _clock.incClockCounter((byte) 4);
      int localAl = (int) (_registers.AX & 0x00FF);

      if (((0x0F & localAl) > 0x09) || (Flag_a() == 1)) {
         localAl = (int) (localAl + 0x06);
         _registers.Flags |= 0x0010;
      } else
         _registers.Flags &= 0xFFEF; // Set A Flag

      if (((0xFF & localAl) > 0x9F) || (Flag_c() == 1)) {
         localAl += 0x60;
         _registers.Flags |= 0x0001;
      } else
         _registers.Flags &= 0xFFFE; // Set C Flag

      WriteRegister(REG_AL, localAl);
      Set_Flags_Byte_SZP((byte) localAl);
   }

   // -------------------------------------------------------------------------
   // 0x2F - DAS - Decimal Adjust for Subtraction
   // -------------------------------------------------------------------------
   private void OpCode_0x2F() {
      _clock.incClockCounter((byte) 4);
      int localAl = (int) (_registers.AX & 0x00FF);

      if (((0x0F & localAl) > 0x09) || (Flag_a() == 1)) {
         localAl -= 0x06;
         _registers.Flags |= 0x0010;
      } else
         _registers.Flags &= 0xFFEF; // Set A Flag

      if (((0xFF & localAl) > 0x9F) || (Flag_c() == 1)) {
         localAl -= 0x60;
         _registers.Flags |= 0x0001;
      } else
         _registers.Flags &= 0xFFFE; // Set C Flag

      WriteRegister(REG_AL, localAl);
      Set_Flags_Byte_SZP((byte) localAl);
   }

   // -------------------------------------------------------------------------
   // 0x37 - AAA - ASCII Adjust for Addition
   // -------------------------------------------------------------------------
   private void OpCode_0x37() {
      byte localFlagA = Flag_a();

      _clock.incClockCounter((byte) 4);
      int localAl = (int) (_registers.AX & 0x00FF);

      if ((0xF & localAl) > 0x09 || localFlagA == 1) {
         localAl += 0x06; // AL = AL + 0x06
         WriteRegister(REG_AL, localAl); // Update AL
         _registers.AX += 0x0100; // AH = AH + 1
         _registers.Flags |= 0x0010; // Set A Flag
      }

      if (localFlagA == 1)
         _registers.Flags |= 0x0001;
      else
         _registers.Flags &= 0xFFFE; // CF = AF

      _registers.AX &= 0xFF0F; // AL = AL & 0x0F
   }

   // -------------------------------------------------------------------------
   // 0x3F - AAS - ASCII Adjust for Subtraction
   // -------------------------------------------------------------------------
   private void OpCode_0x3F() {
      byte localFlagA = Flag_a();

      _clock.incClockCounter((byte) 4);
      int localAl = (int) (_registers.AX & 0x00FF);

      if (((0xF & localAl) > 0x09) || (localFlagA == 1)) {
         localAl -= 0x06; // AL = AL - 0x06
         _registers.AX -= 0x0100; // AH = AH - 1
         WriteRegister(REG_AL, localAl); // Update AL
         _registers.Flags |= 0x0010; // Set A Flag
      }

      if (localFlagA == 1)
         _registers.Flags |= 0x0001;
      else
         _registers.Flags &= 0xFFFE; // CF = AF

      _registers.AX &= 0xFF0F; // AL = AL & 0x0F
   }

   // -------------------------------------------------------------------------
   // [0xD4 0x0A] - AAM - ASCII Adjust for Multiply
   // -------------------------------------------------------------------------
   private void OpCode_0xD4() {
      _clock.incClockCounter((byte) 83);
      byte opCodeDivisor = PfqFetchByte();
      byte localAl = (byte) (_registers.AX & 0x00FF);

      byte localAh = (byte) (localAl / opCodeDivisor);
      localAl = (byte) (localAl % opCodeDivisor);

      Set_Flags_Byte_SZP(localAl);
      _registers.AX = (int) ((localAh << 8) | localAl);
   }

   // -------------------------------------------------------------------------
   // [0xD5 0x0A] - AAD - ASCII Adjust for Division
   // -------------------------------------------------------------------------
   private void OpCode_0xD5() {
      _clock.incClockCounter((byte) 60);
      byte opCodeMultiplier = PfqFetchByte();

      byte localAh = (byte) (_registers.AX >> 8);
      byte localAl = (byte) (_registers.AX & 0x00FF);

      _registers.AX = (int) (0x00FF & ((localAh * opCodeMultiplier) + localAl));

      Set_Flags_Byte_SZP((byte) _registers.AX);
   }

   // -------------------------------------------------------------------------
   // 0xD7 - XLAT - Translate
   // -------------------------------------------------------------------------
   private void OpCode_0xD7() {
      _clock.incClockCounter((byte) 11);

      int localAl = (int) (_registers.AX & 0x00FF);
      int localAddress = (int) (_registers.BX + localAl);

      // Read data from source
      // var localData = Biu_Operation(BiuOperations.MemReadByte,
      // SEGMENT_OVERRIDABLE_TRUE, SEGMENT_DS, localAddress, 0x00);
      byte localData = _biu.readMemoryByte(SegmentOverridableTrue, SegmentRegs.DS, localAddress);
      WriteRegister(REG_AL, localData);
   }

   // -------------------------------------------------------------------------
   // 0xD8 - 0xDF ESC
   // -------------------------------------------------------------------------
   private void OpCode_0xD8() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 2);
      else
         _clock.incClockCounter((byte) 8);
      FetchEa();
      if ((_opCodeFirstByte & 0x1) != 0)
         _clock.incClockCounter((byte) 4); // For incW(Operation)s
   }

   // -------------------------------------------------------------------------
   // [0xE0 0xdd] - LOOPNZ - Loop While Not Zero
   // -------------------------------------------------------------------------
   private void OpCode_0xE0() {
      _registers.CX--;
      if (Flag_z() == 0 && _registers.CX != 0) {
         _clock.incClockCounter((byte) 3);
         Jump_Taken8();
      } else {
         _clock.incClockCounter((byte) 1);
         Jump_Not_Taken8();
      }
   }

   // -------------------------------------------------------------------------
   // [0xE1 0xdd] - LOOPZ - Loop While Zero
   // -------------------------------------------------------------------------
   private void OpCode_0xE1() {

      _registers.CX--;
      if (Flag_z() == 1 && _registers.CX != 0) {
         _clock.incClockCounter((byte) 2);
         Jump_Taken8();
      } else {
         _clock.incClockCounter((byte) 2);
         Jump_Not_Taken8();
      }
   }

   // -------------------------------------------------------------------------
   // [0xE2 0xdd] - LOOP
   // -------------------------------------------------------------------------
   private void OpCode_0xE2() {

      _registers.CX--;
      if (_registers.CX != 0) {
         _clock.incClockCounter((byte) 1);
         Jump_Taken8();
      } else {
         _clock.incClockCounter((byte) 1);
         Jump_Not_Taken8();
      }
   }

   // -------------------------------------------------------------------------
   // 0xF4 - HLT - Halt until Interrupt or Reset asserted
   // -------------------------------------------------------------------------
   private void OpCode_0xF4() {
      // Tell BIU to send HALT onto S Bits
      // Biu_Operation(BiuOperations.SendHalt, SEGMENT_OVERRIDABLE_FALSE, 0x00, 0x00,
      // 0x00);
      _biu.sendHalt();

      boolean directIntr;
      do {
         _clock.waitForFallingEdge();
         _clock.waitForFallingEdge();

         // if (_deviceAdapter.IntrPin != 0 && Flag_i() == 1) direct_intr = true;
         // else direct_intr = false;

         directIntr = pins.getIntrPin() != 0 && Flag_i() == 1;
      } while (!directIntr && !_nmiLatched.IsSet() && pins.getResetPin() == 0);

      pins.setBusStatusPins(BusStatus.Pass);
      _clock.waitForFallingEdge();
      _clock.waitForFallingEdge();
   }

   // -------------------------------------------------------------------------
   // 0xD6 - SETALC
   // -------------------------------------------------------------------------
   private void OpCode_0xD6() {
      _clock.incClockCounter((byte) 2);
      if (Flag_c() == 0)
         _registers.AX &= 0xFF00;
      else
         _registers.AX |= 0x00FF;
   }

   // -------------------------------------------------------------------------
   // 0x9B - WAIT - Wait until TEST_n is asserted or Reset asserted
   // -------------------------------------------------------------------------
   private void OpCode_0x9B() {
      _clock.incClockCounter((byte) 2);

      boolean directIntr;
      do {
         _clock.waitForFallingEdge();
         // if (_deviceAdapter.IntrPin != 0 && Flag_i() == 1) direct_intr = true;
         // else direct_intr = false;

         directIntr = pins.getIntrPin() != 0 && Flag_i() == 1;
      } while (!directIntr && !_nmiLatched.IsSet() && pins.getResetPin() == 0);
   }

   // -------------------------------------------------------------------------
   // 0x8D LEA - REG16 , MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x8D() {
      _clock.incClockCounter((byte) 2);
      CalculateEa();
      WriteRegister(_regFieldTable, _eaAddress);
   }

   // -------------------------------------------------------------------------
   // 0xC4 - LES - REG16 , MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0xC4() {
      _clock.incClockCounter((byte) 24);
      CalculateEa();
      _wordOperation = 1;
      WriteRegister((int) (0x08 | _regField), FetchEa());
      _eaAddress += 0x02;
      _registers.ES = FetchEa();
   }

   // -------------------------------------------------------------------------
   // 0xC5 - LDS - REG16 , MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0xC5() {
      _clock.incClockCounter((byte) 24);
      CalculateEa();
      _wordOperation = 1;
      WriteRegister((int) (0x08 | _regField), FetchEa());
      _eaAddress += 0x02;
      _registers.DS = FetchEa();
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   // Move Opcodes
   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   // -------------------------------------------------------------------------
   // # 0x88 - MOV REG8/MEM8,REG8
   // -------------------------------------------------------------------------
   private void OpCode_0x88() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 2);
      else
         _clock.incClockCounter((byte) 9);
      WriteBackEa(FetchRegister(_regFieldTable));
   }

   // -------------------------------------------------------------------------
   // # 0x89 - MOV REG16/MEM16,REG16
   // -------------------------------------------------------------------------
   private void OpCode_0x89() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 2);
      else
         _clock.incClockCounter((byte) 13);
      WriteBackEa(FetchRegister(_regFieldTable));
   }

   // -------------------------------------------------------------------------
   // # 0x8A - MOV REG8,REG8/MEM8
   // -------------------------------------------------------------------------
   private void OpCode_0x8A() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 2);
      else
         _clock.incClockCounter((byte) 8);
      WriteRegister(_regFieldTable, FetchEa());
   }

   // -------------------------------------------------------------------------
   // # 0x8B - MOV REG16,REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x8B() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 2);
      else
         _clock.incClockCounter((byte) 12);
      WriteRegister(_regFieldTable, FetchEa());
   }

   // -------------------------------------------------------------------------
   // # 0x8C - MOV REG16/MEM16 , SEGREG
   // -------------------------------------------------------------------------
   private void OpCode_0x8C() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 2);
      else
         _clock.incClockCounter((byte) 13);
      _wordOperation = 1;
      _rmFieldTable = (byte) (_rmField | 0x8);
      WriteBackEa(FetchSegReg(_regField));
   }

   // -------------------------------------------------------------------------
   // # 0x8E - MOV SEGREG , REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x8E() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 2);
      else
         _clock.incClockCounter((byte) 12);
      _wordOperation = 1;
      _rmFieldTable = (byte) (_rmField | 0x8);
      WriteSegReg(_regField, FetchEa());
   }

   // -------------------------------------------------------------------------
   // # 0xA0 - MOV AL , MEM8
   // -------------------------------------------------------------------------
   private void OpCode_0xA0() {
      _clock.incClockCounter((byte) 10);
      // WriteRegister(REG_AL,
      // Biu_Operation(BiuOperations.MemReadByte, SEGMENT_OVERRIDABLE_TRUE,
      // SEGMENT_DS, PfqFetchWord(), 0x00));
      WriteRegister(REG_AL, _biu.readMemoryByte(SegmentOverridableTrue, SegmentRegs.DS, PfqFetchWord()));
   }

   // -------------------------------------------------------------------------
   // # 0xA1 - MOV AX , MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0xA1() {
      _clock.incClockCounter((byte) 14);
      // _registers.AX = Biu_Operation(BiuOperations.MemReadWord,
      // SEGMENT_OVERRIDABLE_TRUE, SEGMENT_DS, PfqFetchWord(), 0x00);
      _registers.AX = _biu.readMemoryWord(SegmentOverridableTrue, SegmentRegs.DS, PfqFetchWord());
   }

   // -------------------------------------------------------------------------
   // # 0xA2 - MOV MEM8 , AL
   // -------------------------------------------------------------------------
   private void OpCode_0xA2() {
      _clock.incClockCounter((byte) 10);
      // Biu_Operation(BiuOperations.MemWriteByte, SEGMENT_OVERRIDABLE_TRUE,
      // SEGMENT_DS, PfqFetchWord(), _registers.AX);
      byte registerAl = (byte) (_registers.AX & 0x00FF);
      _biu.writeMemoryByte(SegmentOverridableTrue, SegmentRegs.DS, PfqFetchWord(), registerAl);
   }

   // -------------------------------------------------------------------------
   // # 0xA2 - MOV MEM16 , AX
   // -------------------------------------------------------------------------
   private void OpCode_0xA3() {
      _clock.incClockCounter((byte) 14);
      // Biu_Operation(BiuOperations.MemWriteWord, SEGMENT_OVERRIDABLE_TRUE,
      // SEGMENT_DS, PfqFetchWord(), _registers.AX);
      _biu.writeMemoryWord(SegmentOverridableTrue, SegmentRegs.DS, PfqFetchWord(), _registers.AX);
   }

   // -------------------------------------------------------------------------
   // # 0xC6 - MOV MEM8 , IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0xC6() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 4);
      else
         _clock.incClockCounter((byte) 10);
      WriteBackEa(PfqFetchByte());
   }

   // -------------------------------------------------------------------------
   // # 0xC7 - MOV MEM16 , IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0xC7() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 4);
      else
         _clock.incClockCounter((byte) 14);
      WriteBackEa(PfqFetchWord());
   }

   // ------------------------------------------------------
   // Moves from Immediate to Register
   // ------------------------------------------------------

   // -------------------------------------------------------------------------
   // [0xB0 0xdd] - MOV AL,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0xB0() {
      _clock.incClockCounter((byte) 4);
      _registers.AX = ((0xFF00 & _registers.AX) | PfqFetchByte());
   }

   // -------------------------------------------------------------------------
   // [0xB1 0xdd] - MOV CL,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0xB1() {
      _clock.incClockCounter((byte) 4);
      _registers.CX = ((0xFF00 & _registers.CX) | PfqFetchByte());
   }

   // -------------------------------------------------------------------------
   // [0xB2 0xdd] - MOV DL,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0xB2() {
      _clock.incClockCounter((byte) 4);
      _registers.DX = ((0xFF00 & _registers.DX) | PfqFetchByte());
   }

   // -------------------------------------------------------------------------
   // [0xB3 0xdd] - MOV BL,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0xB3() {
      _clock.incClockCounter((byte) 4);
      _registers.BX = ((0xFF00 & _registers.BX) | PfqFetchByte());
   }

   // -------------------------------------------------------------------------
   // [0xB4 0xdd] - MOV AH,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0xB4() {
      _clock.incClockCounter((byte) 4);
      _registers.AX = ((PfqFetchByte() << 8) | (0x00FF & _registers.AX));
   }

   // -------------------------------------------------------------------------
   // [0xB5 0xdd] - MOV CH,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0xB5() {
      _clock.incClockCounter((byte) 4);
      _registers.CX = ((PfqFetchByte() << 8) | (0x00FF & _registers.CX));
   }

   // -------------------------------------------------------------------------
   // [0xB6 0xdd] - MOV DH,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0xB6() {
      _clock.incClockCounter((byte) 4);
      _registers.DX = ((PfqFetchByte() << 8) | (0x00FF & _registers.DX));
   }

   // -------------------------------------------------------------------------
   // [0xB7 0xdd] - MOV BH,IMM8
   // -------------------------------------------------------------------------
   private void OpCode_0xB7() {
      _clock.incClockCounter((byte) 4);
      _registers.BX = ((PfqFetchByte() << 8) | (0x00FF & _registers.BX));
   }

   // -------------------------------------------------------------------------
   // [0xB8 0xLO xHI] - MOV AX,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0xB8() {
      _clock.incClockCounter((byte) 4);
      _registers.AX = PfqFetchWord();
   }

   // -------------------------------------------------------------------------
   // [0xB9 0xLO xHI] - MOV CX,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0xB9() {
      _clock.incClockCounter((byte) 4);
      _registers.CX = PfqFetchWord();
   }

   // -------------------------------------------------------------------------
   // [0xBA 0xLO xHI] - MOV DX,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0xBA() {
      _clock.incClockCounter((byte) 4);
      _registers.DX = PfqFetchWord();
   }

   // -------------------------------------------------------------------------
   // [0xBB 0xLO xHI] - MOV BX,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0xBB() {
      _clock.incClockCounter((byte) 4);
      _registers.BX = PfqFetchWord();
   }

   // -------------------------------------------------------------------------
   // [0xBC 0xLO xHI] - MOV SP,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0xBC() {
      _clock.incClockCounter((byte) 4);
      _registers.SP = PfqFetchWord();
   }

   // -------------------------------------------------------------------------
   // [0xBD 0xLO xHI] - MOV BP,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0xBD() {
      _clock.incClockCounter((byte) 4);
      _registers.BP = PfqFetchWord();
   }

   // -------------------------------------------------------------------------
   // [0xBE 0xLO xHI] - MOV SI,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0xBE() {
      _clock.incClockCounter((byte) 4);
      _registers.SI = PfqFetchWord();
   }

   // -------------------------------------------------------------------------
   // [0xBF 0xLO xHI] - MOV DI,IMM16
   // -------------------------------------------------------------------------
   private void OpCode_0xBF() {
      _clock.incClockCounter((byte) 4);
      _registers.DI = PfqFetchWord();
   }

   // ------------------------------------------------------
   // XCHG Opcodes
   // ------------------------------------------------------

   // -------------------------------------------------------------------------
   // 0x86 XCHG - REG8 , REG8/MEM8
   // -------------------------------------------------------------------------
   private void OpCode_0x86() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 4);
      else
         _clock.incClockCounter((byte) 17);
      byte temp8 = (byte) FetchEa();
      WriteBackEa(FetchRegister(_regFieldTable));
      WriteRegister(_regFieldTable, temp8);
   }

   // -------------------------------------------------------------------------
   // 0x87 XCHG - REG16 , REG16/MEM16
   // -------------------------------------------------------------------------
   private void OpCode_0x87() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 4);
      else
         _clock.incClockCounter((byte) 25);
      temp16 = FetchEa();
      WriteBackEa(FetchRegister(_regFieldTable));
      WriteRegister(_regFieldTable, temp16);
   }

   // -------------------------------------------------------------------------
   // 0x91 - XCHG - Exchange Accumulator and CX
   // -------------------------------------------------------------------------
   private void OpCode_0x91() {
      _clock.incClockCounter((byte) 3);
      temp16 = _registers.AX;
      _registers.AX = _registers.CX;
      _registers.CX = temp16;
   }

   // -------------------------------------------------------------------------
   // 0x92 - XCHG - Exchange Accumulator and DX
   // -------------------------------------------------------------------------
   private void OpCode_0x92() {
      _clock.incClockCounter((byte) 3);
      temp16 = _registers.AX;
      _registers.AX = _registers.DX;
      _registers.DX = temp16;
   }

   // -------------------------------------------------------------------------
   // 0x93 - XCHG - Exchange Accumulator and BX
   // -------------------------------------------------------------------------
   private void OpCode_0x93() {
      _clock.incClockCounter((byte) 3);
      temp16 = _registers.AX;
      _registers.AX = _registers.BX;
      _registers.BX = temp16;
   }

   // -------------------------------------------------------------------------
   // 0x94 - XCHG - Exchange Accumulator and SP
   // -------------------------------------------------------------------------
   private void OpCode_0x94() {
      _clock.incClockCounter((byte) 3);
      temp16 = _registers.AX;
      _registers.AX = _registers.SP;
      _registers.SP = temp16;
   }

   // -------------------------------------------------------------------------
   // 0x95 - XCHG - Exchange Accumulator and BP
   // -------------------------------------------------------------------------
   private void OpCode_0x95() {
      _clock.incClockCounter((byte) 3);
      temp16 = _registers.AX;
      _registers.AX = _registers.BP;
      _registers.BP = temp16;
   }

   // -------------------------------------------------------------------------
   // 0x96 - XCHG - Exchange Accumulator and SI
   // -------------------------------------------------------------------------
   private void OpCode_0x96() {
      _clock.incClockCounter((byte) 3);
      temp16 = _registers.AX;
      _registers.AX = _registers.SI;
      _registers.SI = temp16;
   }

   // -------------------------------------------------------------------------
   // 0x97 - XCHG - Exchange Accumulator and DI
   // -------------------------------------------------------------------------
   private void OpCode_0x97() {
      _clock.incClockCounter((byte) 3);
      temp16 = _registers.AX;
      _registers.AX = _registers.DI;
      _registers.DI = temp16;
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   // Shift Opcodes
   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   private byte ROL8(byte local_data, byte local_count) {
      byte old_msb = 0;

      while (local_count != 0) {
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         old_msb = (byte) ((local_data & 0x80) >> 7);
         local_data = (byte) ((local_data << 1) | old_msb);
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      byte new_msb = (byte) ((local_data & 0x80) >> 7);
      _registers.Flags = (int) (_registers.Flags | old_msb); // Set C flag
      if (new_msb != Flag_c())
         _registers.Flags |= 0x0800; // Set O flag
      return local_data;
   }

   private int ROL16(int local_data, byte local_count) {
      byte old_msb = 0;

      while (local_count != 0) {
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         old_msb = (byte) ((local_data & 0x8000) >> 15);
         local_data = (int) ((local_data << 1) | old_msb);
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      byte new_msb = (byte) ((local_data & 0x8000) >> 15);
      _registers.Flags = (int) (_registers.Flags | old_msb); // Set C flag
      if (new_msb != Flag_c())
         _registers.Flags |= 0x0800; // Set O flag
      return local_data;
   }

   private byte ROR8(byte local_data, byte local_count) {
      while (local_count != 0) {
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         byte old_lsb = (byte) (local_data << 7);
         local_data = (byte) (old_lsb | (local_data >> 1));
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      byte new_msb = (byte) ((local_data & 0x80) >> 7);
      _registers.Flags |= new_msb; // Set C flag
      if ((local_data & 0x80) != ((local_data & 0x40) << 1))
         _registers.Flags |= 0x0800; // Set O flag
      return local_data;
   }

   private int ROR16(int local_data, byte local_count) {
      while (local_count != 0) {
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         int old_lsb = (int) (local_data << 15);
         local_data = (int) (old_lsb | (local_data >> 1));
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      int new_msb = (int) ((local_data & 0x8000) >> 15);
      _registers.Flags |= new_msb; // Set C flag
      if ((local_data & 0x8000) != ((local_data & 0x4000) << 1))
         _registers.Flags |= 0x0800; // Set O flag
      return local_data;
   }

   private byte RCL8(byte local_data, byte local_count) {
      while (local_count != 0) {
         byte tempCf = Flag_c();
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         byte old_msb = (byte) ((local_data & 0x80) >> 7);
         _registers.Flags |= old_msb; // Set C flag
         local_data = (byte) ((local_data << 1) | tempCf);
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      byte new_msb = (byte) ((local_data & 0x80) >> 7);
      if (new_msb != Flag_c())
         _registers.Flags |= 0x0800; // Set O flag
      return local_data;
   }

   private int RCL16(int local_data, byte local_count) {
      while (local_count != 0) {
         int tempCf = Flag_c();
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         int old_msb = (int) ((local_data & 0x8000) >> 15);
         _registers.Flags |= old_msb; // Set C flag
         local_data = (int) ((local_data << 1) | tempCf);
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      int new_msb = (byte) ((local_data & 0x8000) >> 15);
      if (new_msb != Flag_c())
         _registers.Flags |= 0x0800; // Set O flag
      return local_data;
   }

   private byte RCR8(byte local_data, byte local_count) {
      while (local_count != 0) {
         byte tempCf = (byte) (Flag_c() << 7);
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         byte old_lsb = (byte) (local_data & 0x1);
         _registers.Flags |= old_lsb; // Set C flag
         local_data = (byte) (tempCf | (local_data >> 1));
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      if ((local_data & 0x80) != ((local_data & 0x40) << 1))
         _registers.Flags |= 0x0800; // Set O flag
      return local_data;
   }

   private int RCR16(int local_data, byte local_count) {
      while (local_count != 0) {
         int tempCf = (int) (Flag_c() << 15);
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         int old_lsb = (int) (local_data & 0x1);
         _registers.Flags |= old_lsb; // Set C flag
         local_data = (int) (tempCf | (local_data >> 1));
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      if ((local_data & 0x8000) != ((local_data & 0x4000) << 1))
         _registers.Flags |= 0x0800; // Set O flag
      return local_data;
   }

   private byte SAL8(byte local_data, byte local_count) {
      while (local_count != 0) {
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         byte old_msb = (byte) ((local_data & 0x80) >> 7);
         _registers.Flags |= old_msb; // Set C flag
         local_data = (byte) (local_data << 1); // Perform the shift
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      byte new_msb = (byte) ((local_data & 0x80) >> 7);
      if (new_msb != Flag_c())
         _registers.Flags |= 0x0800; // Set O flag
      Set_Flags_Byte_SZP(local_data);
      return local_data;
   }

   private int SAL16(int local_data, byte local_count) {
      while (local_count != 0) {
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         int old_msb = (int) ((local_data & 0x8000) >> 15);
         _registers.Flags |= old_msb; // Set C flag
         local_data = (int) (local_data << 1); // Perform the shift
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      int new_msb = (int) ((local_data & 0x8000) >> 15);
      if (new_msb != Flag_c())
         _registers.Flags |= 0x0800; // Set O flag
      Set_Flags_Word_SZP(local_data);
      return local_data;
   }

   private byte SHR8(byte local_data, byte local_count) {
      while (local_count != 0) {
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         byte old_lsb = (byte) (local_data & 0x1);
         _registers.Flags |= old_lsb; // Set C flag
         local_data = (byte) (local_data >> 1);
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four clocks per bit
      }

      if ((local_data & 0x80) != ((local_data & 0x40) << 1))
         _registers.Flags |= 0x0800; // Set O flag
      Set_Flags_Byte_SZP(local_data);
      return local_data;
   }

   private int SHR16(int local_data, byte local_count) {
      while (local_count != 0) {
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         int old_lsb = (int) (local_data & 0x1);
         _registers.Flags |= old_lsb; // Set C flag
         local_data = (int) (local_data >> 1);
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      if ((local_data & 0x8000) != ((local_data & 0x4000) << 1))
         _registers.Flags |= 0x0800; // Set O flag
      Set_Flags_Word_SZP(local_data);
      return local_data;
   }

   private byte SAR8(byte local_data, byte local_count) {
      while (local_count != 0) {
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         byte old_msb = (byte) (local_data & 0x80);
         byte old_lsb = (byte) (local_data & 0x01);
         _registers.Flags |= old_lsb; // Set C flag
         local_data = (byte) (old_msb | (local_data >> 1));
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      if ((local_data & 0x80) != ((local_data & 0x40) << 1))
         _registers.Flags |= 0x0800; // Set O flag
      Set_Flags_Byte_SZP(local_data);
      return local_data;
   }

   private int SAR16(int local_data, byte local_count) {
      while (local_count != 0) {
         _registers.Flags &= 0xF7FE; // Zero C ,O Flags
         int old_msb = (int) (local_data & 0x8000);
         int old_lsb = (int) (local_data & 0x0001);
         _registers.Flags |= old_lsb; // Set C flag
         local_data = (int) (old_msb | (local_data >> 1));
         local_count--;
         _clock.incClockCounter((byte) 4); // Add four incclocks (bi)t
      }

      if ((local_data & 0x8000) != ((local_data & 0x4000) << 1))
         _registers.Flags |= 0x0800; // Set O flag
      Set_Flags_Word_SZP(local_data);
      return local_data;
   }

   // -------------------------------------------------------------------------
   // 0xD0 Opcodes
   // -------------------------------------------------------------------------
   private void OpCode_0xD0() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.setClockCounter(_clock.getClockCounter() - 2);
      else
         _clock.incClockCounter((byte) 13);

      switch (_regField) {
         case 0x0:
            WriteBackEa(ROL8((byte) FetchEa(), (byte) 0x1));
            break; // # 0xD0 REG[0] - ROL REG8/MEM8 , 1
         case 0x1:
            WriteBackEa(ROR8((byte) FetchEa(), (byte) 0x1));
            break; // # 0xD0 REG[1] - ROR REG8/MEM8 , 1
         case 0x2:
            WriteBackEa(RCL8((byte) FetchEa(), (byte) 0x1));
            break; // # 0xD0 REG[2] - RCL REG8/MEM8 , 1
         case 0x3:
            WriteBackEa(RCR8((byte) FetchEa(), (byte) 0x1));
            break; // # 0xD0 REG[3] - RCR REG8/MEM8 , 1
         case 0x4:
            WriteBackEa(SAL8((byte) FetchEa(), (byte) 0x1));
            break; // # 0xD0 REG[4] - SAL REG8/MEM8 , 1
         case 0x5:
            WriteBackEa(SHR8((byte) FetchEa(), (byte) 0x1));
            break; // # 0xD0 REG[5] - SHR REG8/MEM8 , 1
         case 0x6:
            WriteBackEa(SHR8((byte) FetchEa(), (byte) 0x1));
            break; // # 0xD0 REG[6] - SHR REG8/MEM8 , 1 ** Duplicate **
         case 0x7:
            WriteBackEa(SAR8((byte) FetchEa(), (byte) 0x1));
            break; // # 0xD0 REG[7] - SAR REG8/MEM8 , 1
      }
   }

   // -------------------------------------------------------------------------
   // 0xD1 Opcodes
   // -------------------------------------------------------------------------
   private void OpCode_0xD1() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.setClockCounter(_clock.getClockCounter() - 2);
      else
         _clock.incClockCounter((byte) 21);

      switch (_regField) {
         case 0x0:
            WriteBackEa(ROL16(FetchEa(), (byte) 0x1));
            break; // # 0xD1 REG[0] - ROL REG16/MEM16 , 1
         case 0x1:
            WriteBackEa(ROR16(FetchEa(), (byte) 0x1));
            break; // # 0xD1 REG[1] - ROR REG16/MEM16 , 1
         case 0x2:
            WriteBackEa(RCL16(FetchEa(), (byte) 0x1));
            break; // # 0xD1 REG[2] - RCL REG16/MEM16 , 1
         case 0x3:
            WriteBackEa(RCR16(FetchEa(), (byte) 0x1));
            break; // # 0xD1 REG[3] - RCR REG16/MEM16 , 1
         case 0x4:
            WriteBackEa(SAL16(FetchEa(), (byte) 0x1));
            break; // # 0xD1 REG[4] - SAL REG16/MEM16 , 1
         case 0x5:
            WriteBackEa(SHR16(FetchEa(), (byte) 0x1));
            break; // # 0xD1 REG[5] - SHR REG16/MEM16 , 1
         case 0x6:
            WriteBackEa(SHR16(FetchEa(), (byte) 0x1));
            break; // # 0xD1 REG[6] - SHR REG16/MEM16 , 1 ** Duplicate **
         case 0x7:
            WriteBackEa(SAR16(FetchEa(), (byte) 0x1));
            break; // # 0xD1 REG[7] - SAR REG16/MEM16 , 1
      }
   }

   // -------------------------------------------------------------------------
   // 0xD2 Opcodes
   // -------------------------------------------------------------------------
   private void OpCode_0xD2() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 8);
      else
         _clock.incClockCounter((byte) 20);

      switch (_regField) {
         case 0x0:
            WriteBackEa(ROL8((byte) FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD2 REG[0] - ROL REG8/MEM8 , CL
         case 0x1:
            WriteBackEa(ROR8((byte) FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD2 REG[1] - ROR REG8/MEM8 , CL
         case 0x2:
            WriteBackEa(RCL8((byte) FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD2 REG[2] - RCL REG8/MEM8 , CL
         case 0x3:
            WriteBackEa(RCR8((byte) FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD2 REG[3] - RCR REG8/MEM8 , CL
         case 0x4:
            WriteBackEa(SAL8((byte) FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD2 REG[4] - SAL REG8/MEM8 , CL
         case 0x5:
            WriteBackEa(SHR8((byte) FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD2 REG[5] - SHR REG8/MEM8 , CL
         case 0x6:
            WriteBackEa(SHR8((byte) FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD2 REG[6] - SHR REG8/MEM8 , CL ** Duplicate **
         case 0x7:
            WriteBackEa(SAR8((byte) FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD2 REG[7] - SAR REG8/MEM8 , CL
      }
   }

   // -------------------------------------------------------------------------
   // 0xD3 Opcodes
   // -------------------------------------------------------------------------
   private void OpCode_0xD3() {
      CalculateEa();
      if (_eaIsRegister == 1)
         _clock.incClockCounter((byte) 8);
      else
         _clock.incClockCounter((byte) 28);

      switch (_regField) {
         case 0x0:
            WriteBackEa(ROL16(FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD3 REG[0] - ROL REG16/MEM16 , CL
         case 0x1:
            WriteBackEa(ROR16(FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD3 REG[1] - ROR REG16/MEM16 , CL
         case 0x2:
            WriteBackEa(RCL16(FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD3 REG[2] - RCL REG16/MEM16 , CL
         case 0x3:
            WriteBackEa(RCR16(FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD3 REG[3] - RCR REG16/MEM16 , CL
         case 0x4:
            WriteBackEa(SAL16(FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD3 REG[4] - SAL REG16/MEM16 , CL
         case 0x5:
            WriteBackEa(SHR16(FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD3 REG[5] - SHR REG16/MEM16 , CL
         case 0x6:
            WriteBackEa(SHR16(FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD3 REG[6] - SHR REG16/MEM16 , CL ** Duplicate **
         case 0x7:
            WriteBackEa(SAR16(FetchEa(), (byte) (_registers.CX & 0x00FF)));
            break; // # 0xD3 REG[7] - SAR REG16/MEM16 , CL
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   // String Opcodes
   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   // #define prefix_repnz() ((prefixFlags & 0x02) == 0 ? 0 : 1)
   private byte Prefix_repnz() {
      return (byte) ((_biu.getPrefixFlags() & 0x02) == 0 ? 0 : 1);
   }

   // #define prefix_repz() ((prefixFlags & 0x04) == 0 ? 0 : 1)
   private byte Prefix_repz() {
      return (byte) ((_biu.getPrefixFlags() & 0x04) == 0 ? 0 : 1);
   }

   // -------------------------------------------------------------------------
   // 0xA4 - MOVSB - Move String - Byte
   // -------------------------------------------------------------------------
   private void OpCode_0xA4() {
      byte interruptPending = 0;

      // if (Prefix_repz() == 0) _clock.incclockCounter((byte)1);
      // [RD] I found that it is an undocumented feature that REPNZ (OpCode 0xF2)
      // works
      // the same as REP (OpCode 0xF3)
      if (Prefix_repz() == 0 && Prefix_repnz() == 0)
         _clock.incClockCounter((byte) 1);
      else
         _clock.incClockCounter((byte) 9); // Add initial clock counts

      do {
         if (Prefix_repz() == 1 || Prefix_repnz() == 1) {
            if (_registers.CX == 0)
               return; // Exit from loop if repeat prefix and CX=0
            if (interruptPending == 1) {
               _registers.IP -= 2;
               return;
            } // Exit from loop to service interrupt - adjusting IP to address of prefix

            _registers.CX--;
         }

         _clock.incClockCounter((byte) 17); // Add clocks per loop iteration  

         // var localData = Biu_Operation(BiuOperations.MemReadByte,
         // SEGMENT_OVERRIDABLE_TRUE, SEGMENT_DS, _registers.SI, 0x00);
         byte localData = _biu.readMemoryByte(SegmentOverridableTrue, SegmentRegs.DS, _registers.SI);

         // Write data to destination - Interrupts sampled on last CLK edge
         // Biu_Operation(BiuOperations.MemWriteByte, SEGMENT_OVERRIDABLE_FALSE,
         // SEGMENT_ES, _registers.DI, localData);
         _biu.writeMemoryByte(SegmentOverridableFalse, SegmentRegs.ES, _registers.DI, localData);

         if (_nmiLatched.IsSet() || (pins.getIntrPin() != 0 && Flag_i() != 0))
            interruptPending = 1;
         else
            interruptPending = 0;

         if (Flag_d() == 0) {
            _registers.SI += 1;
            _registers.DI += 1;
         } else {
            _registers.SI -= 1;
            _registers.DI -= 1;
         }

      } while (Prefix_repz() == 1 || Prefix_repnz() == 1);
   }

   // -------------------------------------------------------------------------
   // 0xA5 - MOVSW - Move String - Word
   // -------------------------------------------------------------------------
   private void OpCode_0xA5() {
      byte interruptPending = 0;

      // if (Prefix_repz() == 0) _clock.incclockCounter((byte)1);
      // [RD] I found that it is an undocumented feature that REPNZ (OpCode 0xF2)
      // works
      // the same as REP (OpCode 0xF3)
      if (Prefix_repz() == 0 && Prefix_repnz() == 0)
         _clock.incClockCounter((byte) 1);
      else
         _clock.incClockCounter((byte) 9); // Add initial clock counter

      do {
         if (Prefix_repz() == 1 || Prefix_repnz() == 1) {
            // Exit from loop if repeat prefix and CX=0
            if (_registers.CX == 0)
               return;

            if (interruptPending == 1) {
               _registers.IP -= 2;
               return;
            } // Exit from loop to service interrupt - adjusting IP to address of prefix

            _registers.CX--;
         }

         _clock.incClockCounter((byte) 17); // Add clocks per loop iteration

         // var localData = Biu_Operation(BiuOperations.MemReadWord,
         // SEGMENT_OVERRIDABLE_TRUE, SEGMENT_DS, _registers.SI, 0x00);
         int localData = _biu.readMemoryWord(SegmentOverridableTrue, SegmentRegs.DS, _registers.SI);

         // Write data to destination - Interrupts sampled on last CLK edge
         // Biu_Operation(BiuOperations.MemWriteWord, SEGMENT_OVERRIDABLE_FALSE,
         // SEGMENT_ES, _registers.DI, localData);
         _biu.writeMemoryWord(SegmentOverridableFalse, SegmentRegs.ES, _registers.DI, localData);

         if (_nmiLatched.IsSet() || (pins.getIntrPin() != 0 && (Flag_i()) != 0))
            interruptPending = 1;
         else
            interruptPending = 0;

         if (Flag_d() == 0) {
            _registers.SI += 2;
            _registers.DI += 2;
         } else {
            _registers.SI -= 2;
            _registers.DI -= 2;
         }

      } while (Prefix_repz() == 1 || Prefix_repnz() == 1);
   }

   // -------------------------------------------------------------------------
   // 0xA6 - CMPSB - Compare String - Byte
   // -------------------------------------------------------------------------
   private void OpCode_0xA6() {
      byte interruptPending = 0;

      if ((Prefix_repz() == 1) || (Prefix_repnz() == 1))
         _clock.incClockCounter((byte) 1);
      else
         _clock.incClockCounter((byte) 9); // Add initial inccl(count)s

      do {
         if (Prefix_repz() == 1 || Prefix_repnz() == 1) {
            if (_registers.CX == 0)
               return; // Exit from loop if repeat prefix and CX=0

            if (interruptPending == 1) {
               _registers.IP -= 2;
               return; // Exit from loop to service interrupt - adjusting IP to address of prefix
            }

            _registers.CX--;
         }

         _clock.incClockCounter((byte) 22); // Add clocks per loop iteration
         // var localData1 = (byte)Biu_Operation(BiuOperations.MemReadByte,
         // SEGMENT_OVERRIDABLE_TRUE, SEGMENT_DS, _registers.SI, 0x00);
         byte localData1 = _biu.readMemoryByte(SegmentOverridableTrue, SegmentRegs.DS, _registers.SI);

         // var localData2 = (byte)Biu_Operation(BiuOperations.MemReadByte,
         // SEGMENT_OVERRIDABLE_FALSE, SEGMENT_ES, _registers.DI, 0x00);
         byte localData2 = _biu.readMemoryByte(SegmentOverridableTrue, SegmentRegs.ES, _registers.DI);

         SubBytes(localData1, localData2); // Perform comparison which sets Flags
         if (_nmiLatched.IsSet() || (pins.getIntrPin() != 0 && Flag_i() != 0))
            interruptPending = 1;
         else
            interruptPending = 0;

         if (Flag_d() == 0) {
            _registers.SI += 1;
            _registers.DI += 1;
         } else {
            _registers.SI -= 1;
            _registers.DI -= 1;
         }

      } while ((Prefix_repz() == 1 && Flag_z() == 1) || (Prefix_repnz() == 1 && Flag_z() == 0));
   }

   // -------------------------------------------------------------------------
   // 0xA7 - CMPSW - Compare String - Word
   // -------------------------------------------------------------------------
   private void OpCode_0xA7() {
      byte interruptPending = 0;

      if (Prefix_repz() == 1 || Prefix_repnz() == 1)
         _clock.incClockCounter((byte) 1);
      else
         _clock.incClockCounter((byte) 9); // Add initial clock counts

      do {
         if ((Prefix_repz() == 1) || (Prefix_repnz() == 1)) {
            if (_registers.CX == 0)
               return; // Exit from loop if repeat prefix and CX=0

            if (interruptPending == 1) {
               _registers.IP -= 2;
               return; // Exit from loop to service interrupt - adjusting IP to address of prefix
            }

            _registers.CX--;
         }

         _clock.incClockCounter((byte) 22); // Add clocks per loop iteration
         // ushort localData1 = Biu_Operation(BiuOperations.MemReadWord,
         // SEGMENT_OVERRIDABLE_TRUE, SEGMENT_DS, _registers.SI, 0x00);
         int localData1 = _biu.readMemoryWord(SegmentOverridableTrue, SegmentRegs.DS, _registers.SI);

         // ushort localData2 = Biu_Operation(BiuOperations.MemReadWord,
         // SEGMENT_OVERRIDABLE_FALSE, SEGMENT_ES, _registers.DI, 0x00);
         int localData2 = _biu.readMemoryWord(SegmentOverridableTrue, SegmentRegs.ES, _registers.DI);

         SubWords(localData1, localData2); // Perform comparison which sets Flags
         if (_nmiLatched.IsSet() || (pins.getIntrPin() != 0 && Flag_i() != 0))
            interruptPending = 1;
         else
            interruptPending = 0;

         if (Flag_d() == 0) {
            _registers.SI += 2;
            _registers.DI += 2;
         } else {
            _registers.SI -= 2;
            _registers.DI -= 2;
         }

      } while ((Prefix_repz() == 1 && Flag_z() == 1) || (Prefix_repnz() == 1 && Flag_z() == 0));
   }

   // -------------------------------------------------------------------------
   // 0xAA - STOSB - Store AL to String - Byte
   // -------------------------------------------------------------------------
   private void OpCode_0xAA() {
      byte interruptPending = 0;

      if (Prefix_repz() == 0)
         _clock.incClockCounter((byte) 1);
      else
         _clock.incClockCounter((byte) 9); // Add initial clock counts

      do {
         if (Prefix_repz() == 1) {
            if (_registers.CX == 0)
               return; // Exit from loop if repeat prefix and CX=0

            if (interruptPending == 1) {
               _registers.IP -= 2;
               return; // Exit from loop to service interrupt - adjusting IP to address of prefix
            }

            _registers.CX--;
         }

         _clock.incClockCounter((byte) 10); // Add clocks per loop iteration
         // Biu_Operation(BiuOperations.MemWriteByte, SEGMENT_OVERRIDABLE_FALSE,
         // SEGMENT_ES, _registers.DI, _registers.AX); // Write AL data to the ES:DI
         // Address
         byte registerAl = (byte) (_registers.AX & 0x00FF);
         _biu.writeMemoryByte(SegmentOverridableFalse, SegmentRegs.ES, _registers.DI, registerAl);
         if (_nmiLatched.IsSet() || (pins.getIntrPin() != 0 && Flag_i() != 0))
            interruptPending = 1;
         else
            interruptPending = 0;

         if (Flag_d() == 0) {
            _registers.DI += 1;
         } else {
            _registers.DI -= 1;
         }

      } while (Prefix_repz() == 1);
   }

   // -------------------------------------------------------------------------
   // 0xAB - STOSW - Store AX to String - Word
   // -------------------------------------------------------------------------
   private void OpCode_0xAB() {
      byte interruptPending = 0;

      if (Prefix_repz() == 0)
         _clock.incClockCounter((byte) 1);
      else
         _clock.incClockCounter((byte) 9); // Add initial clock counts

      do {
         if (Prefix_repz() == 1) {
            if (_registers.CX == 0)
               return; // Exit from loop if repeat prefix and CX=0

            if (interruptPending == 1) {
               _registers.IP -= 2;
               return; // Exit from loop to service interrupt - adjusting IP to address of prefix
            }

            _registers.CX--;
         }

         _clock.incClockCounter((byte) 10); // Add clocks per loop iteration
         // Biu_Operation(BiuOperations.MemWriteWord, SEGMENT_OVERRIDABLE_FALSE,
         // SEGMENT_ES, _registers.DI, _registers.AX); // Write AL data to the ES:DI
         // Address
         _biu.writeMemoryWord(SegmentOverridableFalse, SegmentRegs.ES, _registers.DI, _registers.AX); // Write AL data
                                                                                                      // to the ES:DI
                                                                                                      // Address

         if (_nmiLatched.IsSet() || (pins.getIntrPin() != 0 && Flag_i() != 0))
            interruptPending = 1;
         else
            interruptPending = 0;

         if (Flag_d() == 0) {
            _registers.DI += 2;
         } else {
            _registers.DI -= 2;
         }

      } while (Prefix_repz() == 1);
   }

   // -------------------------------------------------------------------------
   // 0xAC - LODSB - Load String into AL - Byte
   // -------------------------------------------------------------------------
   private void OpCode_0xAC() {
      byte interruptPending = 0;

      if (Prefix_repz() == 0)
         _clock.incClockCounter((byte) 0);
      else
         _clock.incClockCounter((byte) 9); // Add initial clock counts

      do {
         if (Prefix_repz() == 1) {
            if (_registers.CX == 0)
               return; // Exit from loop if repeat prefix and CX=0
            if (interruptPending == 1) {
               _registers.IP -= 2;
               return;
            } // Exit from loop to service interrupt - adjusting IP to address of prefix

            _registers.CX--;
         }

         _clock.incClockCounter((byte) 12); // Add clocks per loop iteration
         // var localData = Biu_Operation(BiuOperations.MemReadByte,
         // SEGMENT_OVERRIDABLE_TRUE, SEGMENT_DS, _registers.SI, 0x00);
         byte localData = _biu.readMemoryByte(SegmentOverridableTrue, SegmentRegs.DS, _registers.SI);

         WriteRegister(REG_AL, localData); // Write data to AL
         if (_nmiLatched.IsSet() || (pins.getIntrPin() != 0 && Flag_i() != 0))
            interruptPending = 1;
         else
            interruptPending = 0;

         if (Flag_d() == 0) {
            _registers.SI += 1;
         } else {
            _registers.SI -= 1;
         }

      } while (Prefix_repz() == 1);
   }

   // -------------------------------------------------------------------------
   // 0xAD - LODSW - Load String into AX - Word
   // -------------------------------------------------------------------------
   private void OpCode_0xAD() {
      byte interruptPending = 0;

      if (Prefix_repz() == 0)
         _clock.incClockCounter((byte) 0);
      else
         _clock.incClockCounter((byte) 9); // Add initial clock counts

      do {
         if (Prefix_repz() == 1) {
            if (_registers.CX == 0)
               return; // Exit from loop if repeat prefix and CX=0

            if (interruptPending == 1) {
               _registers.IP -= 2;
               return; // Exit from loop to service interrupt - adjusting IP to address of prefix
            }

            _registers.CX--;
         }

         _clock.incClockCounter((byte) 12); // Add clocks per loop iteration
         // ushort localData = Biu_Operation(BiuOperations.MemReadWord,
         // SEGMENT_OVERRIDABLE_TRUE, SEGMENT_DS, _registers.SI, 0x00);
         int localData = _biu.readMemoryWord(SegmentOverridableTrue, SegmentRegs.DS, _registers.SI);

         _registers.AX = localData; // Write data to AX
         if (_nmiLatched.IsSet() || (pins.getIntrPin() != 0 && Flag_i() != 0))
            interruptPending = 1;
         else
            interruptPending = 0;

         if (Flag_d() == 0) {
            _registers.SI += 2;
         } else {
            _registers.SI -= 2;
         }

      } while (Prefix_repz() == 1);
   }

   // -------------------------------------------------------------------------
   // # 0xAE - SCASB - Scan String - Byte
   // -------------------------------------------------------------------------
   private void OpCode_0xAE() {
      byte interruptPending = 0;

      if ((Prefix_repz() == 1) || (Prefix_repnz() == 1))
         _clock.incClockCounter((byte) 0);
      else
         _clock.incClockCounter((byte) 9); // Add initial clock counts

      do {
         if ((Prefix_repz() == 1) || (Prefix_repnz() == 1)) {
            if (_registers.CX == 0)
               return; // Exit from loop if repeat prefix and CX=0
            if (interruptPending == 1) {
               _registers.IP -= 2;
               return;
            } // Exit from loop to service interrupt - adjusting IP to address of prefix

            _registers.CX--;
         }

         _clock.incClockCounter((byte) 15); // Add clocks per loop iteration
         // var localData = (byte)Biu_Operation(BiuOperations.MemReadByte,
         // SEGMENT_OVERRIDABLE_TRUE, SEGMENT_ES, _registers.DI, 0x00);
         byte localData = _biu.readMemoryByte(SegmentOverridableTrue, SegmentRegs.ES, _registers.DI);

         SubBytes((byte) (_registers.AX & 0x00FF), localData); // Perform comparison which sets Flags
         if (_nmiLatched.IsSet() || (pins.getIntrPin() != 0 && Flag_i() != 0))
            interruptPending = 1;
         else
            interruptPending = 0;

         if (Flag_d() == 0) {
            _registers.DI += 1;
         } else {
            _registers.DI -= 1;
         }

      } while ((Prefix_repz() == 1 && Flag_z() == 1) || (Prefix_repnz() == 1 && Flag_z() == 0));
   }

   // -------------------------------------------------------------------------
   // # 0xAF - SCASW - Scan String - Word
   // -------------------------------------------------------------------------
   private void OpCode_0xAF() {
      byte interruptPending = 0;

      if ((Prefix_repz() == 1) || (Prefix_repnz() == 1))
         _clock.incClockCounter((byte) 9);
      else
         _clock.incClockCounter((byte) 9); // Add initial clock counts

      do {
         if ((Prefix_repz() == 1) || (Prefix_repnz() == 1)) {
            if (_registers.CX == 0)
               return; // Exit from loop if repeat prefix and CX=0
            if (interruptPending == 1) {
               _registers.IP -= 2;
               return; // Exit from loop to service interrupt - adjusting IP to address of prefix
            }

            _registers.CX--;
         }

         _clock.incClockCounter((byte) 15); // Add clocks per loop iteration
         // ushort localData = Biu_Operation(BiuOperations.MemReadWord,
         // SEGMENT_OVERRIDABLE_TRUE, SEGMENT_ES, _registers.DI, 0x00);
         int localData = _biu.readMemoryWord(SegmentOverridableTrue, SegmentRegs.ES, _registers.DI);

         SubWords(_registers.AX, localData); // Perform comparison which sets Flags
         if (_nmiLatched.IsSet() || (pins.getIntrPin() != 0 && Flag_i() != 0))
            interruptPending = 1;
         else
            interruptPending = 0;

         if (Flag_d() == 0) {
            _registers.DI += 2;
         } else {
            _registers.DI -= 2;
         }

      } while ((Prefix_repz() == 1 && Flag_z() == 1) || (Prefix_repnz() == 1 && Flag_z() == 0));
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   // ------------------------------------------------------------------------------------------------------------
   // Decode the first byte of the opcode
   // ------------------------------------------------------------------------------------------------------------
   private void ExecuteNewInstr() {
      _opCodeFirstByte = PfqFetchByte();

      _instLogger.info("========================================");
      _instLogger.info("--> {0}(0x{1:X2}) {2}", "ExecuteNewInstr", _opCodeFirstByte,
            _opNames[_opCodeFirstByte]);

      switch (_opCodeFirstByte) {
         case (byte) 0x00:
            OpCode_0x00();
            break;
         case (byte) 0x01:
            OpCode_0x01();
            break;
         case (byte) 0x02:
            OpCode_0x02();
            break;
         case (byte) 0x03:
            OpCode_0x03();
            break;
         case (byte) 0x04:
            OpCode_0x04();
            break;
         case (byte) 0x05:
            OpCode_0x05();
            break;
         case (byte) 0x06:
            OpCode_0x06();
            break;
         case (byte) 0x07:
            OpCode_0x07();
            break;
         case (byte) 0x08:
            OpCode_0x08();
            break;
         case (byte) 0x09:
            OpCode_0x09();
            break;
         case (byte) 0x0A:
            OpCode_0x0A();
            break;
         case (byte) 0x0B:
            OpCode_0x0B();
            break;
         case (byte) 0x0C:
            OpCode_0x0C();
            break;
         case (byte) 0x0D:
            OpCode_0x0D();
            break;
         case (byte) 0x0E:
            OpCode_0x0E();
            break;
         case (byte) 0x0F:
            OpCode_0x0F();
            break;
         case (byte) 0x10:
            OpCode_0x10();
            break;
         case (byte) 0x11:
            OpCode_0x11();
            break;
         case (byte) 0x12:
            OpCode_0x12();
            break;
         case (byte) 0x13:
            OpCode_0x13();
            break;
         case (byte) 0x14:
            OpCode_0x14();
            break;
         case (byte) 0x15:
            OpCode_0x15();
            break;
         case (byte) 0x16:
            OpCode_0x16();
            break;
         case (byte) 0x17:
            OpCode_0x17();
            break;
         case (byte) 0x18:
            OpCode_0x18();
            break;
         case (byte) 0x19:
            OpCode_0x19();
            break;
         case (byte) 0x1A:
            OpCode_0x1A();
            break;
         case (byte) 0x1B:
            OpCode_0x1B();
            break;
         case (byte) 0x1C:
            OpCode_0x1C();
            break;
         case (byte) 0x1D:
            OpCode_0x1D();
            break;
         case (byte) 0x1E:
            OpCode_0x1E();
            break;
         case (byte) 0x1F:
            OpCode_0x1F();
            break;
         case (byte) 0x20:
            OpCode_0x20();
            break;
         case (byte) 0x21:
            OpCode_0x21();
            break;
         case (byte) 0x22:
            OpCode_0x22();
            break;
         case (byte) 0x23:
            OpCode_0x23();
            break;
         case (byte) 0x24:
            OpCode_0x24();
            break;
         case (byte) 0x25:
            OpCode_0x25();
            break;
         case (byte) 0x26:
            OpCode_0x26();
            break;
         case (byte) 0x27:
            OpCode_0x27();
            break;
         case (byte) 0x28:
            OpCode_0x28();
            break;
         case (byte) 0x29:
            OpCode_0x29();
            break;
         case (byte) 0x2A:
            OpCode_0x2A();
            break;
         case (byte) 0x2B:
            OpCode_0x2B();
            break;
         case (byte) 0x2C:
            OpCode_0x2C();
            break;
         case (byte) 0x2D:
            OpCode_0x2D();
            break;
         case (byte) 0x2E:
            OpCode_0x2E();
            break;
         case (byte) 0x2F:
            OpCode_0x2F();
            break;
         case (byte) 0x30:
            OpCode_0x30();
            break;
         case (byte) 0x31:
            OpCode_0x31();
            break;
         case (byte) 0x32:
            OpCode_0x32();
            break;
         case (byte) 0x33:
            OpCode_0x33();
            break;
         case (byte) 0x34:
            OpCode_0x34();
            break;
         case (byte) 0x35:
            OpCode_0x35();
            break;
         case (byte) 0x36:
            OpCode_0x36();
            break;
         case (byte) 0x37:
            OpCode_0x37();
            break;
         case (byte) 0x38:
            OpCode_0x38();
            break;
         case (byte) 0x39:
            OpCode_0x39();
            break;
         case (byte) 0x3A:
            OpCode_0x3A();
            break;
         case (byte) 0x3B:
            OpCode_0x3B();
            break;
         case (byte) 0x3C:
            OpCode_0x3C();
            break;
         case (byte) 0x3D:
            OpCode_0x3D();
            break;
         case (byte) 0x3E:
            OpCode_0x3E();
            break;
         case (byte) 0x3F:
            OpCode_0x3F();
            break;
         case (byte) 0x40:
            OpCode_0x40();
            break;
         case (byte) 0x41:
            OpCode_0x41();
            break;
         case (byte) 0x42:
            OpCode_0x42();
            break;
         case (byte) 0x43:
            OpCode_0x43();
            break;
         case (byte) 0x44:
            OpCode_0x44();
            break;
         case (byte) 0x45:
            OpCode_0x45();
            break;
         case (byte) 0x46:
            OpCode_0x46();
            break;
         case (byte) 0x47:
            OpCode_0x47();
            break;
         case (byte) 0x48:
            OpCode_0x48();
            break;
         case (byte) 0x49:
            OpCode_0x49();
            break;
         case (byte) 0x4A:
            OpCode_0x4A();
            break;
         case (byte) 0x4B:
            OpCode_0x4B();
            break;
         case (byte) 0x4C:
            OpCode_0x4C();
            break;
         case (byte) 0x4D:
            OpCode_0x4D();
            break;
         case (byte) 0x4E:
            OpCode_0x4E();
            break;
         case (byte) 0x4F:
            OpCode_0x4F();
            break;
         case (byte) 0x50:
            OpCode_0x50();
            break;
         case (byte) 0x51:
            OpCode_0x51();
            break;
         case (byte) 0x52:
            OpCode_0x52();
            break;
         case (byte) 0x53:
            OpCode_0x53();
            break;
         case (byte) 0x54:
            OpCode_0x54();
            break;
         case (byte) 0x55:
            OpCode_0x55();
            break;
         case (byte) 0x56:
            OpCode_0x56();
            break;
         case (byte) 0x57:
            OpCode_0x57();
            break;
         case (byte) 0x58:
            OpCode_0x58();
            break;
         case (byte) 0x59:
            OpCode_0x59();
            break;
         case (byte) 0x5A:
            OpCode_0x5A();
            break;
         case (byte) 0x5B:
            OpCode_0x5B();
            break;
         case (byte) 0x5C:
            OpCode_0x5C();
            break;
         case (byte) 0x5D:
            OpCode_0x5D();
            break;
         case (byte) 0x5E:
            OpCode_0x5E();
            break;
         case (byte) 0x5F:
            OpCode_0x5F();
            break;
         case (byte) 0x60:
            OpCode_0x70();
            break;
         case (byte) 0x61:
            OpCode_0x71();
            break;
         case (byte) 0x62:
            OpCode_0x72();
            break;
         case (byte) 0x63:
            OpCode_0x73();
            break;
         case (byte) 0x64:
            OpCode_0x74();
            break;
         case (byte) 0x65:
            OpCode_0x75();
            break;
         case (byte) 0x66:
            OpCode_0x76();
            break;
         case (byte) 0x67:
            OpCode_0x77();
            break;
         case (byte) 0x68:
            OpCode_0x78();
            break;
         case (byte) 0x69:
            OpCode_0x79();
            break;
         case (byte) 0x6A:
            OpCode_0x7A();
            break;
         case (byte) 0x6B:
            OpCode_0x7B();
            break;
         case (byte) 0x6C:
            OpCode_0x7C();
            break;
         case (byte) 0x6D:
            OpCode_0x7D();
            break;
         case (byte) 0x6E:
            OpCode_0x7E();
            break;
         case (byte) 0x6F:
            OpCode_0x7F();
            break;
         case (byte) 0x70:
            OpCode_0x70();
            break;
         case (byte) 0x71:
            OpCode_0x71();
            break;
         case (byte) 0x72:
            OpCode_0x72();
            break;
         case (byte) 0x73:
            OpCode_0x73();
            break;
         case (byte) 0x74:
            OpCode_0x74();
            break;
         case (byte) 0x75:
            OpCode_0x75();
            break;
         case (byte) 0x76:
            OpCode_0x76();
            break;
         case (byte) 0x77:
            OpCode_0x77();
            break;
         case (byte) 0x78:
            OpCode_0x78();
            break;
         case (byte) 0x79:
            OpCode_0x79();
            break;
         case (byte) 0x7A:
            OpCode_0x7A();
            break;
         case (byte) 0x7B:
            OpCode_0x7B();
            break;
         case (byte) 0x7C:
            OpCode_0x7C();
            break;
         case (byte) 0x7D:
            OpCode_0x7D();
            break;
         case (byte) 0x7E:
            OpCode_0x7E();
            break;
         case (byte) 0x7F:
            OpCode_0x7F();
            break;
         case (byte) 0x80:
            OpCode_0x80();
            break;
         case (byte) 0x81:
            OpCode_0x81();
            break;
         case (byte) 0x82:
            OpCode_0x80();
            break;
         case (byte) 0x83:
            OpCode_0x83();
            break;
         case (byte) 0x84:
            OpCode_0x84();
            break;
         case (byte) 0x85:
            OpCode_0x85();
            break;
         case (byte) 0x86:
            OpCode_0x86();
            break;
         case (byte) 0x87:
            OpCode_0x87();
            break;
         case (byte) 0x88:
            OpCode_0x88();
            break;
         case (byte) 0x89:
            OpCode_0x89();
            break;
         case (byte) 0x8A:
            OpCode_0x8A();
            break;
         case (byte) 0x8B:
            OpCode_0x8B();
            break;
         case (byte) 0x8C:
            OpCode_0x8C();
            break;
         case (byte) 0x8D:
            OpCode_0x8D();
            break;
         case (byte) 0x8E:
            OpCode_0x8E();
            break;
         case (byte) 0x8F:
            OpCode_0x8F();
            break;
         case (byte) 0x90:
            OpCode_0x90();
            break;
         case (byte) 0x91:
            OpCode_0x91();
            break;
         case (byte) 0x92:
            OpCode_0x92();
            break;
         case (byte) 0x93:
            OpCode_0x93();
            break;
         case (byte) 0x94:
            OpCode_0x94();
            break;
         case (byte) 0x95:
            OpCode_0x95();
            break;
         case (byte) 0x96:
            OpCode_0x96();
            break;
         case (byte) 0x97:
            OpCode_0x97();
            break;
         case (byte) 0x98:
            OpCode_0x98();
            break;
         case (byte) 0x99:
            OpCode_0x99();
            break;
         case (byte) 0x9A:
            OpCode_0x9A();
            break;
         case (byte) 0x9B:
            OpCode_0x9B();
            break;
         case (byte) 0x9C:
            OpCode_0x9C();
            break;
         case (byte) 0x9D:
            OpCode_0x9D();
            break;
         case (byte) 0x9E:
            OpCode_0x9E();
            break;
         case (byte) 0x9F:
            OpCode_0x9F();
            break;
         case (byte) 0xA0:
            OpCode_0xA0();
            break;
         case (byte) 0xA1:
            OpCode_0xA1();
            break;
         case (byte) 0xA2:
            OpCode_0xA2();
            break;
         case (byte) 0xA3:
            OpCode_0xA3();
            break;
         case (byte) 0xA4:
            OpCode_0xA4();
            break;
         case (byte) 0xA5:
            OpCode_0xA5();
            break;
         case (byte) 0xA6:
            OpCode_0xA6();
            break;
         case (byte) 0xA7:
            OpCode_0xA7();
            break;
         case (byte) 0xA8:
            OpCode_0xA8();
            break;
         case (byte) 0xA9:
            OpCode_0xA9();
            break;
         case (byte) 0xAA:
            OpCode_0xAA();
            break;
         case (byte) 0xAB:
            OpCode_0xAB();
            break;
         case (byte) 0xAC:
            OpCode_0xAC();
            break;
         case (byte) 0xAD:
            OpCode_0xAD();
            break;
         case (byte) 0xAE:
            OpCode_0xAE();
            break;
         case (byte) 0xAF:
            OpCode_0xAF();
            break;
         case (byte) 0xB0:
            OpCode_0xB0();
            break;
         case (byte) 0xB1:
            OpCode_0xB1();
            break;
         case (byte) 0xB2:
            OpCode_0xB2();
            break;
         case (byte) 0xB3:
            OpCode_0xB3();
            break;
         case (byte) 0xB4:
            OpCode_0xB4();
            break;
         case (byte) 0xB5:
            OpCode_0xB5();
            break;
         case (byte) 0xB6:
            OpCode_0xB6();
            break;
         case (byte) 0xB7:
            OpCode_0xB7();
            break;
         case (byte) 0xB8:
            OpCode_0xB8();
            break;
         case (byte) 0xB9:
            OpCode_0xB9();
            break;
         case (byte) 0xBA:
            OpCode_0xBA();
            break;
         case (byte) 0xBB:
            OpCode_0xBB();
            break;
         case (byte) 0xBC:
            OpCode_0xBC();
            break;
         case (byte) 0xBD:
            OpCode_0xBD();
            break;
         case (byte) 0xBE:
            OpCode_0xBE();
            break;
         case (byte) 0xBF:
            OpCode_0xBF();
            break;
         case (byte) 0xC0:
            OpCode_0xC2();
            break;
         case (byte) 0xC1:
            OpCode_0xC3();
            break;
         case (byte) 0xC2:
            OpCode_0xC2();
            break;
         case (byte) 0xC3:
            OpCode_0xC3();
            break;
         case (byte) 0xC4:
            OpCode_0xC4();
            break;
         case (byte) 0xC5:
            OpCode_0xC5();
            break;
         case (byte) 0xC6:
            OpCode_0xC6();
            break;
         case (byte) 0xC7:
            OpCode_0xC7();
            break;
         case (byte) 0xC8:
            OpCode_0xCA();
            break;
         case (byte) 0xC9:
            OpCode_0xCB();
            break;
         case (byte) 0xCA:
            OpCode_0xCA();
            break;
         case (byte) 0xCB:
            OpCode_0xCB();
            break;
         case (byte) 0xCC:
            OpCode_0xCC();
            break;
         case (byte) 0xCD:
            OpCode_0xCD();
            break;
         case (byte) 0xCE:
            OpCode_0xCE();
            break;
         case (byte) 0xCF:
            OpCode_0xCF();
            break;
         case (byte) 0xD0:
            OpCode_0xD0();
            break;
         case (byte) 0xD1:
            OpCode_0xD1();
            break;
         case (byte) 0xD2:
            OpCode_0xD2();
            break;
         case (byte) 0xD3:
            OpCode_0xD3();
            break;
         case (byte) 0xD4:
            OpCode_0xD4();
            break;
         case (byte) 0xD5:
            OpCode_0xD5();
            break;
         case (byte) 0xD6:
            OpCode_0xD6();
            break;
         case (byte) 0xD7:
            OpCode_0xD7();
            break;
         case (byte) 0xD8:
            OpCode_0xD8();
            break;
         case (byte) 0xD9:
            OpCode_0xD8();
            break;
         case (byte) 0xDA:
            OpCode_0xD8();
            break;
         case (byte) 0xDB:
            OpCode_0xD8();
            break;
         case (byte) 0xDC:
            OpCode_0xD8();
            break;
         case (byte) 0xDD:
            OpCode_0xD8();
            break;
         case (byte) 0xDE:
            OpCode_0xD8();
            break;
         case (byte) 0xDF:
            OpCode_0xD8();
            break;
         case (byte) 0xE0:
            OpCode_0xE0();
            break;
         case (byte) 0xE1:
            OpCode_0xE1();
            break;
         case (byte) 0xE2:
            OpCode_0xE2();
            break;
         case (byte) 0xE3:
            OpCode_0xE3();
            break;
         case (byte) 0xE4:
            OpCode_0xE4();
            break;
         case (byte) 0xE5:
            OpCode_0xE5();
            break;
         case (byte) 0xE6:
            OpCode_0xE6();
            break;
         case (byte) 0xE7:
            OpCode_0xE7();
            break;
         case (byte) 0xE8:
            OpCode_0xE8();
            break;
         case (byte) 0xE9:
            OpCode_0xE9();
            break;
         case (byte) 0xEA:
            OpCode_0xEA();
            break;
         case (byte) 0xEB:
            OpCode_0xEB();
            break;
         case (byte) 0xEC:
            OpCode_0xEC();
            break;
         case (byte) 0xED:
            OpCode_0xED();
            break;
         case (byte) 0xEE:
            OpCode_0xEE();
            break;
         case (byte) 0xEF:
            OpCode_0xEF();
            break;
         case (byte) 0xF0:
            OpCode_0xF0();
            break;
         case (byte) 0xF1:
            OpCode_0xF0();
            break;
         case (byte) 0xF2:
            OpCode_0xF2();
            break;
         case (byte) 0xF3:
            OpCode_0xF3();
            break;
         case (byte) 0xF4:
            OpCode_0xF4();
            break;
         case (byte) 0xF5:
            OpCode_0xF5();
            break;
         case (byte) 0xF6:
            OpCode_0xF6();
            break;
         case (byte) 0xF7:
            OpCode_0xF7();
            break;
         case (byte) 0xF8:
            OpCode_0xF8();
            break;
         case (byte) 0xF9:
            OpCode_0xF9();
            break;
         case (byte) 0xFA:
            OpCode_0xFA();
            break;
         case (byte) 0xFB:
            OpCode_0xFB();
            break;
         case (byte) 0xFC:
            OpCode_0xFC();
            break;
         case (byte) 0xFD:
            OpCode_0xFD();
            break;
         case (byte) 0xFE:
            OpCode_0xFE();
            break;
         case (byte) 0xFF:
            OpCode_0xFF();
            break;
      }

      _instLogger.info("<-- {0}()", "ExecuteNewInstr");
      _instLogger.info("----------------------------------------");
   }

   // -------------------------------------------------
   // Main loop
   // -------------------------------------------------
   // public Task loop(CancellationToken token)
   // @Override
   // private void run() {
   //    _instLogger.trace("---> Main loop");

   //    for (int i = 0; i <= 32; i++) {
   //       _clock.WaitForFallingEdge();
   //    }

   //    _pfqInAddress = _registers.IP;
   //    prefetch_queue_count = 0;

   //    ResetSequence();

   //    while (true) {
   //       // clockCounter = 0;
   //       if (_deviceAdapter.getResetPin() != 0)
   //          ResetSequence();

   //       // Wait for cycle counter to expire before processing traps or next instruction
   //       if (_clock.getClockCounter() > 0) {
   //          _clock.WaitForFallingEdge();
   //       }

   //       // Don't poll for interrupts between a Prefixes and instructions
   //       if (_clock.getClockCounter() == 0 && !_pauseInterrupts) {
   //          if (_nmiLatched.IsSet()) {
   //             NmiHandler();
   //          } else if (_deviceAdapter.getIntrPin() != 0 && Flag_i() != 0) {
   //             IntrHandler();
   //          } else if (Flag_t() != 0) {
   //             TrapHandler();
   //          }
   //       }

   //       // Process new instruction when previous instruction's cycle counter has expired
   //       // Debounce prefixes after a non-prefix instruction is executed
   //       if (_clock.getClockCounter() == 0) {
   //          _lastInstrSetPrefix = false;
   //          _pauseInterrupts = false;
   //          ExecuteNewInstr();
   //          // clockCounter=0;
   //          if (_lastInstrSetPrefix == false)
   //             _biu.setPrefixFlags((byte) 0x00);
   //       }

   //       // Fill prefetch queue between instructions
   //       if (prefetch_queue_count < 4) {
   //          PfqAddByte();
   //       }
   //    }
   // }

   private enum States {
      POWERUP, RESET, RUN, HALT
   }

   private States state = States.POWERUP;

   // RESET: Cases the processor to immediately terminate its present activity. The signal
   // must transition LOW to HIGH and remain active HIGH for at least four clock cycles. 
   // It restarts execution, as described in the instruction set description, when RESET
   // returns LOW. RESET is internally synchronized
   private byte lstResetPin = 0;
   private int resetClkCnt;
   private int delayClkCnt;

   public void step() {
      _instLogger.trace("---> step()");

      byte curResetPin = pins.getResetPin();
      if (lstResetPin == LOW && curResetPin == HIGH) {
         resetClkCnt = 1;
      }
      if (lstResetPin == HIGH && curResetPin == HIGH) {
         if (resetClkCnt < 4)
            resetClkCnt++;
      } else if (lstResetPin == HIGH && curResetPin == LOW && resetClkCnt >= 4) {
         // it takes 7 clock cycles to reset the system
         delayClkCnt = 7;
         state = States.RESET;
      }
      lstResetPin = curResetPin;

      switch (state) {
         case POWERUP:
            break;
         case RESET:
            if (delayClkCnt > 0)
               delayClkCnt--;
            else {
               pins.setBusStatusPins(BusStatus.Pass);

               _nmiLatched.Clear(); // Debounce NMI

               _clock.setClockCounter(10); // Debounce prefixes and cycle counter
               _lastInstrSetPrefix = false;
               _pauseInterrupts = false;

               // Reset registers
               _registers.Flags = 0x0000;
               _registers.ES = 0;
               _registers.SS = 0;
               _registers.DS = 0;
               _registers.CS = 0xFFFF;
               _registers.IP = 0;

               _pfqInAddress = 0;
               prefetch_queue_count = 0;

               pins.setAddrBusPins(0xFFFF0);
               state = States.RUN;
            }
            break;
         case RUN:
            break;
         case HALT:
            break;
         default:
            break;
      }
      // _pfqInAddress = _registers.IP;
      // prefetch_queue_count = 0;

      // ResetSequence();

      // while (true) {
      //    // clockCounter = 0;
      //    if (_deviceAdapter.getResetPin() != 0)
      //       ResetSequence();

      //    // Wait for cycle counter to expire before processing traps or next instruction
      //    if (_clock.getClockCounter() > 0) {
      //       _clock.WaitForFallingEdge();
      //    }

      //    // Don't poll for interrupts between a Prefixes and instructions
      //    if (_clock.getClockCounter() == 0 && !_pauseInterrupts) {
      //       if (_nmiLatched.IsSet()) {
      //          NmiHandler();
      //       } else if (_deviceAdapter.getIntrPin() != 0 && Flag_i() != 0) {
      //          IntrHandler();
      //       } else if (Flag_t() != 0) {
      //          TrapHandler();
      //       }
      //    }

      //    // Process new instruction when previous instruction's cycle counter has expired
      //    // Debounce prefixes after a non-prefix instruction is executed
      //    if (_clock.getClockCounter() == 0) {
      //       _lastInstrSetPrefix = false;
      //       _pauseInterrupts = false;
      //       ExecuteNewInstr();
      //       // clockCounter=0;
      //       if (_lastInstrSetPrefix == false)
      //          _biu.setPrefixFlags((byte) 0x00);
      //    }

      //    // Fill prefetch queue between instructions
      //    if (prefetch_queue_count < 4) {
      //       PfqAddByte();
      //    }
      // }

   }
}