package nez.x.parsergenerator;

import java.util.ArrayList;
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

public class SlowCParserGenerator extends ParserGenerator {

	public SlowCParserGenerator(String fileName, Grammar peg) {
		super(fileName, peg);
	}

	@Override
	public String getDesc() {
		return "C";
	}

	ArrayList<Integer> funcList = new ArrayList<Integer>();
	boolean PatternMatch = true;

	@Override
	public void generate() {
		this.makeHeader();
		for (Rule r : peg.getRuleList()) {
			if (!r.getLocalName().startsWith("\"")) {
				this.visitRule(r);
			}
		}
		this.makeFooter();
		this.file.flush();
	}

	@Override
	public void makeHeader() {
		this.file.write("// This file is generated by nez/src/nez/x/parsergenerator/CParserGenerator.java");
		this.file.writeNewLine();
		this.file.writeIndent("#include \"libnez/libnez.h\"");
		this.file.writeIndent("#include <stdio.h>");
		this.file.writeNewLine();
		PrototypeDeclGenerator pg = new PrototypeDeclGenerator(this.file);
		pg.generate(this.peg);
		this.file.writeNewLine();
	}

	@Override
	public void makeFooter() {
		this.file.writeIndent("int main(int argc, char* const argv[])");
		this.openBlock();
		this.file.writeIndent("uint64_t start, end;");
		this.file.writeIndent("ParsingContext ctx = nez_CreateParsingContext(argv[1]);");
		this.file.writeIndent("start = timer();");
		this.file.writeIndent("if(!pFile(ctx))");
		this.openBlock();
		this.file.writeIndent("nez_PrintErrorInfo(\"parse error\");");
		this.closeBlock();
		this.file.writeIndent("else if((ctx->cur - ctx->inputs) != ctx->input_size)");
		this.openBlock();
		this.file.writeIndent("nez_PrintErrorInfo(\"unconsume\");");
		this.closeBlock();
		this.file.writeIndent("else");
		this.openBlock();
		this.file.writeIndent("end = timer();");
//		this.file.writeIndent("ParsingObject po = nez_commitLog(ctx,0);");
//		this.file.writeIndent("dump_pego(&po, ctx->inputs, 0);");
		this.file.writeIndent("fprintf(stderr, \"ErapsedTime: %llu msec\\n\", (unsigned long long)end - start);");
		this.file.writeIndent("fprintf(stderr, \"match\");");
		this.closeBlock();
		this.file.writeIndent("return 0;");
		this.file.writeIndent();
		this.closeBlock();
	}

	int fid = 0;

	class FailurePoint {
		int id;
		FailurePoint prev;

		public FailurePoint(int label, FailurePoint prev) {
			this.id = label;
			this.prev = prev;
		}
	}

	FailurePoint fLabel;

	private void initFalureJumpPoint() {
		this.fid = 0;
		fLabel = null;
	}

	private void pushFailureJumpPoint() {
		this.fLabel = new FailurePoint(this.fid++, this.fLabel);
	}

	private void popFailureJumpPoint(Rule r) {
		this.file.decIndent();
		this.file.writeIndent("CATCH_FAILURE" + this.fLabel.id + ":" + "/* " + r.getLocalName() + " */");
		this.file.incIndent();
		this.fLabel = this.fLabel.prev;
	}

	private void popFailureJumpPoint(Expression e) {
		this.file.decIndent();
		this.file.writeIndent("CATCH_FAILURE" + this.fLabel.id + ":" + "/* " + e.getInterningKey() + " */");
		this.file.incIndent();
		this.fLabel = this.fLabel.prev;
	}

	private void jumpFailureJump() {
		this.file.writeIndent("goto CATCH_FAILURE" + this.fLabel.id + ";");
	}

	private void jumpPrevFailureJump() {
		this.file.writeIndent("goto CATCH_FAILURE" + this.fLabel.prev.id + ";");
	}

	private void nonTerminalFuncDecl(String type, String name, String args) {
		this.file.writeIndent(type + " " + name + "(" + args + ")");
	}

	private boolean funcDecl(String type, String name, String args, Expression e) {
		if (!this.funcList.contains(e.getId())) {
			this.funcList.add(e.getId());
			this.file.writeIndent(type + " " + name + "(" + args + ")");
			return true;
		}
		return false;
	}

