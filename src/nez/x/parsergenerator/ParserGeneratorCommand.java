package nez.x.parsergenerator;

import java.lang.reflect.Constructor;
import java.util.TreeMap;

import nez.Grammar;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.main.Verbose;
import nez.util.ConsoleUtils;

public class ParserGeneratorCommand extends Command {

	@Override
	public void exec(CommandConfigure config) {
		Grammar peg = config.getGrammar();
		ParserGenerator gen = loadGenerator(config.getOutputFileName(), peg);
		gen.generate();
	}

	@Override
	public String getDesc() {
		return "ParserGenerator";
	}

	static private TreeMap<String, Class<?>> classMap = new TreeMap<String, Class<?>>();

	static void regist(String type, String className) {
		try {
			Class<?> c = Class.forName(className);
			classMap.put(type, c);
		} catch (ClassNotFoundException e) {
			Verbose.println("unfound class: " + className);
		}
	}

	static {
		regist("c", "nez.x.parsergenerator.CParserGenerator");
	}

	final ParserGenerator loadGenerator(String output, Grammar peg) {
		if (output != null) {
			ParserGenerator gen = null;
			String type = output;
			String fileName = null; // stdout
			int loc = output.lastIndexOf('.');
			if (loc > 0) {
				type = output.substring(loc + 1);
				fileName = output;
			}
			Class<?> c = classMap.get(type);
			if (c == null) {
				fileName = null;
				try {
					c = Class.forName(output);
				} catch (ClassNotFoundException e) {
					showOutputType(output);
				}
			}
			try {
				Constructor<?> ct = c.getConstructor(String.class, Grammar.class);
				gen = (ParserGenerator) ct.newInstance(fileName, peg);
			} catch (Exception e) {
				ConsoleUtils.exit(1, "unable to load: " + output + " due to " + e);
			}
			return gen;
		}
		ConsoleUtils.exit(1, "unspecified output file");
		return null;
	}

	void showOutputType(String output) {
		ConsoleUtils.println("Parser Generator");
		try {
			for (String n : this.classMap.keySet()) {
				String dummy = null;
				Class<?> c = this.classMap.get(n);
				Constructor<?> ct = c.getConstructor(String.class);
				ParserGenerator g = (ParserGenerator) ct.newInstance(dummy, null);
				String s = String.format("%8s - %s", n, g.getDesc());
				ConsoleUtils.println(s);
			}
			ConsoleUtils.exit(1, "Unknown output type (" + output + ") => Try the above !!");
		} catch (Exception e) {
			e.printStackTrace();
			ConsoleUtils.exit(1, "killed");
		}
	}

}
