package com.realityshards;

public class Main {

    public static void main(String[] args) {
	// write your code here


        short sval1 = 30000;
        short sval2 = 20000;
        short sval3 = (short) (sval1 * sval2);
        int val = (sval1 * sval2);
        short sval4 = (short)val;
        System.out.printf("Short value is decimal %d, hex %x\n", sval3, sval3);
        System.out.printf("Int value of short is decimal %d, hex %x\n",
                Short.toUnsignedInt(sval3),
                Short.toUnsignedInt(sval3));
        System.out.printf("Short value of int is decimal %d, hex %x\n", sval4, sval4);
    }
}
