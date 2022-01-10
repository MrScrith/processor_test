package com.realityshards.pycpu;

import com.realityshards.pycpu.interfaces.i_pybus;

import java.io.*;

public class UserRom implements i_pybus
{

    private short BaseAddress = 0;
    private int Size = 0;
    private final short[] RomData;
    private final String Name;


    public UserRom (short baseAddress, int size, String name)
    {
        BaseAddress = baseAddress;
        Size = size;
        RomData = new short[Size];
        Name = name;
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
        return RomData[val];
    }

    @Override
    public boolean write_mem (short address, short value)
    {
        boolean retVal = false;

        if (address >= BaseAddress & address < (BaseAddress + Size)) {
            RomData[address - BaseAddress] = value;
            retVal = true;
        }

        return retVal;
    }

    @Override
    public boolean init (short baseAddress)
    {
        BaseAddress = baseAddress;

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
                    short v1 = (short) (0x00FF & buff[1]);
                    short v2 = (short) (0x00FF & buff[0]);
                    short rval = (short) ((Short.toUnsignedInt(v1) << 8) | Short.toUnsignedInt(v2));
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
