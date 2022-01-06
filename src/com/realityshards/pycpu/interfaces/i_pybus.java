package com.realityshards.pycpu.interfaces;

public interface i_pybus {

    int getSize ();

    short getBaseAddress();

    short read_mem(short address);

    boolean write_mem(short address, short value);

    boolean init(short baseAddress);
}
