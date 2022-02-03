package com.realityshards;

import com.realityshards.pycpu.*;

public class Main {

    public static void main(String[] args) {
	// write your code here
        OsROM osROM;
        UserRom usROM;

        CliArgs cliArgs = new CliArgs(args);

        String OsRomName = cliArgs.switchValue("-osrom", "osrom.dat");

        String UserRomName = cliArgs.switchValue("-usrrom", "userrom.dat");
        int UserRomSize = cliArgs.switchIntValue("-uromsize", 0x1000);

        int RamSize = cliArgs.switchIntValue("-uramsize", 0x1000);



        osROM = new OsROM(0x0000, OsRomName);

        usROM = new UserRom(0x4000, UserRomSize, UserRomName);

        RamBlock RAMBlock = new RamBlock(0x8000, RamSize);

        System.out.println("About to instantiate CPU");

        PyCPU cpu = new PyCPU(usROM, osROM, RAMBlock, null );

        System.out.println("About to initialize CPU");

        cpu.init();

        System.out.println(cpu.toString());

        System.out.println("CPU is initialized, now running 1 cycle");

        cpu.cycle(1);
        System.out.println(cpu.toString());

        System.out.println("CPU is running 1 cycle");

        cpu.cycle(1);
        System.out.println(cpu.toString());

        System.out.println("CPU is running 1 cycle");

        cpu.cycle(1);
        System.out.println(cpu.toString());

        System.out.println("CPU is running 1 cycle");

        cpu.cycle(1);
        System.out.println(cpu.toString());

        System.out.println("CPU Done.");

    }
}
