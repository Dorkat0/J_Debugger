import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


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


            EventSet eventSet = null;
            while ((eventSet = vm.eventQueue().remove()) != null) {
                for (Event event : eventSet) {
                    if (event instanceof ClassPrepareEvent) {
                        System.out.println("1");
                        setBreakPoints(vm, (ClassPrepareEvent)event);
                    }

                    if (event instanceof BreakpointEvent) {
                        event.request().disable();
                        System.out.println("2");
                        displayVariables((BreakpointEvent) event);
                        enableStepRequest(vm, (BreakpointEvent)event);
                    }

                    if (event instanceof StepEvent) {
                        System.out.println("3");
                        displayVariables((StepEvent) event);
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
        //for(int lineNumber: new ArrayList<>(1,2,3) {
        Location location = classType.locationsOfLine(4).get(0);
        BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(location);
        bpReq.enable();
        //}
    }

    public void displayVariables(LocatableEvent event) throws IncompatibleThreadStateException, AbsentInformationException {
        StackFrame stackFrame = event.thread().frame(0);
        if (stackFrame.location().toString().contains("Test")) {
            Map<LocalVariable, Value> visibleVariables = stackFrame.getValues(stackFrame.visibleVariables());
            System.out.println("Variables at " + stackFrame.location().toString() + " > ");
            Iterator var4 = visibleVariables.entrySet().iterator();

            while(var4.hasNext()) {
                Map.Entry<LocalVariable, Value> entry = (Map.Entry)var4.next();
                PrintStream var10000 = System.out;
                String var10001 = ((LocalVariable)entry.getKey()).name();
                var10000.println(var10001 + " = " + entry.getValue());
            }
        }
    }

    public void enableStepRequest(VirtualMachine vm, BreakpointEvent event) {
        String var10000 = event.location().toString();
        String var10001 = "Test";
        if (var10000.contains(var10001 + ":" + this.breakPointLines[this.breakPointLines.length - 1])) {
            StepRequest stepRequest = vm.eventRequestManager().createStepRequest(event.thread(), -2, 2);
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