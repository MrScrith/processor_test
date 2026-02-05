package com.realityshards.pycpu.interfaces;

public interface i_pybus {

    int getSize ();

    char getBaseAddress();

    char read_mem(char address);

    boolean write_mem(char address, char value);

    boolean init();
}
