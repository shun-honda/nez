package nez.main;

import java.io.IOException;

import nez.Grammar;
import nez.Production;
import nez.SourceContext;
import nez.ast.ASTWriter;
import nez.ast.Transformer;
import nez.expr.GrammarChecker;
import nez.expr.NezParser;
import nez.expr.NezParserCombinator;
import nez.util.ConsoleUtils;
import nez.util.FlagUtils;
import nez.util.StringUtils;
import nez.util.UList;
import nez.vm.MemoTable;

public class CommandConfigure {
//	// -X specified
//	private static Class<?> OutputWriterClass = null;

	public String CommandName = "shell";

	// -p konoha.nez
	public String GrammarFile = null; // default

	// -e "peg rule"
	public String GrammarText = null;

	// -e, --expr regular expression
	public String Expression = null;

	// -s, --start
	public String StartingPoint = "File";  // default
	
	// -i, --input
	private int InputFileIndex = -1;  // shell mode
	public UList<String> InputFileLists = new UList<String>(new String[2]);

	// -t, --text
	public String InputText = null;

	// -o, --output
	public String OutputFileName = null;

	// -W
	public int WarningLevel = 1;
	
	// -g
	public int DebugLevel = 1;
	
	// --verbose
	public boolean VerboseMode    = false;
	
	// -O
	public int OptimizationLevel = 2;

	void showUsage(String Message) {
		ConsoleUtils.println("nez <command> optional files");
		ConsoleUtils.println("  -p | --peg <filename>      Specify an Nez grammar file");
		ConsoleUtils.println("  -e | --expr  <text>        Specify an Nez grammar text");
		ConsoleUtils.println("  -i | --input <filenames>   Specify input files");
		ConsoleUtils.println("  -t | --text  <string>      Specify an input text");
		ConsoleUtils.println("  -o | --output <filename>   Specify an output file");
		ConsoleUtils.println("  -s | --start <NAME>        Specify Non-Terminal as the starting point (default: File)");
		ConsoleUtils.println("  -W<num>                    Warning Level (default:1)");
		ConsoleUtils.println("  -O<num>                    Optimization Level (default:2)");
		ConsoleUtils.println("  -g                         Debug Level");
		ConsoleUtils.println("   -Xclassic                 Running on the classic recusive decent parsing");
		ConsoleUtils.println("  --memo:x                   Memo configuration");
		ConsoleUtils.println("     none|packrat|window|slide|notrace");
		ConsoleUtils.println("  --memo:<num>               Expected backtrack distance (default: 256)");
		ConsoleUtils.println("  --verbose                  Printing Debug infomation");
		ConsoleUtils.println("  --verbose:memo             Printing Memoization information");
		ConsoleUtils.println("  -X <class>                 Specify an extension class");
		ConsoleUtils.println("");
		ConsoleUtils.println("The most commonly used nez commands are:");
		ConsoleUtils.println("  parse        Parse -i input or -s string to -o output");
		ConsoleUtils.println("  check        Parse -i input or -s string");
		ConsoleUtils.println("  shell        Try parsing in an interactive way");
		ConsoleUtils.println("  rel          Convert -f file to relations (csv file)");
		ConsoleUtils.println("  nezex        Convert -i regex to peg");
		ConsoleUtils.println("  conv         Convert PEG4d rules to the specified format in -o");
		ConsoleUtils.println("  find         Search nonterminals that can match inputs");
		ConsoleUtils.exit(0, Message);
	}
	
	private int WindowSize = 32;
	private MemoTable defaultTable = MemoTable.newElasticTable(0, 0, 0);
	
