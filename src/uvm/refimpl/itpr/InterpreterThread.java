package uvm.refimpl.itpr;

import uvm.BasicBlock;
import uvm.Function;
import uvm.IdentifiedHelper;
import uvm.ssavalue.Constant;
import uvm.ssavalue.ConvOptr;
import uvm.ssavalue.DoubleConstant;
import uvm.ssavalue.FloatConstant;
import uvm.ssavalue.FunctionConstant;
import uvm.ssavalue.GlobalDataConstant;
import uvm.ssavalue.InstAlloca;
import uvm.ssavalue.InstAllocaHybrid;
import uvm.ssavalue.InstAtomicRMW;
import uvm.ssavalue.InstBinOp;
import uvm.ssavalue.InstBranch;
import uvm.ssavalue.InstBranch2;
import uvm.ssavalue.InstCCall;
import uvm.ssavalue.InstCall;
import uvm.ssavalue.InstCmp;
import uvm.ssavalue.InstCmpXchg;
import uvm.ssavalue.InstConversion;
import uvm.ssavalue.InstExtractValue;
import uvm.ssavalue.InstFence;
import uvm.ssavalue.InstGetElemIRef;
import uvm.ssavalue.InstGetFieldIRef;
import uvm.ssavalue.InstGetFixedPartIRef;
import uvm.ssavalue.InstGetIRef;
import uvm.ssavalue.InstGetVarPartIRef;
import uvm.ssavalue.InstICall;
import uvm.ssavalue.InstIInvoke;
import uvm.ssavalue.InstInsertValue;
import uvm.ssavalue.InstInvoke;
import uvm.ssavalue.InstLandingPad;
import uvm.ssavalue.InstLoad;
import uvm.ssavalue.InstNew;
import uvm.ssavalue.InstNewHybrid;
import uvm.ssavalue.InstNewStack;
import uvm.ssavalue.InstPhi;
import uvm.ssavalue.InstRet;
import uvm.ssavalue.InstRetVoid;
import uvm.ssavalue.InstSelect;
import uvm.ssavalue.InstShiftIRef;
import uvm.ssavalue.InstStore;
import uvm.ssavalue.InstSwitch;
import uvm.ssavalue.InstTailCall;
import uvm.ssavalue.InstThrow;
import uvm.ssavalue.InstTrap;
import uvm.ssavalue.InstWatchPoint;
import uvm.ssavalue.Instruction;
import uvm.ssavalue.IntConstant;
import uvm.ssavalue.NullConstant;
import uvm.ssavalue.Parameter;
import uvm.ssavalue.StructConstant;
import uvm.ssavalue.Value;
import uvm.ssavalue.ValueVisitor;
import uvm.type.Func;
import uvm.type.IRef;
import uvm.type.Int;
import uvm.type.Ref;
import uvm.type.Stack;
import uvm.type.Type;
import uvm.util.ErrorUtils;

public class InterpreterThread implements Runnable {

    private InterpreterStack stack;

    private boolean running = true;

    private InstructionExecutor executor = new InstructionExecutor();

    private ConstantPool constantPool;

    public InterpreterThread(InterpreterStack stack, ConstantPool constantPool) {
        // TODO: Inject more necessary resources.
        this.stack = stack;
        this.constantPool = constantPool;
    }

    @Override
    public void run() {
        while (running) {
            getCurInst().accept(executor);
        }
    }

    private Function getCurFunc() {
        return stack.getTop().getFunc();
    }

    private BasicBlock getCurBb() {
        return stack.getTop().getCurBb();
    }

    public int getCurInstIndex() {
        return stack.getTop().getCurInstIndex();
    }

    public Instruction getCurInst() {
        return stack.getTop().getCurInst();
    }

    @SuppressWarnings("unchecked")
    public <T extends ValueBox> T getValueBox(Value value) {
        if (value instanceof Constant) {
            return (T) constantPool.getValueBox((Constant) value);
        } else {
            return (T) stack.getTop().getValueBox(value);
        }
    }

