package nez.vm;

import java.util.HashMap;

import nez.SourceContext;
import nez.ast.Tag;
import nez.expr.ByteChar;
import nez.expr.ByteMap;
import nez.expr.DefIndent;
import nez.expr.DefSymbol;
import nez.expr.Expression;
import nez.expr.IsIndent;
import nez.expr.IsSymbol;
import nez.expr.Link;
import nez.expr.Replace;
import nez.expr.Rule;
import nez.expr.Sequence;
import nez.expr.Tagging;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public abstract class Instruction {
	protected Expression  e;
	public Instruction next;
	public int id;
	public boolean label = false;
	public Instruction(Expression e, Instruction next) {
		this.e = e;
		this.id = -1;
		this.next = next;
	}
	
	Instruction branch() {
		return null;
	}
	
	protected static Instruction labeling(Instruction inst) {
		if(inst != null) {
			inst.label = true;
		}
		return inst;
	}
	
	protected static String label(Instruction inst) {
		return "L"+inst.id;
	}

	public final String getName() {
		return this.getClass().getSimpleName();
	}

	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName());
	}
	
	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		stringfy(sb);
		return sb.toString();
	}
	
	boolean debug() {
		return false;
	}
	
	abstract Instruction exec(Context sc) throws TerminationException;
		
	public static boolean run(Instruction code, SourceContext sc) {
		boolean result = false;
		try {
			while(true) {
				code = code.exec(sc);
			}
		}
		catch (TerminationException e) {
			result = e.status;
		}
		return result;
	}

	public static boolean debug(Instruction code, SourceContext sc) {
		boolean result = false;
		String u = "Start";
		UList<String> stack = new UList<String>(new String[128]);
		stack.add("Start");
		try {
			while(true) {
				if(code instanceof ICallPush) {
					stack.add(u);
					u = ((ICallPush)code).rule.getLocalName();
				}
				if(code instanceof IRet) {
					u = stack.ArrayValues[stack.size()-1];
					stack.clear(stack.size()-1);
				}
				ConsoleUtils.println(u + "(" + sc.getPosition() + ")  " + code.id + " " + code);
				code = code.exec(sc);
			}
		}
		catch (TerminationException e) {
			result = e.status;
		}
		return result;
	}
	
	static void makeList(Instruction inst, UList<Instruction> l, HashMap<Integer, Instruction> m) {
		while(inst != null && !m.containsKey(inst.id)) {
			m.put(inst.id, inst);
			l.add(inst);
			if(inst.branch() != null) {
				makeList(inst.next, l, m);
				makeList(inst.branch(), l, m);
				return;
			}
			if(inst instanceof ICallPush) {
				makeList(((ICallPush) inst).jump, l, m);
			}
			inst = inst.next;
		}
	}

}

interface StackOperation {

}

class IFail extends Instruction implements StackOperation {
	IFail(Expression e) {
		super(e, null);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIFail();
	}
}

