package nez.x.minivm;

import java.util.ArrayList;
import java.util.List;

public class Module {
	List<Function> child;

	public Module() {
		this.child = new ArrayList<Function>();
	}

	public int size() {
		return child.size();
	}

	public boolean add(Function func) {
		return this.child.add(func);
	}

	public Function get(int index) {
		return this.child.get(index);
	}

	public Function get(String funcName) {
		for (int i = 0; i < this.size(); i++) {
			if (this.get(i).funcName.equals(funcName)) {
				return this.get(i);
			}
		}
		throw new RuntimeException("Error: Nonterminal Rule not found " + funcName);
	}

	public Function remove(int index) {
		return this.child.remove(index);
	}

	public Function remove(String funcName) {
		for (int i = 0; i < this.size(); i++) {
			if (this.get(i).funcName.equals(funcName)) {
				return this.remove(i);
			}
		}
		throw new RuntimeException("Error: Nonterminal Rule not found " + funcName);
	}

	public String stringfy(StringBuilder sb) {
		for (int i = 0; i < size(); i++) {
			this.get(i).stringfy(sb);
		}
		return sb.toString();
	}
}
