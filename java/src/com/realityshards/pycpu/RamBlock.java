package com.realityshards.pycpu;

import com.realityshards.pycpu.interfaces.i_pybus;

import java.util.Arrays;

public class RamBlock implements i_pybus
{
    char BaseAddress = 0;
    int Size;
    char[] RamData;

    public RamBlock (int baseAddress, int size)
    {
        Size = size;
        BaseAddress = (char)baseAddress;
        RamData = new char[Size];
    }

    @Override
    public int getSize ()
    {
        return Size;
    }

    @Override
    public char getBaseAddress ()
    {
        return BaseAddress;
    }

    @Override
    public char read_mem (char address)
    {
        int val = 0;

        if (address >= BaseAddress & address < (BaseAddress + Size)) {
            val = address - BaseAddress;
        }
        return RamData[val];
    }

    @Override
    public boolean write_mem (char address, char value)
    {
        boolean retVal = false;

        if (address >= BaseAddress & address < (BaseAddress + Size)) {
            RamData[address - BaseAddress] = value;
            retVal = true;
        }

        return retVal;
    }

    @Override
    public boolean init ()
    {
        Arrays.fill(RamData, (char)0);
        return true;
    }
}
