import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.sql.SQLOutput;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class J_Debugger {

    private int[] breakPointLines = {3, 5};

    public static void main(String[] args) {
        J_Debugger debugger = new J_Debugger();
        debugger.LaunchingConnector();
    }

    public void LaunchingConnector() {
        System.out.println("Hello world!");

        //attaching();

        directlyAttaching();

        System.out.println("Done");
    }


    private ThreadReference getMainThread(VirtualMachine vm) {
        return vm.allThreads()
                .stream()
                .filter(t -> "main".equals(t.name()))
                .findFirst()
                .orElse(null);
    }

    private VirtualMachine attachToVirtualMachine(AttachingConnector connector)
            throws IOException, IllegalConnectorArgumentsException {
        final Map<String, Connector.Argument> arguments = connector.defaultArguments();
        arguments.get("hostname").setValue("localhost");
        arguments.get("port").setValue("8000");
        return connector.attach(arguments);
    }

    private AttachingConnector getConnectorByTransportName(VirtualMachineManager manager, String transport) {
        return manager.attachingConnectors()
                .stream()
                .filter(c -> transport.equals(c.transport().name()))
                .findFirst()
                .orElse(null);
    }

    private void directlyAttaching() {
        final VirtualMachineManager manager = Bootstrap.virtualMachineManager();
        final AttachingConnector connector = getConnectorByTransportName(manager, "dt_socket");
        if (connector == null) {
            System.err.printf("Connector with transport %s not found%n", "dt_socket");
            return;
        }

        try {
            final VirtualMachine vm = attachToVirtualMachine(connector);

            ThreadReference mainThread  = getMainThread(vm);
            if (mainThread == null) {
                System.err.println("Main thread not found");
                return;
            }

            ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
            classPrepareRequest.addClassFilter("Test");
            classPrepareRequest.enable();

            Scanner scanner = new Scanner(System.in);

            EventSet eventSet = null;
            while ((eventSet = vm.eventQueue().remove()) != null) {
                for (Event event : eventSet) {
                    if (event instanceof ClassPrepareEvent) {
                        System.out.println("Setting Breakpoints");
                        setBreakPoints(vm, (ClassPrepareEvent)event);
                    }

                    if (event instanceof BreakpointEvent) {
                        event.request().disable();
                        BreakpointEvent ev = (BreakpointEvent) event;

                        System.out.println("Breakpoint reached at line:" +  ev.location().lineNumber());

                        boolean con = true;
                        while (con) {
                            System.out.println("what do you want to do?");
                            switch (scanner.nextLine()) {
                                case "p" -> {   // print Variables
                                    displayVariables((BreakpointEvent) event);
                                }
                                case "n" ->   // run to next breakpoint
                                        con = false;
                                case "i" -> {   // step in
                                    //enableStepRequest(vm, (BreakpointEvent)event);
                                    if (ev.location().toString().contains("Test" + ":" + breakPointLines[breakPointLines.length - 1])) {
                                        StepRequest stepRequest = vm.eventRequestManager().createStepRequest(ev.thread(), StepRequest.STEP_LINE, StepRequest.STEP_INTO);
                                        stepRequest.addClassFilter("*Test");
                                        stepRequest.addCountFilter(1);
                                        stepRequest.enable();
                                    }
                                    con = false;
                                }
                                case "o" -> {   //step over
                                    if (ev.location().toString().contains("Test" + ":" + breakPointLines[breakPointLines.length - 1])) {
                                        StepRequest stepRequest = vm.eventRequestManager().createStepRequest(ev.thread(), StepRequest.STEP_LINE, StepRequest.STEP_OVER);
                                        stepRequest.addClassFilter("*Test");
                                        stepRequest.addCountFilter(1);
                                        stepRequest.enable();
                                    }
                                    con = false;
                                }
                                default -> con = false;
                            }
                        }
                    }

                    if (event instanceof StepEvent) {
                        System.out.println("StepEvent");
                        //displayVariables((StepEvent) event);
                        StepEvent se = (StepEvent)event;
                        System.out.println("step halted in " + se.location().method().name() + " at ");
                        System.out.println(se.location().lineNumber() + ", " + se.location().codeIndex());
                        printVars(se.thread().frame(0));
                        vm.eventRequestManager().deleteEventRequest(se.request());
                    }
                    vm.resume();
                }
            }



            mainThread.resume();
            vm.resume();
            vm.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void attaching() {
        AttachingConnector con = null;

        for (Connector c: Bootstrap.virtualMachineManager().allConnectors()) {
            if (c.name().equals("com.sun.jdi.SocketAttach")) {
                con = (AttachingConnector) c; break;
            }
        }
        Map<String, Connector.Argument> args = con.defaultArguments();
        args.get("port").setValue("8000");


        try {
            VirtualMachine vm = con.attach(args);

/*            Process proc = vm.process();
            new Redirection(proc.getErrorStream(), System.err).start();
            new Redirection(proc.getInputStream(), System.out).start();
            //attaching();*/

            List<ThreadReference> threads = vm.allThreads();
            for (ThreadReference t : threads) {
                if (t.name().equals("main")) {
                    StackFrame frame = t.frames().get(0);
                    printVars(frame);
                    break;
                }
            }



        } catch (Exception e) {
            e.printStackTrace();
            /* error */
        }
    }

    static void printVars(StackFrame frame) {
        try {
            for (LocalVariable v: frame.visibleVariables()) {
                System.out.println(v.name() + ": " + v.type().name() + " = ");
                printValue(frame.getValue(v));
                System.out.println();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void setBreakPoints(VirtualMachine vm, ClassPrepareEvent event) throws AbsentInformationException {
        ClassType classType = (ClassType) event.referenceType();
        for(int br: breakPointLines) {
            Location location = classType.locationsOfLine(br).get(0);
            BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(location);
            bpReq.enable();
        }
    }

    public void displayVariables(LocatableEvent event) throws IncompatibleThreadStateException, AbsentInformationException {
        StackFrame stackFrame = event.thread().frame(0);
        if (stackFrame.location().toString().contains("Test")) {
            Map<LocalVariable, Value> visibleVariables = stackFrame.getValues(stackFrame.visibleVariables());
            System.out.println("Variables at " + stackFrame.location().lineNumber() + " > ");
            Iterator it = visibleVariables.entrySet().iterator();

            while(it.hasNext()) {
                Map.Entry<LocalVariable, Value> entry = (Map.Entry)it.next();
                System.out.println(entry.getKey().name() + " = " + entry.getValue());
            }
        }
    }

    public void enableStepRequest(VirtualMachine vm, BreakpointEvent event) {
        if(event.location().toString().contains("Test"+":"+breakPointLines[breakPointLines.length-1])) {
            StepRequest stepRequest = vm.eventRequestManager().createStepRequest(event.thread(), StepRequest.STEP_LINE, StepRequest.STEP_OVER);
            stepRequest.enable();
        }

    }
    static void printValue(Value val) {
        if (val instanceof IntegerValue) {
            System.out.println(((IntegerValue)val).value() + " ");
        } else if (val instanceof StringReference) {
            System.out.println(((StringReference)val).value() + " ");
        } else if (val instanceof ArrayReference) {
            for (Value v: ((ArrayReference)val).getValues()) {
                printValue(v);
                System.out.println();
            }
        } //TODO else
    }

}