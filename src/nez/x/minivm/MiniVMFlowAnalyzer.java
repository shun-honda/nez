package nez.x.minivm;

import java.util.HashMap;
import java.util.Stack;

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

public class MiniVMFlowAnalyzer extends GrammarVisitor {

	HashMap<String, RuleReferences> ruleAnalysisMap = new HashMap<String, RuleReferences>();
	HashMap<Expression, ExprFlow> flowAnalysisMap = new HashMap<Expression, ExprFlow>();
	Stack<Expression> exprTraceStack = new Stack<Expression>();

	public void analyze(Grammar peg) {
		for (Rule r : peg.getRuleList()) {
			if (!r.getLocalName().startsWith("\"")) {
				this.visitRule(r);
			}
		}
	}

	public Expression push(Expression e) {
		return this.exprTraceStack.push(e);
	}

	public Expression pop() {
		return this.exprTraceStack.pop();
	}

	public Expression peek() {
		if (this.exprTraceStack.size() != 0) {
			return this.exprTraceStack.peek();
		}
		return null;
	}

	public void visitRule(Rule e) {
		e.getExpression().visit(this);
	}

	public void visitNonTerminal(NonTerminal e) {
		String ruleName = e.getLocalName();
		RuleReferences c = this.ruleAnalysisMap.get(ruleName);
		if (c == null) {
			c = new RuleReferences(e.getRule(), 1);
			this.ruleAnalysisMap.put(ruleName, c);
		}
		else {
			c.rc++;
		}
	}

	public void visitEmpty(Empty e) {
	}

	public void visitFailure(Failure e) {
	}

	public void visitByteChar(ByteChar e) {
	}

	public void visitByteMap(ByteMap e) {
	}

	public void visitAnyChar(AnyChar e) {
	}

	public void visitNot(Not e) {
		Expression top = this.peek();
		if (top instanceof Choice) {
			this.flowAnalysisMap.put(e, ExprFlow.ChoiceNot);
		}
		else {
			this.flowAnalysisMap.put(e, ExprFlow.Default);
		}
		this.push(e);
		e.get(0).visit(this);
		this.pop();
	}

	public void visitAnd(And e) {
		Expression top = this.peek();
		if (top instanceof Choice) {
			this.flowAnalysisMap.put(e, ExprFlow.ChoiceAnd);
		}
		else {
			this.flowAnalysisMap.put(e, ExprFlow.Default);
		}
		this.push(e);
		e.get(0).visit(this);
		this.pop();
	}

	public void visitOption(Option e) {
		Expression top = this.peek();
		if (top instanceof Choice) {
			this.flowAnalysisMap.put(e, ExprFlow.ChoiceOption);
		}
		else {
			this.flowAnalysisMap.put(e, ExprFlow.Default);
		}
		this.push(e);
		e.get(0).visit(this);
		this.pop();
	}

	public void visitRepetition(Repetition e) {
		Expression top = this.peek();
		if (top instanceof Choice) {
			this.flowAnalysisMap.put(e, ExprFlow.ChoiceRepetition);
		}
		else {
			this.flowAnalysisMap.put(e, ExprFlow.Default);
		}
		this.push(e);
		e.get(0).visit(this);
		this.pop();
	}

	public void visitRepetition1(Repetition1 e) {
		this.push(e);
		e.get(0).visit(this);
		this.pop();
	}

	public void visitSequence(Sequence e) {
		this.push(e);
		for (int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
		this.pop();
	}

	public void visitChoice(Choice e) {
		Expression top = this.peek();
		if (top != null) {
			if (top instanceof Unary && !(top instanceof Link || top instanceof Repetition1)) {
				this.flowAnalysisMap.put(e, ExprFlow.UnaryChoice);
			}
			else {
				this.flowAnalysisMap.put(e, ExprFlow.Default);
			}
			this.push(e);
			for (int i = 0; i < e.size(); i++) {
				Expression inner = e.get(i);
				if (i < e.size() - 1 && inner instanceof Unary && !(inner instanceof Link || inner instanceof Repetition1)) {
					throw new RuntimeException("unreachable element is located in the Choice: " + e.getClass());
				}
				this.flowAnalysisMap.put(inner, ExprFlow.Default);
				inner.visit(this);
			}
			this.pop();
		}
		else {
			this.push(e);
			for (int i = 0; i < e.size(); i++) {
				Expression inner = e.get(i);
				this.flowAnalysisMap.put(inner, ExprFlow.Default);
				inner.visit(this);
			}
			this.pop();
		}
	}

	public void visitNew(New e) {
	}

	public void visitCapture(Capture e) {
	}

	public void visitLink(Link e) {
		this.push(e);
		e.get(0).visit(this);
		this.pop();
	}

	public void visitTagging(Tagging e) {
	}

	public void visitReplace(Replace e) {
	}

	@Override
	protected void visitExpression(Expression e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

}

class AnalysisResult {
}

class RuleReferences {
	Rule rule;
	int rc;

	public RuleReferences(Rule rule, int rc) {
		this.rule = rule;
		this.rc = rc;
	}
}

enum ExprFlow {
	UnaryChoice,
	UnaryNot,
	UnaryAnd,
	UnaryOption,
	UnaryRepetition,
	ChoiceNot,
	ChoiceAnd,
	ChoiceOption,
	ChoiceRepetition,
	Default
}
