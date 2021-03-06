package uvm.refimpl;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import uvm.Bundle;
import uvm.CFG;
import uvm.ir.text.input.TestingHelper;
import uvm.refimpl.facade.MicroVM;
import uvm.refimpl.facade.MicroVMClient;
import uvm.refimpl.itpr.IntBox;
import uvm.refimpl.itpr.InterpreterFrame;
import uvm.refimpl.itpr.InterpreterStack;
import uvm.refimpl.itpr.InterpreterThread;
import uvm.refimpl.itpr.RefBox;
import uvm.refimpl.itpr.TagRef64Box;
import uvm.refimpl.itpr.ValueBox;
import uvm.refimpl.mem.scanning.ObjectMarker;
import uvm.ssavalue.Value;
import uvm.util.LogUtil;

public class TestMicroVMRefImplGC {

    private static MicroVM microVM;
    private static MicroVMHelper h;

    private static MicroVMClient client = new DumbClient() {
        @Override
        public void markExternalRoots(ObjectMarker marker) {
            // no external roots
        }

        @Override
        public Long onTrap(InterpreterThread thread, ValueBox trapValue) {
            return trapHandler.onTrap(thread, trapValue);
        }

    };

    private static interface TrapHandler {
        public Long onTrap(InterpreterThread thread, ValueBox trapValue);
    }

    private static TrapHandler trapHandler;

