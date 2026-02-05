package com.realityshards.pycpu;

import com.realityshards.pycpu.interfaces.i_pybus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class OsROM implements i_pybus {

    private char RomBaseAddress = 0;
    private static final char RomSize = 0x2000;
    private char[] RomData;
    private String RomFile;

    public OsROM (int romBaseAddress, String romFileName) {
        RomBaseAddress = (char)romBaseAddress;
        RomData = new char[RomSize];
        RomFile = romFileName;
    }

    public OsROM (char romBaseAddress ) {
        RomBaseAddress = romBaseAddress;
        RomData = new char[RomSize];
        RomFile = "osrom.dat";
    }
    @Override
    public int getSize ()
    {
        return RomSize;
    }

    @Override
    public char getBaseAddress() {
        return RomBaseAddress;
    }

    @Override
    public char read_mem(char address) {
        int val = 0;

        if ( (RomBaseAddress + address) < (RomBaseAddress + RomSize ) )
        {
            val = address - RomBaseAddress;
        }
        return RomData[val];
    }

    @Override
    public boolean write_mem(char address, char value) {
        return false;
    }

    @Override
    public boolean init() {

        String curDir = System.getProperty("user.dir");
        System.out.println("The current working directory is: " + curDir);
        try
        {
            // create reader
            FileInputStream fis = new FileInputStream(new File(RomFile));
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
                char v1 = (char) (0x00FF & buff[1]);
                char v2 = (char) (0x00FF & buff[0]);
                char rval = (char) (( v1 << 8 ) | v2);
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
