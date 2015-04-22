package nez.x.minivm;

class OptimizerOption {
	public boolean useInlining = false;
	public boolean useMappedChoice = false;
	public boolean useFusionInstruction = false;
	public boolean useFusionOperand = false;
	public boolean useStackCaching = false;
	public boolean useFlowAnalysis = false;

	public OptimizerOption() {
	}

	public OptimizerOption setInlining(boolean Inlining) {
		this.useInlining = Inlining;
		return this;
	}

	public OptimizerOption setMappedChoice(boolean MappedChoice) {
		this.useMappedChoice = MappedChoice;
		return this;
	}

	public OptimizerOption setFusionInstruction(boolean FusionInstruction) {
		this.useFusionInstruction = FusionInstruction;
		return this;
	}

	public OptimizerOption setFusionOperand(boolean FusionOperand) {
		this.useFusionOperand = FusionOperand;
		return this;
	}

	public OptimizerOption setStackCaching(boolean StackCaching) {
		this.useStackCaching = StackCaching;
		return this;
	}

	public OptimizerOption setFlowAnalysis(boolean FlowAnalysis) {
		this.useFlowAnalysis = FlowAnalysis;
		return this;
	}
}

public class Optimizer {
	Module module;
	private final OptimizerOption option;

	public Optimizer(Module module, OptimizerOption option) {
		this.module = module;
		this.option = option;
	}

	public void optimize() {
		for (int i = 0; i < this.module.size(); i++) {
			this.optimizeFunction(this.module.get(i));
		}
	}

	public void optimizeFunction(Function func) {
		for (int i = 0; i < func.size(); i++) {
			BasicBlock bb = func.get(i);

			for (int j = 0; j < bb.size(); j++) {
				Instruction inst = bb.get(j);
				if (inst instanceof NOTCHAR) {
					if (bb.get(j + 1) instanceof ANY) {
						NOTCHAR ir = (NOTCHAR) bb.remove(j);
						bb.remove(j);
						NOTCHARANY nca = new NOTCHARANY(ir.expr, ir.jump, ir.getc(0));
						nca.addBasicBlock(j, bb);
						inst = nca;
					}
				}
				if (inst instanceof JumpInstruction) {
					JumpInstruction jinst = (JumpInstruction) inst;
					optimizeJump(func, bb, jinst.jump, jinst, j);
				}
				else if (inst instanceof JumpMatchingInstruction) {
					JumpMatchingInstruction jinst = (JumpMatchingInstruction) inst;
					optimizeJumpMatching(func, bb, jinst.jump, jinst, j);
				}
				// else if(inst instanceof PUSHp || inst instanceof LOADp1 ||
				// inst instanceof LOADp2 || inst instanceof LOADp3) {
				// if(j != bb.size() - 1) {
				// optimizeStackOperation(func, bb, j + 1);
				// }
				// }
			}
		}
	}

	public void optimizeJump(Function func, BasicBlock bb, BasicBlock jump, JumpInstruction jinst, int index) {
		if (jump.size() == 0) {
			int nextJumpBBIndex = func.indexOf(jump) + 1;
			if (nextJumpBBIndex == func.size()) {
				return;
			}
			jump = func.get(nextJumpBBIndex);
			optimizeJump(func, bb, jump, jinst, index);
			return;
		}
		int currentIndex = func.indexOf(bb) + 1;
		while (func.get(currentIndex).size() == 0) {
			currentIndex++;
		}
		if (func.indexOf(jump) == currentIndex && index == bb.size() - 1) {
			bb.remove(index);
		}
		else if (jump.get(0) instanceof JUMP) {
			JUMP tmp = (JUMP) jump.get(0);
			jinst.jump = tmp.jump;
			optimizeJump(func, bb, tmp.jump, jinst, index);
		}
		else if (jump.get(0) instanceof RET && jinst instanceof JUMP) {
			Instruction ret = jump.get(0);
			bb.remove(index);
			bb.add(index, ret);
		}
	}

	public void optimizeJumpMatching(Function func, BasicBlock bb, BasicBlock jump, JumpMatchingInstruction jinst, int index) {
		if (jump.size() == 0) {
			int nextJumpBBIndex = func.indexOf(jump) + 1;
			if (nextJumpBBIndex == func.size()) {
				return;
			}
			jump = func.get(nextJumpBBIndex);
			optimizeJumpMatching(func, bb, jump, jinst, index);
			return;
		}
		if (jump.get(0) instanceof JUMP) {
			JUMP tmp = (JUMP) jump.get(0);
			jinst.jump = tmp.jump;
			optimizeJumpMatching(func, bb, tmp.jump, jinst, index);
		}
	}
}