    private void error(String string) {
        ErrorUtils.uvmError("Function " + IdentifiedHelper.repr(getCurFunc())
                + " BB " + IdentifiedHelper.repr(getCurBb()) + " inst "
                + IdentifiedHelper.repr(getCurInst()) + " : " + string);
    }

    private long getInt(Value opnd) {
        return ((IntBox) getValueBox(opnd)).getValue();
    }

    private void setInt(Value opnd, long val) {
        ((IntBox) getValueBox(opnd)).setValue(val);
    }

    private float getFloat(Value opnd) {
        return ((FloatBox) getValueBox(opnd)).getValue();
    }

    private void setFloat(Value opnd, float val) {
        ((FloatBox) getValueBox(opnd)).setValue(val);
    }

    private double getDouble(Value opnd) {
        return ((DoubleBox) getValueBox(opnd)).getValue();
    }

    private void setDouble(Value opnd, double val) {
        ((DoubleBox) getValueBox(opnd)).setValue(val);
    }

    private static long pu(long n, long l) {
        return OpHelper.prepareUnsigned(n, l);
    }

    private static long ps(long n, long l) {
        return OpHelper.prepareSigned(n, l);
    }

    private static long up(long n, long l) {
        return OpHelper.unprepare(n, l);
    }

    private class InstructionExecutor implements ValueVisitor<Void> {

        @Override
        public Void visitIntConstant(IntConstant constant) {
            // Not Value. Not used.
            return null;
        }

        @Override
        public Void visitFloatConstant(FloatConstant constant) {
            // Not Value. Not used.
            return null;
        }

        @Override
        public Void visitDoubleConstant(DoubleConstant constant) {
            // Not Value. Not used.
            return null;
        }

        @Override
        public Void visitStructConstant(StructConstant constant) {
            // Not Value. Not used.
            return null;
        }

        @Override
        public Void visitNullConstant(NullConstant constant) {
            // Not Value. Not used.
            return null;
        }

        @Override
        public Void visitGlobalDataConstant(GlobalDataConstant constant) {
            // Not Value. Not used.
            return null;
        }

        @Override
        public Void visitFunctionConstant(FunctionConstant constant) {
            // Not Value. Not used.
            return null;
        }

        @Override
        public Void visitParameter(Parameter parameter) {
            // Not Value. Not used.
            return null;
        }

        @Override
        public Void visitBinOp(InstBinOp inst) {
            Type type = inst.getType();
            if (type instanceof Int) {
                Int t = (Int) type;
                long l = t.getSize();
                long v1 = getInt(inst.getOp1());
                long v2 = getInt(inst.getOp2());
                long rv = 0;

                switch (inst.getOptr()) {
                case ADD:
                    rv = up(pu(v1, l) + pu(v2, l), l);
                    break;
                case SUB:
                    rv = up(pu(v1, l) - pu(v2, l), l);
                    break;
                case MUL:
                    rv = up(pu(v1, l) * pu(v2, l), l);
                    break;
                case UDIV:
                    rv = up(pu(v1, l) / pu(v2, l), l);
                    break;
                case SDIV:
                    rv = up(ps(v1, l) / ps(v2, l), l);
                    break;
                case UREM:
                    rv = up(pu(v1, l) % pu(v2, l), l);
                    break;
                case SREM:
                    rv = up(ps(v1, l) % ps(v2, l), l);
                    break;
                case SHL:
                    rv = up(pu(v1, l) << pu(v2, l), l);
                    break;
                case LSHR:
                    rv = up(pu(v1, l) >>> pu(v2, l), l);
                    break;
                case ASHR:
                    rv = up(ps(v1, l) >> pu(v2, l), l);
                    break;
                case AND:
                    rv = up(pu(v1, l) & pu(v2, l), l);
                    break;
                case OR:
                    rv = up(pu(v1, l) | pu(v2, l), l);
                    break;
                case XOR:
                    rv = up(pu(v1, l) ^ pu(v2, l), l);
                    break;
                default:
                    error("Unexpected op for int binop "
                            + inst.getOptr().toString());
                }

                setInt(inst, rv);
            } else if (type instanceof uvm.type.Float) {
                float v1 = getFloat(inst.getOp1());
                float v2 = getFloat(inst.getOp2());
                float rv = 0;

                switch (inst.getOptr()) {
                case FADD:
                    rv = v1 + v2;
                    break;
                case FSUB:
                    rv = v1 - v2;
                    break;
                case FMUL:
                    rv = v1 * v2;
                    break;
                case FDIV:
                    rv = v1 / v2;
                    break;
                case FREM:
                    rv = v1 % v2;
                    break;
                default:
                    error("Unexpected op for float binop "
                            + inst.getOptr().toString());
                }

                setFloat(inst, rv);
            } else if (type instanceof uvm.type.Double) {
                double v1 = getDouble(inst.getOp1());
                double v2 = getDouble(inst.getOp2());
                double rv = 0;

                switch (inst.getOptr()) {
                case FADD:
                    rv = v1 + v2;
                    break;
                case FSUB:
                    rv = v1 - v2;
                    break;
                case FMUL:
                    rv = v1 * v2;
                    break;
                case FDIV:
                    rv = v1 / v2;
                    break;
                case FREM:
                    rv = v1 % v2;
                    break;
                default:
                    error("Unexpected op for double binop "
                            + inst.getOptr().toString());
                }

                setDouble(inst, rv);
            } else {
                error("Bad type for binary operation: "
                        + type.getClass().getName());
            }
            return null;
        }

