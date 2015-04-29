package nez.x.parsergenerator;

import nez.Grammar;
import nez.expr.And;
import nez.expr.AnyChar;
import nez.expr.ByteChar;
import nez.expr.ByteMap;
import nez.expr.Capture;
import nez.expr.Choice;
import nez.expr.Empty;
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

public abstract class ParserGenerator extends GrammarVisitor {
	final protected FileBuilder file;
	final protected Grammar peg;

	public ParserGenerator(String fileName, Grammar peg) {
		this.file = new FileBuilder(fileName);
		this.peg = peg;
	}

	public abstract void generate();

	public abstract String getDesc();

	public abstract void makeHeader();

	public abstract void makeFooter();

	public abstract void visitRule(Rule e);

	public abstract void visitNonTerminal(NonTerminal e);

	public abstract void visitEmpty(Empty e);

	public abstract void visitFailure(Failure e);

	public abstract void visitByteChar(ByteChar e);

	public abstract void visitByteMap(ByteMap e);

	public abstract void visitAnyChar(AnyChar e);

	public abstract void visitNot(Not e);

	public abstract void visitAnd(And e);

	public abstract void visitOption(Option e);

	public abstract void visitRepetition(Repetition e);

	public abstract void visitRepetition1(Repetition1 e);

	public abstract void visitSequence(Sequence e);

	public abstract void visitChoice(Choice e);

	public abstract void visitNew(New e);

	public abstract void visitCapture(Capture e);

	public abstract void visitLink(Link e);

	public abstract void visitTagging(Tagging e);

	public abstract void visitReplace(Replace e);
}
