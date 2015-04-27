package nez.x.minivm;

import java.util.ArrayList;
import java.util.List;

import nez.expr.Expression;

public abstract class Instruction {
	public Opcode op;
	public Expression expr;
	public BasicBlock parent;

	public Instruction(Expression expr) {
		this.expr = expr;
	}

	public Instruction setParent(BasicBlock bb) {
		this.parent = bb;
		return this;
	}

	protected abstract void stringfy(StringBuilder sb);

	@Override
	public abstract String toString();
}

class EXIT extends Instruction {
	public EXIT(Expression expr) {
		super(expr);
		this.op = Opcode.EXIT;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  EXIT");
	}

	@Override
	public String toString() {
		return "EXIT";
	}
}

class CALL extends Instruction {
	String ruleName;
	int jumpIndex;

	public CALL(Expression expr, String ruleName) {
		super(expr);
		this.op = Opcode.CALL;
		this.ruleName = ruleName;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  CALL ");
		sb.append(this.ruleName);
	}

	@Override
	public String toString() {
		return "CALL " + this.ruleName + "(" + this.jumpIndex + ")";
	}
}

class RET extends Instruction {
	public RET(Expression expr) {
		super(expr);
		this.op = Opcode.RET;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  RET");
	}

	@Override
	public String toString() {
		return "RET";
	}
}

abstract class JumpInstruction extends Instruction {
	BasicBlock jump;

	public JumpInstruction(Expression expr, BasicBlock jump) {
		super(expr);
		this.jump = jump;
	}

	public BasicBlock getJumpPoint() {
		return jump;
	}
}

class JUMP extends JumpInstruction {
	public JUMP(Expression expr, BasicBlock jump) {
		super(expr, jump);
		this.op = Opcode.JUMP;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  JUMP ");
		sb.append("jump:" + this.jump.getBBName());
	}

	@Override
	public String toString() {
		return "JUMP " + this.jump.codeIndex;
	}
}

class IFFAIL extends JumpInstruction {
	public IFFAIL(Expression expr, BasicBlock jump) {
		super(expr, jump);
		this.op = Opcode.IFFAIL;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  IFFAIL ");
		sb.append("jump:" + this.jump.getBBName());
	}

	@Override
	public String toString() {
		return "CONDBRANCH " + this.jump.codeIndex;
	}
}

abstract class MatchingInstruction extends Instruction {
	List<Integer> cdata;

	public MatchingInstruction(Expression expr, int... cdata) {
		super(expr);
		this.cdata = new ArrayList<Integer>();
		for (int element : cdata) {
			this.cdata.add(element);
		}
	}

	public MatchingInstruction(Expression expr) {
		super(expr);
		this.cdata = new ArrayList<Integer>();
	}

	public int size() {
		return this.cdata.size();
	}

	public int getc(int index) {
		return this.cdata.get(index);
	}

	public void append(int c) {
		this.cdata.add(c);
	}
}

abstract class JumpMatchingInstruction extends MatchingInstruction {
	BasicBlock jump;

	public JumpMatchingInstruction(Expression expr, BasicBlock jump, int... cdata) {
		super(expr, cdata);
		this.jump = jump;
	}

	public JumpMatchingInstruction(Expression expr, BasicBlock jump) {
		super(expr);
		this.jump = jump;
	}

	public BasicBlock getJumpPoint() {
		return jump;
	}
}

class CHAR extends JumpMatchingInstruction {
	public CHAR(Expression expr, BasicBlock jump, int byteChar) {
		super(expr, jump, byteChar);
		this.op = Opcode.CHAR;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  CHAR ");
		sb.append(this.getc(0) + " ");
		sb.append("jump:" + this.jump.getBBName());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CHAR ");
		sb.append(this.getc(0) + " ");
		sb.append(this.jump.codeIndex);
		return sb.toString();
	}
}

