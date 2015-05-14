package nez.x.minivm;

public enum Opcode {
	exit,
	succ,
	fail,
	jump,
	call,
	RET,
	IFFAIL,
	CHAR,
	CHARMAP,
	STRING,
	ANY,
	PUSHpos,
	POPpos,
	GETpos,
	STOREpos,
	NOTCHAR,
	NOTCHARMAP,
	NOTSTRING,
	OPTIONALCHAR,
	OPTIONALCHARMAP,
	OPTIONALSTRING,
	ZEROMORECHARMAP
}