	public void parseCommandOption(String[] args) {
		int index = 0;
		if(args.length > 0) {
			if(!args[0].startsWith("-")) {
				CommandName = args[0];
				index = 1;
			}
		}
		while (index < args.length) {
			String argument = args[index];
			if (!argument.startsWith("-")) {
				break;
			}
			index = index + 1;
			if (argument.equals("-X") && (index < args.length)) {
				try {
					Class<?> c = Class.forName(args[index]);
//					if(ParsingWriter.class.isAssignableFrom(c)) {
//						OutputWriterClass = c;
//					}
				} catch (ClassNotFoundException e) {
					ConsoleUtils.exit(1, "-X specified class is not found: " + args[index]);
				}
				index = index + 1;
			}
			else if ((argument.equals("-p") || argument.equals("--peg")) && (index < args.length)) {
				GrammarFile = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-e") || argument.equals("--expr")) && (index < args.length)) {
				GrammarText = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-t") || argument.equals("--text")) && (index < args.length)) {
				InputText = args[index];
				index = index + 1;
				InputFileIndex = 0;
			}
			else if ((argument.equals("-i") || argument.equals("--input")) && (index < args.length)) {
				InputFileLists = new UList<String>(new String[4]);
				while(index < args.length && !args[index].startsWith("-")) {
					InputFileLists.add(args[index]);
					index = index + 1;
					InputFileIndex = 0;
				}
			}
			else if ((argument.equals("-o") || argument.equals("--output")) && (index < args.length)) {
				OutputFileName = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-s") || argument.equals("--start")) && (index < args.length)) {
				StartingPoint = args[index];
				index = index + 1;
			}
//			else if (argument.startsWith("-O")) {
//				OptimizationLevel = StringUtils.parseInt(argument.substring(2), 2);
//			}
//			else if (argument.startsWith("-W")) {
//				WarningLevel = StringUtils.parseInt(argument.substring(2), 2);
//			}
//			else if (argument.startsWith("-g")) {
//				DebugLevel = StringUtils.parseInt(argument.substring(2), 1);
//			}
			else if(argument.startsWith("--memo")) {
				if(argument.equals("--memo:none")) {
					ProductionOption = FlagUtils.unsetFlag(ProductionOption, Production.PackratParsing);
				}
				else if(argument.equals("--memo:packrat")) {
					defaultTable = MemoTable.newPackratHashTable(0, 0, 0);
				}
				else {
					int w = StringUtils.parseInt(argument.substring(7), -1);
					if(w >= 0) {
						WindowSize  = w;
					}
					else {
						showUsage("unknown option: " + argument);
					}
				}
			}
			else if(argument.startsWith("-Xconfig")) {
				ProductionOption = StringUtils.parseInt(argument.substring(9), Production.DefaultOption);
				Verbose.println("configuration: " + Production.stringfyOption(ProductionOption, ", "));
			}
			else if(argument.startsWith("-Xrec")) {
				RecorderFileName = "nezrec.csv";
				if(argument.endsWith(".csv")) {
					RecorderFileName = argument.substring(6);
				}
				Verbose.println("recording " + RecorderFileName);
			}
			else if(argument.startsWith("--verbose")) {
				if(argument.equals("--verbose:memo")) {
					Verbose.PackratParsing = true;
				}
				else if(argument.equals("--verbose:peg")) {
					Verbose.Grammar = true;
				}
				else if(argument.equals("--verbose:vm")) {
					Verbose.VirtualMachine = true;
				}
				else if(argument.equals("--verbose:debug")) {
					Verbose.Debug = true;
				}
				else if(argument.equals("--verbose:backtrack")) {
					Verbose.Backtrack = true;
				}
				else if(argument.equals("--verbose:none")) {
					Verbose.General = false;
				}
				else {
					Verbose.setAll();
					Verbose.println("unknown verbose option: " + argument);
				}
			}
			else {
				this.showUsage("unknown option: " + argument);
			}
		}
	}

	public final Command getCommand() {
		Command com = Command.getCommand(this.CommandName);
		if(com == null) {
			this.showUsage("unknown command: " + this.CommandName);
		}
		return com;
	}
	
	public final Grammar getGrammar() {
		if(GrammarFile != null) {
			try {
				NezParser p = new NezParser();
				return p.load(SourceContext.loadSource(GrammarFile), new GrammarChecker(this.OptimizationLevel));
			} catch (IOException e) {
				ConsoleUtils.exit(1, "cannot open " + GrammarFile + "; " + e.getMessage());
			}
		}
		if(GrammarText != null) {
			NezParser p = new NezParser();
			return p.load(SourceContext.newStringSourceContext(GrammarText), new GrammarChecker(this.OptimizationLevel));
		}
		ConsoleUtils.println("unspecifed grammar");
		return NezParserCombinator.newGrammar();
	}

	public final Production getProduction(String start, int option) {
		if(start == null) {
			start = this.StartingPoint;
		}
		return getGrammar().getProduction(start, option);
	}

	private int ProductionOption = Production.DefaultOption;
	
	public final Production getProduction(String start) {
		Production p = getGrammar().getProduction(start, ProductionOption);
		if(p == null) {
			ConsoleUtils.exit(1, "undefined nonterminal: " + start);
		}
		p.config(this.defaultTable, WindowSize);
		return p;
	}

	public final Production getProduction() {
		return this.getProduction(this.StartingPoint);
	}

	public final boolean hasInput() {
		if(this.InputFileIndex == -1) {
			this.InputText = Command.readMultiLine(">>> ", "... ");
			return this.InputText != null;
		}
		return this.InputText != null || this.InputFileIndex < this.InputFileLists.size();
	}
	
	public final SourceContext getInputSourceContext() {
		if(this.InputText != null) {
			String text = this.InputText;
			this.InputText = null;
			return SourceContext.newStringSourceContext(text);
		}
		if(this.InputFileIndex < this.InputFileLists.size()) {
			String f = this.InputFileLists.ArrayValues[this.InputFileIndex];
			this.InputFileIndex ++;
			try {
				return SourceContext.loadSource(f);
			} catch (IOException e) {
				ConsoleUtils.exit(1, "cannot open: " + f);
			}
		}
		return SourceContext.newStringSourceContext(""); // empty input
	}

	public String getOutputFileName(SourceContext input) {
		return null;
	}

	public final String getOutputFileName() {
		return this.OutputFileName;
	}

	public String RecorderFileName = null;

	public final Recorder getRecorder() {
		if(RecorderFileName != null) {
			Recorder rec = new Recorder(RecorderFileName);
			rec.setText("nez", Command.Version);
			rec.setText("config", Production.stringfyOption(ProductionOption, ";"));
			return rec;
		}
		return null;
	}

	public final Transformer getTransformer() {
		return new ASTWriter();
	}

}

