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

        int UserRamSize = cliArgs.switchIntValue("-uramsize", 0x1000);



        osROM = new OsROM((short)0x0000, OsRomName);

        usROM = new UserRom((short)0x4000, UserRomSize, UserRomName);

        RamBlock usRAM = new RamBlock((short)0x8000, UserRamSize);

        System.out.println("About to instantiate CPU");

        PyCPU cpu = new PyCPU(usROM, osROM, usRAM, null );

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
