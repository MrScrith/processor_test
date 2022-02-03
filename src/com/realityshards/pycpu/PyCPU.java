package com.realityshards.pycpu;


import com.realityshards.pycpu.interfaces.i_pybus;
//import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class PyCPU
{
    // Definition of instructions
    private static final byte INST_COPY = 0x00;
    private static final byte INST_SET0 = 0x01;
    private static final byte INST_SET1 = 0x02;
    private static final byte INST_SETN1 = 0x03;
    private static final byte INST_UINC = 0x04;
    private static final byte INST_SINC = 0x05;
    private static final byte INST_UDEC = 0x06;
    private static final byte INST_SDEC = 0x07;
    private static final byte INST_UADD = 0x08;
    private static final byte INST_SADD = 0x09;
    private static final byte INST_USUB = 0x0A;
    private static final byte INST_SSUB = 0x0B;
    private static final byte INST_UMUL = 0x0C;
    private static final byte INST_SMUL = 0x0D;
    private static final byte INST_UDIV = 0x0E;
    private static final byte INST_SDIV = 0x0F;
    private static final byte INST_AND = 0x10;
    private static final byte INST_OR = 0x11;
    private static final byte INST_NOT = 0x12;
    private static final byte INST_NEG = 0x13;
    private static final byte INST_BSL = 0x14;
    private static final byte INST_BSR = 0x15;
    private static final byte INST_SETVAL = 0x16;
    private static final byte INST_FNC = 0x17;
    private static final byte INST_FNR = 0x18;

    // Definition of jumps
    private static final byte JUMP_NONE = 0x0;
    private static final byte JUMP_GTZ  = 0x1;
    private static final byte JUMP_EZ   = 0x2;
    private static final byte JUMP_GTEZ = 0x3;
    private static final byte JUMP_LTZ  = 0x4;
    private static final byte JUMP_NZ   = 0x5;
    private static final byte JUMP_LTEZ = 0x6;
    private static final byte JUMP_JUMP = 0x7;

    // Definition of registers
    private static final byte REG_JUMP = 0x0;
    private static final byte REG_MEMADD = 0x1;
    private static final byte REG_MEMDATA = 0x2;
    private static final byte REG_GP0 = 0x3;
    private static final byte REG_GP1 = 0x4;
    private static final byte REG_GP2 = 0x5;
    private static final byte REG_GP3 = 0x6;
    private static final byte REG_GP4 = 0x7;
    private static final byte REG_GP5 = 0x8;
    private static final byte REG_GP6 = 0x9;
    private static final byte REG_GP7 = 0xA;
    private static final byte REG_INST = 0xB;
    private static final byte REG_PC = 0xC;
    private static final byte REG_STACK = 0xD;
    private static final byte REG_ALU = 0xE;
    private static final byte REG_FLAGS = 0xF;

    // These flags are set in the flag register on ALU operations
    // These values should be publicly available to make it easier to test for them
    // being set by an operation.
    public static final short FLAG_CARRY_BIT    = 0x0001;
    public static final short FLAG_BORROW_BIT   = 0x0002;
    public static final short FLAG_ZERO_BIT     = 0x0004;
    public static final short FLAG_NEGATIVE_BIT = 0x0008;
    public static final short FLAG_RESET_BIT    = (short)0x8000;
    public static final short FLAG_ERROR_BIT    = 0x4000;

    public static final int SHORT_UNSIGNED_MAX = 65535;

    private final i_pybus userRom;
    private final i_pybus mainRom;
    private final i_pybus ramBlock;
    private final ArrayList<i_pybus> peripherals = new ArrayList<i_pybus>();

    private short RegJump;    // Jump Register (if jump instruction set this is the address to jump to).
    private short RegMemAdd;  // Memory Address Register
    private short RegMemData; // Memory Data Register (value to write to or read from memory)
    private short RegInst;    // Instruction Register
    private short RegPC;      // Program Counter Register
    private short RegStack;   // Stack Pointer Register
    private short RegALU;     // ALU Output Register
    private short RegFlags;   // Flags Register
    private final short[] RegGp = new short[8];    // 8 General Purpose Registers.

    public PyCPU (i_pybus uRom, i_pybus mRom, i_pybus uRam, i_pybus[] periphs)
    {
        userRom = uRom;
        mainRom = mRom;
        ramBlock = uRam;
        if ( periphs != null )
        {
            Collections.addAll(peripherals,periphs);
        }
    }



    public boolean init()
    {
        boolean retVal = true;

        RegJump = 0;
        RegMemAdd = 0;
        RegMemData = 0;
        RegInst = 0;
        RegALU = 0;
        RegFlags = 0;
        Arrays.fill(RegGp,(short)0);

        // Init RAM and ROMs
        mainRom.init();
        RegPC = mainRom.getBaseAddress();

        if ( userRom != null)
        {
            userRom.init();
        }
        else
        {
            RegFlags |= FLAG_ERROR_BIT;
            retVal = false;
        }

        if ( ramBlock != null)
        {
            ramBlock.init();

            RegStack = ramBlock.getBaseAddress();
        }
        else
        {
            RegFlags |= FLAG_ERROR_BIT;
            retVal = false;
        }

        loadNextInstruction(false);

        return retVal;
    }

    public void cycle(int count)
    {
        for ( int i = 0; i < count; i++ )
        {
            // Execute is evaluated first, then it's return is
            // passed to load, which decides if the PC is incremented or not
            // (aka. if a jump instruction happened or not)
            loadNextInstruction(executeInstruction());
        }
    }

    public boolean reset(boolean cold)
    {
        boolean retVal = true;

        if( cold )
        {
            retVal = init();
        }
        else
        {
            RegPC = 0;
            RegStack = 0; // TODO Same as init, find where it starts.
            RegFlags |= FLAG_RESET_BIT;
        }

        return retVal;
    }

    public boolean addPeripheral(i_pybus periph)
    {
        return peripherals.add(periph);
    }

    public boolean removePeripheral(int periphIndex)
    {
        i_pybus item = peripherals.remove(periphIndex);

        return (item != null);
    }

    private boolean executeInstruction()
    {
        // IIIII JJJ AAAA BBBB
        byte inst = (byte)((RegInst >> 11) & 0x1F);
        byte source = (byte)((RegInst >> 4) & 0xF);
        byte dest = (byte)(RegInst & 0xF);
        byte jump = (byte)((RegInst >> 8) & 0x7);
        boolean doJump = false;

        switch ( inst )
        {
            case INST_COPY:
                instruction_copy(source, dest);
                break;
            case INST_SET0:
                instruction_set(dest, 0);
                break;
            case INST_SET1:
                instruction_set(dest, 1);
                break;
            case INST_SETN1:
                instruction_set(dest, -1);
                break;
            case INST_SINC:
                instruction_inc(source, true);
                break;
            case INST_UINC:
                instruction_inc(source, false);
                break;
            case INST_SDEC:
                instruction_dec(source, true);
                break;
            case INST_UDEC:
                instruction_dec(source, false);
                break;
            case INST_SADD:
                instruction_add(source, dest, true);
                break;
            case INST_UADD:
                instruction_add(source, dest, false);
                break;
            case INST_SSUB:
                instruction_sub(source, dest, true);
                break;
            case INST_USUB:
                instruction_sub(source, dest, false);
                break;
            case INST_SMUL:
                instruction_mul(source, dest, true);
                break;
            case INST_UMUL:
                instruction_mul(source,dest, false);
                break;
            case INST_SDIV:
                instruction_div(source, dest, true);
                break;
            case INST_UDIV:
                instruction_div(source, dest, false);
                break;
            case INST_AND:
                instruction_and(source, dest);
                break;
            case INST_OR:
                instruction_or(source, dest);
                break;
            case INST_NOT:
                instruction_not(source);
                break;
            case INST_NEG:
                instruction_neg(source);
                break;
            case INST_BSL:
                instruction_bsl(source);
                break;
            case INST_BSR:
                instruction_bsr(source);
                break;
            case INST_SETVAL:
                instruction_setval(dest);
                break;
            case INST_FNC:
                instruction_fnc();
                doJump = true;
                jump = JUMP_NONE;
                // Set jump to None to ignore any jump instructions that might have
                // accidentally been tacked on.
                break;
            case INST_FNR:
                instruction_fnr();
                doJump = true;
                jump = JUMP_NONE;
                // Set jump to None to ignore any jump instructions that might have
                // accidentally been tacked on.
                break;
        }


        switch ( jump )
        {
            case JUMP_NONE:
                // NO action
                break;
            case JUMP_GTZ:
                if ( ( RegFlags & (FLAG_ZERO_BIT | FLAG_NEGATIVE_BIT )) == 0 )
                {
                    doJump = true;
                }
                break;
            case JUMP_EZ:
                if ( ( RegFlags & FLAG_ZERO_BIT ) > 0 )
                {
                    doJump = true;
                }
                break;
            case JUMP_GTEZ:
                if ( ( RegFlags & ( FLAG_NEGATIVE_BIT )) == 0 )
                {
                    doJump = true;
                }
                break;
            case JUMP_LTZ:
                if ( ( RegFlags & ( FLAG_NEGATIVE_BIT ) ) > 0 )
                {
                    doJump = true;
                }
                break;
            case JUMP_LTEZ:
                if ( ( RegFlags & (FLAG_ZERO_BIT | FLAG_NEGATIVE_BIT )) > 0 )
                {
                    doJump = true;
                }
                break;
            case JUMP_NZ:
                if ( ( RegFlags & ( FLAG_ZERO_BIT ) ) == 0 )
                {
                    doJump = true;
                }
                break;
            case JUMP_JUMP:
                doJump = true;
        }

        if ( doJump )
        {
            // Load the address to jump to.
            RegPC = RegJump;
        }

        return !doJump;
    }

    private void instruction_copy(byte source, byte dest)
    {
        write_to_reg(dest,read_from_reg(source));
    }

    private void instruction_set (byte dest, int value)
    {
        write_to_reg(dest,(short)value);

        if ( value == 0 )
        {
            RegFlags |= FLAG_ZERO_BIT;
        }
        else if ( value < 0 )
        {
            RegFlags |= FLAG_NEGATIVE_BIT;
        }
    }

    private void instruction_inc (byte source, boolean signed)
    {
        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        if ( signed )
        {
            int tmpVal = read_from_reg(source) + 1;

            if ( tmpVal == 0 )
            {
                RegFlags |= FLAG_ZERO_BIT;
            }
            else if ( tmpVal > Short.MAX_VALUE )
            {
                tmpVal = Short.MIN_VALUE;
                RegFlags |= FLAG_CARRY_BIT | FLAG_NEGATIVE_BIT;
            }
            else if ( tmpVal < 0 )
            {
                RegFlags |= FLAG_NEGATIVE_BIT;
            }

            RegALU = (short)tmpVal;

        }
        else
        {
            int val = Short.toUnsignedInt(read_from_reg(source));
            val += 1;
            if ( val > SHORT_UNSIGNED_MAX)
            {
                val = 0;
                RegFlags |= FLAG_CARRY_BIT | FLAG_ZERO_BIT;
            }

            RegALU = (short)val;
        }
    }

    private void instruction_dec (byte source, boolean signed)
    {
        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        if ( signed )
        {
            int tmpVal = read_from_reg(source) - 1;

            if ( tmpVal == 0 )
            {
                RegFlags |= FLAG_ZERO_BIT;
            }
            else if ( tmpVal > Short.MAX_VALUE )
            {
                tmpVal = Short.MIN_VALUE;
                RegFlags |= FLAG_CARRY_BIT | FLAG_NEGATIVE_BIT;
            }
            else if ( tmpVal < 0 )
            {
                RegFlags |= FLAG_NEGATIVE_BIT;
            }

            RegALU = (short)tmpVal;

        }
        else
        {
            int val = Short.toUnsignedInt(read_from_reg(source));
            val -= 1;
            if ( val > SHORT_UNSIGNED_MAX)
            {
                val = 0;
                RegFlags |= FLAG_CARRY_BIT | FLAG_ZERO_BIT;
            }

            RegALU = (short)val;
        }
    }

    private void instruction_add(byte source, byte source_two, boolean signed)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        if ( signed )
        {
            tmpVal = read_from_reg(source) + read_from_reg(source_two);

            if ( tmpVal > Short.MAX_VALUE )
            {
                RegFlags |= FLAG_CARRY_BIT;
            }

        }
        else
        {
            tmpVal = Short.toUnsignedInt(read_from_reg(source));
            tmpVal += Short.toUnsignedInt(read_from_reg(source_two));
            if ( tmpVal > SHORT_UNSIGNED_MAX )
            {
                tmpVal = 0;
                RegFlags |= FLAG_CARRY_BIT;
            }
        }


        RegALU = (short)tmpVal;

        if ( RegALU < 0 )
        {
            RegFlags |= FLAG_NEGATIVE_BIT;
        }
        else if ( RegALU == 0 )
        {
            RegFlags |= FLAG_ZERO_BIT;
        }

    }


    private void instruction_sub(byte source, byte source_two, boolean signed)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        if ( signed )
        {
            tmpVal = read_from_reg(source) + read_from_reg(source_two);

            if ( tmpVal > Short.MAX_VALUE )
            {
                RegFlags |= FLAG_CARRY_BIT;
            }

        }
        else
        {
            tmpVal = Short.toUnsignedInt(read_from_reg(source));
            tmpVal += Short.toUnsignedInt(read_from_reg(source_two));
            if ( tmpVal > SHORT_UNSIGNED_MAX )
            {
                tmpVal = 0;
                RegFlags |= FLAG_CARRY_BIT;
            }
        }


        RegALU = (short)tmpVal;

        if ( RegALU < 0 )
        {
            RegFlags |= FLAG_NEGATIVE_BIT;
        }
        else if ( RegALU == 0 )
        {
            RegFlags |= FLAG_ZERO_BIT;
        }

    }

    private void instruction_mul(byte source, byte source_two, boolean signed)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        if ( signed )
        {
            tmpVal = read_from_reg(source) * read_from_reg(source_two);

            if ( tmpVal > Short.MAX_VALUE )
            {
                RegFlags |= FLAG_CARRY_BIT;
            }

        }
        else
        {
            tmpVal = Short.toUnsignedInt(read_from_reg(source));
            tmpVal = tmpVal * Short.toUnsignedInt(read_from_reg(source_two));
            if ( tmpVal > SHORT_UNSIGNED_MAX )
            {
                tmpVal = 0;
                RegFlags |= FLAG_CARRY_BIT;
            }
        }


        RegALU = (short)tmpVal;

        if ( tmpVal > Short.MAX_VALUE )
        {
            RegFlags |= FLAG_CARRY_BIT;
        }
        else if ( tmpVal < 0 )
        {
            RegFlags |= FLAG_NEGATIVE_BIT;
        }
        else if ( tmpVal == 0 )
        {
            RegFlags |= FLAG_ZERO_BIT;
        }
    }


    private void instruction_div(byte source, byte source_two, boolean signed)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;


        if ( signed )
        {
            tmpVal = read_from_reg(source) / read_from_reg(source_two);

        }
        else
        {
            tmpVal = Short.toUnsignedInt(read_from_reg(source));
            tmpVal = tmpVal /  Short.toUnsignedInt(read_from_reg(source_two));
        }

        RegALU = (short)tmpVal;

        if ( tmpVal > Short.MAX_VALUE )
        {
            RegFlags |= FLAG_CARRY_BIT;
        }
        else if ( tmpVal < 0 )
        {
            RegFlags |= FLAG_NEGATIVE_BIT;
        }
        else if ( tmpVal == 0 )
        {
            RegFlags |= FLAG_ZERO_BIT;
        }
    }

    private void instruction_and (byte source, byte source_two)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        tmpVal = read_from_reg(source) & read_from_reg(source_two);

        if ( tmpVal == 0 )
        {
            RegFlags |= FLAG_ZERO_BIT;
        }

        RegALU = (short)tmpVal;
    }

    private void instruction_or (byte source, byte source_two)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        tmpVal = read_from_reg(source) | read_from_reg(source_two);

        if ( tmpVal == 0 )
        {
            RegFlags |= FLAG_ZERO_BIT;
        }

        RegALU = (short)tmpVal;
    }

    private void instruction_not (byte source)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        tmpVal = read_from_reg(source) ^ SHORT_UNSIGNED_MAX;

        if ( tmpVal == 0 )
        {
            RegFlags |= FLAG_ZERO_BIT;
        }

        RegALU = (short)tmpVal;
    }

    private void instruction_neg (byte source)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        tmpVal = -read_from_reg(source);

        if ( tmpVal == 0 )
        {
            RegFlags |= FLAG_ZERO_BIT;
        }

        RegALU = (short)tmpVal;
    }

    private void instruction_bsl (byte source )
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        tmpVal = read_from_reg(source) << 1;

        if ( tmpVal == 0 )
        {
            RegFlags |= FLAG_ZERO_BIT;
        }

        RegALU = (short)tmpVal;
    }

    private void instruction_bsr (byte source )
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        tmpVal = read_from_reg(source) >> 1;

        if ( tmpVal == 0 )
        {
            RegFlags |= FLAG_ZERO_BIT;
        }

        RegALU = (short)tmpVal;
    }

    private void instruction_setval (byte dest)
    {
        loadNextInstruction(true);
        write_to_reg(dest, read_from_reg(REG_INST));
    }

    private void instruction_fnc()
    {
        // Process for stack operation.
        // Stack register points to the beginning of the stack
        // Beginning of the stack holds the next available address (aka, end of the stack)
        //
        //
        // On call a 'function call' happens
        //  1 : Stack End is loaded into MemAddr
        //  2 : Current stack address is pushed to memory, MemAddr incremented
        //  3 : Next PC value is pushed to memory, MemAddr incremented
        //  4 : Stack address is updated to current address, next memory address written to this address
        //  5 : Execution continues at location of new function
        //


        // Function call stack instruction

        //  1 : Stack End is loaded into MemAddr
        RegMemAdd = RegStack;
        updateMemoryAddress();

        //  2 : Current stack address is pushed to memory, MemAddr incremented
        RegMemData = RegStack;
        writeMemoryData();
        RegMemAdd = (short)(RegMemAdd + 1);
        updateMemoryAddress();

        //  3 : Next PC value is pushed to memory, MemAddr incremented
        RegMemData = (short)(RegPC + 1);
        writeMemoryData();
        RegMemAdd = (short)(RegMemAdd + 1);
        updateMemoryAddress();

        //  4 : Stack address is updated to current address, next memory address written to this address
        RegStack = RegMemAdd;
        RegMemData = (short)(RegMemAdd + 1);
        writeMemoryData();

        //  5 : Execution continues at location of new function
    }

    private void instruction_fnr()
    {
        // Process for stack operation.
        // Stack register points to the beginning of the stack
        // Beginning of the stack holds the next available address (aka, end of the stack)
        //
        // On call a 'function return' happens
        //  1 : Next PC value is pulled from memory and written to PC Reg
        //  2 : Previous stack address is pulled from memory and written to Stack Reg
        //  3 : Execution continues at location of function return.

        // Function return stack instruction

        //  1 : Next PC value is pulled from memory and written to PC Reg
        // Next PC value is Stack Address - 1
        RegMemAdd = (short)(RegStack - 1);
        updateMemoryAddress();
        RegJump = RegMemData; // put in Jump register and allow the jump action to handle it.

        //  2 : Previous stack address is pulled from memory and written to Stack Reg
        RegMemAdd = (short)(RegStack - 2);
        updateMemoryAddress();
        RegStack = RegMemData;

        //  3 : Execution continues at location of function return.
    }

    private void write_to_reg(byte dest, short value)
    {
        switch ( dest )
        {
            case REG_JUMP:
                RegJump = value;
                break;
            case REG_MEMADD:
                RegMemAdd = value;
                updateMemoryAddress(); // actions taken on update of value (like it would be in an actual CPU)
                break;
            case REG_MEMDATA:
                RegMemData = value;
                writeMemoryData(); // actions taken on update of value (like it would be in an actual CPU)
                break;
            case REG_GP0:
                RegGp[0] = value;
                break;
            case REG_GP1:
                RegGp[1] = value;
                break;
            case REG_GP2:
                RegGp[2] = value;
                break;
            case REG_GP3:
                RegGp[3] = value;
                break;
            case REG_GP4:
                RegGp[4] = value;
                break;
            case REG_GP5:
                RegGp[5] = value;
                break;
            case REG_GP6:
                RegGp[6] = value;
                break;
            case REG_GP7:
                RegGp[7] = value;
                break;
        }

    }

    private short read_from_reg(byte source)
    {
        short value = 0;
        switch ( source )
        {
            case REG_JUMP:
                value = RegJump;
                break;
            case REG_MEMADD:
                value = RegMemAdd;
                break;
            case REG_MEMDATA:
                value = RegMemData;
                break;
            case REG_GP0:
                value = RegGp[0];
                break;
            case REG_GP1:
                value = RegGp[1];
                break;
            case REG_GP2:
                value = RegGp[2];
                break;
            case REG_GP3:
                value = RegGp[3];
                break;
            case REG_GP4:
                value = RegGp[4];
                break;
            case REG_GP5:
                value = RegGp[5];
                break;
            case REG_GP6:
                value = RegGp[6];
                break;
            case REG_GP7:
                value = RegGp[7];
                break;
            case REG_INST:
                value = RegInst;
                break;
            case REG_PC:
                value = RegPC;
                break;
            case REG_STACK:
                value = RegStack;
                break;
            case REG_ALU:
                value = RegALU;
                break;
            case REG_FLAGS:
                value = RegFlags;
                RegFlags = 0; // Flags register is 'clear on read'
                break;

        }

        return value;
    }

    private void loadNextInstruction(boolean incPC)
    {
        if ( incPC )
        {
            RegPC++;
        }

        if (  RegPC < 0x1000 )
        {
            // Code is in OS ROM space
            RegInst = mainRom.read_mem(RegPC);
        }
        else if ( RegPC > 0x4000 & RegPC < 0x7FFF )
        {
            // Code is in User ROM space
            RegInst = userRom.read_mem(RegPC);
        }
        else
        {
            // We are off in the weeds, reset to address 0 and set error and reset flags
            RegPC = 0;
            RegFlags |= (FLAG_RESET_BIT | FLAG_ERROR_BIT);
            RegInst = mainRom.read_mem(RegPC);
        }
    }

    @Override
    public String toString() {
        return "pycpu{" +
                "RegJump=0x" + Integer.toUnsignedString((((int)RegJump) & 0x0000FFFF),16) +
                ", RegMemAdd=0x" + Integer.toUnsignedString((((int)RegMemAdd) & 0x0000FFFF),16) +
                ", RegMemData=0x" + Integer.toUnsignedString((((int)RegMemData) & 0x0000FFFF),16) +
                ", RegInst=0x" + Integer.toUnsignedString((((int)RegInst) & 0x0000FFFF),16) +
                ", RegPC=0x" + Integer.toUnsignedString((((int)RegPC) & 0x0000FFFF),16) +
                ", RegStack=0x" + Integer.toUnsignedString((((int)RegStack) & 0x0000FFFF),16) +
                ", RegALU=0x" + Integer.toUnsignedString((((int)RegALU) & 0x0000FFFF),16) +
                ", RegFlags=0x" + Integer.toUnsignedString((((int)RegFlags) & 0x0000FFFF),16) +
                ", RegGp=" + Arrays.toString(RegGp) +
                '}';
    }

    private void updateMemoryAddress()
    {
        if ( RegMemAdd > ramBlock.getBaseAddress() &
                RegMemAdd < (ramBlock.getBaseAddress() + ramBlock.getSize()) )
        {
            RegMemData = ramBlock.read_mem(RegMemAdd);
        }
        else
        {
            RegFlags |= FLAG_ERROR_BIT;
        }
    }

    private void writeMemoryData()
    {
        if ( RegMemAdd > ramBlock.getBaseAddress() &
                RegMemAdd < (ramBlock.getBaseAddress() + ramBlock.getSize()) )
        {
            ramBlock.write_mem(RegMemAdd, RegMemData);
        }
        else
        {
            RegFlags |= FLAG_ERROR_BIT;
        }

    }
}