	private void openBlock() {
		this.file.write(" {");
		this.file.incIndent();
	}

	private void closeBlock() {
		this.file.decIndent();
		this.file.writeIndent("}");
	}

	private void gotoLabel(String label) {
		this.file.writeIndent("goto " + label + ";");
	}

	private void exitLabel(String label) {
		this.file.decIndent();
		this.file.writeIndent(label + ": ;; /* <- this is required for avoiding empty statement */");
		this.file.incIndent();
	}

	private void let(String type, String var, String expr) {
		if (type != null) {
			this.file.writeIndent(type + " " + var + " = " + expr + ";");
		}
		else {
			this.file.writeIndent("" + var + " = " + expr + ";");
		}
	}

	private void consume() {
		this.file.writeIndent("ctx->cur++;");
	}

	private void printFunc(String name) {
		this.file.writeIndent("dump_func(\"" + name + "\");");
	}

	private void dec() {
		this.file.writeIndent("dec();");
	}

	private void If(String expr) {
		this.file.writeIndent("if(" + expr + ")");
	}

	private void Else() {
		this.file.writeIndent("else");
	}

	private void returnVal(String val) {
		this.file.writeIndent("return " + val + ";");
	}

	private String getExprName(Expression e) {
		if (e instanceof Repetition1) {
			return "OneMore";
		}
		if (e instanceof Repetition) {
			return "ZeroMore";
		}
//		if (e instanceof Link) {
//			return getExprName(e.get(0));
//		}
//		if (e instanceof NonTerminal) {
//			return ((NonTerminal) e).getLocalName();
//		}
		return e.getClass().getSimpleName();
	}

	private boolean IfFunc(Expression e, String prefix) {
		if (!(e instanceof New || e instanceof Capture || e instanceof Tagging || e instanceof Replace)) {
			if (e instanceof NonTerminal) {
				this.If(prefix + "p" + ((NonTerminal) e).getLocalName() + "(ctx)");
				return true;
			}
			if (e instanceof Link) {
				return this.IfFunc(e.get(0), prefix);
			}
			this.If(prefix + "p" + this.getExprName(e) + e.getId() + "(ctx)");
			return true;
		}
		return false;
	}

	@Override
	public void visitRule(Rule e) {
		Expression inner = e.getExpression();
		this.initFalureJumpPoint();
		this.nonTerminalFuncDecl("int", "p" + e.getLocalName(), "ParsingContext ctx");
		this.openBlock();
		this.IfFunc(inner, "");
		this.openBlock();
		this.returnVal("1");
		this.closeBlock();
		this.Else();
		this.openBlock();
		this.returnVal("0");
		this.closeBlock();
		this.closeBlock();
		this.file.writeNewLine();
		e.getExpression().visit(this);
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
	}

	@Override
	public void visitEmpty(Empty e) {
	}

	@Override
	public void visitFailure(Failure e) {
		if (this.funcDecl("int", "pFailure" + e.getId(), "ParsingContext ctx", e)) {
			this.openBlock();
			this.returnVal("0");
			this.closeBlock();
			this.file.writeNewLine();
		}
	}

	public String stringfyByte(int byteChar) {
		char c = (char) byteChar;
		switch (c) {
		case '\n':
			return ("'\\n'");
		case '\t':
			return ("'\\t'");
		case '\r':
			return ("'\\r'");
		case '\"':
			return ("\'\\\"\'");
		case '\\':
			return ("'\\\\'");
		}
		return "\'" + c + "\'";
	}

	@Override
	public void visitByteChar(ByteChar e) {
		if (this.funcDecl("int", "pByteChar" + e.getId(), "ParsingContext ctx", e)) {
			this.openBlock();
			this.If("*ctx->cur != " + this.stringfyByte(e.byteChar));
			this.openBlock();
			this.returnVal("0");
			this.closeBlock();
			this.consume();
			this.returnVal("1");
			this.closeBlock();
			this.file.writeNewLine();
		}
	}

	private int searchEndChar(boolean[] b, int s) {
		for (; s < 256; s++) {
			if (!b[s]) {
				return s - 1;
			}
		}
		return 255;
	}

