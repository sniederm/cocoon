/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.components.flow.java;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.ClassLoaderRepository;
import org.apache.bcel.verifier.exc.AssertionViolatedException;
import org.apache.bcel.verifier.structurals.*;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

/**
 * The classloader breakes the methods of the classes into pieces and
 * add intercepting code to suspend the execution of the method.
 * 
 * This code is based on the original idea of the BRAKES project.
 * (http://www.cs.kuleuven.ac.be/~eddy/BRAKES/brakes.html).
 *
 * @author <a href="mailto:stephan@apache.org">Stephan Michels</a>
 * @author <a href="mailto:tcurdt@apache.org">Torsten Curdt</a>
 * @version CVS $Id: ContinuationClassLoader.java,v 1.1 2004/03/29 17:47:21 stephan Exp $
 */
public class ContinuationClassLoader extends ClassLoader {

    private static final String CONTINUATION_CLASS =
            "org.apache.cocoon.components.flow.java.Continuation";

    private static final ObjectType CONTINUATION_TYPE = 
            new ObjectType(CONTINUATION_CLASS);

    private static final String STACK_CLASS =
            "org.apache.cocoon.components.flow.java.ContinuationStack";

    private static final ObjectType STACK_TYPE =
            new ObjectType(STACK_CLASS);

    private static final String CONTINUABLE_CLASS =
            "org.apache.cocoon.components.flow.java.Continuable";

    private static final String CONTINUATION_METHOD = "currentContinuation";

    private static final String STACK_METHOD = "getStack";

    private static final String POP_METHOD = "pop";

    private static final String PUSH_METHOD = "push";

    private static final String RESTORING_METHOD = "isRestoring";

    private static final String CAPURING_METHOD = "isCapturing";

    private static boolean currentMethodStatic;

    public ContinuationClassLoader(ClassLoader parent) {
        super(parent);

        if (parent instanceof ContinuationClassLoader)
            throw new IllegalArgumentException("Cannot cascade ContinuationClassLoader");

        Repository.setRepository(new ClassLoaderRepository(parent));
    }

    protected synchronized Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {

        //System.out.println("load class "+name+" classloader="+this);

        Class c = super.loadClass(name, resolve);

        if ((Continuable.class.isAssignableFrom(c)) && 
            (!ContinuationCapable.class.isAssignableFrom(c)) && 
            (!c.isInterface())) {
            JavaClass clazz = Repository.lookupClass(c);

            byte data[] = transform(clazz);
            c = defineClass(name, data, 0, data.length);
        }

        if (c == null)
            throw new ClassNotFoundException(name);

        if (resolve)
            resolveClass(c);

        return c;
    }