        @Override
        public Void visitCmp(InstCmp inst) {
            Type type = inst.getType();
            if (type instanceof Int) {
                Int t = (Int) type;
                long l = t.getSize();
                long v1 = getInt(inst.getOp1());
                long v2 = getInt(inst.getOp2());
                boolean rv = false;

                switch (inst.getOptr()) {
                case EQ:
                    rv = pu(v1, l) == pu(v2, l);
                    break;
                case NE:
                    rv = pu(v1, l) != pu(v2, l);
                    break;
                case ULT:
                    rv = pu(v1, l) < pu(v2, l);
                    break;
                case ULE:
                    rv = pu(v1, l) <= pu(v2, l);
                    break;
                case UGT:
                    rv = pu(v1, l) > pu(v2, l);
                    break;
                case UGE:
                    rv = pu(v1, l) >= pu(v2, l);
                    break;
                case SLT:
                    rv = ps(v1, l) < ps(v2, l);
                    break;
                case SLE:
                    rv = ps(v1, l) <= ps(v2, l);
                    break;
                case SGT:
                    rv = ps(v1, l) > ps(v2, l);
                    break;
                case SGE:
                    rv = ps(v1, l) >= ps(v2, l);
                    break;
                default:
                    error("Unexpected op for int cmp "
                            + inst.getOptr().toString());
                }

                setInt(inst, rv ? 1 : 0);
            } else if (type instanceof uvm.type.Float) {
                float v1 = getFloat(inst.getOp1());
                float v2 = getFloat(inst.getOp2());
                boolean rv = false;

                switch (inst.getOptr()) {
                case FTRUE:
                    rv = true;
                    break;
                case FFALSE:
                    rv = false;
                    break;
                case FUNO:
                    rv = Float.isNaN(v1) || Float.isNaN(v2);
                    break;
                case FUEQ:
                    rv = Float.isNaN(v1) || Float.isNaN(v2) || v1 == v2;
                    break;
                case FUNE:
                    rv = Float.isNaN(v1) || Float.isNaN(v2) || v1 != v2;
                    break;
                case FULT:
                    rv = Float.isNaN(v1) || Float.isNaN(v2) || v1 < v2;
                    break;
                case FULE:
                    rv = Float.isNaN(v1) || Float.isNaN(v2) || v1 <= v2;
                    break;
                case FUGT:
                    rv = Float.isNaN(v1) || Float.isNaN(v2) || v1 > v2;
                    break;
                case FUGE:
                    rv = Float.isNaN(v1) || Float.isNaN(v2) || v1 >= v2;
                    break;
                case FORD:
                    rv = !Float.isNaN(v1) && !Float.isNaN(v2);
                    break;
                case FOEQ:
                    rv = !Float.isNaN(v1) && !Float.isNaN(v2) && v1 == v2;
                    break;
                case FONE:
                    rv = !Float.isNaN(v1) && !Float.isNaN(v2) && v1 != v2;
                    break;
                case FOLT:
                    rv = !Float.isNaN(v1) && !Float.isNaN(v2) && v1 < v2;
                    break;
                case FOLE:
                    rv = !Float.isNaN(v1) && !Float.isNaN(v2) && v1 <= v2;
                    break;
                case FOGT:
                    rv = !Float.isNaN(v1) && !Float.isNaN(v2) && v1 > v2;
                    break;
                case FOGE:
                    rv = !Float.isNaN(v1) && !Float.isNaN(v2) && v1 >= v2;
                    break;
                default:
                    error("Unexpected op for float cmp "
                            + inst.getOptr().toString());
                }

                setInt(inst, rv ? 1 : 0);
            } else if (type instanceof uvm.type.Double) {
                double v1 = getDouble(inst.getOp1());
                double v2 = getDouble(inst.getOp2());
                boolean rv = false;

                switch (inst.getOptr()) {
                case FTRUE:
                    rv = true;
                    break;
                case FFALSE:
                    rv = false;
                    break;
                case FUNO:
                    rv = Double.isNaN(v1) || Double.isNaN(v2);
                    break;
                case FUEQ:
                    rv = Double.isNaN(v1) || Double.isNaN(v2) || v1 == v2;
                    break;
                case FUNE:
                    rv = Double.isNaN(v1) || Double.isNaN(v2) || v1 != v2;
                    break;
                case FULT:
                    rv = Double.isNaN(v1) || Double.isNaN(v2) || v1 < v2;
                    break;
                case FULE:
                    rv = Double.isNaN(v1) || Double.isNaN(v2) || v1 <= v2;
                    break;
                case FUGT:
                    rv = Double.isNaN(v1) || Double.isNaN(v2) || v1 > v2;
                    break;
                case FUGE:
                    rv = Double.isNaN(v1) || Double.isNaN(v2) || v1 >= v2;
                    break;
                case FORD:
                    rv = !Double.isNaN(v1) && !Double.isNaN(v2);
                    break;
                case FOEQ:
                    rv = !Double.isNaN(v1) && !Double.isNaN(v2) && v1 == v2;
                    break;
                case FONE:
                    rv = !Double.isNaN(v1) && !Double.isNaN(v2) && v1 != v2;
                    break;
                case FOLT:
                    rv = !Double.isNaN(v1) && !Double.isNaN(v2) && v1 < v2;
                    break;
                case FOLE:
                    rv = !Double.isNaN(v1) && !Double.isNaN(v2) && v1 <= v2;
                    break;
                case FOGT:
                    rv = !Double.isNaN(v1) && !Double.isNaN(v2) && v1 > v2;
                    break;
                case FOGE:
                    rv = !Double.isNaN(v1) && !Double.isNaN(v2) && v1 >= v2;
                    break;
                default:
                    error("Unexpected op for double cmp "
                            + inst.getOptr().toString());
                }

                setInt(inst, rv ? 1 : 0);
            } else if (type instanceof Ref) {
                RefBox op1 = getValueBox(inst.getOp1());
                RefBox op2 = getValueBox(inst.getOp2());

                long v1 = op1.getAddr();
                long v2 = op2.getAddr();
                boolean rv = false;

                switch (inst.getOptr()) {
                case EQ:
                    rv = v1 == v2;
                    break;
                case NE:
                    rv = v1 != v2;
                    break;
                default:
                    error("Unexpected op for Ref cmp "
                            + inst.getOptr().toString());
                }

                IntBox rvBox = getValueBox(inst);
                rvBox.setValue(rv ? 1 : 0);
            } else if (type instanceof IRef) {
                IRefBox op1 = getValueBox(inst.getOp1());
                IRefBox op2 = getValueBox(inst.getOp2());

                long v1 = op1.getAddr();
                long v2 = op2.getAddr();
                boolean rv = false;

                switch (inst.getOptr()) {
                case EQ:
                    rv = v1 == v2;
                    break;
                case NE:
                    rv = v1 != v2;
                    break;
                default:
                    error("Unexpected op for IRef cmp "
                            + inst.getOptr().toString());
                }

                IntBox rvBox = getValueBox(inst);
                rvBox.setValue(rv ? 1 : 0);
            } else if (type instanceof Func) {
                FuncBox op1 = getValueBox(inst.getOp1());
                FuncBox op2 = getValueBox(inst.getOp2());

                Function v1 = op1.getFunc();
                Function v2 = op2.getFunc();
                boolean rv = false;

                switch (inst.getOptr()) {
                case EQ:
                    rv = v1 == v2;
                    break;
                case NE:
                    rv = v1 != v2;
                    break;
                default:
                    error("Unexpected op for Func cmp "
                            + inst.getOptr().toString());
                }

                IntBox rvBox = getValueBox(inst);
                rvBox.setValue(rv ? 1 : 0);
            } else if (type instanceof uvm.type.Thread) {
                ThreadBox op1 = getValueBox(inst.getOp1());
                ThreadBox op2 = getValueBox(inst.getOp2());

                InterpreterThread v1 = op1.getThread();
                InterpreterThread v2 = op2.getThread();
                boolean rv = false;

                switch (inst.getOptr()) {
                case EQ:
                    rv = v1 == v2;
                    break;
                case NE:
                    rv = v1 != v2;
                    break;
                default:
                    error("Unexpected op for Thread cmp "
                            + inst.getOptr().toString());
                }

                IntBox rvBox = getValueBox(inst);
                rvBox.setValue(rv ? 1 : 0);
            } else if (type instanceof Stack) {
                StackBox op1 = getValueBox(inst.getOp1());
                StackBox op2 = getValueBox(inst.getOp2());

                InterpreterStack v1 = op1.getStack();
                InterpreterStack v2 = op2.getStack();
                boolean rv = false;

                switch (inst.getOptr()) {
                case EQ:
                    rv = v1 == v2;
                    break;
                case NE:
                    rv = v1 != v2;
                    break;
                default:
                    error("Unexpected op for Stack cmp "
                            + inst.getOptr().toString());
                }

                IntBox rvBox = getValueBox(inst);
                rvBox.setValue(rv ? 1 : 0);
            } else {
                error("Bad type for comparison: " + type.getClass().getName());
            }
            return null;

        }