    private static void setTrapHandler(TrapHandler trapHandler) {
        TestMicroVMRefImplGC.trapHandler = trapHandler;
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        LogUtil.disableAll();
        LogUtil.enableLoggers("RBPA", "LOS", "SIC", "SIDM", "SIS");

        try {
            microVM = new MicroVM();
            microVM.setClient(client);
            h = new MicroVMHelper(microVM);
            Bundle bundle = TestingHelper
                    .parseUir("tests/uvm-refimpl-test/gc-tests.uir");
            microVM.addBundle(bundle);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    private ValueBox getValueBox(InterpreterFrame frame, String name) {
        CFG cfg = frame.getCfg();
        Value val = cfg.getInstNs().getByName(name);
        return frame.getValueBox(val);
    }

    @Test
    public void testKeepGlobal() throws Exception {
        InterpreterStack stack = h.makeStack(h.func("@keepglobal"));

        setTrapHandler(new TrapHandler() {
            @Override
            public Long onTrap(InterpreterThread thread, ValueBox trapValueBox) {
                String trapName = thread.getStack().getTop().getCurInst()
                        .getName();
                if (trapName.equals("%gctrap")) {
                    microVM.getMemoryManager().getHeap()
                            .mutatorTriggerAndWaitForGCEnd(false);
                    return null;
                } else if (trapName.equals("%checktrap")) {
                    List<ValueBox> kas = thread.getStack().getTop()
                            .dumpKeepAlives();

                    long obj2 = ((RefBox) kas.get(0)).getAddr();
                    long obj2val = ((IntBox) kas.get(1)).getValue().longValue();

                    assertNotEquals(0, obj2);
                    assertEquals(42, obj2val);

                    return null;
                } else {
                    fail("Who set that trap: " + trapName);
                    return null;
                }
            }

        });

        InterpreterThread thread = microVM.newThread(stack);
        thread.join();

    }

    @Test
    public void testNoKeepAlloca() throws Exception {
        InterpreterStack stack = h.makeStack(h.func("@nokeepalloca"));

        setTrapHandler(new TrapHandler() {
            @Override
            public Long onTrap(InterpreterThread thread, ValueBox trapValueBox) {
                String trapName = thread.getStack().getTop().getCurInst()
                        .getName();
                if (trapName.equals("%gctrap")) {
                    microVM.getMemoryManager().getHeap()
                            .mutatorTriggerAndWaitForGCEnd(false);
                    return null;
                } else {
                    fail("Who set that trap: " + trapName);
                    return null;
                }
            }

        });

        InterpreterThread thread = microVM.newThread(stack);
        thread.join();

    }

    @Test
    public void testBreadcrumbs() throws Exception {
        InterpreterStack stack = h.makeStack(h.func("@breadcrumbs"));

        setTrapHandler(new TrapHandler() {
            @Override
            public Long onTrap(InterpreterThread thread, ValueBox trapValueBox) {
                return null;
            }
        });

        InterpreterThread thread = microVM.newThread(stack);
        thread.join();

    }

    @Test
    public void testWeakRef() throws Exception {
        InterpreterStack stack = h.makeStack(h.func("@testweakref"));

        setTrapHandler(new TrapHandler() {
            @Override
            public Long onTrap(InterpreterThread thread, ValueBox trapValueBox) {
                String trapName = thread.getStack().getTop().getCurInst()
                        .getName();
                if (trapName.equals("%gctrap")) {
                    microVM.getMemoryManager().getHeap()
                            .mutatorTriggerAndWaitForGCEnd(false);
                    return null;
                } else if (trapName.equals("%checknztrap")) {
                    List<ValueBox> kas = thread.getStack().getTop()
                            .dumpKeepAlives();

                    long refval = ((RefBox) kas.get(0)).getAddr();

                    assertNotEquals(0, refval);

                    return null;
                } else if (trapName.equals("%checkztrap")) {
                    List<ValueBox> kas = thread.getStack().getTop()
                            .dumpKeepAlives();

                    long refval = ((RefBox) kas.get(0)).getAddr();

                    assertEquals(0, refval);

                    return null;
                } else {
                    fail("Who set that trap: " + trapName);
                    return null;
                }
            }

        });

        InterpreterThread thread = microVM.newThread(stack);
        thread.join();

    }

    @Test
    public void testTagRefGC() {
        InterpreterStack stack = h.makeStack(h.func("@testtagrefgc"));

        setTrapHandler(new TrapHandler() {
            @Override
            public Long onTrap(InterpreterThread thread, ValueBox trapValueBox) {
                String trapName = thread.getStack().getTop().getCurInst()
                        .getName();
                if (trapName.equals("%gctrap")) {
                    microVM.getMemoryManager().getHeap()
                            .mutatorTriggerAndWaitForGCEnd(false);
                    return null;
                } else if (trapName.equals("%checktrap")) {
                    List<ValueBox> kas = thread.getStack().getTop()
                            .dumpKeepAlives();

                    TagRef64Box tr = (TagRef64Box) kas.get(0);
                    long refv = ((RefBox) kas.get(1)).getAddr();
                    long tagv = ((IntBox) kas.get(2)).getValue().longValue();
                    long iv = ((IntBox) kas.get(3)).getValue().longValue();

                    assertTrue(tr.isRef());
                    assertNotEquals(0, refv);
                    assertEquals(13, tagv);
                    assertEquals(42, iv);

                    return null;
                } else {
                    fail("Who set that trap: " + trapName);
                    return null;
                }
            }

        });

        InterpreterThread thread = microVM.newThread(stack);
        thread.join();

    }

    @Test
    public void testTagRefGCMem() {
        InterpreterStack stack = h.makeStack(h.func("@testtagrefgcmem"));

        setTrapHandler(new TrapHandler() {
            @Override
            public Long onTrap(InterpreterThread thread, ValueBox trapValueBox) {
                String trapName = thread.getStack().getTop().getCurInst()
                        .getName();
                if (trapName.equals("%gctrap")) {
                    microVM.getMemoryManager().getHeap()
                            .mutatorTriggerAndWaitForGCEnd(false);
                    return null;
                } else if (trapName.equals("%checktrap")) {
                    List<ValueBox> kas = thread.getStack().getTop()
                            .dumpKeepAlives();

                    TagRef64Box tr = (TagRef64Box) kas.get(0);
                    long refv = ((RefBox) kas.get(1)).getAddr();
                    long tagv = ((IntBox) kas.get(2)).getValue().longValue();
                    long iv = ((IntBox) kas.get(3)).getValue().longValue();

                    assertTrue(tr.isRef());
                    assertNotEquals(0, refv);
                    assertEquals(13, tagv);
                    assertEquals(42, iv);

                    return null;
                } else {
                    fail("Who set that trap: " + trapName);
                    return null;
                }
            }

        });

        InterpreterThread thread = microVM.newThread(stack);
        thread.join();

    }

    @Test
    public void testBreadcrumbsTr64() throws Exception {
        LogUtil.enableLoggers("MDS");
        
        InterpreterStack stack = h.makeStack(h.func("@breadcrumbstr64"));

        setTrapHandler(new TrapHandler() {
            @Override
            public Long onTrap(InterpreterThread thread, ValueBox trapValueBox) {
                return null;
            }
        });

        InterpreterThread thread = microVM.newThread(stack);
        thread.join();
        
        LogUtil.disableLoggers("MDS");

    }
}
