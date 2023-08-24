package com.realityshards.pycpu;

public class utils {

    public static int signed_char_to_int(char value)
    {
        int result = 0xEFFF & value;

        if ( 0x8000 == (value & 0x8000))
        {
            result = result | 0xFFFF0000;
        }

        return result;
    }

    public static char signed_int_to_char(int value)
    {
        char result = (char)(0xEFFF & value);

        if ( value < 0)
        {
            result = (char)(result | 0x8000);
        }

        return result;
    }
}
