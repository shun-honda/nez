package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.StringUtils;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.MatchByteMap;
import nez.vm.Compiler;

public class ByteMap extends Terminal {
	public boolean[] charMap; // Immutable
	ByteMap(SourcePosition s, int beginChar, int endChar) {
		super(s);
		this.charMap = newMap();
		appendRange(this.charMap, beginChar, endChar);
	}
	ByteMap(SourcePosition s, boolean[] b) {
		super(s);
		this.charMap = b;
	}
	
	public final static boolean[] newMap() {
		return new boolean[257];
	}

	public final static void appendRange(boolean[] b, int beginChar, int endChar) {
		for(int c = beginChar; c <= endChar; c++) {
			b[c] = true;
		}
	}

	@Override
	public String getPredicate() {
		return "byte " + StringUtils.stringfyByteMap(this.charMap);
	}
	@Override
	public String getInterningKey() { 
		return "[" +  StringUtils.stringfyByteMap(this.charMap);
	}
	
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public short acceptByte(int ch) {
		return (charMap[ch]) ? Accept : Reject;
	}
	@Override
	public boolean match(SourceContext context) {
		if(this.charMap[context.byteAt(context.pos)]) {
			context.consume(1);
			return true;
		}
		return context.failure2(this);
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return new MatchByteMap(bc, this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		int c = 0;
		for(boolean b: this.charMap) {
			if(b) {
				c += 1;
			}
		}
		return c;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		int c = 0;
		for(int ch = 0; ch < 127; ch++) {
			if(this.charMap[ch]) {
				c += 1;
			}
			if(c == p) {
				sb.append((char)ch);
			}
		}
	}

}