class IFailPush extends Instruction implements StackOperation {
	public final Instruction failjump;
	IFailPush(Expression e, Instruction failjump, Instruction next) {
		super(e, next);
		this.failjump = labeling(failjump);
	}
	@Override
	Instruction branch() {
		return this.failjump;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIFailPush(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		super.stringfy(sb);
		sb.append(" ");
		sb.append(label(this.failjump));
		sb.append("  ## " + e);
	}
}

class IFailPop extends Instruction implements StackOperation {
	IFailPop(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIFailPop(this);
	}
}

class IFailSkip extends Instruction {
	IFailSkip(Expression e) {
		super(e, null);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIFailSkip(this);
	}
}

/*
 * IFailCheckSkip
 * Check unconsumed repetition
 */

class IFailCheckSkip extends IFailSkip {
	IFailCheckSkip(Expression e) {
		super(e);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIFailSkip_(this);
	}
}

class ICallPush extends Instruction implements StackOperation {
	Rule rule;
	public Instruction jump = null;
	ICallPush(Rule rule, Instruction next) {
		super(rule, next);
		this.rule = rule;
	}
	void setResolvedJump(Instruction jump) {
		assert(this.jump == null);
		this.jump = labeling(this.next);
		this.next = labeling(jump);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opICallPush(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		super.stringfy(sb);
		sb.append(" " + label(jump) + "   ## " + rule.getLocalName());
	}
}

class IRet extends Instruction implements StackOperation {
	IRet(Rule e) {
		super(e, null);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIRet();
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		super.stringfy(sb);
		sb.append("  ## " + ((Rule)e).getLocalName());
	}
}

class IPosPush extends Instruction {
	IPosPush(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIPosPush(this);
	}
}

class IPosBack extends Instruction {
	public IPosBack(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIPopBack(this);
	}
}

class IExit extends Instruction {
	boolean status;
	IExit(boolean status) {
		super(null, null);
		this.status = status;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		throw new TerminationException(status);
	}
}

class IAnyChar extends Instruction {
	IAnyChar(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIAnyChar(this);
	}
}

class IByteChar extends Instruction {
	public final boolean optional;
	public final int byteChar;
	IByteChar(ByteChar e, boolean optional, Instruction next) {
		super(e, next);
		this.byteChar = e.byteChar;
		this.optional = optional;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIByteChar(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		super.stringfy(sb);
		sb.append(" ");
		sb.append(StringUtils.stringfyByte(byteChar));
	}
}

class IByteMap extends Instruction {
	public final boolean optional;
	public final boolean[] byteMap;
	IByteMap(ByteMap e, boolean optional, Instruction next) {
		super(e, next);
		this.byteMap = e.charMap;
		this.optional = optional;
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIByteMap(this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		super.stringfy(sb);
		sb.append(" ");
		sb.append(StringUtils.stringfyByteMap(byteMap));
	}
}

interface Construction {
	
}

class INodePush extends Instruction {
	INodePush(Link e, Instruction next) {
		super(e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodePush(this);
	}
}

class INodeStore extends Instruction {
	public final int index;
	INodeStore(Link e, Instruction next) {
		super(e, next);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodeStore(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		super.stringfy(sb);
		sb.append(" " + index);
	}
}

class INew extends Instruction {
	int shift;
	INew(Expression e, Instruction next) {
		super(e, next);
		this.shift = 0;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opINew(this);
	}
}

class ILeftNew extends Instruction {
	int shift;
	ILeftNew(Expression e, Instruction next) {
		super(e, next);
		this.shift = 0;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opILeftNew(this);
	}
}


class ICapture extends Instruction {
	int shift;
	ICapture(Expression e, Instruction next) {
		super(e, next);
		this.shift = 0;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opICapture(this);
	}
}

class IReplace extends Instruction {
	public final String value;
	IReplace(Replace e, Instruction next) {
		super(e, next);
		this.value = e.value;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIReplace(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		super.stringfy(sb);
		sb.append(" " + StringUtils.quoteString('"', value, '"'));
	}
}

class ITag extends Instruction {
	public final Tag tag;
	ITag(Tagging e, Instruction next) {
		super(e, next);
		this.tag = e.tag;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opITag(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		super.stringfy(sb);
		sb.append(" " + StringUtils.quoteString('"', tag.name, '"'));
	}
}

interface Memoization {

}

class ILookup extends IFailPush implements Memoization {
	final MemoPoint memoPoint;
	final Instruction skip;
	ILookup(Expression e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, failjump, next);
		this.memoPoint = m;
		this.skip = labeling(skip);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		super.stringfy(sb);
		sb.append(" " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opILookup(this);
	}
}

class IStateLookup extends ILookup {
	IStateLookup(Expression e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, m, next, skip, failjump);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIStateLookup(this);
	}
}

class IMemoize extends Instruction implements Memoization {
	final MemoPoint memoPoint;
	IMemoize(Expression e, MemoPoint m, Instruction next) {
		super(e, next);
		this.memoPoint = m;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName() + " " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIMemoize(this);
	}
}

class IStateMemoize extends IMemoize {
	IStateMemoize(Expression e, MemoPoint m, Instruction next) {
		super(e, m, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIStateMemoize(this);
	}
}

class IMemoizeFail extends IFail implements Memoization {
	MemoPoint memoPoint;
	IMemoizeFail(Expression e, MemoPoint m) {
		super(e);
		this.memoPoint = m;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName() + " " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIMemoizeFail(this);
	}
}

class IStateMemoizeFail extends IMemoizeFail {
	IStateMemoizeFail(Expression e, MemoPoint m) {
		super(e, m);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIStateMemoizeFail(this);
	}
}

class ILookupNode extends ILookup {
	final int index;
	ILookupNode(Link e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, m, next, skip, failjump);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opLookupNode(this);
	}
}

class LookupNode2 extends ILookupNode {
	final int index;
	LookupNode2(Link e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(e, m, next, skip, failjump);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opLookupNode2(this);
	}
}

class MemoizeNode extends INodeStore implements Memoization {
	final MemoPoint memoPoint;
	MemoizeNode(Link e, MemoPoint m, Instruction next) {
		super(e, next);
		this.memoPoint = m;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName() + " " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoizeNode(this);
	}
}

class MemoizeNode2 extends MemoizeNode {
	MemoizeNode2(Link e, MemoPoint m, Instruction next) {
		super(e, m, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoizeNode2(this);
	}
}

/* Symbol */

class IDefSymbol extends Instruction {
	Tag tableName;
	IDefSymbol(DefSymbol e, Instruction next) {
		super(e, next);
		this.tableName = e.table;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("def ");
		sb.append(tableName.name);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIDefSymbol(this);
	}
}

class IIsSymbol extends Instruction {
	Tag tableName;
	boolean checkLastSymbolOnly;
	IIsSymbol(IsSymbol e, boolean checkLastSymbolOnly, Instruction next) {
		super(e, next);
		this.tableName = e.table;
		this.checkLastSymbolOnly = checkLastSymbolOnly;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("is ");
		sb.append(tableName.name);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIIsSymbol(this);
	}
}

class IDefIndent extends Instruction {
	IDefIndent(DefIndent e, Instruction next) {
		super(e, next);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("defindent");
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIDefIndent(this);
	}
}

class IIsIndent extends Instruction {
	IIsIndent(IsIndent e, Instruction next) {
		super(e, next);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("indent");
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opIIsIndent(this);
	}
}

class ITablePush extends Instruction {
	ITablePush(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("tablepush");
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opITablePush(this);
	}
}

class ITablePop extends Instruction {
	public ITablePop(Expression e, Instruction next) {
		super(e, next);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("tablepop");
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opITablePop(this);
	}
}


/* Specialization */

class NByteMap extends Instruction {
	public final boolean[] byteMap;
	NByteMap(ByteMap e, Instruction next) {
		super(e, next);
		this.byteMap = e.charMap;
	}
	NByteMap(ByteChar e, Instruction next) {
		super(e, next);
		this.byteMap = ByteMap.newMap();
		this.byteMap[e.byteChar] = true;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNByteMap(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("not ");
		sb.append(StringUtils.stringfyByteMap(byteMap));
	}
}

class RByteMap extends Instruction {
	public final boolean[] byteMap;
	RByteMap(ByteMap e, Instruction next) {
		super(e, next);
		this.byteMap = e.charMap;
	}
	RByteMap(ByteChar e, Instruction next) {
		super(e, next);
		this.byteMap = ByteMap.newMap();
		this.byteMap[e.byteChar] = true;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opRByteMap(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("repeat ");
		sb.append(StringUtils.stringfyByteMap(byteMap));
	}
}


class IMultiChar extends Instruction {
	boolean optional = false;
	final byte[] utf8;
	final int    len;
	public IMultiChar(Sequence e, boolean optional, Instruction next) {
		super(e, next);
		this.utf8 = e.extractMultiChar(0, e.size());
		this.len = this.utf8.length;
		this.optional = optional;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMultiChar(this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName());
		for(int i = 0; i < utf8.length; i++) {
			sb.append(" ");
			sb.append(StringUtils.stringfyByte(utf8[i] & 0xff));
		}
	}
}

class NMultiChar extends IMultiChar {
	NMultiChar(Sequence e, Instruction next) {
		super(e, false, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNMultiChar(this);
	}
}