//class CHARMAP extends JumpMatchingInstruction {
//	public CHARMAP(Expression expr, BasicBlock jump) {
//		super(expr, jump);
//		this.op = Opcode.CHARMAP;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  CHARMAP ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//		sb.append(this.jump.getBBName());
//	}
//
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append("CHARMAP ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//		sb.append(this.jump.codeIndex);
//		return sb.toString();
//	}
//}
//
//class STRING extends JumpMatchingInstruction {
//	public STRING(Expression expr, BasicBlock jump) {
//		super(expr, jump);
//		this.op = Opcode.STRING;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  STRING ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//		sb.append("jump:" + this.jump.getBBName());
//	}
//
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append("STRING ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//		sb.append(this.jump.codeIndex);
//		return sb.toString();
//	}
//}

class ANY extends JumpMatchingInstruction {
	public ANY(Expression expr, BasicBlock jump, int... cdata) {
		super(expr, jump, cdata);
		this.op = Opcode.ANY;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  ANY ");
		sb.append("jump:" + this.jump.getBBName());
	}

	@Override
	public String toString() {
		return "ANY " + this.jump.codeIndex;
	}
}

abstract class StackOperateInstruction extends Instruction {
	public StackOperateInstruction(Expression expr) {
		super(expr);
	}
}

class PUSHpos extends StackOperateInstruction {
	public PUSHpos(Expression expr) {
		super(expr);
		this.op = Opcode.PUSHpos;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  PUSHpos");
	}

	@Override
	public String toString() {
		return "PUSHpos";
	}
}

//class PUSHmark extends StackOperateInstruction {
//	public PUSHmark(Expression expr) {
//		super(expr);
//		this.op = Opcode.PUSHmark;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  PUSHmark");
//	}
//
//	@Override
//	public String toString() {
//		return "PUSHmark";
//	}
//}

class GETpos extends StackOperateInstruction {
	public GETpos(Expression expr) {
		super(expr);
		this.op = Opcode.GETpos;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  GETpos");
	}

	@Override
	public String toString() {
		return "GETpos";
	}
}

class STOREpos extends StackOperateInstruction {
	public STOREpos(Expression expr) {
		super(expr);
		this.op = Opcode.STOREpos;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  STOREpos");
	}

	@Override
	public String toString() {
		return "STOREpos";
	}
}

class POPpos extends StackOperateInstruction {
	public POPpos(Expression expr) {
		super(expr);
		this.op = Opcode.POPpos;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  POPpos");
	}

	@Override
	public String toString() {
		return "POPpos";
	}
}

class STOREflag extends Instruction {
	int val;

	public STOREflag(Expression expr, int val) {
		super(expr);
		this.op = Opcode.STOREflag;
		this.val = val;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("  STOREflag");
		sb.append(this.val);
	}

	@Override
	public String toString() {
		return "STOREflag " + this.val;
	}
}

//class NEW extends Instruction {
//	public NEW(Expression expr) {
//		super(expr);
//		this.op = Opcode.NEW;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  NEW");
//	}
//
//	@Override
//	public String toString() {
//		return "NEW";
//	}
//}
//
//class LEFTJOIN extends Instruction {
//	int index;
//
//	public LEFTJOIN(Expression expr, int index) {
//		super(expr);
//		this.op = Opcode.LEFTJOIN;
//		this.index = index;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  LEFTJOIN ");
//		sb.append(this.index);
//	}
//
//	@Override
//	public String toString() {
//		return "LEFTJOIN " + this.index;
//	}
//}
//
//class COMMIT extends Instruction {
//	int index;
//
//	public COMMIT(Expression expr, int index) {
//		super(expr);
//		this.op = Opcode.COMMIT;
//		this.index = index;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  COMMIT ");
//		sb.append(this.index);
//	}
//
//	@Override
//	public String toString() {
//		return "COMMIT " + this.index;
//	}
//}
//
//class ABORT extends Instruction {
//	public ABORT(Expression expr) {
//		super(expr);
//		this.op = Opcode.ABORT;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  ABORT");
//	}
//
//	@Override
//	public String toString() {
//		return "ABORT";
//	}
//}
//
//class CAPTURE extends Instruction {
//	public CAPTURE(Expression expr) {
//		super(expr);
//		this.op = Opcode.CAPTURE;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  CAPTURE");
//	}
//
//	@Override
//	public String toString() {
//		return "CAPTURE";
//	}
//}
//
//class TAG extends Instruction {
//	String cdata;
//
//	public TAG(Expression expr, String cdata) {
//		super(expr);
//		this.op = Opcode.TAG;
//		this.cdata = cdata;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  TAG ");
//		sb.append(this.cdata);
//	}
//
//	@Override
//	public String toString() {
//		return "TAG " + this.cdata;
//	}
//}
//
//class VALUE extends Instruction {
//	String cdata;
//
//	public VALUE(Expression expr, String cdata) {
//		super(expr);
//		this.op = Opcode.VALUE;
//		this.cdata = cdata;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  VALUE ");
//		sb.append(this.cdata);
//	}
//
//	@Override
//	public String toString() {
//		return "VALUE " + this.cdata;
//	}
//}

//class MEMOIZE extends Instruction {
//	int memoPoint;
//
//	public MEMOIZE(Expression expr, int memoPoint) {
//		super(expr);
//		this.op = Opcode.MEMOIZE;
//		this.memoPoint = memoPoint;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  MEMOIZE " + this.memoPoint);
//	}
//
//	@Override
//	public String toString() {
//		return "MEMOIZE " + this.memoPoint;
//	}
//}
//
//class LOOKUP extends JumpInstruction {
//	int memoPoint;
//
//	public LOOKUP(Expression expr, BasicBlock jump, int memoPoint) {
//		super(expr, jump);
//		this.op = Opcode.LOOKUP;
//		this.memoPoint = memoPoint;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  LOOKUP " + this.memoPoint);
//	}
//
//	@Override
//	public String toString() {
//		return "LOOKUP " + this.memoPoint + " " + this.jump.codeIndex;
//	}
//}
//
//class MEMOIZENODE extends Instruction {
//	int memoPoint;
//
//	public MEMOIZENODE(Expression expr, int memoPoint) {
//		super(expr);
//		this.op = Opcode.MEMOIZENODE;
//		this.memoPoint = memoPoint;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  MEMOIZENODE " + this.memoPoint);
//	}
//
//	@Override
//	public String toString() {
//		return "MEMOIZENODE " + this.memoPoint;
//	}
//}
//
//class LOOKUPNODE extends JumpInstruction {
//	int memoPoint;
//	int index;
//
//	public LOOKUPNODE(Expression expr, BasicBlock jump, int memoPoint, int index) {
//		super(expr, jump);
//		this.op = Opcode.LOOKUPNODE;
//		this.memoPoint = memoPoint;
//		this.index = index;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  LOOKUPNODE " + this.memoPoint + " " + this.index);
//	}
//
//	@Override
//	public String toString() {
//		return "LOOKUPNODE " + this.memoPoint + " " + this.index + " " + this.jump.codeIndex;
//	}
//}

//class NOTCHAR extends JumpMatchingInstruction {
//	public NOTCHAR(Expression expr, BasicBlock jump, int... cdata) {
//		super(expr, jump, cdata);
//		this.op = Opcode.NOTCHAR;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  NOTCHAR ");
//		sb.append(this.getc(0));
//		sb.append(" jump:" + this.jump.getBBName());
//	}
//
//	@Override
//	public String toString() {
//		return "NOTCHAR " + this.getc(0) + " " + this.jump.codeIndex;
//	}
//}
//
//class NOTCHARMAP extends JumpMatchingInstruction {
//	public NOTCHARMAP(Expression expr, BasicBlock jump) {
//		super(expr, jump);
//		this.op = Opcode.NOTCHARMAP;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  NOTCHARMAP ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//		sb.append(this.jump.getBBName());
//	}
//
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append("NOTCHARMAP ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//		sb.append(this.jump.codeIndex);
//		return sb.toString();
//	}
//}
//
//class NOTSTRING extends JumpMatchingInstruction {
//	public NOTSTRING(Expression expr, BasicBlock jump) {
//		super(expr, jump);
//		this.op = Opcode.NOTSTRING;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  NOTSTRING ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//		sb.append(this.jump.getBBName());
//	}
//
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append("NOTSTRING ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//		sb.append(this.jump.codeIndex);
//		return sb.toString();
//	}
//}
//
//class NOTCHARANY extends JumpMatchingInstruction {
//	public NOTCHARANY(Expression expr, BasicBlock jump, int... cdata) {
//		super(expr, jump, cdata);
//		this.op = Opcode.NOTCHARANY;
//	}
//
//	public void addBasicBlock(int index, BasicBlock bb) {
//		this.parent = bb;
//		this.parent.add(index, this);
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  NOTCHARANY ");
//		sb.append(this.getc(0));
//		sb.append(" jump:" + this.jump.getBBName());
//	}
//
//	@Override
//	public String toString() {
//		return "NOTCHARANY " + this.getc(0) + " " + this.jump.codeIndex;
//	}
//}
//
//class OPTIONALCHAR extends MatchingInstruction {
//	public OPTIONALCHAR(Expression expr, int... cdata) {
//		super(expr, cdata);
//		this.op = Opcode.OPTIONALCHAR;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  OPTIONALCHAR ");
//		sb.append(this.getc(0));
//	}
//
//	@Override
//	public String toString() {
//		return "OPTIONALCHAR " + this.getc(0);
//	}
//}
//
//class OPTIONALCHARMAP extends MatchingInstruction {
//	public OPTIONALCHARMAP(Expression expr) {
//		super(expr);
//		this.op = Opcode.OPTIONALCHARMAP;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  OPTIONALCHARMAP ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//	}
//
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append("OPTIONALCHARMAP ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//		return sb.toString();
//	}
//}
//
//class OPTIONALSTRING extends MatchingInstruction {
//	public OPTIONALSTRING(Expression expr) {
//		super(expr);
//		this.op = Opcode.OPTIONALSTRING;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  OPTIONALSTRING ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//	}
//
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append("OPTIONALSTRING ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//		return sb.toString();
//	}
//}
//
//class ZEROMORECHARMAP extends MatchingInstruction {
//	public ZEROMORECHARMAP(Expression expr) {
//		super(expr);
//		this.op = Opcode.ZEROMORECHARMAP;
//	}
//
//	@Override
//	protected void stringfy(StringBuilder sb) {
//		sb.append("  ZEROMORECHARMAP ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//	}
//
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append("ZEROMORECHARMAP ");
//		for (int i = 0; i < this.size(); i++) {
//			sb.append(this.getc(i) + " ");
//		}
//		return sb.toString();
//	}
//}
