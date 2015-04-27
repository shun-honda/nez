package nez.x.minivm;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import nez.Grammar;
import nez.expr.And;
import nez.expr.AnyChar;
import nez.expr.ByteChar;
import nez.expr.ByteMap;
import nez.expr.Capture;
import nez.expr.Choice;
import nez.expr.Empty;
import nez.expr.Expression;
import nez.expr.Failure;
import nez.expr.GrammarVisitor;
import nez.expr.Link;
import nez.expr.New;
import nez.expr.NonTerminal;
import nez.expr.Not;
import nez.expr.Option;
import nez.expr.Repetition;
import nez.expr.Repetition1;
import nez.expr.Replace;
import nez.expr.Rule;
import nez.expr.Sequence;
import nez.expr.Tagging;
import nez.expr.Unary;
import nez.runtime.MemoPoint;

public class MiniVMCompiler extends GrammarVisitor {

	private final OptimizerOption option;
	private final MiniVMFlowAnalyzer analyzer;
	private Optimizer optimizer;
	private IRBuilder builder;

	boolean optChoiceMode = true;
	boolean inlining = false;

	boolean PatternMatching = true;

	Grammar peg;
	Module module;
	Function func;
	HashMap<Integer, MemoPoint> memoMap;
	HashMap<String, Boolean> inliningMap;

	public MiniVMCompiler(int level, Grammar peg) {
		this.peg = peg;
		this.module = new Module();
		this.option = new OptimizerOption();
		switch (level) {
//		case 1:
//			this.option.setFlowAnalysis(true);
//			break;
//		case 2:
//			this.option.setFusionInstruction(true);
//			break;
//		case 3:
//			this.option.setInlining(true);
//			break;
//		case 4:
//			this.option.setFlowAnalysis(true);
//			this.option.setFusionInstruction(true);
//			this.option.setInlining(true);
		default:
			break;
		}
		this.analyzer = new MiniVMFlowAnalyzer();
		this.analyzer.analyze(peg);
		this.optimizer = new Optimizer(this.module, this.option);
		this.builder = new IRBuilder(this.module);
		this.memoMap = new HashMap<Integer, MemoPoint>();
		this.inliningMap = new HashMap<String, Boolean>();
	}

	int codeIndex;

	public void writeByteCode(String grammerfileName, String outputFileName, Grammar peg) {
		// generateProfileCode(peg);
		// System.out.println("choiceCase: " + choiceCaseCount +
		// "\nconstructor: " + constructorCount);
		byte[] byteCode = new byte[this.codeIndex * 256];
		int pos = 0;
		// Version of the specification (2 byte)
		byte[] version = new byte[2];
		version[0] = 0;
		version[1] = 9;
		byteCode[pos] = version[0];
		pos++;
		byteCode[pos] = version[1];
		pos++;

		// Length of grammerfileName (4 byte)
		int fileNamelen = grammerfileName.length();
		pos = write32(byteCode, fileNamelen, pos);

		// GrammerfileName (n byte)
		byte[] name = grammerfileName.getBytes();
		for (int i = 0; i < fileNamelen; i++) {
			byteCode[pos] = name[i];
			pos++;
		}

		// pool_size_info
//		int poolSizeInfo = 1064;
//		pos = write32(byteCode, poolSizeInfo, pos);

		// rule table
		int ruleSize = this.module.size();
		pos = write32(byteCode, ruleSize, pos);
		for (int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			byte[] ruleName = func.funcName.getBytes();
			int ruleNamelen = ruleName.length;
			long entryPoint = func.get(0).codeIndex;
			pos = write32(byteCode, ruleNamelen, pos);
			for (byte element : ruleName) {
				byteCode[pos] = element;
				pos++;
			}
			pos = write64(byteCode, entryPoint, pos);
		}

		// memo table size (use memo hash)
//		int memoTableSize = this.memoMap.size();
//		pos = write32(byteCode, memoTableSize, pos);

		// Length of byte code (8 byte)
		long byteCodelength = this.codeIndex;
		pos = write64(byteCode, byteCodelength, pos);

		int index = 0;
		// byte code (m byte)
		for (int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			for (int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				for (int k = 0; k < bb.size(); k++) {
					Instruction inst = bb.get(k);
					pos = writeOpcode(inst, byteCode, pos, index);
					index++;
				}
			}
		}

		System.out.println("bytecode size: " + pos + "[byte]");

		try {
			if (outputFileName == null) {
				System.out.println("unspecified outputfile");
				System.exit(0);
			}
			FileOutputStream fos = new FileOutputStream(outputFileName);
			for (int i = 0; i < pos; i++) {
				fos.write(byteCode[i]);
			}
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int write16(byte[] byteCode, long num, int pos) {
		byteCode[pos] = (byte) (0x000000ff & num);
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 8));
		pos++;
		return pos;
	}

