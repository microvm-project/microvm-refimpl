package uvm.ssavalue;

import uvm.BasicBlock;
import uvm.IdentifiedHelper;
import uvm.OpCode;
import uvm.type.Type;

/**
 * An unconditional branching.
 */
public class InstBranch extends Instruction {

    private BasicBlock target;

    public InstBranch() {
    }

    public InstBranch(BasicBlock target) {
        this.target = target;
    }

    public BasicBlock getTarget() {
        return target;
    }

    public void setTarget(BasicBlock target) {
        this.target = target;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public int opcode() {
        return OpCode.BRANCH;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", getClass().getSimpleName(),
                IdentifiedHelper.repr(getTarget()));
    }
    
    @Override
    public <T> T accept(ValueVisitor<T> visitor) {
        return visitor.visitBranch(this);
    }
}