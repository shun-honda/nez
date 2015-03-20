package nez.x;

import nez.Grammar;
import nez.Production;
import nez.SourceContext;
import nez.ast.AST;
import nez.ast.Node;
import nez.ast.Transformer;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.main.Recorder;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class RegexCommand extends Command {
	SourceContext file;
	String NezFile = "sample/regex.nez";
	String RegexFile;
	UList<String> inputFileList;

	@Override
	public void exec(CommandConfigure config) {
		init(config);
		Recorder rec = config.getRecorder();
		Production p = config.getProduction(config.StartingPoint);
		Node node = parse(config, rec, p, false);
		String outputfile = config.getOutputFileName();
		if (outputfile == null) {
			outputfile = file.getResourceName() + ".nez";
			int index = outputfile.indexOf("/");
			while(index > -1) {
				outputfile = outputfile.substring(index+1);
				index = outputfile.indexOf("/");
			}
			outputfile = "gen/" + outputfile;
		}
		GrammarConverter conv = new RegexConverter(new Grammar(file.getResourceName()), outputfile);
		conv.convert((AST) node);
		config.GrammarFile = outputfile;
		config.setInputFileList(inputFileList);
		rec = config.getRecorder();
		p = conv.grammar.getProduction(config.StartingPoint, Production.RegexOption);
		parse(config, rec, p, true);
	}
	
	private void init(CommandConfigure config) {
		RegexFile = config.GrammarFile;
		config.GrammarFile = NezFile;
		inputFileList = config.InputFileLists;
		config.InputFileLists = new UList<String>(new String[1]);
		config.InputFileLists.add(RegexFile);
	}

	private Node parse(CommandConfigure config, Recorder rec, Production p, boolean writeAST) {
		if(p == null) {
			ConsoleUtils.exit(1, "undefined nonterminal: " + config.StartingPoint);
		}
		p.record(rec);
		Node node = null;
		while(config.hasInput()) {
			file = config.getInputSourceContext();
			Transformer trans = config.getTransformer();
			file.start(rec);
			node = p.parse(file, trans.newNode());
			file.done(rec);
			if(node == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			if(rec != null) {
				rec.log();
			}
			if (writeAST) {
				trans.transform(config.getOutputFileName(file), node);
				ConsoleUtils.println("nez: match");
			}
		}
		return node;
	}
}
