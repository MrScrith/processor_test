package com.realityshards.pycpu;

import com.realityshards.pycpu.interfaces.i_pybus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class OsROM implements i_pybus {

    private short RomBaseAddress = 0;
    private static final short RomSize = 0x1000;
    private short[] RomData;

    public OsROM (short romBaseAddress) {
        RomBaseAddress = romBaseAddress;
        RomData = new short[RomSize];
    }

    @Override
    public int getSize ()
    {
        return RomSize;
    }

    @Override
    public short getBaseAddress() {
        return RomBaseAddress;
    }

    @Override
    public short read_mem(short address) {
        int val = 0;


        if ( address >= RomBaseAddress & address < (RomBaseAddress + RomSize ) )
        {
            val = address - RomBaseAddress;
        }
        return RomData[val];
    }

    @Override
    public boolean write_mem(short address, short value) {
        return false;
    }

    @Override
    public boolean init(short baseAddress) {
        RomBaseAddress = baseAddress;

        String curDir = System.getProperty("user.dir");
        System.out.println("The current working directory is: " + curDir);
        try
        {
            // create reader
            FileInputStream fis = new FileInputStream(new File("osrom.dat"));
            BufferedInputStream reader = new BufferedInputStream(fis);

            // read a byte at a time
            int ch, offset = 0;
            byte[] buff = new byte[2];

            System.out.printf("Number of bytes available: %d\n", reader.available());

            while((ch = reader.read(buff, 0, 2)) != -1)
            {
                if ( ch != 2 )
                {
                    // if we read less than 2 bytes than the dat file is corrupted.
                    break;
                }
                short v1 = (short) (0x00FF & buff[0]);
                short v2 = (short) (0x00FF & buff[1]);
                short rval = (short) (( Short.toUnsignedInt(v1) << 8 ) | Short.toUnsignedInt(v2));
                RomData[offset] = rval;
                offset++;
                if ( offset > RomSize)
                {
                    break;
                }
            }

            reader.close();
        }
        catch ( IOException ex )
        {
            System.out.print(ex.getMessage());
            ex.printStackTrace();
        }
        return true;
    }



}
