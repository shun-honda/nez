package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.vm.Compiler;
import nez.vm.Instruction;

public class DefIndent extends Unconsumed {
	DefIndent(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "defindent";
	}
	@Override
	public boolean match(SourceContext context) {
		String indent = context.getIndentText(context.getPosition());
		context.pushSymbolTable(NezTag.Indent, indent);
		return true;
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeDefIndent(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		String token = gep.addIndent();
		sb.append(token);
	}

}