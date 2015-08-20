package nez.x.minivm;

import nez.expr.Expression;

public class IRBuilder {
	private BasicBlock curBB;
	private Module module;

	public IRBuilder(Module m) {
		this.curBB = null;
		this.module = m;
	}

	public void setCurrentBB(BasicBlock bb) {
		this.curBB = bb;
	}

	public BasicBlock getCurrentBB() {
		return this.curBB;
	}

	public Instruction createEXIT(Expression e) {
		return curBB.append(new EXIT(e));
	}

	public Instruction createJUMP(Expression e, BasicBlock jump) {
		return curBB.append(new JUMP(e, jump));
	}

	public Instruction createCALL(Expression e, String ruleName) {
		return curBB.append(new CALL(e, ruleName));
	}

	public Instruction createRET(Expression e) {
		return curBB.append(new RET(e));
	}

	public Instruction createIFFAIL(Expression e, BasicBlock jump) {
		return curBB.append(new IFFAIL(e, jump));
	}

	public Instruction createCHAR(Expression e, BasicBlock jump, int byteChar) {
		return curBB.append(new CHAR(e, jump, byteChar));
	}

	public Instruction createCHARMAP(Expression e, BasicBlock jump) {
		return curBB.append(new CHARMAP(e, jump));
	}

	public Instruction createSTRING(Expression e, BasicBlock jump) {
		return curBB.append(new STRING(e, jump));
	}

	public Instruction createANY(Expression e, BasicBlock jump) {
		return curBB.append(new ANY(e, jump));
	}

	public Instruction createPUSHpos(Expression e) {
		return curBB.append(new PUSHpos(e));
	}

	// public Instruction createPUSHmark(Expression e) {
	// return curBB.append(new PUSHmark(e));
	// }

	public Instruction createPOPpos(Expression e) {
		return curBB.append(new POPpos(e));
	}

	public Instruction createGETpos(Expression e) {
		return curBB.append(new GETpos(e));
	}

	public Instruction createSTOREpos(Expression e) {
		return curBB.append(new STOREpos(e));
	}

	public Instruction createSUCC(Expression e) {
		return curBB.append(new SUCC(e));
	}

	public Instruction createFAIL(Expression e) {
		return curBB.append(new FAIL(e));
	}

	// public Instruction createNEW(Expression e) {
	// return curBB.append(new NEW(e));
	// }
	//
	// public Instruction createLEFTJOIN(Expression e, int index) {
	// return curBB.append(new LEFTJOIN(e, index));
	// }
	//
	// public Instruction createCAPTURE(Expression e) {
	// return curBB.append(new CAPTURE(e));
	// }
	//
	// public Instruction createCOMMIT(Expression e, int index) {
	// return curBB.append(new COMMIT(e, index));
	// }
	//
	// public Instruction createABORT(Expression e) {
	// return curBB.append(new ABORT(e));
	// }
	//
	// public Instruction createTAG(Expression e, String tag) {
	// return curBB.append(new TAG(e, tag));
	// }
	//
	// public Instruction createVALUE(Expression e, String value) {
	// return curBB.append(new VALUE(e, value));
	// }

	// public Instruction createMEMOIZE(Expression e, int memoPoint) {
	// return curBB.append(new MEMOIZE(e, memoPoint));
	// }
	//
	// public Instruction createLOOKUP(Expression e, BasicBlock jump, int
	// memoPoint) {
	// return curBB.append(new LOOKUP(e, jump, memoPoint));
	// }
	//
	// public Instruction createMEMOIZENODE(Expression e, int memoPoint) {
	// return curBB.append(new MEMOIZENODE(e, memoPoint));
	// }
	//
	// public Instruction createLOOKUPNODE(Expression e, BasicBlock jump, int
	// memoPoint, int index) {
	// return curBB.append(new LOOKUPNODE(e, jump, memoPoint, index));
	// }

	public Instruction createNOTCHAR(Expression e, BasicBlock jump) {
		return curBB.append(new NOTCHAR(e, jump));
	}

	public Instruction createNOTCHAR(Expression e, BasicBlock jump, int byteChar) {
		return curBB.append(new NOTCHAR(e, jump, byteChar));
	}

	public Instruction createNOTSTRING(Expression e, BasicBlock jump) {
		return curBB.append(new NOTSTRING(e, jump));
	}

	public Instruction createOPTIONALCHARMAP(Expression e) {
		return curBB.append(new OPTIONALCHARMAP(e));
	}

	public Instruction createZEROMORECHARMAP(Expression e) {
		return curBB.append(new ZEROMORECHARMAP(e));
	}
}