    private byte[] transform(JavaClass javaclazz) throws ClassNotFoundException {

        // make all methods of java class continuable
        System.out.println("transforming flow class " + javaclazz.getClassName());

        /*try {
            FileOutputStream fos = new FileOutputStream(javaclazz.getClassName() + ".orig.java");
            JasminVisitor v = new JasminVisitor(javaclazz, fos);
            v.start();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        ClassGen clazz = new ClassGen(javaclazz);

        ConstantPoolGen cp = clazz.getConstantPool();

        // obsolete, but neccesary to execute the InvokeContext
        InstConstraintVisitor icv = new InstConstraintVisitor();
        icv.setConstantPoolGen(cp);

        // vistor to build the frame information
        ExecutionVisitor ev = new ExecutionVisitor();
        ev.setConstantPoolGen(cp);

        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            MethodGen method = new MethodGen(methods[i], clazz.getClassName(), cp);

            currentMethodStatic = methods[i].isStatic();

            if (isValid(method)) {

                // analyse the code of the method to create the frame
                // information about every instruction
                //System.out.println("analyse " + methods[i].getName());
                ControlFlowGraph cfg = new ControlFlowGraph(method);
                analyse(clazz, method, cfg, icv, ev);

                // add intercepting code 
                //System.out.println("rewriting " + methods[i].getName());
                rewrite(method, cfg);

                /*InstructionHandle handle = method.getInstructionList().getStart();
                do {
                  System.out.println(handle);
                } while ((handle = handle.getNext()) != null);*/

                // make last optional check for consistency
                //System.out.println("check " + methods[i].getName());

                /*try {
                    cfg = new ControlFlowGraph(method);
                    analyse(clazz, method, cfg, icv, ev);
                    //printFrameInfo(method, cfg);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new ClassNotFoundException("Rewritten method is not consistent", e);
                }*/

                //methods[i] = method.getMethod();
                clazz.replaceMethod(methods[i], method.getMethod());
            }
        }

        clazz.addInterface("org.apache.cocoon.components.flow.java.ContinuationCapable");

        /*try {
            FileOutputStream fos = new FileOutputStream(clazz.getClassName() + ".rewritten.java");
            JasminVisitor v = new JasminVisitor(clazz.getJavaClass(), fos);
            v.start();
        } catch (Exception e) {
            e.printStackTrace();
        }*/


        /*byte[] changed = clazz.getJavaClass().getBytes();
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(clazz.getClassName() + ".rewritten");
            out.write(changed);
            out.flush();
            out.close();
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
        }*/

        return clazz.getJavaClass().getBytes();
    }

    private boolean isValid(MethodGen m) {
        if (m.getName().equals(Constants.CONSTRUCTOR_NAME))
            return false;
        if (m.getName().equals(Constants.STATIC_INITIALIZER_NAME))
            return false;
        if (m.isNative() || m.isAbstract())
            return false;
        return true;
    }

    private void analyse(ClassGen clazz, MethodGen method, ControlFlowGraph cfg,
                         InstConstraintVisitor icv, ExecutionVisitor ev) {
        // build the initial frame situation for this method.
        Frame vanillaFrame = new Frame(method.getMaxLocals(), method.getMaxStack());
        if (!method.isStatic()) {
            if (method.getName().equals(Constants.CONSTRUCTOR_NAME)) {
                Frame._this = new UninitializedObjectType(new ObjectType(clazz.getClassName()));
                vanillaFrame.getLocals().set(0, new UninitializedObjectType(new ObjectType(clazz.getClassName())));
            } else {
                Frame._this = null;
                vanillaFrame.getLocals().set(0, new ObjectType(clazz.getClassName()));
            }
        }
        // fill local variables with parameter types
        Type[] argtypes = method.getArgumentTypes();
        int twoslotoffset = 0;
        for (int j = 0; j < argtypes.length; j++) {
            if ((argtypes[j] == Type.SHORT) ||
                (argtypes[j] == Type.BYTE) ||
                (argtypes[j] == Type.CHAR) ||
                (argtypes[j] == Type.BOOLEAN)) {
                argtypes[j] = Type.INT;
            }
            vanillaFrame.getLocals().set(twoslotoffset + j + (method.isStatic() ? 0 : 1), argtypes[j]);
            if (argtypes[j].getSize() == 2) {
                twoslotoffset++;
                vanillaFrame.getLocals().set(twoslotoffset + j + (method.isStatic() ? 0 : 1), Type.UNKNOWN);
            }
        }

        icv.setMethodGen(method);

        Vector ics = new Vector(); // Type: InstructionContext
        Vector ecs = new Vector(); // Type: ArrayList (of InstructionContext)

        InstructionContext start = cfg.contextOf(method.getInstructionList().getStart());

        start.execute(vanillaFrame, new ArrayList(), icv, ev);
        // new ArrayList() <=>  no Instruction was executed before
        //                  => Top-Level routine (no jsr call before)
        ics.add(start);
        ecs.add(new ArrayList());

        while (!ics.isEmpty()) {

            InstructionContext u = (InstructionContext) ics.remove(0);
            ArrayList ec = (ArrayList) ecs.remove(0);

            ArrayList oldchain = (ArrayList) (ec.clone());
            ArrayList newchain = (ArrayList) (ec.clone());
            newchain.add(u);

            if ((u.getInstruction().getInstruction()) instanceof RET) {
                // We can only follow _one_ successor, the one after the
                // JSR that was recently executed.
                RET ret = (RET) (u.getInstruction().getInstruction());
                ReturnaddressType t = (ReturnaddressType) u.getOutFrame(oldchain).getLocals().get(ret.getIndex());
                InstructionContext theSuccessor = cfg.contextOf(t.getTarget());

                if (theSuccessor.execute(u.getOutFrame(oldchain), newchain, icv, ev)) {
                    ics.add(theSuccessor);
                    ecs.add(newchain.clone());
                }
            } else { // "not a ret"
                // Normal successors. Add them to the queue of successors.
                InstructionContext[] succs = u.getSuccessors();
                for (int s = 0; s < succs.length; s++) {
                    InstructionContext v = succs[s];

                    if (v.execute(u.getOutFrame(oldchain), newchain, icv, ev)) {
                        ics.add(v);
                        ecs.add(newchain.clone());
                    }
                }
            }

            // Exception Handlers. Add them to the queue of successors.
            ExceptionHandler[] exc_hds = u.getExceptionHandlers();
            for (int s = 0; s < exc_hds.length; s++) {
                InstructionContext v = cfg.contextOf(exc_hds[s].getHandlerStart());
                // TODO: the "oldchain" and "newchain" is used to determine the subroutine
                // we're in (by searching for the last JSR) by the InstructionContext
                // implementation. Therefore, we should not use this chain mechanism
                // when dealing with exception handlers.

                LocalVariables newLocals = u.getOutFrame(oldchain).getLocals();
                OperandStack newStack = new OperandStack(
                        u.getOutFrame(oldchain).getStack().maxStack(),
                        (exc_hds[s].getExceptionType() == null
                        ? Type.THROWABLE
                        : exc_hds[s].getExceptionType()));
                Frame newFrame = new Frame(newLocals, newStack);

                if (v.execute(newFrame, new ArrayList(), icv, ev)) {
                    ics.add(v);
                    ecs.add(new ArrayList());
                }
            }
        }
    }

    private void printFrameInfo(MethodGen method, ControlFlowGraph cfg) {
        InstructionHandle handle = method.getInstructionList().getStart();
        do {
            System.out.println(handle);
            try {
                InstructionContext context = cfg.contextOf(handle);

                Frame f = context.getOutFrame(new ArrayList());

                LocalVariables lvs = f.getLocals();
                System.out.print("Locales: ");
                for (int i = 0; i < lvs.maxLocals(); i++) {
                    System.out.print(lvs.get(i) + ",");
                }

                OperandStack os = f.getStack();
                System.out.print(" Stack: ");
                for (int i = 0; i < os.size(); i++) {
                    System.out.print(os.peek(i) + ",");
                }
                System.out.println();
            }
            catch (AssertionViolatedException ave) {
                System.out.println("no frame information");
            }
        }
        while ((handle = handle.getNext()) != null);
    }

    private void rewrite(MethodGen method, ControlFlowGraph cfg) throws ClassNotFoundException {

        InstructionFactory insFactory = new InstructionFactory(method.getConstantPool());
        Vector invokeIns = new Vector();
        int count = 0;
        InstructionList insList = method.getInstructionList();
        InstructionHandle ins = insList.getStart();
        InstructionList restorer = new InstructionList();
        while (ins != null) {
            InstructionHandle next = ins.getNext();

            // if not traversed by the analyser, then don't rewrite
            InstructionContext context = null;
            Frame frame = null;
            try {
                context = cfg.contextOf(ins);
                frame = context.getOutFrame(new ArrayList());
            } catch (AssertionViolatedException ave) {}


            if ((frame!=null) && (rewriteable(method, ins))) {
                // Add frame saver and restorer for the current breakpoint
              
                // determine type of object for the method invocation
                InvokeInstruction invoke = (InvokeInstruction)ins.getInstruction();
                Type[] arguments = invoke.getArgumentTypes(method.getConstantPool());  
                ObjectType objecttype = null;
                if (!(invoke instanceof INVOKESTATIC)) {
                    objecttype = (ObjectType)context.getInFrame().getStack().peek(arguments.length);
                }
                 
                InstructionList rList = restoreFrame(method, ins, insFactory, frame, objecttype);
                insList.append(ins, saveFrame(method, ins, count++, insFactory, frame));
                invokeIns.addElement(rList.getStart());
                restorer.append(rList);
            } 

            // remove all new's
            if ((frame != null) && (ins.getInstruction().getOpcode() == Constants.NEW)) {
                try {
                    // remove additional dup's
                    while ((next != null) && (next.getInstruction().getOpcode() == Constants.DUP)) {
                        context = cfg.contextOf(next);
                        frame = context.getOutFrame(new ArrayList());
                        InstructionHandle newnext = next.getNext();
                        insList.delete(next);
                        next = newnext;
                    }

                    InstructionTargeter[] targeter = ins.getTargeters();
                    if (targeter != null) {
                        InstructionHandle newnext = ins.getNext();
                        for (int i = 0; i < targeter.length; i++)
                            targeter[i].updateTarget(ins, newnext);
                    }
                    insList.delete(ins);
                } catch (TargetLostException tle) {
                    throw new ClassNotFoundException(tle.getMessage(), tle);
                }
            } else if ((frame != null) && (ins.getInstruction().getOpcode() == Constants.INVOKESPECIAL)) {
                // duplicate stack before invokespecial to insert uninitialized object
                frame = context.getInFrame();

                InvokeInstruction invoke = (InvokeInstruction)ins.getInstruction();
                Type[] arguments = invoke.getArgumentTypes(method.getConstantPool());

                OperandStack os = frame.getStack();
                Type type = os.peek(arguments.length);
                if (type instanceof UninitializedObjectType) {
                    ObjectType objecttype = ((UninitializedObjectType) type).getInitialized();

                    InstructionList duplicator = duplicateStack(method, invoke, objecttype);
    
                    InstructionTargeter[] targeter = ins.getTargeters();
                    if (targeter!=null) {
                        InstructionHandle newnext = duplicator.getStart();
                        for(int i=0; i<targeter.length; i++)
                            targeter[i].updateTarget(ins, newnext);
                    }

                    insList.insert(ins, duplicator);
                }
            }

            ins = next;
        }
        InstructionHandle firstIns = insList.getStart();
        if (count > 0) {
            InstructionHandle[] tableTargets = new InstructionHandle[count];
            int[] match = new int[count];
            for (int i = 0; i < count; i++)
                match[i] = i;
            invokeIns.copyInto(tableTargets);

            insList.insert(restorer);

            // select frame restorer
            insList.insert(new TABLESWITCH(match, tableTargets, firstIns));
            insList.insert(insFactory.createInvoke(STACK_CLASS, getPopMethod(Type.INT), Type.INT, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
            insList.insert(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));

            //insList.insert(insFactory.createPrintln("--- restoring invocation "+method)); 

            // test if the continuation should be restored
            insList.insert(new IFEQ(firstIns));
            insList.insert(insFactory.createInvoke(CONTINUATION_CLASS, RESTORING_METHOD, Type.BOOLEAN, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
            insList.insert(insFactory.createLoad(CONTINUATION_TYPE, method.getMaxLocals()));
        }

        // get stack from current continuation and store in the last local variable
        insList.insert(insFactory.createStore(STACK_TYPE, method.getMaxLocals()+1));
        insList.insert(insFactory.createInvoke(CONTINUATION_CLASS, STACK_METHOD, STACK_TYPE,
                       Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        InstructionHandle restore_handle = insList.insert(insFactory.createLoad(CONTINUATION_TYPE, method.getMaxLocals()));

        // if not continuation exists, create empty stack
        insList.insert(new GOTO(firstIns));
        insList.insert(insFactory.createStore(STACK_TYPE, method.getMaxLocals()+1));
        insList.insert(insFactory.createInvoke(STACK_CLASS, Constants.CONSTRUCTOR_NAME, Type.VOID, Type.NO_ARGS, Constants. INVOKESPECIAL));
        insList.insert(insFactory.createDup(STACK_TYPE.getSize()));
        insList.insert(insFactory.createNew(STACK_TYPE));

        // test if no current continuation exists
        insList.insert(new IFNONNULL(restore_handle));
        insList.insert(insFactory.createLoad(CONTINUATION_TYPE, method.getMaxLocals()));

        // get current continuation and store in the next to last local variable
        insList.insert(insFactory.createStore(CONTINUATION_TYPE, method.getMaxLocals()));
        insList.insert(insFactory.createInvoke(CONTINUATION_CLASS, CONTINUATION_METHOD, CONTINUATION_TYPE,
                       Type.NO_ARGS, Constants.INVOKESTATIC));

        // make room for additional objects
        method.setMaxLocals(method.getMaxLocals() + 2);
        method.setMaxStack(method.getMaxStack() + 2);
    }

    private InstructionList duplicateStack(MethodGen method, InvokeInstruction invoke, ObjectType objecttype) throws ClassNotFoundException {

        // reconstruction of an uninitialed object to call the constructor.
        InstructionFactory insFactory = new InstructionFactory(method.getConstantPool());
        InstructionList insList = new InstructionList();

        Type[] arguments = invoke.getArgumentTypes(method.getConstantPool());
        // pop all arguments for the constructor from the stack
        for (int i = arguments.length-1; i>=0; i--) {
            Type type = arguments[i];
            if (type instanceof BasicType) {
                if ((type.getSize() < 2) && (!type.equals(Type.FLOAT)))
                    type = Type.INT;
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                insList.append(new SWAP());
                insList.append(insFactory.createInvoke(STACK_CLASS, getPushMethod(type), Type.VOID, new Type[]{type}, Constants.INVOKEVIRTUAL));
            } else if (type instanceof ReferenceType) {
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                insList.append(new SWAP());
                insList.append(insFactory.createInvoke(STACK_CLASS, getPushMethod(Type.OBJECT), Type.VOID, new Type[]{Type.OBJECT}, Constants.INVOKEVIRTUAL));
            }
        }
        
        // create uninitialzed object
        insList.append(insFactory.createNew(objecttype));
        insList.append(insFactory.createDup(objecttype.getSize()));
          
        // return the arguments into the stack
        for (int i = 0; i<arguments.length; i++) {
            Type type = arguments[i];
            if (type instanceof BasicType) {
                if ((type.getSize() < 2) && (!type.equals(Type.FLOAT)))
                    type = Type.INT;
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                insList.append(insFactory.createInvoke(STACK_CLASS, getPopMethod(type), type, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
            } else if (type instanceof ReferenceType) {
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                insList.append(insFactory.createInvoke(STACK_CLASS, getPopMethod(Type.OBJECT), Type.OBJECT, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
                if (!type.equals(Type.OBJECT))
                    insList.append(insFactory.createCast(Type.OBJECT, type));
            }
        }
        return insList;
    }

    private boolean rewriteable(MethodGen method, InstructionHandle handle) throws ClassNotFoundException {

        // check in the invocation can be a breakpoint.
        int opcode = handle.getInstruction().getOpcode();
        boolean invokeSpecialSuper = false;
        if (opcode == Constants.INVOKESPECIAL) {
            InvokeInstruction ivs = (InvokeInstruction) handle.getInstruction();
            String mName = ivs.getMethodName(method.getConstantPool());
            invokeSpecialSuper = !mName.equals(Constants.CONSTRUCTOR_NAME);
        }

        if ((opcode == Constants.INVOKEVIRTUAL) ||
            (opcode == Constants.INVOKESTATIC) ||
            (opcode == Constants.INVOKEINTERFACE) ||
            (invokeSpecialSuper)) {

            int index = ((InvokeInstruction) handle.getInstruction()).getIndex();
            String classname = getObjectType(method.getConstantPool().getConstantPool(), index).getClassName();

            // rewrite invocation if object is continuable or a continuation object
            return Repository.implementationOf(classname, CONTINUABLE_CLASS) ||
                   Repository.instanceOf(classname, CONTINUATION_CLASS);
        }
        return false;
    }

    private InstructionList saveFrame(MethodGen method, InstructionHandle handle, int pc,
                                      InstructionFactory insFactory, Frame frame) {
        InstructionList insList = new InstructionList();

        // Remove needless return type from stack
        InvokeInstruction inv = (InvokeInstruction) handle.getInstruction();
        Type returnType = getReturnType(method.getConstantPool().getConstantPool(), inv.getIndex());
        if (returnType.getSize() > 0)
            insList.insert(insFactory.createPop(returnType.getSize()));
        boolean skipFirst = returnType.getSize() > 0;

        //insList.append(insFactory.createPrintln("save stack"));

        // save stack
        OperandStack os = frame.getStack();
        for (int i = skipFirst ? 1 : 0; i < os.size(); i++) {
            Type type = os.peek(i);
            if (type instanceof BasicType) {
                if ((type.getSize() < 2) && (!type.equals(Type.FLOAT)))
                    type = Type.INT;
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                insList.append(new SWAP()); // TODO: check for types with two words on stack
                insList.append(insFactory.createInvoke(STACK_CLASS, getPushMethod(type), Type.VOID, new Type[]{type}, Constants.INVOKEVIRTUAL));
            } else if (type == null) {
                insList.append(InstructionConstants.POP);
            } else if (type instanceof UninitializedObjectType) {
                // After the remove of new, there shouldn't be a
                // uninitialized object on the stack
            } else if (type instanceof ReferenceType) {
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                insList.append(new SWAP());
                insList.append(insFactory.createInvoke(STACK_CLASS, getPushMethod(Type.OBJECT), Type.VOID, new Type[]{Type.OBJECT}, Constants.INVOKEVIRTUAL));
            }
        }

        //insList.insert(insFactory.createPrintln("--- capturing invocation "+method));

        // add isCapturing test
        insList.insert(new IFEQ(handle.getNext()));

        // test if the continuation should be captured after the invocation
        insList.insert(insFactory.createInvoke(CONTINUATION_CLASS, CAPURING_METHOD, Type.BOOLEAN, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        insList.insert(insFactory.createLoad(CONTINUATION_TYPE, method.getMaxLocals()));

        // test if continuation exists
        insList.insert(new IFNULL(handle.getNext()));
        insList.insert(insFactory.createLoad(CONTINUATION_TYPE, method.getMaxLocals()));
  
        //insList.append(insFactory.createPrintln("save local variables"));

        // save local variables
        LocalVariables lvs = frame.getLocals();
        for (int i = 0; i < lvs.maxLocals(); i++) {
            Type type = lvs.get(i);
            if (type instanceof BasicType) {
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                insList.append(InstructionFactory.createLoad(type, i));
                if ((type.getSize() < 2) && (!type.equals(Type.FLOAT)))
                    type = Type.INT;
                insList.append(insFactory.createInvoke(STACK_CLASS, getPushMethod(type), Type.VOID, new Type[]{type}, Constants.INVOKEVIRTUAL));
            } else if (type == null) {
                // no need to save null
            } else if (type instanceof UninitializedObjectType) {
                // no need to save uninitialized objects
            } else if (type instanceof ReferenceType) {
                if (i == 0 && !currentMethodStatic) {
                    // remember current object
                    insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                    insList.append(InstructionFactory.createLoad(type, i));
                    insList.append(insFactory.createInvoke(STACK_CLASS, PUSH_METHOD + "Reference", Type.VOID, new Type[]{Type.OBJECT}, Constants.INVOKEVIRTUAL));
                }
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                insList.append(InstructionFactory.createLoad(type, i));
                insList.append(insFactory.createInvoke(STACK_CLASS, getPushMethod(Type.OBJECT), Type.VOID, new Type[]{Type.OBJECT}, Constants.INVOKEVIRTUAL));
            }
        }

        // save programcounter
        insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
        insList.append(new PUSH(method.getConstantPool(), pc));
        insList.append(insFactory.createInvoke(STACK_CLASS, getPushMethod(Type.INT), Type.VOID, new Type[]{Type.INT}, Constants.INVOKEVIRTUAL));

        // return NULL result
        insList.append(InstructionFactory.createNull(method.getReturnType()));
        insList.append(InstructionFactory.createReturn(method.getReturnType()));

        return insList;
    }

    private InstructionList restoreFrame(MethodGen method, InstructionHandle handle, InstructionFactory insFactory, Frame frame, ObjectType objecttype) {
        InstructionList insList = new InstructionList();

        //insList.append(insFactory.createPrintln("restore local variables"));

        // restore local variables
        LocalVariables lvs = frame.getLocals();
        for (int i = lvs.maxLocals()-1; i >= 0; i--) {
            Type type = lvs.get(i);
            if (type instanceof BasicType) {
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                if ((type.getSize() < 2) && (!type.equals(Type.FLOAT)))
                    type = Type.INT;
                insList.append(insFactory.createInvoke(STACK_CLASS, getPopMethod(type), type, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
                insList.append(InstructionFactory.createStore(type, i));
            }
            else if (type == null) {
                insList.append(new ACONST_NULL());
                insList.append(InstructionFactory.createStore(new ObjectType("<null object>"), i));
            }
            else if (type instanceof UninitializedObjectType) {
                // No uninitilaized objects should be found
                // in the local variables.
            }
            else if (type instanceof ReferenceType) {
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                insList.append(insFactory.createInvoke(STACK_CLASS, getPopMethod(Type.OBJECT), Type.OBJECT, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
                if (!type.equals(Type.OBJECT) && (!type.equals(Type.NULL))) {
                    insList.append(insFactory.createCast(Type.OBJECT, type));
                }
                insList.append(InstructionFactory.createStore(type, i));
            }
        }

        InvokeInstruction inv = (InvokeInstruction) handle.getInstruction();
        Type returnType = getReturnType(method.getConstantPool().getConstantPool(), inv.getIndex());
        boolean skipFirst = returnType.getSize() > 0;

        //insList.append(insFactory.createPrintln("restore stack"));

        // restore stack
        OperandStack os = frame.getStack();
        for (int i = os.size() - 1; i >= (skipFirst ? 1 : 0); i--) {
            Type type = os.peek(i);
            if (type instanceof BasicType) {
                if ((type.getSize() < 2) && (!type.equals(Type.FLOAT)))
                    type = Type.INT;
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                insList.append(insFactory.createInvoke(STACK_CLASS, getPopMethod(type), type, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
            } else if (type == null) {
                insList.append(new ACONST_NULL());
            } else if (type instanceof UninitializedObjectType) {
                // After the remove of new, there shouldn't be a
                // uninitialized object on the stack
            } else if (type instanceof ReferenceType) {
                insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
                insList.append(insFactory.createInvoke(STACK_CLASS, getPopMethod(Type.OBJECT), Type.OBJECT, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
                if (!type.equals(Type.OBJECT))
                    insList.append(insFactory.createCast(Type.OBJECT, type));
            }
        }

        // retrieve current object
        if (!(inv instanceof INVOKESTATIC)) {
            insList.append(insFactory.createLoad(STACK_TYPE, method.getMaxLocals()+1));
            insList.append(insFactory.createInvoke(STACK_CLASS, POP_METHOD + "Reference", Type.OBJECT, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
            insList.append(insFactory.createCast(Type.OBJECT, objecttype));
        }

        // Create null types for the parameters of the method invocation
        Type[] paramTypes = getParamTypes(method.getConstantPool().getConstantPool(), inv.getIndex());
        for (int j = 0; j < paramTypes.length; j++) {
            insList.append(InstructionFactory.createNull(paramTypes[j]));
        }

        // go to last invocation
        insList.append(new GOTO(handle));
        return insList;
    }

    private ObjectType getObjectType(ConstantPool cp, int index) {
        ConstantCP cmr = (ConstantCP) cp.getConstant(index);
        String sig = cp.getConstantString(cmr.getClassIndex(), Constants.CONSTANT_Class);
        return new ObjectType(sig.replace('/', '.'));
    }

    private Type[] getParamTypes(ConstantPool cp, int index) {
        ConstantCP cmr = (ConstantCP) cp.getConstant(index);
        ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(cmr.getNameAndTypeIndex());
        String sig = ((ConstantUtf8) cp.getConstant(cnat.getSignatureIndex())).getBytes();
        return Type.getArgumentTypes(sig);
    }

    private Type getReturnType(ConstantPool cp, int index) {
        ConstantCP cmr = (ConstantCP) cp.getConstant(index);
        ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(cmr.getNameAndTypeIndex());
        String sig = ((ConstantUtf8) cp.getConstant(cnat.getSignatureIndex())).getBytes();
        return Type.getReturnType(sig);
    }

    private String getPopMethod(Type type) {
        if (type.equals(Type.BOOLEAN))
            return POP_METHOD + "Int";
        else if (type.equals(Type.CHAR))
            return POP_METHOD + "Int";
        else if (type.equals(Type.FLOAT))
            return POP_METHOD + "Float";
        else if (type.equals(Type.DOUBLE))
            return POP_METHOD + "Double";
        else if (type.equals(Type.BYTE))
            return POP_METHOD + "Int";
        else if (type.equals(Type.SHORT))
            return POP_METHOD + "Int";
        else if (type.equals(Type.INT))
            return POP_METHOD + "Int";
        else if (type.equals(Type.LONG))
            return POP_METHOD + "Long";
        else if (type.equals(Type.VOID))
            return POP_METHOD + "Object";
        else if (type.equals(Type.OBJECT))
            return POP_METHOD + "Object";

        return POP_METHOD + "Object";
    }

    private String getPushMethod(Type type) {
        if (type.equals(Type.BOOLEAN))
            return PUSH_METHOD + "Int";
        else if (type.equals(Type.CHAR))
            return PUSH_METHOD + "Int";
        else if (type.equals(Type.FLOAT))
            return PUSH_METHOD + "Float";
        else if (type.equals(Type.DOUBLE))
            return PUSH_METHOD + "Double";
        else if (type.equals(Type.BYTE))
            return PUSH_METHOD + "Int";
        else if (type.equals(Type.SHORT))
            return PUSH_METHOD + "Int";
        else if (type.equals(Type.INT))
            return PUSH_METHOD + "Int";
        else if (type.equals(Type.LONG))
            return PUSH_METHOD + "Long";
        else if (type.equals(Type.VOID))
            return PUSH_METHOD + "Object";
        else if (type.equals(Type.OBJECT))
            return PUSH_METHOD + "Object";

        return PUSH_METHOD + "Object";
    }
}
