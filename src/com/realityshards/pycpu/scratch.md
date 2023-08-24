New assembler strategy:

 * Function objects: 
   * Each function gets their own object block
   * Object block has 
     * list of variables
     * list of execution lines
 * Variable list:
   * Each variable has an index and an initial value. Index is the position from the stack for that variable
 * Execution lines:
   * Each line of execution placed in list as a tuple
     * Val 0 in tuple: line of execution 
     * Val 1 in tuple: number of assembly lines this will take
       * 0 for labels
       * 1 for normal lines of code
       * 2 for "Set Value" instructions (instruction and value)
 
First task of assembler is to parse the asm file, filling each function object.
Function 0 is the 'main' function that starts on boot.
This function has a few initial values and actions but immediately executes first function call to do something
Variables must have an initial value, if there is a variable that gets declared without an initial value the value of 0 is written to the stack.
Variables with @@ are global variables and are stored in the stack for function 0, thus function 0 has its own variables as well as global variables stored in its stack.
A global list of variables will be maintained so when writing the output the assembler can lookup the position from 0 where the value will be stored.

 * Labels: (labelName)
 * LocalVariables: @varName=2
 * GlobalVariables: @@varName=2
 * Comments: # comment
 * Functions: !funcName

fnc:funcName