package com.realityshards.pycpu;

import com.realityshards.pycpu.interfaces.i_pybus;

import java.io.*;

public class UserRom implements i_pybus
{

    private char BaseAddress = 0;
    private int Size = 0;
    private final char[] RomData;
    private final String Name;


    public UserRom (int baseAddress, int size, String name)
    {
        BaseAddress = (char)baseAddress;
        Size = size;
        RomData = new char[Size];
        Name = name;
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

        if ( (BaseAddress + address) < (BaseAddress + Size ) )
        {
            val = address - BaseAddress;
        }
        return RomData[val];
    }

    @Override
    public boolean write_mem (char address, char value)
    {
        boolean retVal = false;

        if (address >= BaseAddress & address < (BaseAddress + Size)) {
            RomData[address - BaseAddress] = value;
            retVal = true;
        }

        return retVal;
    }

    @Override
    public boolean init ()
    {
        boolean retVal = false;

        File datFile = new File(Name + ".dat");

        if (datFile.exists()) {
            try {
                // create reader
                FileInputStream fis = new FileInputStream(datFile);
                BufferedInputStream reader = new BufferedInputStream(fis);

                // read a byte at a time
                int ch, offset = 0;
                byte[] buff = new byte[2];

                System.out.printf("Number of bytes available: %d\n", reader.available());

                while ((ch = reader.read(buff, 0, 2)) != -1) {
                    if (ch != 2) {
                        // if we read less than 2 bytes than the dat file is corrupted.
                        retVal = false;
                        break;
                    }
                    retVal = true;
                    // Little endian, so value 0x1234 is stored 3412
                    char v1 = (char) (0x00FF & buff[1]);
                    char v2 = (char) (0x00FF & buff[0]);
                    char rval = (char) ((v1 << 8) | v2);
                    RomData[offset] = rval;
                    offset++;
                    if (offset > Size) {
                        break;
                    }
                }

                reader.close();
            }
            catch (IOException ex) {
                retVal = false;
                System.out.print(ex.getMessage());
                ex.printStackTrace();
            }

        }
        return retVal;
    }

    public boolean save ()
    {
        boolean retVal = true;

        File datFile = new File(Name + ".dat");

        if ( datFile.canWrite() & datFile.isFile() )
        {
            try
            {
                FileOutputStream fos = new FileOutputStream(datFile);

                for ( int i = 0; i < Size; i++)
                {
                    fos.write(RomData[i]);
                }

                fos.close();
            }
            catch ( IOException ex)
            {
                retVal = false;
                System.out.print(ex.getMessage());
                ex.printStackTrace();
            }
        }
        return retVal;
    }
}