	@Override
	public void visitByteMap(ByteMap e) {
		boolean b[] = e.byteMap;
		if (this.funcDecl("int", "pByteMap" + e.getId(), "ParsingContext ctx", e)) {
			this.openBlock();
			for (int start = 0; start < 256; start++) {
				if (b[start]) {
//				int end = searchEndChar(b, start + 1);
//				if (start == end) {
					this.If("*ctx->cur ==" + this.stringfyByte(start));
					this.openBlock();
					this.consume();
					this.returnVal("1");
					this.closeBlock();
//				}
//				else {
//					this.file.writeIndent("if(" + this.stringfyByte(start) + "<= *ctx->cur" + " && *ctx->cur <=" + this.stringfyByte(end) + ")");
//					this.openBlock();
//					this.consume();
//					this.gotoLabel(label);
//					this.closeBlock();
//					start = end;
//				}
				}
			}
			this.returnVal("0");
			this.closeBlock();
			this.file.writeNewLine();
		}
	}

	@Override
	public void visitAnyChar(AnyChar e) {
		if (this.funcDecl("int", "pAnyChar" + e.getId(), "ParsingContext ctx", e)) {
			this.openBlock();
			this.If("*ctx->cur == 0");
			this.openBlock();
			this.returnVal("0");
			this.closeBlock();
			this.consume();
			this.returnVal("1");
			this.closeBlock();
			this.file.writeNewLine();
		}
	}

	@Override
	public void visitNot(Not e) {
		if (this.funcDecl("int", "pNot" + e.getId(), "ParsingContext ctx", e)) {
			this.openBlock();
			String backtrack = "c" + this.fid;
			Expression inner = e.get(0);
			this.let("char*", backtrack, "ctx->cur");
			this.IfFunc(inner, "");
			this.openBlock();
			this.let(null, "ctx->cur", backtrack);
			this.returnVal("0");
			this.closeBlock();
			this.let(null, "ctx->cur", backtrack);
			this.returnVal("1");
			this.closeBlock();
			this.file.writeNewLine();
			inner.visit(this);
		}
	}

	@Override
	public void visitAnd(And e) {
		if (this.funcDecl("int", "pAnd" + e.getId(), "ParsingContext ctx", e)) {
			this.openBlock();
			String backtrack = "c" + this.fid;
			Expression inner = e.get(0);
			this.let("char*", backtrack, "ctx->cur");
			this.IfFunc(inner, "");
			this.openBlock();
			this.let(null, "ctx->cur", backtrack);
			this.returnVal("1");
			this.closeBlock();
			this.let(null, "ctx->cur", backtrack);
			this.returnVal("0");
			this.closeBlock();
			this.file.writeNewLine();
			e.get(0).visit(this);
		}
	}

	@Override
	public void visitOption(Option e) {
		if (this.funcDecl("int", "pOption" + e.getId(), "ParsingContext ctx", e)) {
			this.openBlock();
			String backtrack = "c" + this.fid;
			Expression inner = e.get(0);
			this.let("char*", backtrack, "ctx->cur");
			this.IfFunc(inner, "");
			this.openBlock();
			this.returnVal("1");
			this.closeBlock();
			this.let(null, "ctx->cur", backtrack);
			this.returnVal("1");
			this.closeBlock();
			e.get(0).visit(this);
		}
	}

	@Override
	public void visitRepetition(Repetition e) {
		if (this.funcDecl("int", "pZeroMore" + e.getId(), "ParsingContext ctx", e)) {
			this.openBlock();
			String backtrack = "c" + this.fid;
			Expression inner = e.get(0);
			this.let("char*", backtrack, "ctx->cur");
			this.file.writeIndent("while(1)");
			this.openBlock();
			this.IfFunc(inner, "!");
			this.openBlock();
			this.let(null, "ctx->cur", backtrack);
			this.returnVal("1");
			this.closeBlock();
			this.let(null, backtrack, "ctx->cur");
			this.closeBlock();
			this.closeBlock();
			this.file.writeNewLine();
			inner.visit(this);
		}
	}