        @Override
        public Void visitConversion(InstConversion inst) {
            Type ft = inst.getFromType();
            Type tt = inst.getToType();
            Value opnd = inst.getOpnd();
            switch (inst.getOptr()) {
            case TRUNC: {
                long tl = ((Int) tt).getSize();
                long od = getInt(opnd);
                long rv = OpHelper.trunc(od, tl);
                setInt(inst, rv);
                break;
            }
            case ZEXT: {
                long fl = ((Int) ft).getSize();
                long tl = ((Int) tt).getSize();
                long od = getInt(opnd);
                long rv = OpHelper.zext(od, fl, tl);
                setInt(inst, rv);
                break;
            }
            case SEXT: {
                long fl = ((Int) ft).getSize();
                long tl = ((Int) tt).getSize();
                long od = getInt(opnd);
                long rv = OpHelper.sext(od, fl, tl);
                setInt(inst, rv);
                break;
            }
            case FPTRUNC: {
                double od = getDouble(opnd);
                float rv = (float) od;
                setFloat(inst, rv);
                break;
            }
            case FPEXT: {
                float od = getFloat(opnd);
                double rv = (double) od;
                setDouble(inst, rv);
                break;
            }
            case FPTOUI:
            case FPTOSI: {
                long tl = ((Int) tt).getSize();
                long rv;
                if (ft instanceof uvm.type.Float) {
                    rv = (long) getFloat(opnd);
                } else if (ft instanceof uvm.type.Double) {
                    rv = (long) getDouble(opnd);
                } else {
                    ErrorUtils.uvmError("Bad type for FPTOxI: "
                            + IdentifiedHelper.repr(ft));
                    return null;
                }
                rv = OpHelper.truncFromLong(rv, tl);
                setInt(inst, rv);
                break;
            }

            case UITOFP:
            case SITOFP: {
                long fl = ((Int) ft).getSize();
                long fv = getInt(opnd);

                if (inst.getOptr() == ConvOptr.UITOFP) {
                    fv = pu(fv, fl);
                } else {
                    fv = ps(fv, fl);
                }

                if (tt instanceof uvm.type.Float) {
                    float rv = (float) fv;
                    setFloat(inst, rv);
                } else if (tt instanceof uvm.type.Double) {
                    double rv = (double) fv;
                    setDouble(inst, rv);
                } else {
                    ErrorUtils.uvmError("Bad type for xITOFP: "
                            + IdentifiedHelper.repr(tt));
                    return null;
                }
                break;
            }
            case BITCAST:
                if (ft instanceof Int && ((Int) ft).getSize() == 32
                        && tt instanceof uvm.type.Float) {
                    int fv = (int) pu(getInt(opnd), 32);
                    float rv = Float.intBitsToFloat(fv);
                    setFloat(inst, rv);
                } else if (ft instanceof Int && ((Int) ft).getSize() == 64
                        && tt instanceof uvm.type.Double) {
                    long fv = getInt(opnd);
                    double rv = Double.longBitsToDouble(fv);
                    setDouble(inst, rv);
                } else if (ft instanceof uvm.type.Float && tt instanceof Int
                        && ((Int) tt).getSize() == 32) {
                    float fv = getFloat(opnd);
                    long rv = (long) (Float.floatToRawIntBits(fv));
                    setInt(inst, rv);
                } else if (ft instanceof uvm.type.Double && tt instanceof Int
                        && ((Int) tt).getSize() == 64) {
                    double fv = getDouble(opnd);
                    long rv = Double.doubleToRawLongBits(fv);
                    setInt(inst, rv);
                } else {
                    ErrorUtils.uvmError("Bad type for BITCAST: "
                            + IdentifiedHelper.repr(ft) + " and "
                            + IdentifiedHelper.repr(tt));
                    return null;
                }
            case REFCAST: {
                RefBox fb = getValueBox(opnd);
                RefBox rb = getValueBox(inst);
                rb.setAddr(fb.getAddr());
                break;
            }
            case IREFCAST: {
                IRefBox fb = getValueBox(opnd);
                IRefBox rb = getValueBox(inst);
                rb.setBase(fb.getBase());
                rb.setOffset(fb.getOffset());
                break;
            }
            case FUNCCAST: {
                FuncBox fb = getValueBox(opnd);
                FuncBox rb = getValueBox(inst);
                rb.setFunc(fb.getFunc());
                break;
            }
            default:
                ErrorUtils.uvmError("Unknown conversion operator "
                        + inst.getOptr().toString());
                return null;
            }
            return null;
        }

