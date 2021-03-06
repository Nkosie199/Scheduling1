package simulator; 
/**
 * A CPU object stores the currently executing process, provides a method
 * for performing context switches, and records the 
 * number of context switches that have occurred during execution.
 * 
 * @author Stephan Jamieson
 * @version 8/3/15
 */
public class CPU  {

    private ProcessControlBlock currentProcess;
    private int contextSwitches;
    
    CPU() {
        contextSwitches = 0;
        currentProcess = null;
    }  
    
    /**
     * Obtain the currently executing process.
     */
    public ProcessControlBlock getCurrentProcess() {
        assert(!isIdle());
        return currentProcess;
    }

    /**
     * Exeucte the current process in user space until it terminates or blocks for IO.
     * <p>
     * The CPU will update the system timer to indicate the amout of user time spent processing.
     * <p>
     * The method returns the quantity of time used.
     * <p>
     * @return number of time units used.
     */
    int execute() {
        int units = -1;
        if (this.isIdle()) {
            units = 0;
        }
        else {
            Instruction instr = getCurrentProcess().getInstruction();
//            System.out.println("CPU execute() debug current process: "+getCurrentProcess());
//            System.out.println("CPU execute() debug print instr: "+instr);
            assert(instr instanceof CPUInstruction);
            try{
                units = ((CPUInstruction)instr).execute();
            }
            catch(Exception e){
                System.out.println("");
                System.out.println("Error from CPU.execute(): "+e);
                System.out.println("");
            }
            Config.getSimulationClock().advanceUserTime(units);
            
            if (getCurrentProcess().hasNextInstruction()) { 
                // && getCurrentProcess().getInstruction().getClass().equals(IOInstruction.class) [additional check]
                
                getCurrentProcess().nextInstruction();  //sets instruction to the index of the next instruction
                instr = getCurrentProcess().getInstruction();
//                System.out.println("CPU execute() debug print instr (2): "+instr);
                assert(instr instanceof IOInstruction);
                try{
                    IOInstruction ioInst = (IOInstruction)getCurrentProcess().getInstruction();
                    TRACE.SYSCALL(SystemCall.IO_REQUEST, Config.getDevice(ioInst.getDeviceID()).toString(), ioInst.getDuration(), getCurrentProcess());
                    Config.getSimulationClock().logSystemCall();
                    Config.getKernel().syscall(SystemCall.IO_REQUEST, ioInst.getDeviceID(), new Integer(ioInst.getDuration()));
                    TRACE.SYSCALL_END();
                }
                catch(Exception e){
                    System.out.println("");
                    System.out.println("Error from CPU.execute(): "+e);
                    System.out.println("");
                }
                
            }
            else {
                // Terminate process
                TRACE.SYSCALL(SystemCall.TERMINATE_PROCESS, getCurrentProcess());
                Config.getSimulationClock().logSystemCall();
                Config.getKernel().syscall(SystemCall.TERMINATE_PROCESS, getCurrentProcess().getPID());
                TRACE.SYSCALL_END();
            }            
        }
        return units;

    }
    /**
     * Execute the current process in user space for the given number of time units.
     * <p>
     * If the current cpu burst can complete in the given time, then the CPU will execute 
     * the next instruction in the 'program'. This must be a system call (either I/O or terminate).
     * Either will cause this process to be switched out.
     * <p>
     * The CPU will update the system timer to indicate the amount of user time spent processing.
     * <p>
     * The method returns the quantity of unused time unit. A value greater than zero means that 
     * the current cpu burst was completed. A value of zero means the current cpu 
     * burst may or may not have completed.
     * 
     * @return number of unused time units.
     */
    int execute(int timeUnits) {
        int remainder = -1;
        if (this.isIdle()) {
            Config.getSimulationClock().advanceSystemTime(timeUnits);
            remainder = 0;
        }
        else {
            Instruction instr = getCurrentProcess().getInstruction();
//            System.out.println("CPU execute(timeUnits) debug print instr: "+instr);
            assert(instr instanceof CPUInstruction);
            try{
                remainder = ((CPUInstruction)instr).execute(timeUnits);
            }
            catch(Exception e){
                System.out.println("");
                System.out.println("Error from CPU.execute(timeUnits) "+e);
                System.out.println("");
            }
            if (remainder>=0) {
                // CPU burst completed.
                // Invoke following IO instruction.
                Config.getSimulationClock().advanceUserTime(timeUnits-remainder);

                if (getCurrentProcess().hasNextInstruction()) {
                    getCurrentProcess().nextInstruction();
                    assert(getCurrentProcess().getInstruction() instanceof IOInstruction);
                    IOInstruction ioInst = (IOInstruction)getCurrentProcess().getInstruction();
                    TRACE.SYSCALL(SystemCall.IO_REQUEST, Config.getDevice(ioInst.getDeviceID()).toString(), ioInst.getDuration(), getCurrentProcess());
                    Config.getSimulationClock().logSystemCall();
                    Config.getKernel().syscall(SystemCall.IO_REQUEST, ioInst.getDeviceID(), new Integer(ioInst.getDuration()));
                    TRACE.SYSCALL_END();

                }
                else {
                    // Terminate process
                    TRACE.SYSCALL(SystemCall.TERMINATE_PROCESS, getCurrentProcess());
                    Config.getSimulationClock().logSystemCall();
                    Config.getKernel().syscall(SystemCall.TERMINATE_PROCESS, getCurrentProcess().getPID());
                    TRACE.SYSCALL_END();
                }
            }
            else {
                remainder = 0;
                Config.getSimulationClock().advanceUserTime(timeUnits);
            }
            
        }
        return remainder;
    }
        
    /**
     * Determine whether the CPU is idle (<code>getCurrentProcess()==null</code>).
     */
    public boolean isIdle() { return currentProcess==null; }
    
    private static String format(ProcessControlBlock process) {
        if (process==null) {
            return "{Idle}";
        }
        else {
            return process.toString();
        }
    }
  
    /**
     * Switch the current process out and the given process in. 
     * 
     * @return the previously executing process.
     */    
    public ProcessControlBlock contextSwitch(ProcessControlBlock process) {
        contextSwitches++;
        ProcessControlBlock out = currentProcess;;
        currentProcess = process;
        TRACE.PRINTF(1, "Time: %010d Kernel: Context Switch %s, %s).\n", Config.getSimulationClock().getSystemTime(), format(out), format(process)); 
        Config.getSimulationClock().logContextSwitch();
        
        // MY CODE FOR AUTOMARKER
        try{
            currentProcess.setState(ProcessControlBlock.State.RUNNING);
        }catch(Exception e){          
        }
        // END OF MY CODE FOR AUTOMARKER
        
        return out;
    }
    
    /**
     * Obtain the number of context switches.
     */
    public int getContextSwitches() { return contextSwitches; }
}
