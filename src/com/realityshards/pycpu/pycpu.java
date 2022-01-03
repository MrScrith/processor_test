package com.realityshards.pycpu;


import com.realityshards.pycpu.interfaces.i_pybus;
import com.sun.deploy.security.SelectableSecurityManager;

import java.util.Arrays;

public class pycpu {

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
    private static final byte INST_ADMINWRITE = 0xF;


    private i_pybus userRom;
    private i_pybus mainRom;
    private i_pybus userRam;
    private i_pybus mainRam;
    private i_pybus[] peripherals;

    private int RegJump;    // Jump Register (if jump instruction set this is the address to jump to).
    private int RegMemAdd;  // Memory Address Register
    private int RegMemData; // Memory Data Register (value to write to or read from memory)
    private int RegInst;    // Instruction Register
    private int RegPC;      // Program Counter Register
    private int RegStack;   // Stack Pointer Register
    private int RegALU;     // ALU Output Register
    private int RegFlags;   // Flags Register
    private int[] RegGp = new int[5];    // 5 General Purpose Registers.

    public pycpu(i_pybus uRom, i_pybus mRom, i_pybus[] periphs)
    {
        userRom = uRom;
        mainRom = mRom;
        peripherals = periphs.clone();
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
        Arrays.fill(RegGp,0);
        /*
            64k Address space:
                OS ROM is 4k
                OS RAM is 4k
                Peripheral space is 8K
                User ROM is 16k, 1k to 16k can be utilized (based on item)
                User RAM is 32k, 1k to 32k can be utilized (based on item)
         */
        mainRom.init(0x1000);
        mainRam.init(0x2000);
        if ( userRom != null)
        {
            userRom.init(0x4000);
        }
        else
        {
            retVal = false;
        }

        if ( userRam != null)
        {
            userRam.init(0x8000);
        }
        else
        {
            retVal = false;
        }

        return retVal;
    }

    public boolean cycle(int count)
    {
        for ( int i = 0; i < count; i++ )
        {
            executeInstruction();
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
        }

        return retVal;
    }

    public boolean addPeripheral(i_pybus periph)
    {

    }

    public boolean removePeripheral(int periphIndex)
    {

    }

    private void executeInstruction()
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
                instruction_copy(source, dest, jump, signed, modPos);
                break;
            case INST_SET:
                instruction_set(source, dest, jump, signed, modPos);
                break;
            case INST_INCDEC:
                instruction_incdec(source, dest, jump, signed, modPos);
                break;
            case INST_ADDSUB:
                instruction_addsub(source, dest, jump, signed, modPos);
                break;
            case INST_MULDIV:
                instruction_muldiv(source, dest, jump, signed, modPos);
                break;
            case INST_ANDOR:
                instruction_andor(source, dest, jump, signed, modPos);
                break;
            case INST_NOTNEG:
                instruction_notneg(source, dest, jump, signed, modPos);
                break;
            case INST_SHIFT:
                instruction_shift(source, dest, jump, signed, modPos);
                break;
            case INST_SETVAL:
                instruction_setval(source, dest, jump, signed, modPos);
                break;
            case INST_STACK:
                instruction_stack(source, dest, jump, signed, modPos);
                break;
            case INST_ADMINWRITE:
                instruction_adminwrite(source, dest, jump, signed, modPos);
                break;
        }
    }

    private void instruction_copy(byte source, byte dest, byte jump, boolean signed, boolean modPos)
    {

    }


    private void instruction_set(byte source, byte dest, byte jump, boolean signed, boolean modPos)
    {

    }

    private void instruction_incdec(byte source, byte dest, byte jump, boolean signed, boolean modPos)
    {

    }

    private void instruction_addsub(byte source, byte dest, byte jump, boolean signed, boolean modPos)
    {

    }

    private void instruction_muldiv(byte source, byte dest, byte jump, boolean signed, boolean modPos)
    {

    }

    private void instruction_andor(byte source, byte dest, byte jump, boolean signed, boolean modPos)
    {

    }

    private void instruction_notneg(byte source, byte dest, byte jump, boolean signed, boolean modPos)
    {

    }

    private void instruction_shift(byte source, byte dest, byte jump, boolean signed, boolean modPos)
    {

    }

    private void instruction_setval(byte source, byte dest, byte jump, boolean signed, boolean modPos)
    {

    }

    private void instruction_stack(byte source, byte dest, byte jump, boolean signed, boolean modPos)
    {

    }

    private void instruction_adminwrite(byte source, byte dest, byte jump, boolean signed, boolean modPos)
    {

    }

    private void write_to_reg(byte dest, int value)
    {
        private int RegJump;    // Jump Register (if jump instruction set this is the address to jump to).
        private int RegMemAdd;  // Memory Address Register
        private int RegMemData; // Memory Data Register (value to write to or read from memory)
        private int RegInst;    // Instruction Register
        private int RegPC;      // Program Counter Register
        private int RegStack;   // Stack Pointer Register
        private int RegALU;     // ALU Output Register
        private int RegFlags;   // Flags Register
        private int[] RegGp = new int[5];    // 5 General Purpose Registers.
    }

    private int read_from_reg(byte source)
    {

    }
}