	private int write32(byte[] byteCode, long num, int pos) {
		byteCode[pos] = (byte) (0x000000ff & (num));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 8));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 16));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 24));
		pos++;
		return pos;
	}

	private int write64(byte[] byteCode, long num, int pos) {
		pos = write32(byteCode, num, pos);
		pos = write32(byteCode, num >> 32, pos);
		return pos;
	}

	private int writeCdataByteCode(byte[] byteCode, String cdata, int pos) {
		int j = 0;
		pos = write16(byteCode, cdata.length(), pos);
		byte[] nameByte = cdata.getBytes();
		while (j < cdata.length()) {
			byteCode[pos] = nameByte[j];
			j++;
			pos++;
		}
		return pos;
	}

	private int writeOpcode(Instruction code, byte[] byteCode, int pos, int index) {
		byteCode[pos] = (byte) code.op.ordinal();
		pos++;
		switch (code.op) {
		case JUMP:
			pos = write32(byteCode, ((JUMP) code).jump.codeIndex - index, pos);
			break;
		case CALL:
			pos = write32(byteCode, ((CALL) code).jumpIndex - index, pos);
			break;
		case IFFAIL:
			pos = write32(byteCode, ((JumpInstruction) code).jump.codeIndex - index, pos);
			break;
		case CHAR:
			pos = write32(byteCode, ((CHAR) code).getc(0), pos);
			pos = write32(byteCode, ((CHAR) code).jump.codeIndex - index, pos);
			break;
//		case CHARMAP:
//			CHARMAP charset = (CHARMAP) code;
//			pos = write16(byteCode, charset.size(), pos);
//			for (int j = 0; j < charset.size(); j++) {
//				pos = write32(byteCode, charset.getc(j), pos);
//			}
//			pos = write32(byteCode, charset.jump.codeIndex - index, pos);
//			break;
//		case STRING:
//			pos = write16(byteCode, ((STRING) code).size(), pos);
//			for (int j = 0; j < ((STRING) code).size(); j++) {
//				pos = write32(byteCode, ((STRING) code).getc(j), pos);
//			}
//			pos = write32(byteCode, ((STRING) code).jump.codeIndex - index, pos);
//			break;
		case ANY:
			pos = write32(byteCode, ((ANY) code).jump.codeIndex - index, pos);
			break;
		case STOREflag:
			pos = write32(byteCode, ((STOREflag) code).val, pos);
			break;
//		case LEFTJOIN:
//			pos = write32(byteCode, ((LEFTJOIN) code).index, pos);
//			break;
//		case COMMIT:
//			pos = write32(byteCode, ((COMMIT) code).index, pos);
//			break;
//		case TAG:
//			pos = writeCdataByteCode(byteCode, ((TAG) code).cdata, pos);
//			break;
//		case VALUE:
//			pos = writeCdataByteCode(byteCode, ((VALUE) code).cdata, pos);
//			break;
//		case NOTCHAR:
//			NOTCHAR nc = (NOTCHAR) code;
//			pos = write32(byteCode, nc.getc(0), pos);
//			pos = write32(byteCode, nc.jump.codeIndex - index, pos);
//			break;
//		case NOTCHARMAP:
//			NOTCHARMAP ncs = (NOTCHARMAP) code;
//			pos = write16(byteCode, ncs.size(), pos);
//			for (int j = 0; j < ncs.size(); j++) {
//				pos = write32(byteCode, ncs.getc(j), pos);
//			}
//			pos = write32(byteCode, ncs.jump.codeIndex - index, pos);
//			break;
//		case NOTSTRING:
//			NOTSTRING ns = (NOTSTRING) code;
//			pos = write16(byteCode, ns.size(), pos);
//			for (int j = 0; j < ns.size(); j++) {
//				pos = write32(byteCode, ns.getc(j), pos);
//			}
//			pos = write32(byteCode, ns.jump.codeIndex - index, pos);
//			break;
//		case NOTCHARANY:
//			NOTCHARANY nca = (NOTCHARANY) code;
//			pos = write32(byteCode, nca.getc(0), pos);
//			pos = write32(byteCode, nca.jump.codeIndex - index, pos);
//			break;
//		case OPTIONALCHAR:
//			pos = write32(byteCode, ((OPTIONALCHAR) code).getc(0), pos);
//			break;
//		case OPTIONALCHARMAP:
//			OPTIONALCHARMAP ocs = (OPTIONALCHARMAP) code;
//			pos = write16(byteCode, ocs.size(), pos);
//			for (int j = 0; j < ocs.size(); j++) {
//				pos = write32(byteCode, ocs.getc(j), pos);
//			}
//			break;
//		case OPTIONALSTRING:
//			OPTIONALSTRING os = (OPTIONALSTRING) code;
//			pos = write16(byteCode, os.size(), pos);
//			for (int j = 0; j < os.size(); j++) {
//				pos = write32(byteCode, os.getc(j), pos);
//			}
//			break;
//		case ZEROMORECHARMAP:
//			ZEROMORECHARMAP zcs = (ZEROMORECHARMAP) code;
//			pos = write16(byteCode, zcs.size(), pos);
//			for (int j = 0; j < zcs.size(); j++) {
//				pos = write32(byteCode, zcs.getc(j), pos);
//			}
//			break;
		default:
			break;
		}
		return pos;
	}

	private void setInsertPoint(BasicBlock bb) {
		bb.setInsertPoint(this.func);
		this.builder.setCurrentBB(bb);
	}

	class FailureBB {
		BasicBlock fbb;
		FailureBB prev;

		public FailureBB(BasicBlock bb, FailureBB prev) {
			this.fbb = bb;
			this.prev = prev;
		}
	}

	FailureBB fLabel = null;

	private void pushFailureJumpPoint(BasicBlock bb) {
		this.fLabel = new FailureBB(bb, this.fLabel);
	}

	private void popFailureJumpPoint(Rule r) {
		this.fLabel = this.fLabel.prev;
	}

	private BasicBlock popFailureJumpPoint(Expression e) {
		BasicBlock fbb = this.fLabel.fbb;
		this.fLabel = this.fLabel.prev;
		return fbb;
	}

	private BasicBlock jumpFailureJump() {
		return this.fLabel.fbb;
	}

	private BasicBlock jumpPrevFailureJump() {
		return this.fLabel.prev.fbb;
	}

	// @Override
	// public void formatGrammar(Grammar peg, StringBuilder sb) {
	// this.peg = peg;
	// this.formatHeader();
	// for(Rule r: peg.getRuleList()) {
	// if (r.getLocalName().equals("File")) {
	// this.formatRule(r, sb);
	// break;
	// }
	// }
	// for(Rule r: peg.getRuleList()) {
	// if (!r.getLocalName().equals("File")) {
	// if (!r.getLocalName().startsWith("\"")) {
	// this.formatRule(r, sb);
	// }
	// }
	// }
	// this.formatFooter();
	// }

	StringBuilder sb;

	public void formatHeader() {
		System.out.println("\nGenerate Byte Code\n");
		// writeCode(Instruction.EXIT);
		Function func = new Function(this.module, "EXIT");
		this.inliningMap.put(func.funcName, false);
		if (this.option.useInlining) {
			for (Rule rule : this.peg.getRuleList()) {
				this.inliningMap.put(rule.getLocalName(), false);
			}
		}
		BasicBlock bb = new BasicBlock(func);
		this.builder.setCurrentBB(bb);
		this.builder.createEXIT(null);
	}

	public void formatFooter() {
		System.out.println(this.module.stringfy(this.sb));
		if (this.option.useFlowAnalysis) {
			this.optimizer.optimize();
		}
		if (this.option.useInlining) {
			int i = 0;
			while (i < this.module.size()) {
				if (this.inliningMap.get(this.module.get(i).funcName)) {
					this.module.remove(i);
				}
				else {
					i++;
				}
			}
		}
		this.labeling();
		this.dumpLastestCode();
	}

	HashMap<String, Integer> callMap = new HashMap<String, Integer>();

	private void labeling() {
		int codeIndex = 0;
		for (int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			this.callMap.put(func.funcName, codeIndex);
			for (int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				bb.codeIndex = codeIndex;
				codeIndex += bb.size();
			}
		}
		this.codeIndex = codeIndex;
		for (int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			for (int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				for (int k = 0; k < bb.size(); k++) {
					Instruction inst = bb.get(k);
					if (inst instanceof CALL) {
						CALL cinst = (CALL) inst;
						cinst.jumpIndex = this.callMap.get(cinst.ruleName);
					}
				}
			}
		}
	}

	private void dumpLastestCode() {
		int codeIndex = 0;
		for (int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			for (int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				for (int k = 0; k < bb.size(); k++) {
					Instruction inst = bb.get(k);
					System.out.println("[" + codeIndex + "] " + inst.toString());
					codeIndex++;
				}
			}
		}
	}

	private boolean checkCharMap(Choice e) {
		for (int i = 0; i < e.size(); i++) {
			Expression inner = e.get(i);
			if (!(inner instanceof ByteChar) && !(inner instanceof ByteMap)) {
				return false;
			}
		}
		return true;
	}

	private boolean checkString(Sequence e) {
		for (int i = 0; i < e.size(); i++) {
			if (!(e.get(i) instanceof ByteChar)) {
				return false;
			}
		}
		return true;
	}

	BasicBlock currentFailBB;

