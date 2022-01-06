package com.realityshards;

import com.realityshards.pycpu.OsROM;
import com.realityshards.pycpu.RamBlock;
import com.realityshards.pycpu.UserRom;
import com.realityshards.pycpu.pycpu;

public class Main {

    public static void main(String[] args) {
	// write your code here

        OsROM osROM = new OsROM((short)0x0000);
        UserRom usROM = new UserRom((short)0x4000, 0x1000, "test1");
        RamBlock usRAM = new RamBlock((short)0x8000, 0x1000);

        System.out.println("About to instantiate CPU");

        pycpu cpu = new pycpu(usROM, osROM, usRAM, null );

        System.out.println("About to initialize CPU");

        cpu.init();

        System.out.println(cpu.toString());

        System.out.println("CPU is initialized, now running 10 cycles");

        cpu.cycle(10);
        System.out.println(cpu.toString());

        System.out.println("CPU is running 10 cycles");

        cpu.cycle(10);
        System.out.println(cpu.toString());

        System.out.println("CPU is running 10 cycles");

        cpu.cycle(10);
        System.out.println(cpu.toString());

        System.out.println("CPU is running 10 cycles");

        cpu.cycle(10);
        System.out.println(cpu.toString());

    }
}