	@Override
	public void visitRepetition1(Repetition1 e) {
		if (this.funcDecl("int", "pOneMore" + e.getId(), "ParsingContext ctx", e)) {
			this.openBlock();
			String backtrack = "c" + this.fid;
			Expression inner = e.get(0);
			this.IfFunc(inner, "!");
			this.openBlock();
			this.returnVal("0");
			this.closeBlock();
			this.let("char*", backtrack, "ctx->cur");
			this.file.writeIndent("while(1)");
			this.openBlock();
			this.IfFunc(inner, "!");
			this.openBlock();
			this.let(null, "ctx->cur", backtrack);
			this.returnVal("1");
			this.closeBlock();
			this.let(null, backtrack, "ctx->cur");
			this.closeBlock();
			this.closeBlock();
			this.file.writeNewLine();
			inner.visit(this);
		}
	}

	@Override
	public void visitSequence(Sequence e) {
		if (this.funcDecl("int", "pSequence" + e.getId(), "ParsingContext ctx", e)) {
			this.openBlock();
			for (int i = 0; i < e.size(); i++) {
				Expression inner = e.get(i);
				if (this.IfFunc(inner, "!")) {
					this.openBlock();
					this.returnVal("0");
					this.closeBlock();
				}
			}
			this.returnVal("1");
			this.closeBlock();
			this.file.writeNewLine();
		}
		for (int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitChoice(Choice e) {
		if (this.funcDecl("int", "pChoice" + e.getId(), "ParsingContext ctx", e)) {
			this.openBlock();
			this.fid++;
			String backtrack = "c" + this.fid;
			this.let("char*", backtrack, "ctx->cur");
			for (int i = 0; i < e.size(); i++) {
				Expression inner = e.get(i);
				this.IfFunc(inner, "");
				this.openBlock();
				this.returnVal("1");
				this.closeBlock();
				this.let(null, "ctx->cur", backtrack);
			}
			this.returnVal("0");
			this.closeBlock();
			this.file.writeNewLine();
			for (int i = 0; i < e.size(); i++) {
				e.get(i).visit(this);
			}
		}
	}

	Stack<String> markStack = new Stack<String>();

	@Override
	public void visitNew(New e) {
		if (!PatternMatch) {
			this.pushFailureJumpPoint();
			String mark = "mark" + this.fid;
			this.markStack.push(mark);
			this.file.writeIndent("int " + mark + " = nez_markLogStack(ctx);");
			this.file.writeIndent("nez_pushDataLog(ctx, LazyNew_T, ctx->cur - ctx->inputs, -1, NULL, NULL);");
		}
	}

	@Override
	public void visitCapture(Capture e) {
		if (!PatternMatch) {
			String label = "EXIT_CAPTURE" + this.fid;
			this.file.writeIndent("nez_pushDataLog(ctx, LazyCapture_T, ctx->cur - ctx->inputs, 0, NULL, NULL);");
			this.gotoLabel(label);
			this.popFailureJumpPoint(e);
			this.file.writeIndent("nez_abortLog(ctx, " + this.markStack.pop() + ");");
			this.jumpFailureJump();
			this.exitLabel(label);
		}
	}

	@Override
	public void visitLink(Link e) {
		if (!PatternMatch) {
			this.pushFailureJumpPoint();
			String mark = "mark" + this.fid;
			String label = "EXIT_LINK" + this.fid;
			String po = "ctx->left"; //+ this.fid;
			this.file.writeIndent("int " + mark + " = nez_markLogStack(ctx);");
			e.get(0).visit(this);
			this.let(null, po, "nez_commitLog(ctx, " + mark + ")");
			this.file.writeIndent("nez_pushDataLog(ctx, LazyLink_T, 0, " + e.index + ", NULL, " + po + ");");
			this.gotoLabel(label);
			this.popFailureJumpPoint(e);
			this.file.writeIndent("nez_abortLog(ctx, " + mark + ");");
			this.jumpFailureJump();
			this.exitLabel(label);
		}
		else {
			e.get(0).visit(this);
		}
	}

	@Override
	public void visitTagging(Tagging e) {
		if (!PatternMatch) {
			this.file.writeIndent("nez_pushDataLog(ctx, LazyTag_T, 0, 0, \"" + e.tag.getName() + "\", NULL);");
		}
	}

	@Override
	public void visitReplace(Replace e) {
	}

	@Override
	protected void visitExpression(Expression e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

}