        @Override
        public Void visitSelect(InstSelect inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitBranch(InstBranch inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitBranch2(InstBranch2 inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitSwitch(InstSwitch inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitPhi(InstPhi inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitCall(InstCall inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitInvoke(InstInvoke inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitTailCall(InstTailCall inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitRet(InstRet inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitRetVoid(InstRetVoid inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitThrow(InstThrow inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitLandingPad(InstLandingPad inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitExtractValue(InstExtractValue inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitInsertValue(InstInsertValue inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitNew(InstNew inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitNewHybrid(InstNewHybrid inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitAlloca(InstAlloca inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitAllocaHybrid(InstAllocaHybrid inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitGetIRef(InstGetIRef inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitGetFieldIRef(InstGetFieldIRef inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitGetElemIRef(InstGetElemIRef inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitShiftIRef(InstShiftIRef inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitGetFixedPartIRef(InstGetFixedPartIRef inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitGetVarPartIRef(InstGetVarPartIRef inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitLoad(InstLoad inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitStore(InstStore inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitCmpXchg(InstCmpXchg inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitAtomicRMW(InstAtomicRMW inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitFence(InstFence inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitTrap(InstTrap inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitWatchPoint(InstWatchPoint inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitCCall(InstCCall inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitNewStack(InstNewStack inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitICall(InstICall inst) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Void visitIInvoke(InstIInvoke inst) {
            // TODO Auto-generated method stub
            return null;
        }

    }

}