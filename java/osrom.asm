# OS v0.1

# Set GP Registers to 0 to start out.
0 : set gp0 0 : 0x0060
1 : set gp1 0 : 0x0080
2 : set gp2 0 : 0x00A0
3 : set gp3 0 : 0x00C0
4 : set gp4 0 : 0x00E0

# Set Jump register to beginning of User ROM area
5 : set jmp   : 0x8001
6 : 4000      : 0x4000
7 : set mar   : 0x8021
8 : 8100      : 0x8100
9 : jsr       : 0x9001

10 : set jmp  : 0x8001
11 : 0x000C   : 0x000C

12 : jmp      : 0xF00F