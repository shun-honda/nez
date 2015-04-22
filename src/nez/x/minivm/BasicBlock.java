package nez.x.minivm;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock {
	Function parent;
	int codeIndex;
	List<Instruction> insts;
	List<BasicBlock> succs;
	List<BasicBlock> preds;

	public BasicBlock() {
		this.insts = new ArrayList<Instruction>();
		this.succs = new ArrayList<BasicBlock>();
		this.preds = new ArrayList<BasicBlock>();
	}

	public BasicBlock(Function parent) {
		this();
		this.parent = parent;
		this.parent.append(this);
	}

	public void setInsertPoint(Function func) {
		this.parent = func;
		this.parent.append(this);
	}

	public Instruction get(int index) {
		return this.insts.get(index);
	}

	public Instruction append(Instruction inst) {
		this.insts.add(inst);
		return inst;
	}

	public BasicBlock add(int index, Instruction inst) {
		this.insts.add(index, inst);
		return this;
	}

	public Instruction remove(int index) {
		return this.insts.remove(index);
	}

	public int size() {
		return this.insts.size();
	}

	public int indexOf(Instruction inst) {
		return this.insts.indexOf(inst);
	}

	public String getBBName() {
		return "bb" + this.parent.indexOf(this);
	}

	public void stringfy(StringBuilder sb) {
		for(int i = 0; i < this.size(); i++) {
			this.get(i).stringfy(sb);
			sb.append("\n");
		}
	}
}
