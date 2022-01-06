package com.realityshards.pycpu;

import com.realityshards.pycpu.interfaces.i_pybus;

import java.util.Arrays;

public class RamBlock implements i_pybus
{
    short BaseAddress = 0;
    int Size;
    short[] RamData;

    public RamBlock (short baseAddress, int size)
    {
        Size = size;
        BaseAddress = baseAddress;
        RamData = new short[Size];
    }

    @Override
    public int getSize ()
    {
        return Size;
    }

    @Override
    public short getBaseAddress ()
    {
        return BaseAddress;
    }

    @Override
    public short read_mem (short address)
    {
        int val = 0;

        if (address >= BaseAddress & address < (BaseAddress + Size)) {
            val = address - BaseAddress;
        }
        return RamData[val];
    }

    @Override
    public boolean write_mem (short address, short value)
    {
        boolean retVal = false;

        if (address >= BaseAddress & address < (BaseAddress + Size)) {
            RamData[address - BaseAddress] = value;
            retVal = true;
        }

        return retVal;
    }

    @Override
    public boolean init (short baseAddress)
    {
        Arrays.fill(RamData, (short)0);
        return true;
    }
}
