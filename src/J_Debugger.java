import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.*;


public class J_Debugger {

    private int[] breakPointLines = {7, 10, 12};
    private HashSet<String> currentlyActiveMethods = new HashSet<>();

    public static void main(String[] args) {
        J_Debugger debugger = new J_Debugger();
        debugger.LaunchingConnector();
    }

    public void LaunchingConnector() {
        System.out.println("Debugger started!");

        directlyAttaching();

        System.out.println("Debugger ended");
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

/*            MethodEntryRequest methodEntryRequest = vm.eventRequestManager().createMethodEntryRequest();
            methodEntryRequest.addClassFilter("Test");
            methodEntryRequest.enable();

            MethodExitRequest methodExitRequest = vm.eventRequestManager().createMethodExitRequest();
            methodExitRequest.addClassFilter("Test");
            methodExitRequest.enable();*/

            EventSet eventSet = null;
            while ((eventSet = vm.eventQueue().remove()) != null) {
                for (Event event : eventSet) {
                    if (event instanceof ClassPrepareEvent) {
                        System.out.println("Setting Breakpoints");
                        setBreakPoints(vm, (ClassPrepareEvent)event);
                    }

                    if (event instanceof BreakpointEvent) {
                        event.request().disable();
                        LocatableEvent ev = (LocatableEvent) event;
                        System.out.println("Breakpoint reached in " + ev.location().method().name() + " at line:" +  ev.location().lineNumber());
                        menu(vm, ev);
                    }

                    if (event instanceof StepEvent) {
                        LocatableEvent se = (LocatableEvent)event;
                        System.out.print("step halted in " + se.location().method().name() + " at ");
                        System.out.println(se.location().lineNumber());     //", " + se.location().codeIndex()  //actually not needed
                        vm.eventRequestManager().deleteEventRequest(se.request());
                        menu(vm, se);
                    }

                    if (event instanceof  MethodEntryEvent) {
                        currentlyActiveMethods.add(((MethodEntryEvent) event).method().name());
                    }

                    if (event instanceof MethodExitEvent) {
                        currentlyActiveMethods.remove(((MethodExitEvent) event).method().name());
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

    public void setBreakPoints(VirtualMachine vm, ClassPrepareEvent event) throws AbsentInformationException {
        ClassType classType = (ClassType) event.referenceType();
        for(int br: breakPointLines) {
            Location location = classType.locationsOfLine(br).get(0);
            BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(location);
            bpReq.enable();
        }
    }

    public void singleStep(VirtualMachine vm, LocatableEvent ev, int depth) {
        StepRequest stepRequest = vm.eventRequestManager().createStepRequest(ev.thread(), StepRequest.STEP_LINE, depth);
        stepRequest.addClassFilter("*Test");
        stepRequest.addCountFilter(1);
        stepRequest.enable();
    }

    public void displayVariables(LocatableEvent event) throws IncompatibleThreadStateException, AbsentInformationException {
        StackFrame stackFrame = event.thread().frame(0);
        if (stackFrame.location().toString().contains("Test")) {
            Map<LocalVariable, Value> visibleVariables = stackFrame.getValues(stackFrame.visibleVariables());
            System.out.println("Variables at " + stackFrame.location().toString() + " : ");
            Iterator it = visibleVariables.entrySet().iterator();

            while(it.hasNext()) {
                Map.Entry<LocalVariable, Value> entry = (Map.Entry)it.next();
                System.out.println("   " + entry.getKey().name() + " = " + entry.getValue());
                if (entry.getValue().equals("StringReverence") || entry.equals("IntegerValue")) {
                    System.out.println("Not an String or int");
                }
                //TODO
            }
        }
    }

    public void menu(VirtualMachine vm, LocatableEvent ev) throws IncompatibleThreadStateException, AbsentInformationException {
        Scanner scanner = new Scanner(System.in);
        boolean con = true;
        while (con) {
            System.out.println("what do you want to do?");

            switch (scanner.nextLine()) {
                case "p" -> {   // print Variables
                    displayVariables(ev);
                }
                case "n" ->   // run to next breakpoint
                        con = false;
                case "i" -> {   // step in
                    singleStep(vm, ev, StepRequest.STEP_INTO);
                    con = false;
                }
                case "o" -> {   //step over
                    singleStep(vm, ev, StepRequest.STEP_OVER);
                    con = false;
                }
                case "m" -> {
                    System.out.println(currentlyActiveMethods);
                }
                default -> con = false;
            }
        }
    }
}

/*    static void printVars(StackFrame frame) {
        try {
            for (LocalVariable v: frame.visibleVariables()) {
                System.out.println(v.name() + ": " + v.type().name() + " = ");
                printValue(frame.getValue(v));
                System.out.println();
            }
        } catch (Exception e) { e.printStackTrace(); }
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
    }*/

