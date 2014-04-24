package uvm.ssavalue;

import uvm.OpCode;
import uvm.type.Type;

public class IntConstant extends Constant {
    private Type type;
    private long value;

    public IntConstant() {
    }

    public IntConstant(Type type, long value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + type + " = " + value;
    }

    @Override
    public int opcode() {
        return OpCode.INT_IMM;
    }

    @Override
    public <T> T accept(ValueVisitor<T> visitor) {
        return visitor.visitIntConstant(this);
    }
}