//	private int checkWriteChoiceCharset(Choice e, int index, BasicBlock bb, BasicBlock fbb, boolean UnaryChoice) {
//		int charCount = 0;
//		for (int i = index; i < e.size(); i++) {
//			if (e.get(i) instanceof ByteChar || e.get(i) instanceof ByteMap) {
//				charCount++;
//			}
//			else {
//				break;
//			}
//		}
//		if (charCount <= 1) {
//			fbb = new BasicBlock();
//			this.pushFailureJumpPoint(fbb);
//			e.get(index).visit(this);
//			this.backTrackFlag = true;
//			this.currentFailBB = fbb;
//			return index++;
//		}
//		if (charCount != e.size()) {
//			backTrackFlag = true;
//			fbb = new BasicBlock();
//			this.currentFailBB = fbb;
//			this.pushFailureJumpPoint(fbb);
//		}
//		writeCharsetCode(e, index, charCount);
//		return index + charCount - 1;
//	}
//
//	private final int writeSequenceCode(Expression e, int index, int size) {
//		int count = 0;
//		for (int i = index; i < size; i++) {
//			if (e.get(i) instanceof ByteChar) {
//				count++;
//			}
//			else {
//				break;
//			}
//		}
//		if (count <= 1) {
//			e.get(index).visit(this);
//			return index++;
//		}
//		STRING str = (STRING) this.builder.createSTRING(e, this.jumpFailureJump());
//		for (int i = index; i < index + count; i++) {
//			str.append(((ByteChar) e.get(i)).byteChar);
//		}
//		return index + count - 1;
//	}

	// The depth to control the stack caching optimization
	int depth = 0;

	private boolean checkSC(Expression e) {
		int depth = this.depth;
		if (e instanceof NonTerminal) {
			e = getNonTerminalRule(e);
		}
		if (e instanceof Unary) {
			if (this.depth++ < 2) {
				if (this.option.useFusionInstruction) {
					if (e instanceof Not) {
						if (checkOptNot((Not) e)) {
							this.depth = depth;
							return true;
						}
					}
					else if (e instanceof Repetition) {
						if (checkOptRepetition((Repetition) e)) {
							this.depth = depth;
							return true;
						}
					}
					else if (e instanceof Option) {
						if (checkOptOptional((Option) e)) {
							this.depth = depth;
							return true;
						}
					}
				}
				boolean check = checkSC(((Unary) e).get(0));
				this.depth = depth;
				return check;
			}
			this.depth = depth;
			return false;
		}
		if (e instanceof Sequence || e instanceof Choice || e instanceof New) {
			if (depth < 2) {
				for (int i = 0; i < e.size(); i++) {
					if (!checkSC(e.get(i))) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
		return true;
	}

	private boolean checkOptRepetition(Repetition e) {
		Expression inner = e.get(0);
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof ByteMap) {
			return true;
		}
		if (inner instanceof Choice) {
			return checkCharMap((Choice) inner);
		}
		return false;
	}

	private boolean checkOptNot(Not e) {
		Expression inner = e.get(0);
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof ByteChar) {
			return true;
		}
		if (inner instanceof ByteMap) {
			return true;
		}
		if (inner instanceof AnyChar) {
			return true;
		}
		if (inner instanceof Choice) {
			return checkCharMap((Choice) inner);
		}
		if (inner instanceof Sequence) {
			return checkString((Sequence) inner);
		}
		return false;
	}

	private boolean checkOptOptional(Option e) {
		Expression inner = e.get(0);
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof ByteChar) {
			return true;
		}
		if (inner instanceof ByteMap) {
			return true;
		}
		if (inner instanceof Choice) {
			return checkCharMap((Choice) inner);
		}
		if (inner instanceof Sequence) {
			return checkString((Sequence) inner);
		}
		return false;
	}

//	private void writeCharsetCode(Expression e, int index, int charCount) {
//		CHARMAP inst = (CHARMAP) this.builder.createCHARMAP(e, this.jumpFailureJump());
//		for (int i = index; i < index + charCount; i++) {
//			if (e.get(i) instanceof ByteChar) {
//				inst.append(((ByteChar) e.get(i)).byteChar);
//			}
//			else if (e.get(i) instanceof ByteMap) {
//				ByteMap br = (ByteMap) e.get(i);
//				for (int c = 0; c < 256; c++) {
//					if (br.byteMap[c]) {
//						inst.append(c);
//					}
//				}
//			}
//			else {
//				System.out.println("Error: Not Char Content in Charset");
//			}
//		}
//	}

