package com.realityshards.pycpu.interfaces;

public interface i_pybus {

    short baseAddress();

    short read_mem(short address);

    boolean write_mem(short address, short value);

    boolean init(short baseAddress);
}
