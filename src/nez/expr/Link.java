package nez.expr;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Compiler;
import nez.vm.Instruction;

public class Link extends Unary {
	public int index;
	Link(SourcePosition s, Expression e, int index) {
		super(s, e);
		this.index = index;
	}
	@Override
	public String getPredicate() { 
		return (index != -1) ? "link " + index : "link";
	}
	@Override
	public String getInterningKey() {
		return (index != -1) ? "@" + index : "@";
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newLink(this.s, e, this.index) : this;
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return this.inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.OperationType;
	}
	@Override
	public Expression checkNodeTransition(GrammarChecker checker, NodeTransition c) {
		if(c.required != NodeTransition.OperationType) {
			checker.reportWarning(s, "unexpected @ => removed");
			return this.inner.removeNodeOperator();
		}
		c.required = NodeTransition.ObjectType;
		Expression inn = inner.checkNodeTransition(checker, c);
		if(c.required != NodeTransition.OperationType) {
			checker.reportWarning(s, "no object created at @ => removed");
			c.required = NodeTransition.OperationType;
			return inn;
		}
		c.required = NodeTransition.OperationType;
		this.inner = inn;
		return this;
	}
	@Override
	public Expression removeNodeOperator() {
		return inner.removeNodeOperator();
	}
	@Override
	public short acceptByte(int ch) {
		return inner.acceptByte(ch);
	}
	@Override
	public boolean match(SourceContext context) {
		Node left = context.left;
		int mark = context.startConstruction();
		if(this.inner.matcher.match(context)) {
			if(context.left != left) {
				context.commitConstruction(mark, context.left);
				context.lazyLink(left, this.index, context.left);
			}
			context.left = left;
			left = null;
			return true;
		}
		context.abortConstruction(mark);			
		left = null;
		return false;
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeLink(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		return inner.pattern(gep);
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		this.inner.examplfy(gep, sb, p);
	}

}