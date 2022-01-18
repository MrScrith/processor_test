package com.realityshards.pycpu;


import com.realityshards.pycpu.interfaces.i_pybus;
//import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class PyCPU
{
    // Definition of instructions
    private static final byte INST_COPY = 0x0;
    private static final byte INST_SET = 0x1;
    private static final byte INST_INCDEC = 0x2;
    private static final byte INST_ADDSUB = 0x3;
    private static final byte INST_MULDIV = 0x4;
    private static final byte INST_ANDOR = 0x5;
    private static final byte INST_NOTNEG = 0x6;
    private static final byte INST_SHIFT = 0x7;
    private static final byte INST_SETVAL = 0x8;
    private static final byte INST_STACK = 0x9;

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
    // These instructions are 'read only' so cannot be used for a 'destination'
    // they are only updated by internal processes.
    // the only exception is the flags register which is 'clear on read', which is in effect an external action.
    private static final byte REG_INST = 0x8;
    private static final byte REG_PC = 0x9;
    private static final byte REG_STACK = 0xA;
    private static final byte REG_ALU = 0xB;
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

    private i_pybus userRom;
    private i_pybus mainRom;
    private i_pybus ramBlock;
    private ArrayList<i_pybus> peripherals = new ArrayList<i_pybus>();

    private short RegJump;    // Jump Register (if jump instruction set this is the address to jump to).
    private short RegMemAdd;  // Memory Address Register
    private short RegMemData; // Memory Data Register (value to write to or read from memory)
    private short RegInst;    // Instruction Register
    private short RegPC;      // Program Counter Register
    private short RegStack;   // Stack Pointer Register
    private short RegALU;     // ALU Output Register
    private short RegFlags;   // Flags Register
    private short[] RegGp = new short[5];    // 5 General Purpose Registers.

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
        RegPC = 0;
        RegStack = 0;
        RegALU = 0;
        RegFlags = 0;
        Arrays.fill(RegGp,(short)0);
        /*
            64k Address space:
                0x0000 - 0x0FFF : OS ROM is 4k
                0x1000 - 0x1FFF : OS RAM is 4k
                0x2000 - 0x3FFF : Peripheral space is 8K
                0x4000 - 0x7FFF : User ROM is 16k, 1k to 16k can be utilized (based on item)
                0x8000 - 0xFFFF : User RAM is 32k, 1k to 32k can be utilized (based on item)
         */
        mainRom.init((short)0x0000);
        //mainRam.init((short)0x2000);
        if ( userRom != null)
        {
            userRom.init((short)0x4000);
        }
        else
        {
            RegFlags |= FLAG_ERROR_BIT;
            retVal = false;
        }

        if ( ramBlock != null)
        {
            ramBlock.init((short)0x8000);
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
        // IIII SSSS DDDT JJJP
        byte inst = (byte)((RegInst >> 12) & 0xF);
        byte source = (byte)((RegInst >> 8) & 0xF);
        byte dest = (byte)((RegInst >> 5) & 0x7);
        byte jump = (byte)((RegInst >> 1) & 0x7);
        boolean signed = ( (RegInst & 0x0010) == 0x0010);
        boolean modPos = ( (RegInst & 0x0001) == 0x0001);

        switch ( inst )
        {
            case INST_COPY:
                instruction_copy(source, dest, signed, modPos);
                break;
            case INST_SET:
                instruction_set(dest, modPos);
                break;
            case INST_INCDEC:
                instruction_incdec(source, signed, modPos);
                break;
            case INST_ADDSUB:
                instruction_addsub(source, dest, signed, modPos);
                break;
            case INST_MULDIV:
                instruction_muldiv(source, dest, signed, modPos);
                break;
            case INST_ANDOR:
                instruction_andor(source, dest, modPos);
                break;
            case INST_NOTNEG:
                instruction_notneg(source, modPos);
                break;
            case INST_SHIFT:
                instruction_shift(source, modPos);
                break;
            case INST_SETVAL:
                instruction_setval(dest);
                break;
            case INST_STACK:
                instruction_stack(source, dest, signed, modPos);
                break;
        }

        boolean doJump = false;

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

    private void instruction_copy(byte source, byte dest, boolean signed, boolean modPos)
    {
        if ( modPos )
        {
            write_to_reg(dest,read_from_reg(source));
        }
        else
        {
            write_to_reg(dest, (short)0x0);
            RegFlags |= FLAG_ZERO_BIT;
        }

    }

    private void instruction_set (byte dest, boolean modPos)
    {
        if ( modPos )
        {
            write_to_reg(dest,(short)1);
        }
        else
        {
            write_to_reg(dest, (short)-1);
            RegFlags |= FLAG_NEGATIVE_BIT;
        }
    }

    private void instruction_incdec (byte source, boolean signed, boolean modPos)
    {
        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        if ( modPos )
        {
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
        else
        {
            if ( signed )
            {
                int tmpVal = read_from_reg(source) - 1;

                if ( tmpVal == 0 )
                {
                    RegFlags |= FLAG_ZERO_BIT;
                }
                else if ( tmpVal < Short.MIN_VALUE )
                {
                    tmpVal = Short.MAX_VALUE;
                    RegFlags |= FLAG_BORROW_BIT;
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

                if ( val == 0)
                {
                    RegFlags |= FLAG_ZERO_BIT;
                }
                if ( val < 0 )
                {
                    val = SHORT_UNSIGNED_MAX;
                    RegFlags |= FLAG_BORROW_BIT;
                }

                RegALU = (short)val;
            }
        }
    }

    private void instruction_addsub(byte source, byte source_two, boolean signed, boolean modPos)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        if ( modPos )
        {
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
        }
        else
        {
            if ( signed )
            {
                tmpVal =  read_from_reg(source) - read_from_reg(source_two);

                if ( tmpVal < Short.MIN_VALUE )
                {
                    RegFlags |= FLAG_BORROW_BIT;
                    tmpVal = Short.MAX_VALUE;
                }

            }
            else
            {
                tmpVal = Short.toUnsignedInt(read_from_reg(source));
                tmpVal -= Short.toUnsignedInt(read_from_reg(source_two));

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

    private void instruction_muldiv(byte source, byte source_two, boolean signed, boolean modPos)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        if ( modPos )
        {
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
        }
        else
        {
            if ( signed )
            {
                tmpVal = read_from_reg(source) / read_from_reg(source_two);

            }
            else
            {
                tmpVal = Short.toUnsignedInt(read_from_reg(source));
                tmpVal = tmpVal /  Short.toUnsignedInt(read_from_reg(source_two));
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

    private void instruction_andor (byte source, byte source_two, boolean modPos)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        if ( modPos )
        {

            tmpVal = read_from_reg(source) & read_from_reg(source_two);

        }
        else
        {

            tmpVal = read_from_reg(source) | read_from_reg(source_two);

        }

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

        RegALU = (short)tmpVal;
    }

    private void instruction_notneg (byte source, boolean modPos)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        if ( modPos )
        {
            tmpVal = read_from_reg(source) ^ SHORT_UNSIGNED_MAX;
        }
        else
        {

            tmpVal = -read_from_reg(source);

        }

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

        RegALU = (short)tmpVal;
    }

    private void instruction_shift (byte source, boolean modPos)
    {
        int tmpVal;

        // This is an ALU operation, clear the flags register of ALU flags.
        RegFlags &= 0xFFF0;

        if ( modPos )
        {
            tmpVal = read_from_reg(source) << 1;
        }
        else
        {

            tmpVal = read_from_reg(source) >> 1;

        }

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

        RegALU = (short)tmpVal;
    }

    private void instruction_setval (byte dest)
    {
        loadNextInstruction(true);
        write_to_reg(dest, read_from_reg(REG_INST));
    }

    // TODO Implement Stack Instruction
    private void instruction_stack(byte source, byte dest, boolean signed, boolean modPos)
    {
        // Process for stack operation.
        // Stack register points to the beginning of the stack
        // Assembler keeps track of offset from stack beginning
        // Stack is kept in OS Ram for OS operations and User RAM for User operation
        //
        // On call, with a pos mod, a 'function call' happens
        //  1 : Current stack address is pushed to memory
        //  2 : Next PC value is pushed to memory
        //  3 : Stack address is updated to next address
        //  4 : Value of GP1 is copied to PC (target of function call)
        //  5 : Execution continues at location of new function
        //
        // On call, with a neg mod, a 'function return' happens
        //  1 : Next PC value is popped from memory and written to PC Reg
        //  2 : Previous stack address is pulled from memory and written to Stack Reg
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
                "RegJump=" + RegJump +
                ", RegMemAdd=" + RegMemAdd +
                ", RegMemData=" + RegMemData +
                ", RegInst=" + RegInst +
                ", RegPC=" + RegPC +
                ", RegStack=" + RegStack +
                ", RegALU=" + RegALU +
                ", RegFlags=" + RegFlags +
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
