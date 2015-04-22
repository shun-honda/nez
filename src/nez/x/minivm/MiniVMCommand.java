package nez.x.minivm;

import nez.Grammar;
import nez.expr.Rule;
import nez.main.Command;
import nez.main.CommandConfigure;

public class MiniVMCommand extends Command {

	@Override
	public void exec(CommandConfigure config) {
		Grammar g = config.getGrammar();
		MiniVMCompiler c = new MiniVMCompiler(config.OptimizationLevel, g);
		c.sb = new StringBuilder();
		this.generate(c, g);
		c.writeByteCode(config.GrammarFile, config.OutputFileName, g);
	}

	public void generate(MiniVMCompiler c, Grammar peg) {
		c.formatHeader();
		for (Rule r : peg.getRuleList()) {
			if (r.getLocalName().equals("File")) {
				c.visitRule(r);
				break;
			}
		}
		for (Rule r : peg.getRuleList()) {
			if (!r.getLocalName().equals("File")) {
				if (!r.getLocalName().startsWith("\"")) {
					c.visitRule(r);
				}
			}
		}
		c.formatFooter();
	}

	@Override
	public String getDesc() {
		return "minivm";
	}

}