//	private void optimizeChoice(Choice e) {
//		if (this.checkCharMap(e)) {
//			writeCharsetCode(e, 0, e.size());
//		}
//	}

	private Expression getNonTerminalRule(Expression e) {
		while (e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}

	private void writeNotCode(Not e) {
		if (this.option.useFlowAnalysis) {
			if (this.analyzer.flowAnalysisMap.get(e).equals(ExprFlow.ChoiceNot)) {
				BasicBlock fbb = new BasicBlock();
				this.pushFailureJumpPoint(fbb);
//				this.builder.createPUSHpos(e);
				e.get(0).visit(this);
				if (this.lastChoiceElementIsUnary) {
					this.builder.createSTOREpos(e);
					this.builder.createSTOREflag(e, 1);
					this.builder.createJUMP(e, this.jumpPrevFailureJump());
					this.popFailureJumpPoint(e);
					this.setInsertPoint(fbb);
					this.builder.setCurrentBB(fbb);
					this.builder.createGETpos(e);
					this.builder.createSTOREflag(e, 0);
					return;
				}
				else {
					this.builder.createGETpos(e);
					this.builder.createJUMP(e, this.jumpPrevFailureJump());
					this.popFailureJumpPoint(e);
					this.setInsertPoint(fbb);
					this.builder.setCurrentBB(fbb);
					this.builder.createGETpos(e);
					this.builder.createSTOREflag(e, 0);
					return;
				}
			}
		}
		BasicBlock fbb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.builder.createPUSHpos(e);
		e.get(0).visit(this);
		this.builder.createSTOREpos(e);
		this.builder.createSTOREflag(e, 1);
		this.builder.createJUMP(e, this.jumpPrevFailureJump());
		this.popFailureJumpPoint(e);
		this.setInsertPoint(fbb);
		this.builder.setCurrentBB(fbb);
		this.builder.createSTOREpos(e);
		this.builder.createSTOREflag(e, 0);
	}

//	private void writeSCNotCode(Not e) {
//		if (this.depth == 0) {
//			BasicBlock fbb = new BasicBlock();
//			this.pushFailureJumpPoint(fbb);
//			this.builder.createLOADp1(e);
//			this.depth++;
//			e.get(0).visit(this);
//			this.depth--;
//			this.builder.createSTOREp1(e);
//			this.builder.createSTOREflag(e, 1);
//			this.builder.createJUMP(e, this.jumpPrevFailureJump());
//			this.popFailureJumpPoint(e);
//			this.setInsertPoint(fbb);
//			this.builder.setCurrentBB(fbb);
//			this.builder.createSTOREp1(e);
//			this.builder.createSTOREflag(e, 0);
//		} else if (depth == 1) {
//			BasicBlock fbb = new BasicBlock();
//			this.pushFailureJumpPoint(fbb);
//			this.builder.createLOADp2(e);
//			this.depth++;
//			e.get(0).visit(this);
//			this.depth--;
//			this.builder.createSTOREp2(e);
//			this.builder.createSTOREflag(e, 1);
//			this.builder.createJUMP(e, this.jumpPrevFailureJump());
//			this.popFailureJumpPoint(e);
//			this.setInsertPoint(fbb);
//			this.builder.setCurrentBB(fbb);
//			this.builder.createSTOREp2(e);
//			this.builder.createSTOREflag(e, 0);
//		} else {
//			BasicBlock fbb = new BasicBlock();
//			this.pushFailureJumpPoint(fbb);
//			this.builder.createLOADp3(e);
//			this.depth++;
//			e.get(0).visit(this);
//			this.depth--;
//			this.builder.createSTOREp3(e);
//			this.builder.createSTOREflag(e, 1);
//			this.builder.createJUMP(e, this.jumpPrevFailureJump());
//			this.popFailureJumpPoint(e);
//			this.setInsertPoint(fbb);
//			this.builder.setCurrentBB(fbb);
//			this.builder.createSTOREp3(e);
//			this.builder.createSTOREflag(e, 0);
//		}
//	}

//	private void writeNotCharMapCode(Choice e) {
//		NOTCHARMAP inst = (NOTCHARMAP) this.builder.createNOTCHARMAP(e, this.jumpFailureJump());
//		for (int i = 0; i < e.size(); i++) {
//			if (e.get(i) instanceof ByteChar) {
//				inst.append(((ByteChar) e.get(i)).byteChar);
//			}
//			else if (e.get(i) instanceof ByteMap) {
//				ByteMap br = (ByteMap) e.get(i);
//				for (int c = 0; c < 256; c++) {
//					if (br.byteMap[c]) {
//						inst.append(c);
//					}
//				}
//			}
//			else {
//				System.out.println("Error: Not Char Content in Charset");
//			}
//		}
//	}
//
//	private void writeNotStringCode(Sequence e) {
//		NOTSTRING inst = (NOTSTRING) this.builder.createNOTSTRING(e, this.jumpFailureJump());
//		for (int i = 0; i < e.size(); i++) {
//			inst.append(((ByteChar) e.get(i)).byteChar);
//		}
//	}
//
//	private boolean optimizeNot(Not e) {
//		Expression inner = e.get(0);
//		if (inner instanceof NonTerminal) {
//			inner = getNonTerminalRule(inner);
//		}
//		if (inner instanceof ByteChar) {
//			this.builder.createNOTCHAR(inner, this.jumpFailureJump(), ((ByteChar) inner).byteChar);
//			return true;
//		}
//		if (inner instanceof ByteMap) {
//			ByteMap br = (ByteMap) inner;
//			NOTCHARMAP inst = (NOTCHARMAP) this.builder.createNOTCHARMAP(inner, this.jumpFailureJump());
//			for (int c = 0; c < 256; c++) {
//				if (br.byteMap[c]) {
//					inst.append(c);
//				}
//			}
//			return true;
//		}
//		if (inner instanceof Choice) {
//			if (checkCharMap((Choice) inner)) {
//				writeNotCharMapCode((Choice) inner);
//				return true;
//			}
//		}
//		if (inner instanceof Sequence) {
//			if (checkString((Sequence) inner)) {
//				writeNotStringCode((Sequence) inner);
//				return true;
//			}
//		}
//		return false;
//	}

	private void writeOptionalCode(Option e) {
		if (this.option.useFlowAnalysis) {
			if (this.analyzer.flowAnalysisMap.get(e).equals(ExprFlow.ChoiceOption)) {
				BasicBlock fbb = new BasicBlock();
				BasicBlock mergebb = new BasicBlock();
				this.pushFailureJumpPoint(fbb);
				e.get(0).visit(this);
				if (this.lastChoiceElementIsUnary) {
					this.builder.createJUMP(e, mergebb);
					this.popFailureJumpPoint(e);
					this.setInsertPoint(fbb);
					this.builder.createSTOREflag(e, 0);
					this.builder.createGETpos(e);
					this.builder.setCurrentBB(mergebb);
					this.setInsertPoint(mergebb);
				}
				else {
					this.builder.createJUMP(e, mergebb);
					this.popFailureJumpPoint(e);
					this.setInsertPoint(fbb);
					this.builder.createSTOREflag(e, 0);
					this.builder.createGETpos(e);
					this.builder.setCurrentBB(mergebb);
					this.setInsertPoint(mergebb);
				}
				return;
			}
		}
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.builder.createPUSHpos(e);
		e.get(0).visit(this);
		this.builder.createPOPpos(e);
		this.builder.createJUMP(e, mergebb);
		this.popFailureJumpPoint(e);
		this.setInsertPoint(fbb);
		this.builder.createSTOREflag(e, 0);
		this.builder.createSTOREpos(e);
		this.builder.setCurrentBB(mergebb);
		this.setInsertPoint(mergebb);
	}

//	private void writeSCOptionalCode(Option e) {
//		BasicBlock fbb = new BasicBlock();
//		BasicBlock mergebb = new BasicBlock();
//		this.pushFailureJumpPoint(fbb);
//		if (this.depth == 0) {
//			this.builder.createLOADp1(e);
//			this.depth++;
//			e.get(0).visit(this);
//			this.depth--;
//			this.builder.createJUMP(e, mergebb);
//			this.popFailureJumpPoint(e);
//			this.setInsertPoint(fbb);
//			this.builder.createSTOREflag(e, 0);
//			this.builder.createSTOREp1(e);
//			this.builder.setCurrentBB(mergebb);
//			this.setInsertPoint(mergebb);
//		} else if (this.depth == 1) {
//			this.builder.createLOADp2(e);
//			this.depth++;
//			e.get(0).visit(this);
//			this.depth--;
//			this.builder.createJUMP(e, mergebb);
//			this.popFailureJumpPoint(e);
//			this.setInsertPoint(fbb);
//			this.builder.createSTOREflag(e, 0);
//			this.builder.createSTOREp2(e);
//			this.builder.setCurrentBB(mergebb);
//			this.setInsertPoint(mergebb);
//		} else {
//			this.builder.createLOADp3(e);
//			this.depth++;
//			e.get(0).visit(this);
//			this.depth--;
//			this.builder.createJUMP(e, mergebb);
//			this.popFailureJumpPoint(e);
//			this.setInsertPoint(fbb);
//			this.builder.createSTOREflag(e, 0);
//			this.builder.createSTOREp3(e);
//			this.builder.setCurrentBB(mergebb);
//			this.setInsertPoint(mergebb);
//		}
//	}

//	private void writeOptionalByteMapCode(ByteMap e) {
//		OPTIONALCHARMAP inst = (OPTIONALCHARMAP) this.builder.createOPTIONALCHARMAP(e);
//		for (int c = 0; c < 256; c++) {
//			if (e.byteMap[c]) {
//				inst.append(c);
//			}
//		}
//	}
//
//	private void writeOptionalCharMapCode(Choice e) {
//		OPTIONALCHARMAP inst = (OPTIONALCHARMAP) this.builder.createOPTIONALCHARMAP(e);
//		for (int i = 0; i < e.size(); i++) {
//			if (e.get(i) instanceof ByteChar) {
//				inst.append(((ByteChar) e.get(i)).byteChar);
//			}
//			else if (e.get(i) instanceof ByteMap) {
//				ByteMap br = (ByteMap) e.get(i);
//				for (int c = 0; c < 256; c++) {
//					if (br.byteMap[c]) {
//						inst.append(c);
//					}
//				}
//			}
//			else {
//				System.out.println("Error: Not Char Content in Charset");
//			}
//		}
//	}
//
//	private void writeOptionalStringCode(Sequence e) {
//		OPTIONALSTRING inst = (OPTIONALSTRING) this.builder.createOPTIONALSTRING(e);
//		for (int i = 0; i < e.size(); i++) {
//			inst.append(((ByteChar) e.get(i)).byteChar);
//		}
//	}

	private boolean optimizeOptional(Option e) {
//		Expression inner = e.get(0);
//		if (inner instanceof NonTerminal) {
//			inner = getNonTerminalRule(inner);
//		}
//		if (inner instanceof ByteChar) {
//			this.builder.createOPTIONALCHAR(inner, ((ByteChar) inner).byteChar);
//			return true;
//		}
//		if (inner instanceof ByteMap) {
//			writeOptionalByteMapCode((ByteMap) inner);
//			return true;
//		}
//		if (inner instanceof Choice) {
//			if (checkCharMap((Choice) inner)) {
//				writeOptionalCharMapCode((Choice) inner);
//				return true;
//			}
//		}
//		if (inner instanceof Sequence) {
//			if (checkString((Sequence) inner)) {
//				writeOptionalStringCode((Sequence) inner);
//				return true;
//			}
//		}
		return false;
	}

	private void writeRepetitionCode(Repetition e) {
//		if (this.option.useFlowAnalysis) {
//			if (this.analyzer.flowAnalysisMap.get(e).equals(ExprFlow.ChoiceRepetition)) {
//				BasicBlock bb = new BasicBlock(this.func);
//				BasicBlock fbb = new BasicBlock();
//				BasicBlock mergebb = new BasicBlock();
//				this.pushFailureJumpPoint(fbb);
//				this.builder.setCurrentBB(bb);
//				e.get(0).visit(this);
//				if (this.lastChoiceElementIsUnary) {
//					this.builder.createJUMP(e, bb);
//					this.popFailureJumpPoint(e);
//					this.setInsertPoint(fbb);
//					this.builder.createSTOREflag(e, 0);
//					this.builder.createSTOREpos(e);
//					this.setInsertPoint(mergebb);
//					this.builder.setCurrentBB(mergebb);
//				}
//				else {
//					this.builder.createJUMP(e, bb);
//					this.popFailureJumpPoint(e);
//					this.setInsertPoint(fbb);
//					this.builder.createSTOREflag(e, 0);
//					this.builder.createGETpos(e);
//					this.setInsertPoint(mergebb);
//					this.builder.setCurrentBB(mergebb);
//				}
//				return;
//			}
//		}
		BasicBlock bb = new BasicBlock(this.func);
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.builder.setCurrentBB(bb);
		this.builder.createPUSHpos(e);
		e.get(0).visit(this);
		this.builder.createPOPpos(e);
		this.builder.createJUMP(e, bb);
		this.popFailureJumpPoint(e);
		this.setInsertPoint(fbb);
		this.builder.createSTOREflag(e, 0);
		this.builder.createSTOREpos(e);
		this.setInsertPoint(mergebb);
		this.builder.setCurrentBB(mergebb);
	}

//	private void writeSCRepetitionCode(Repetition e) {
//		BasicBlock bb = new BasicBlock(this.func);
//		BasicBlock fbb = new BasicBlock();
//		BasicBlock mergebb = new BasicBlock();
//		this.pushFailureJumpPoint(fbb);
//		this.builder.setCurrentBB(bb);
//		if (this.depth == 0) {
//			this.builder.createLOADp1(e);
//			this.depth++;
//			e.get(0).visit(this);
//			this.depth--;
//			this.builder.createJUMP(e, bb);
//			this.popFailureJumpPoint(e);
//			this.setInsertPoint(fbb);
//			this.builder.createSTOREflag(e, 0);
//			this.builder.createSTOREp1(e);
//			this.setInsertPoint(mergebb);
//			this.builder.setCurrentBB(mergebb);
//		} else if (this.depth == 1) {
//			this.builder.createLOADp2(e);
//			this.depth++;
//			e.get(0).visit(this);
//			this.depth--;
//			this.builder.createJUMP(e, bb);
//			this.popFailureJumpPoint(e);
//			this.setInsertPoint(fbb);
//			this.builder.createSTOREflag(e, 0);
//			this.builder.createSTOREp2(e);
//			this.setInsertPoint(mergebb);
//			this.builder.setCurrentBB(mergebb);
//		} else {
//			this.builder.createLOADp3(e);
//			this.depth++;
//			e.get(0).visit(this);
//			this.depth--;
//			this.builder.createJUMP(e, bb);
//			this.popFailureJumpPoint(e);
//			this.setInsertPoint(fbb);
//			this.builder.createSTOREflag(e, 0);
//			this.builder.createSTOREp3(e);
//			this.setInsertPoint(mergebb);
//			this.builder.setCurrentBB(mergebb);
//		}
//	}

//	private void writeZeroMoreByteMapCode(ByteMap e, ZEROMORECHARMAP inst) {
//		for (int c = 0; c < 256; c++) {
//			if (e.byteMap[c]) {
//				inst.append(c);
//			}
//		}
//	}
//
//	private void writeZeroMoreByteMapCode(ByteMap e) {
//		ZEROMORECHARMAP inst = (ZEROMORECHARMAP) this.builder.createZEROMORECHARMAP(e);
//		writeZeroMoreByteMapCode(e, inst);
//	}
//
//	private void writeZeroMoreCharsetCode(Choice e) {
//		ZEROMORECHARMAP inst = (ZEROMORECHARMAP) this.builder.createZEROMORECHARMAP(e);
//		for (int i = 0; i < e.size(); i++) {
//			if (e.get(i) instanceof ByteChar) {
//				inst.append(((ByteChar) e.get(i)).byteChar);
//			}
//			else if (e.get(i) instanceof ByteMap) {
//				ByteMap br = (ByteMap) e.get(i);
//				writeZeroMoreByteMapCode(br, inst);
//			}
//			else {
//				System.out.println("Error: Not Char Content in Charset");
//			}
//		}
//	}
//
//	private boolean optimizeRepetition(Repetition e) {
//		Expression inner = e.get(0);
//		if (inner instanceof NonTerminal) {
//			inner = getNonTerminalRule(inner);
//		}
//		if (inner instanceof ByteMap) {
//			writeZeroMoreByteMapCode((ByteMap) inner);
//			return true;
//		}
//		if (inner instanceof Choice) {
//			if (checkCharMap((Choice) inner)) {
//				writeZeroMoreCharsetCode((Choice) inner);
//				return true;
//			}
//		}
//		return false;
//	}

//	MemoPoint issueMemoPoint(String label, Expression e) {
//		if (this.PackratParsing) {
//			Integer key = e.getId();
//			assert (e.getId() != 0);
//			MemoPoint m = this.memoMap.get(key);
//			if (m == null) {
//				m = new MemoPoint(this.memoMap.size(), label, e, false);
//				this.memoMap.put(key, m);
//			}
//			return m;
//		}
//		return null;
//	}

	public void visitRule(Rule e) {
		this.func = new Function(this.module, e.getLocalName());
		this.builder.setCurrentBB(new BasicBlock(this.func));
		BasicBlock fbb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		e.getExpression().visit(this);
		this.popFailureJumpPoint(e);
		this.setInsertPoint(fbb);
		this.builder.createRET(e.getExpression());
	}

	int ruleSize;

	public void visitNonTerminal(NonTerminal e) {
		if (this.option.useInlining) {
			RuleReferences r = this.analyzer.ruleAnalysisMap.get(e.getLocalName());
			BasicBlock currentBB = this.builder.getCurrentBB();
			if (r != null) {
				if (r.rc == 1) {
					this.inliningMap.put(e.getLocalName(), true);
					Expression ne = getNonTerminalRule(e);
					this.builder.setCurrentBB(new BasicBlock(this.func));
					ne.visit(this);
					return;
				}
			}
			System.out.println("inlining miss: " + e.ruleName);
			BasicBlock rbb = new BasicBlock();
			this.builder.setCurrentBB(currentBB);
			this.builder.createCALL(e, e.ruleName);
			this.setInsertPoint(rbb);
			this.builder.createIFFAIL(e, this.jumpFailureJump());
			BasicBlock bb = new BasicBlock(this.func);
			this.builder.setCurrentBB(bb);
		}
		else {
			BasicBlock rbb = new BasicBlock();
//			Rule r = e.getRule();
//			if (this.PackratParsing) {
//				if (this.PatternMatching || r.isPurePEG()) {
//					Expression ref = Factory.resolveNonTerminal(r.getExpression());
//					MemoPoint m = this.issueMemoPoint(r.getUniqueName(), ref);
//					if (m != null) {
//						this.builder.createLOOKUP(e, rbb, m.id);
//					}
//					this.builder.createCALL(e, e.ruleName);
//					this.builder.createMEMOIZE(e, m.id);
//					this.setInsertPoint(rbb);
//					this.builder.createIFFAIL(e, this.jumpFailureJump());
//					BasicBlock bb = new BasicBlock(this.func);
//					this.builder.setCurrentBB(bb);
//					return;
//				}
//			}
			this.builder.createCALL(e, e.ruleName);
			this.setInsertPoint(rbb);
			this.builder.createIFFAIL(e, this.jumpFailureJump());
			BasicBlock bb = new BasicBlock(this.func);
			this.builder.setCurrentBB(bb);
		}
	}

	public void visitEmpty(Empty e) {
	}

	public void visitFailure(Failure e) {
		this.builder.createSTOREflag(e, 1);
	}

	public void visitByteChar(ByteChar e) {
		this.builder.createCHAR(e, this.jumpFailureJump(), e.byteChar);
	}

	public void visitByteMap(ByteMap e) {
		if (this.option.useFusionInstruction) {
//			CHARMAP inst = (CHARMAP) this.builder.createCHARMAP(e, this.jumpFailureJump());
//			for (int c = 0; c < 256; c++) {
//				if (e.byteMap[c]) {
//					inst.append(c);
//				}
//			}
		}
		else {
			BasicBlock fbb = null;
			BasicBlock endbb = new BasicBlock();
			BasicBlock mergebb = new BasicBlock();
			this.builder.createPUSHpos(e);
			int max = 0;
			for (int i = 0; i < 256; i++) {
				if (e.byteMap[i]) {
					max = i;
				}
			}
			for (int i = 0; i <= max; i++) {
				if (e.byteMap[i]) {
					fbb = new BasicBlock();
					this.builder.createCHAR(e, fbb, i);
					this.builder.createJUMP(e, endbb);
					this.setInsertPoint(fbb);
					if (i != max) {
						this.builder.createSTOREflag(e, 0);
						this.builder.createGETpos(e);
					}
					else {
						this.builder.createSTOREpos(e);
					}
					this.builder.setCurrentBB(fbb);
				}
			}
			this.builder.createJUMP(e, this.jumpFailureJump());
			this.setInsertPoint(endbb);
			this.builder.createPOPpos(e);
			this.setInsertPoint(mergebb);
		}
	}

	public void visitAnyChar(AnyChar e) {
		this.builder.createANY(e, this.jumpFailureJump());
	}

	public void visitNot(Not e) {
		if (this.option.useFusionInstruction) {
//			if (!optimizeNot(e)) {
//				if (this.option.useStackCaching && checkSC(e.get(0))) {
//					writeSCNotCode(e);
//					return;
//				}
//				writeNotCode(e);
//			}
		}
//		else if (this.option.useStackCaching && checkSC(e.get(0))) {
//			writeSCNotCode(e);
//			return;
//		}
		else {
			writeNotCode(e);
		}
	}

	public void visitAnd(And e) {
		if (this.option.useFlowAnalysis) {
			if (this.analyzer.flowAnalysisMap.get(e).equals(ExprFlow.ChoiceAnd)) {
				BasicBlock fbb = new BasicBlock();
				this.pushFailureJumpPoint(fbb);
				this.builder.createPUSHpos(e);
				e.get(0).visit(this);
				this.popFailureJumpPoint(e);
				this.setInsertPoint(fbb);
				this.builder.createGETpos(e);
				this.builder.createIFFAIL(e, this.jumpFailureJump());
				this.builder.setCurrentBB(fbb);
				return;
			}
		}
		BasicBlock fbb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.builder.createPUSHpos(e);
		e.get(0).visit(this);
		this.popFailureJumpPoint(e);
		this.setInsertPoint(fbb);
		this.builder.createSTOREpos(e);
		this.builder.createIFFAIL(e, this.jumpFailureJump());
		this.builder.setCurrentBB(fbb);
	}

	public void visitOption(Option e) {
		if (this.option.useFusionInstruction) {
			if (!optimizeOptional(e)) {
//				if (this.option.useStackCaching && checkSC(e.get(0))) {
//					writeSCOptionalCode(e);
//					return;
//				}
				writeOptionalCode(e);
			}
		}
//		else if (this.option.useStackCaching && checkSC(e.get(0))) {
//			writeSCOptionalCode(e);
//			return;
//		}
		else {
			writeOptionalCode(e);
		}
	}

	public void visitRepetition(Repetition e) {
		if (this.option.useFusionInstruction) {
//			if (!optimizeRepetition(e)) {
//				if (this.option.useStackCaching && checkSC(e.get(0))) {
//					writeSCRepetitionCode(e);
//					return;
//				}
//				writeRepetitionCode(e);
//			}
		}
//		else if (this.option.useStackCaching && checkSC(e.get(0))) {
//			writeSCRepetitionCode(e);
//			return;
//		}
		else {
			writeRepetitionCode(e);
		}
	}

	public void visitRepetition1(Repetition1 e) {
		e.get(0).visit(this);
		if (this.option.useFusionInstruction) {
//			if (!optimizeRepetition(e)) {
//				if (this.option.useStackCaching && checkSC(e.get(0))) {
//					writeSCRepetitionCode(e);
//					return;
//				}
//				writeRepetitionCode(e);
//			}
		}
//		else if (this.option.useStackCaching && checkSC(e.get(0))) {
//			writeSCRepetitionCode(e);
//			return;
//		}
		else {
			writeRepetitionCode(e);
		}
	}

	public void visitSequence(Sequence e) {
		for (int i = 0; i < e.size(); i++) {
			if (this.option.useFusionInstruction) {
//				i = writeSequenceCode(e, i, e.size());
			}
			else {
				e.get(i).visit(this);
			}
		}
	}

	boolean backTrackFlag = false;
	boolean lastChoiceElementIsUnary = false;

	public void visitChoice(Choice e) {
		if (this.option.useMappedChoice && optChoiceMode) {
//			this.optimizeChoice(e);
		}
		if (this.option.useFlowAnalysis) {
			if (this.analyzer.flowAnalysisMap.get(e).equals(ExprFlow.UnaryChoice)) {
				if (this.option.useFusionInstruction) {
					boolean backTrackFlag = this.backTrackFlag = false;
					BasicBlock bb = null;
					BasicBlock fbb = null;
					BasicBlock endbb = new BasicBlock();
					for (int i = 0; i < e.size(); i++) {
						Expression inner = e.get(i);
						if (this.analyzer.flowAnalysisMap.get(inner).equals(ExprFlow.Default)) {
//							i = checkWriteChoiceCharset(e, i, bb, fbb, true);
							backTrackFlag = this.backTrackFlag;
							if (backTrackFlag) {
								fbb = this.currentFailBB;
								this.builder.createJUMP(e, endbb);
								this.popFailureJumpPoint(inner);
								this.setInsertPoint(fbb);
								if (i != e.size() - 1) {
									this.builder.createSTOREflag(e, 0);
									this.builder.createGETpos(e);
								}
								this.builder.setCurrentBB(fbb);
							}
						}
						else {
							fbb = new BasicBlock();
							this.pushFailureJumpPoint(fbb);
							inner.visit(this);
							if (i != e.size() - 1) {
								this.builder.createJUMP(e, endbb);
							}
							this.popFailureJumpPoint(inner);
							this.setInsertPoint(fbb);
						}
					}
					if (backTrackFlag) {
						this.builder.createJUMP(e, this.jumpFailureJump());
						this.setInsertPoint(endbb);
					}
					this.backTrackFlag = false;
				}
				else {
					BasicBlock fbb = null;
					BasicBlock endbb = new BasicBlock();
					for (int i = 0; i < e.size(); i++) {
						Expression inner = e.get(i);
						if (this.analyzer.flowAnalysisMap.get(inner).equals(ExprFlow.Default)) {
							fbb = new BasicBlock();
							this.pushFailureJumpPoint(fbb);
							e.get(i).visit(this);
							this.builder.createJUMP(e, endbb);
							this.popFailureJumpPoint(e.get(i));
							this.setInsertPoint(fbb);
							if (i != e.size() - 1) {
								this.builder.createSTOREflag(e, 0);
								this.builder.createGETpos(e);
							}
							this.builder.setCurrentBB(fbb);
						}
						else {
							fbb = new BasicBlock();
							this.pushFailureJumpPoint(fbb);
							inner.visit(this);
							if (i != e.size() - 1) {
								this.builder.createJUMP(e, endbb);
							}
							this.popFailureJumpPoint(inner);
							this.setInsertPoint(fbb);
						}
					}
					this.builder.createJUMP(e, this.jumpFailureJump());
					this.setInsertPoint(endbb);
				}
			}
			else if (this.option.useFusionInstruction) {
//				if (this.analyzer.flowAnalysisMap.get(e).equals(ExprFlow.CharClassChoice)) {
				if (this.analyzer.checkCharset(e)) {
//					writeCharsetCode(e, 0, e.size());
					return;
				}
				boolean backTrackFlag = this.backTrackFlag = false;
				BasicBlock bb = null;
				BasicBlock fbb = null;
				BasicBlock endbb = new BasicBlock();
				BasicBlock mergebb = new BasicBlock();
				this.builder.createPUSHpos(e);
				for (int i = 0; i < e.size(); i++) {
					Expression inner = e.get(i);
					if (this.analyzer.flowAnalysisMap.get(inner).equals(ExprFlow.Default)) {
//						i = checkWriteChoiceCharset(e, i, bb, fbb, false);
						backTrackFlag = this.backTrackFlag;
						if (backTrackFlag) {
							fbb = this.currentFailBB;
							this.builder.createJUMP(e, endbb);
							this.popFailureJumpPoint(inner);
							this.setInsertPoint(fbb);
							if (i != e.size() - 1) {
								this.builder.createSTOREflag(e, 0);
								this.builder.createGETpos(e);
							}
							else {
								this.builder.createSTOREpos(e);
							}
							this.builder.setCurrentBB(fbb);
						}
					}
					else {
						fbb = new BasicBlock();
						if (i == e.size() - 1) {
							this.lastChoiceElementIsUnary = true;
						}
						else {
							this.pushFailureJumpPoint(fbb);
						}
						inner.visit(this);
						if (i != e.size() - 1) {
							this.builder.createJUMP(e, endbb);
							this.popFailureJumpPoint(inner);
							this.setInsertPoint(fbb);
						}
						this.popFailureJumpPoint(inner);
						this.setInsertPoint(fbb);
					}
				}
				if (backTrackFlag) {
					if (!this.lastChoiceElementIsUnary) {
						this.builder.createJUMP(e, this.jumpFailureJump());
					}
					else {
						this.lastChoiceElementIsUnary = false;
					}
					this.setInsertPoint(endbb);
					this.builder.createPOPpos(e);
					this.setInsertPoint(mergebb);
				}
				this.backTrackFlag = false;
			}
			else {
				BasicBlock fbb = null;
				BasicBlock endbb = new BasicBlock();
				BasicBlock mergebb = new BasicBlock();
				this.builder.createPUSHpos(e);
				for (int i = 0; i < e.size(); i++) {
					Expression inner = e.get(i);
					if (this.analyzer.flowAnalysisMap.get(inner).equals(ExprFlow.Default)) {
						fbb = new BasicBlock();
						this.pushFailureJumpPoint(fbb);
						e.get(i).visit(this);
						this.builder.createJUMP(e, endbb);
						this.popFailureJumpPoint(e.get(i));
						this.setInsertPoint(fbb);
						if (i != e.size() - 1) {
							this.builder.createSTOREflag(e, 0);
							this.builder.createGETpos(e);
						}
						else {
							this.builder.createSTOREpos(e);
						}
						this.builder.setCurrentBB(fbb);
					}
					else {
						fbb = new BasicBlock();
						if (i == e.size() - 1) {
							this.lastChoiceElementIsUnary = true;
						}
						else {
							this.pushFailureJumpPoint(fbb);
						}
						inner.visit(this);
						if (i != e.size() - 1) {
							this.builder.createJUMP(e, endbb);
							this.popFailureJumpPoint(inner);
							this.setInsertPoint(fbb);
						}
					}
				}
				if (!this.lastChoiceElementIsUnary) {
					this.builder.createJUMP(e, this.jumpFailureJump());
				}
				else {
					this.lastChoiceElementIsUnary = false;
				}
				this.setInsertPoint(endbb);
				this.builder.createPOPpos(e);
				this.setInsertPoint(mergebb);
			}
		}
		else {
			if (this.option.useFusionInstruction) {
//				if (this.analyzer.flowAnalysisMap.get(e).equals(ExprFlow.CharClassChoice)) {
				if (this.analyzer.checkCharset(e)) {
//					writeCharsetCode(e, 0, e.size());
					return;
				}
				boolean backTrackFlag = this.backTrackFlag = false;
				BasicBlock bb = null;
				BasicBlock fbb = null;
				BasicBlock endbb = new BasicBlock();
				BasicBlock mergebb = new BasicBlock();
				this.builder.createPUSHpos(e);
				for (int i = 0; i < e.size(); i++) {
					Expression inner = e.get(i);
//					i = checkWriteChoiceCharset(e, i, bb, fbb, false);
					backTrackFlag = this.backTrackFlag;
					if (backTrackFlag) {
						fbb = this.currentFailBB;
						this.builder.createJUMP(e, endbb);
						this.popFailureJumpPoint(inner);
						this.setInsertPoint(fbb);
						if (i != e.size() - 1) {
							this.builder.createSTOREflag(e, 0);
							this.builder.createGETpos(e);
						}
						else {
							this.builder.createSTOREpos(e);
						}
						this.builder.setCurrentBB(fbb);
					}
				}
				if (backTrackFlag) {
					this.builder.createJUMP(e, this.jumpFailureJump());
					this.setInsertPoint(endbb);
					this.builder.createPOPpos(e);
					this.setInsertPoint(mergebb);
				}
				this.backTrackFlag = false;
			}
			else {
				BasicBlock fbb = null;
				BasicBlock endbb = new BasicBlock();
				BasicBlock mergebb = new BasicBlock();
				this.builder.createPUSHpos(e);
				for (int i = 0; i < e.size(); i++) {
					fbb = new BasicBlock();
					this.pushFailureJumpPoint(fbb);
					e.get(i).visit(this);
					this.builder.createJUMP(e, endbb);
					this.popFailureJumpPoint(e.get(i));
					this.setInsertPoint(fbb);
					if (i != e.size() - 1) {
						this.builder.createSTOREflag(e, 0);
						this.builder.createGETpos(e);
					}
					else {
						this.builder.createSTOREpos(e);
					}
					this.builder.setCurrentBB(fbb);
				}
				this.builder.createJUMP(e, this.jumpFailureJump());
				this.setInsertPoint(endbb);
				this.builder.createPOPpos(e);
				this.setInsertPoint(mergebb);
			}
		}
	}

	public void visitNew(New e) {
//		if (PatternMatching) {
//			for (int i = 0; i < e.size(); i++) {
//				if (this.option.useFusionInstruction) {
////					i = writeSequenceCode(e, i, e.size());
//				}
//				else {
//					e.get(i).visit(this);
//				}
//			}
//		}
//		else {
//			BasicBlock fbb = new BasicBlock();
//			this.pushFailureJumpPoint(fbb);
//			if (e.lefted) {
//				this.builder.createLEFTJOIN(e, 0);
//			}
//			else {
//				this.builder.createNEW(e);
//			}
//		}
	}

	public void visitCapture(Capture e) {
//		if (!PatternMatching) {
//			BasicBlock mergebb = new BasicBlock();
//			this.builder.createCAPTURE(e);
//			this.builder.createPOPpos(e);
//			this.builder.createJUMP(e, mergebb);
//			BasicBlock fbb = this.popFailureJumpPoint(e);
//			this.setInsertPoint(fbb);
//			this.builder.createABORT(e);
//			this.builder.createJUMP(e, this.jumpFailureJump());
//			this.setInsertPoint(mergebb);
//			this.builder.setCurrentBB(mergebb);
//		}
	}

	public void visitLink(Link e) {
//		if (PatternMatching) {
//			e.get(0).visit(this);
//		}
////		else if (this.PackratParsing) {
////			BasicBlock fbb = new BasicBlock();
////			BasicBlock mergebb = new BasicBlock();
////			this.pushFailureJumpPoint(fbb);
////			Expression ref = Factory.resolveNonTerminal(e.get(0));
////			MemoPoint m = this.issueMemoPoint(e.toString(), ref);
////			if (m != null) {
////				this.builder.createLOOKUPNODE(e, mergebb, m.id, e.index);
////			}
////			this.builder.createPUSHmark(e);
////			e.get(0).visit(this);
////			this.builder.createCOMMIT(e, e.index);
////			if (m != null) {
////				this.builder.createMEMOIZENODE(e, m.id);
////			}
////			this.builder.createJUMP(e, mergebb);
////			this.popFailureJumpPoint(e);
////			this.setInsertPoint(fbb);
////			this.builder.createABORT(e);
////			if (m != null) {
////				this.builder.createMEMOIZENODE(e, m.id);
////			}
////			this.builder.createJUMP(e, this.jumpFailureJump());
////			this.setInsertPoint(mergebb);
////			this.builder.setCurrentBB(mergebb);
////		}
//		else {
//			BasicBlock fbb = new BasicBlock();
//			BasicBlock mergebb = new BasicBlock();
//			this.pushFailureJumpPoint(fbb);
//			this.builder.createPUSHmark(e);
//			e.get(0).visit(this);
//			this.builder.createCOMMIT(e, e.index);
//			this.builder.createJUMP(e, mergebb);
//			this.popFailureJumpPoint(e);
//			this.setInsertPoint(fbb);
//			this.builder.createABORT(e);
//			this.builder.createJUMP(e, this.jumpFailureJump());
//			this.setInsertPoint(mergebb);
//			this.builder.setCurrentBB(mergebb);
//		}
	}

	public void visitTagging(Tagging e) {
//		if (!this.PatternMatching) {
//			this.builder.createTAG(e, "#" + e.tag.toString());
//		}
	}

	public void visitReplace(Replace e) {
//		if (!this.PatternMatching) {
//			this.builder.createVALUE(e, e.value);
//		}
	}

	@Override
	public void visitExpression(Expression e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}
}
