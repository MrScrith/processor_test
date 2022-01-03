package com.realityshards.pycpu.interfaces;

public interface i_pybus {

    int baseAddress();

    int read_mem(int address);

    boolean write_mem(int address, int value);

    boolean init(int baseAddress);
}
