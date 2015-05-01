package nez.x.parsergenerator;

import java.util.ArrayList;

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
import nez.util.FileBuilder;

public class PrototypeDeclGenerator extends GrammarVisitor {

	FileBuilder file;
	boolean PatternMatch = true;
	ArrayList<Integer> funcList = new ArrayList<Integer>();

	public PrototypeDeclGenerator(FileBuilder file) {
		this.file = file;
	}

	public void generate(Grammar peg) {
		for (Rule r : peg.getRuleList()) {
			if (!r.getLocalName().startsWith("\"")) {
				this.visitRule(r);
			}
		}
	}

	private void nonTerminalFuncDecl(String type, String name, String args) {
		this.file.writeIndent(type + " " + name + "(" + args + ");");
	}

	private void funcDecl(String type, String name, String args, Expression e) {
		if (!this.funcList.contains(e.getId())) {
			this.funcList.add(e.getId());
			this.file.writeIndent(type + " " + name + "(" + args + ");");
		}
	}

	public void visitRule(Rule e) {
		nonTerminalFuncDecl("int", "p" + e.getLocalName(), "ParsingContext ctx");
		e.getExpression().visit(this);
	}

	public void visitNonTerminal(NonTerminal e) {
	}

	public void visitEmpty(Empty e) {
	}

	public void visitFailure(Failure e) {
		funcDecl("int", "pFailure" + e.getId(), "ParsingContext ctx", e);
	}

	public void visitByteChar(ByteChar e) {
		funcDecl("int", "pByteChar" + e.getId(), "ParsingContext ctx", e);
	}

	public void visitByteMap(ByteMap e) {
		funcDecl("int", "pByteMap" + e.getId(), "ParsingContext ctx", e);
	}

	public void visitAnyChar(AnyChar e) {
		funcDecl("int", "pAnyChar" + e.getId(), "ParsingContext ctx", e);
	}

	public void visitNot(Not e) {
		funcDecl("int", "pNot" + e.getId(), "ParsingContext ctx", e);
		e.get(0).visit(this);
	}

	public void visitAnd(And e) {
		funcDecl("int", "pAnd" + e.getId(), "ParsingContext ctx", e);
		e.get(0).visit(this);
	}

	public void visitOption(Option e) {
		funcDecl("int", "pOption" + e.getId(), "ParsingContext ctx", e);
		e.get(0).visit(this);
	}

	public void visitRepetition(Repetition e) {
		funcDecl("int", "pZeroMore" + e.getId(), "ParsingContext ctx", e);
		e.get(0).visit(this);
	}

	public void visitRepetition1(Repetition1 e) {
		funcDecl("int", "pOneMore" + e.getId(), "ParsingContext ctx", e);
		e.get(0).visit(this);
	}

	public void visitSequence(Sequence e) {
		funcDecl("int", "pSequence" + e.getId(), "ParsingContext ctx", e);
		for (int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}

	public void visitChoice(Choice e) {
		funcDecl("int", "pChoice" + e.getId(), "ParsingContext ctx", e);
		for (int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}

	public void visitNew(New e) {
		if (!PatternMatch) {
			funcDecl("int", "pNew" + e.getId(), "ParsingContext ctx", e);
		}
	}

	public void visitCapture(Capture e) {
		if (!PatternMatch) {
			funcDecl("int", "pCapture" + e.getId(), "ParsingContext ctx", e);
		}
	}

	public void visitLink(Link e) {
		if (!PatternMatch) {
			funcDecl("int", "pLink" + e.getId(), "ParsingContext ctx", e);
			e.get(0).visit(this);
		}
		else {
			e.get(0).visit(this);
		}
	}

	public void visitTagging(Tagging e) {
		if (!PatternMatch) {
			funcDecl("int", "pTagging" + e.getId(), "ParsingContext ctx", e);
		}
	}

	public void visitReplace(Replace e) {
	}

	@Override
	protected void visitExpression(Expression e) {
		// TODO Auto-generated method stub

	}

}
