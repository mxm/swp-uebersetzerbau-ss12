package parser;

import java.util.Stack;
import java.util.Vector;

import lexer.ILexer;
import lexer.IToken.TokenType;
import lexer.SyntaxErrorException;
import lexer.Token;
import lombok.Getter;

public class Parser {
	private ILexer lexer;
	private ParseTable table;
	private Stack<String> stack;
	private Vector<String> outputs;

	@Getter
	private ISyntaxTree syntaxTree;

	private static final String[] nonTerminals = { "program", "funcs", "func",
			"func'", "optparams", "params", "params'", "block", "decls",
			"decl", "type", "type'", "stmts", "stmt", "stmt'", "stmt''", "loc",
			"loc'", "loc''", "assign", "assign'", "bool", "bool'", "join",
			"join'", "equality", "equality'", "rel", "rel'", "expr", "expr'",
			"term", "term'", "unary", "factor", "factor'", "optargs", "args",
			"args'", "basic" };

	private static TokenType[] terminals;

	private static final String EPSILON = "ε";

	public Parser(ILexer lexer) {
		this.lexer = lexer;

		terminals = TokenType.OP_ADD.getDeclaringClass().getEnumConstants();

		table = new ParseTable(nonTerminals, terminals);
		try {
			fillParseTable();
		} catch (ParserException e) {
			e.printStackTrace();
		}

		stack = new Stack<String>();
		outputs = new Vector<String>();
		syntaxTree = new NonTerminal("program");
	}

	private void initStack() {
		stack.clear();
		stack.push("$");
		stack.push("program");
	}

	private boolean isTerminal(String t) {
		for (int i = 0; i < terminals.length; i++) {
			if (terminals[i].toString().equals(t))
				return true;
		}

		return false;
	}

	public void parse() throws ParserException {

		if (table.isAmbigous()) {
			throw new ParserException(
					"Parsing table is ambigous! Won't start syntax analysis");
		}

		initStack();
		String peek;
		Token token = null;
		try {
			token = lexer.getNextToken();
		} catch (SyntaxErrorException e) {
			e.printStackTrace();
		}

		do {
			peek = stack.peek();
			if (isTerminal(peek) || peek.equals("$")) {
				if (peek.equals(token.getType().toString())
						|| (peek.equals("$") && token.getType() == TokenType.EOF)) {

					stack.pop();
					try {
						token = lexer.getNextToken();
					} catch (SyntaxErrorException e) {
						e.printStackTrace();
					}
				} else {
					throw new ParserException("Wrong token " + token
							+ " in input");
				}
			} else /** stack symbol is non-terminal */
			{
				String prod = table.getEntry(peek, token.getType());
				stack.pop();

				String[] tmp = prod.split("::=");
				if (tmp.length == 2) {
					String[] prods = tmp[1].split(" ");
					for (int i = prods.length - 1; i >= 0; i--) {
						if (!prods[i].trim().equals("")
								&& !prods[i].equals(EPSILON)) {
							stack.push(prods[i]);
						}
					}
					outputs.add(prod);
				} else if (prod.trim().equals("")) {
					throw new ParserException(
							"Syntax error: No rule in parsing table (Stack: "
									+ peek + ", token: " + token + ")");
				} else {
					throw new ParserException(
							"Wrong structur in parsing table! Productions should "
									+ "be of the form: X ::= Y1 Y2 ... Yk! Problem with: "
									+ prod + "(Stack: " + peek + ", token: "
									+ token + ")");
				}
			}
		} while (!stack.peek().equals("$"));

		createSyntaxTree();
	}

	/**
	 * Call to create the corresponding parse tree from the previous parse call.
	 */
	private void createSyntaxTree() {
		for (String t : outputs) {

			String[] tmp = t.split("::=");
			String[] tmp2 = tmp[1].split(" ");

			ISyntaxTree node = getNextOccurrence(tmp[0].trim());
			for (int i = 0; i < tmp2.length; i++) {
				if (!tmp2[i].equals("")) {
					ISyntaxTree newNode;
					if (isAllUpper(tmp2[i]) || tmp2[i].equals(EPSILON)) {
						newNode = new Terminal(tmp2[i]);
					} else {
						newNode = new NonTerminal(tmp2[i]);
					}
					node.addTree(newNode);
				}
			}
		}

		printParseTree(syntaxTree, 0);
	}

	/**
	 * Helper function to distinguish between terminals and non-terminals.
	 * 
	 * @param s
	 * @return
	 */
	private boolean isAllUpper(String s) {
		for (char c : s.toCharArray()) {
			if (Character.isLetter(c) && Character.isLowerCase(c)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Using Depth-First search to find the node.
	 * 
	 * @param name
	 * @return the next occurrence of the given node, if it has no children yet.
	 */
	private ISyntaxTree getNextOccurrence(String name) {

		if (syntaxTree.getName().equals(name)
				&& syntaxTree.getChildrenCount() == 0) {
			return syntaxTree;
		}

		Stack<ISyntaxTree> stack = new Stack<ISyntaxTree>();
		stack.push(syntaxTree);

		while (!stack.isEmpty()) {
			ISyntaxTree tmp = stack.pop();
			if (tmp.getName().equals(name) && tmp.getChildrenCount() == 0) {
				return tmp;
			} else // push all children onto the stack backwards
			{
				int count = tmp.getChildrenCount();
				for (int i = count - 1; i >= 0; i--) {
					stack.push(tmp.getChild(i));
				}
			}
		}
		return null;
	}

	/**
	 * Prints the generated parse tree turned by 90 degree clockwise. Read
	 * direction is still from the top to the bottom.
	 * 
	 * @param tree
	 * @param depth
	 */
	private void printParseTree(ISyntaxTree tree, int depth) {

		for (int i = 0; i <= depth; i++) {
			System.out.print("\t");
		}
		System.out.print(tree.getName());
		System.out.println("");
		int count = tree.getChildrenCount();
		if (count != 0) {
			for (int i = 0; i < count; i++) {
				printParseTree(tree.getChild(i), depth + 1);
			}
		}
	}

	/**
	 * Cells should be filled by Productions of the form: X ::= Y1 Y2 ... Yk
	 * Treats basic as { INT_TYPE, REAL_TYPE, STRING_TYPE, BOOL_TYPE }!
	 * 
	 * @throws ParserException
	 */
	private void fillParseTable() throws ParserException {
		// program
		table.setEntry("program", TokenType.DEF, "program ::= funcs");

		// funcs
		table.setEntry("funcs", TokenType.DEF, "funcs ::=  func funcs");
		table.setEntry("funcs", TokenType.EOF, "funcs ::= ε");

		// func'
		table.setEntry("func", TokenType.DEF,
				"func ::=  DEF type ID LPAREN optparams RPAREN func'");
		table.setEntry("func'", TokenType.OP_SEMIC, "func' ::= OP_SEMIC");
		table.setEntry("func'", TokenType.LBRACE, "func' ::= block");

		// optparams
		table.setEntry("optparams", TokenType.RPAREN, "optparams ::= ε");
		table.setEntry("optparams", TokenType.INT_TYPE, "optparams ::= params");
		table.setEntry("optparams", TokenType.REAL_TYPE, "optparams ::= params");
		table.setEntry("optparams", TokenType.STRING_TYPE,
				"optparams ::= params");
		table.setEntry("optparams", TokenType.BOOL_TYPE, "optparams ::= params");
		table.setEntry("optparams", TokenType.RECORD, "optparams ::= params");

		// params
		table.setEntry("params", TokenType.INT_TYPE,
				"params ::= type ID params'");
		table.setEntry("params", TokenType.REAL_TYPE,
				"params ::= type ID params'");
		table.setEntry("params", TokenType.STRING_TYPE,
				"params ::= type ID params'");
		table.setEntry("params", TokenType.BOOL_TYPE,
				"params ::= type ID params'");
		table.setEntry("params", TokenType.RECORD, "params ::= type ID params'");

		// params'
		table.setEntry("params'", TokenType.RPAREN, "params' ::= ε");
		table.setEntry("params'", TokenType.OP_COMMA,
				"params' ::= OP_COMMA params");

		// block
		table.setEntry("block", TokenType.LBRACE,
				"block ::= LBRACE decls stmts RBRACE");

		// decls
		table.setEntry("decls", TokenType.ID, "decls ::= ε");
		table.setEntry("decls", TokenType.LBRACE, "decls ::= ε");
		table.setEntry("decls", TokenType.RBRACE, "decls ::= ε");
		table.setEntry("decls", TokenType.IF, "decls ::= ε");
		table.setEntry("decls", TokenType.WHILE, "decls ::= ε");
		table.setEntry("decls", TokenType.DO, "decls ::= ε");
		table.setEntry("decls", TokenType.BREAK, "decls ::= ε");
		table.setEntry("decls", TokenType.RETURN, "decls ::= ε");
		table.setEntry("decls", TokenType.PRINT, "decls ::= ε");
		table.setEntry("decls", TokenType.INT_TYPE, "decls ::=  decl decls");
		table.setEntry("decls", TokenType.REAL_TYPE, "decls ::=  decl decls");
		table.setEntry("decls", TokenType.STRING_TYPE, "decls ::=  decl decls");
		table.setEntry("decls", TokenType.BOOL_TYPE, "decls ::=  decl decls");
		table.setEntry("decls", TokenType.RECORD, "decls ::=  decl decls");

		// decl
		table.setEntry("decl", TokenType.INT_TYPE, "decl ::=  type ID OP_SEMIC");
		table.setEntry("decl", TokenType.REAL_TYPE,
				"decl ::=  type ID OP_SEMIC");
		table.setEntry("decl", TokenType.STRING_TYPE,
				"decl ::=  type ID OP_SEMIC");
		table.setEntry("decl", TokenType.BOOL_TYPE,
				"decl ::=  type ID OP_SEMIC");
		table.setEntry("decl", TokenType.RECORD, "decl ::=  type ID OP_SEMIC");

		// type
		table.setEntry("type", TokenType.INT_TYPE, "type ::=  basic type'");
		table.setEntry("type", TokenType.REAL_TYPE, "type ::=  basic type'");
		table.setEntry("type", TokenType.STRING_TYPE, "type ::=  basic type'");
		table.setEntry("type", TokenType.BOOL_TYPE, "type ::=  basic type'");
		table.setEntry("type", TokenType.RECORD,
				"type ::= RECORD LBRACE decls RBRACE type'");

		// type'
		table.setEntry("type'", TokenType.ID, "type' ::= ε");
		table.setEntry("type'", TokenType.LBRACKET,
				"type' ::= LBRACKET INT_LITERAL RBRACKET type' ");

		// stmts
		table.setEntry("stmts", TokenType.ID, "stmts ::= stmt stmts");
		table.setEntry("stmts", TokenType.LBRACE, "stmts ::= stmt stmts");
		table.setEntry("stmts", TokenType.IF, "stmts ::= stmt stmts");
		table.setEntry("stmts", TokenType.WHILE, "stmts ::= stmt stmts");
		table.setEntry("stmts", TokenType.DO, "stmts ::= stmt stmts");
		table.setEntry("stmts", TokenType.BREAK, "stmts ::= stmt stmts");
		table.setEntry("stmts", TokenType.RETURN, "stmts ::= stmt stmts");
		table.setEntry("stmts", TokenType.PRINT, "stmts ::= stmt stmts");
		table.setEntry("stmts", TokenType.RBRACE, "stmts ::= ε");

		// stmt
		table.setEntry("stmt", TokenType.ID, "stmt ::= assign OP_SEMIC");
		table.setEntry("stmt", TokenType.LBRACE, "stmt ::= block");
		table.setEntry("stmt", TokenType.IF,
				"stmt ::= IF LPAREN assign RPAREN stmt stmt'");
		table.setEntry("stmt", TokenType.WHILE,
				"stmt ::= WHILE LPAREN assign RPAREN stmt ");
		table.setEntry("stmt", TokenType.DO,
				"stmt ::= DO stmt WHILE LPAREN assign RPAREN OP_SEMIC ");
		table.setEntry("stmt", TokenType.BREAK, "stmt ::= BREAK OP_SEMIC ");
		table.setEntry("stmt", TokenType.RETURN, "stmt ::= RETURN stmt''");
		table.setEntry("stmt", TokenType.PRINT, "stmt ::= PRINT loc OP_SEMIC ");

		// stmt'
		table.setEntry("stmt'", TokenType.ID, "stmt' ::= ε");
		table.setEntry("stmt'", TokenType.LBRACE, "stmt' ::= ε");
		table.setEntry("stmt'", TokenType.RBRACE, "stmt' ::= ε");
		table.setEntry("stmt'", TokenType.IF, "stmt' ::= ε");
		table.setEntry("stmt'", TokenType.WHILE, "stmt' ::= ε");
		table.setEntry("stmt'", TokenType.DO, "stmt' ::= ε");
		table.setEntry("stmt'", TokenType.BREAK, "stmt' ::= ε");
		table.setEntry("stmt'", TokenType.RETURN, "stmt' ::= ε");
		table.setEntry("stmt'", TokenType.PRINT, "stmt' ::= ε");
		table.setEntry("stmt'", TokenType.ELSE, "stmt' ::= ELSE stmt");

		// stmt''
		table.setEntry("stmt''", TokenType.ID, "stmt'' ::= loc OP_SEMIC");
		table.setEntry("stmt''", TokenType.OP_SEMIC, "stmt'' ::= OP_SEMIC");

		// loc
		table.setEntry("loc", TokenType.ID, "loc ::=   ID loc'' ");

		// loc'
		table.setEntry("loc'", TokenType.LBRACKET,
				"loc' ::= LBRACKET assign RBRACKET");
		table.setEntry("loc'", TokenType.OP_DOT, "loc' ::= OP_DOT ID");

		// loc''
		table.setEntry("loc''", TokenType.LBRACKET, "loc'' ::= loc' loc'' ");
		table.setEntry("loc''", TokenType.OP_DOT, "loc'' ::= loc' loc'' ");
		table.setEntry("loc''", TokenType.LPAREN, "loc'' ::= ε");
		table.setEntry("loc''", TokenType.OP_SEMIC, "loc'' ::= ε");
		table.setEntry("loc''", TokenType.OP_LT, "loc'' ::= ε");
		table.setEntry("loc''", TokenType.OP_LE, "loc'' ::= ε");
		table.setEntry("loc''", TokenType.OP_GE, "loc'' ::= ε");
		table.setEntry("loc''", TokenType.OP_GT, "loc'' ::= ε");
		table.setEntry("loc''", TokenType.OP_ADD, "loc'' ::= ε");
		table.setEntry("loc''", TokenType.OP_MINUS, "loc'' ::= ε");
		table.setEntry("loc''", TokenType.OP_MUL, "loc'' ::= ε");
		table.setEntry("loc''", TokenType.OP_DIV, "loc'' ::= ε");

		// assign
		table.setEntry("assign", TokenType.ID, "assign ::= bool assign'");
		table.setEntry("assign", TokenType.LPAREN, "assign ::= bool assign'");
		table.setEntry("assign", TokenType.OP_MINUS, "assign ::= bool assign'");
		table.setEntry("assign", TokenType.OP_NOT, "assign ::= bool assign'");
		table.setEntry("assign", TokenType.INT_LITERAL,
				"assign ::= bool assign'");
		table.setEntry("assign", TokenType.REAL_LITERAL,
				"assign ::= bool assign'");
		table.setEntry("assign", TokenType.STRING_LITERAL,
				"assign ::= bool assign'");
		table.setEntry("assign", TokenType.BOOL_LITERAL,
				"assign ::= bool assign'");

		// assign'
		table.setEntry("assign'", TokenType.OP_ASSIGN,
				"assign' ::= OP_ASSIGN assign assign'");
		table.setEntry("assign'", TokenType.RPAREN, "assign' ::= ε ");
		table.setEntry("assign'", TokenType.RBRACKET, "assign' ::= ε ");
		table.setEntry("assign'", TokenType.OP_COMMA, "assign' ::= ε ");
		table.setEntry("assign'", TokenType.OP_SEMIC, "assign' ::= ε ");
		// table.setEntry("assign'", TokenType.OP_ASSIGN, "assign' ::= ε ");
		// TODO: This is right but still leads to errors

		// bool
		table.setEntry("bool", TokenType.ID, "bool ::=  join bool'");
		table.setEntry("bool", TokenType.LPAREN, "bool ::=  join bool'");
		table.setEntry("bool", TokenType.INT_LITERAL, "bool ::=  join bool'");
		table.setEntry("bool", TokenType.OP_MINUS, "bool ::=  join bool'");
		table.setEntry("bool", TokenType.OP_NOT, "bool ::=  join bool'");
		table.setEntry("bool", TokenType.REAL_LITERAL, "bool ::=  join bool'");
		table.setEntry("bool", TokenType.STRING_LITERAL, "bool ::=  join bool'");
		table.setEntry("bool", TokenType.BOOL_LITERAL, "bool ::=  join bool'");

		// bool'
		table.setEntry("bool'", TokenType.RPAREN, "bool' ::= ε ");
		table.setEntry("bool'", TokenType.RBRACKET, "bool' ::= ε ");
		table.setEntry("bool'", TokenType.OP_ASSIGN, "bool' ::= ε ");
		table.setEntry("bool'", TokenType.OP_COMMA, "bool' ::= ε ");
		table.setEntry("bool'", TokenType.OP_SEMIC, "bool' ::= ε ");
		table.setEntry("bool'", TokenType.OP_OR, "bool' ::= OP_OR join bool'");

		// join
		table.setEntry("join", TokenType.ID, "join ::= equality join'");
		table.setEntry("join", TokenType.LPAREN, "join ::= equality join'");
		table.setEntry("join", TokenType.INT_LITERAL, "join ::= equality join'");
		table.setEntry("join", TokenType.OP_MINUS, "join ::= equality join'");
		table.setEntry("join", TokenType.OP_NOT, "join ::= equality join'");
		table.setEntry("join", TokenType.REAL_LITERAL,
				"join ::= equality join'");
		table.setEntry("join", TokenType.STRING_LITERAL,
				"join ::= equality join'");
		table.setEntry("join", TokenType.BOOL_LITERAL,
				"join ::= equality join'");

		// join'
		table.setEntry("join'", TokenType.RPAREN, "join' ::= ε ");
		table.setEntry("join'", TokenType.RBRACKET, "join' ::= ε ");
		table.setEntry("join'", TokenType.OP_ASSIGN, "join' ::= ε ");
		table.setEntry("join'", TokenType.OP_COMMA, "join' ::= ε ");
		table.setEntry("join'", TokenType.OP_OR, "join' ::= ε ");
		table.setEntry("join'", TokenType.OP_SEMIC, "join' ::= ε ");
		table.setEntry("join'", TokenType.OP_AND,
				"join' ::= OP_AND equality join'");

		// equality
		table.setEntry("equality", TokenType.ID, "equality ::= rel equality'");
		table.setEntry("equality", TokenType.LPAREN,
				"equality ::= rel equality'");
		table.setEntry("equality", TokenType.INT_LITERAL,
				"equality ::= rel equality'");
		table.setEntry("equality", TokenType.OP_MINUS,
				"equality ::= rel equality'");
		table.setEntry("equality", TokenType.OP_NOT,
				"equality ::= rel equality'");
		table.setEntry("equality", TokenType.REAL_LITERAL,
				"equality ::= rel equality'");
		table.setEntry("equality", TokenType.STRING_LITERAL,
				"equality ::= rel equality'");
		table.setEntry("equality", TokenType.BOOL_LITERAL,
				"equality ::= rel equality'");

		// equality'
		table.setEntry("equality'", TokenType.RPAREN, "equality' ::= ε ");
		table.setEntry("equality'", TokenType.RBRACKET, "equality' ::= ε ");
		table.setEntry("equality'", TokenType.OP_ASSIGN, "equality' ::= ε ");
		table.setEntry("equality'", TokenType.OP_COMMA, "equality' ::= ε ");
		table.setEntry("equality'", TokenType.OP_OR, "equality' ::= ε ");
		table.setEntry("equality'", TokenType.OP_AND, "equality' ::= ε ");
		table.setEntry("equality'", TokenType.OP_SEMIC, "equality' ::= ε ");
		table.setEntry("equality'", TokenType.OP_EQ,
				"equality' ::= OP_EQ rel equality'");
		table.setEntry("equality'", TokenType.OP_NE,
				"equality' ::= OP_NE rel equality'");

		// rel
		table.setEntry("rel", TokenType.ID, "rel ::= expr  rel'");
		table.setEntry("rel", TokenType.LPAREN, "rel ::= expr  rel'");
		table.setEntry("rel", TokenType.INT_LITERAL, "rel ::= expr  rel'");
		table.setEntry("rel", TokenType.OP_MINUS, "rel ::= expr  rel'");
		table.setEntry("rel", TokenType.OP_NOT, "rel ::= expr  rel'");
		table.setEntry("rel", TokenType.REAL_LITERAL, "rel ::= expr  rel'");
		table.setEntry("rel", TokenType.STRING_LITERAL, "rel ::= expr  rel'");
		table.setEntry("rel", TokenType.BOOL_LITERAL, "rel ::= expr  rel'");

		// rel'
		table.setEntry("rel'", TokenType.RPAREN, "rel' ::= ε ");
		table.setEntry("rel'", TokenType.RBRACKET, "rel' ::= ε ");
		table.setEntry("rel'", TokenType.OP_ASSIGN, "rel' ::= ε ");
		table.setEntry("rel'", TokenType.OP_COMMA, "rel' ::= ε ");
		table.setEntry("rel'", TokenType.OP_SEMIC, "rel' ::= ε ");
		table.setEntry("rel'", TokenType.OP_OR, "rel' ::= ε ");
		table.setEntry("rel'", TokenType.OP_AND, "rel' ::= ε ");
		table.setEntry("rel'", TokenType.OP_EQ, "rel' ::= ε ");
		table.setEntry("rel'", TokenType.OP_NE, "rel' ::= ε ");
		table.setEntry("rel'", TokenType.OP_LT, "rel' ::= OP_LT expr");
		table.setEntry("rel'", TokenType.OP_LE, "rel' ::= OP_LE expr");
		table.setEntry("rel'", TokenType.OP_GE, "rel' ::= OP_GE expr");
		table.setEntry("rel'", TokenType.OP_GT, "rel' ::= OP_GT expr");

		// expr
		table.setEntry("expr", TokenType.ID, "expr ::= term expr'");
		table.setEntry("expr", TokenType.LPAREN, "expr ::= term expr'");
		table.setEntry("expr", TokenType.INT_LITERAL, "expr ::= term expr'");
		table.setEntry("expr", TokenType.OP_MINUS, "expr ::= term expr'");
		table.setEntry("expr", TokenType.OP_NOT, "expr ::= term expr'");
		table.setEntry("expr", TokenType.REAL_LITERAL, "expr ::= term expr'");
		table.setEntry("expr", TokenType.STRING_LITERAL, "expr ::= term expr'");
		table.setEntry("expr", TokenType.BOOL_LITERAL, "expr ::= term expr'");

		// expr'
		table.setEntry("expr'", TokenType.OP_LT, "expr' ::= ε ");
		table.setEntry("expr'", TokenType.OP_LE, "expr' ::= ε ");
		table.setEntry("expr'", TokenType.OP_GE, "expr' ::= ε ");
		table.setEntry("expr'", TokenType.OP_GT, "expr' ::= ε ");
		table.setEntry("expr'", TokenType.OP_ADD, "expr' ::= OP_ADD term expr'");
		table.setEntry("expr'", TokenType.OP_MINUS,
				"expr' ::= OP_MINUS term expr'");

		// term
		table.setEntry("term", TokenType.ID, "expr ::= unary term'");
		table.setEntry("term", TokenType.LPAREN, "expr ::= unary term'");
		table.setEntry("term", TokenType.INT_LITERAL, "expr ::= unary term'");
		table.setEntry("term", TokenType.OP_MINUS, "expr ::= unary term'");
		table.setEntry("term", TokenType.OP_NOT, "expr ::= unary term'");
		table.setEntry("term", TokenType.REAL_LITERAL, "expr ::= unary term'");
		table.setEntry("term", TokenType.STRING_LITERAL, "expr ::= unary term'");
		table.setEntry("term", TokenType.BOOL_LITERAL, "expr ::= unary term'");

		// term'
		table.setEntry("term'", TokenType.OP_LT, "term' ::= ε ");
		table.setEntry("term'", TokenType.OP_LE, "term' ::= ε ");
		table.setEntry("term'", TokenType.OP_GE, "term' ::= ε ");
		table.setEntry("term'", TokenType.OP_GT, "term' ::= ε ");
		table.setEntry("term'", TokenType.OP_ADD, "term' ::= ε ");
		table.setEntry("term'", TokenType.OP_MINUS, "term' ::= ε ");
		table.setEntry("term'", TokenType.OP_MUL,
				"term' ::= OP_MUL unary term'");
		table.setEntry("term'", TokenType.OP_DIV,
				"term' ::= OP_DIV unary term'");

		// unary
		table.setEntry("unary", TokenType.ID, "unary ::= factor");
		table.setEntry("unary", TokenType.LPAREN, "unary ::= factor");
		table.setEntry("unary", TokenType.INT_LITERAL, "unary ::= factor");
		table.setEntry("unary", TokenType.REAL_LITERAL, "unary ::= factor");
		table.setEntry("unary", TokenType.STRING_LITERAL, "unary ::= factor");
		table.setEntry("unary", TokenType.BOOL_LITERAL, "unary ::= factor");
		table.setEntry("unary", TokenType.OP_MINUS, "unary ::= OP_MINUS unary");
		table.setEntry("unary", TokenType.OP_NOT, "unary ::= OP_NOT unary");

		// factor
		table.setEntry("factor", TokenType.ID, "factor ::= loc factor'");
		table.setEntry("factor", TokenType.LPAREN,
				"factor ::= LPAREN assign RPAREN");
		table.setEntry("factor", TokenType.INT_LITERAL,
				"factor ::= INT_LITERAL");
		table.setEntry("factor", TokenType.REAL_LITERAL,
				"factor ::= REAL_LITERAL");
		table.setEntry("factor", TokenType.STRING_LITERAL,
				"factor ::= STRING_LITERAL");
		table.setEntry("factor", TokenType.BOOL_LITERAL,
				"factor ::= BOOL_LITERAL");

		// factor'
		table.setEntry("factor'", TokenType.LPAREN,
				"factor' ::= LPAREN optargs RPAREN ");
		table.setEntry("factor'", TokenType.OP_LT, "factor' ::= ε ");
		table.setEntry("factor'", TokenType.OP_LE, "factor' ::= ε ");
		table.setEntry("factor'", TokenType.OP_GE, "factor' ::= ε ");
		table.setEntry("factor'", TokenType.OP_GT, "factor' ::= ε ");
		table.setEntry("factor'", TokenType.OP_ADD, "factor' ::= ε ");
		table.setEntry("factor'", TokenType.OP_MINUS, "factor' ::= ε ");
		table.setEntry("factor'", TokenType.OP_MUL, "factor' ::= ε ");
		table.setEntry("factor'", TokenType.OP_DIV, "factor' ::= ε ");

		// optargs
		table.setEntry("optargs", TokenType.ID, "optargs ::= args");
		table.setEntry("optargs", TokenType.OP_NOT, "optargs ::= args");
		table.setEntry("optargs", TokenType.OP_MINUS, "optargs ::= args");
		table.setEntry("optargs", TokenType.LPAREN, "optargs ::= args");
		table.setEntry("optargs", TokenType.INT_LITERAL, "optargs ::= args");
		table.setEntry("optargs", TokenType.REAL_LITERAL, "optargs ::= args");
		table.setEntry("optargs", TokenType.BOOL_LITERAL, "optargs ::= args");
		table.setEntry("optargs", TokenType.STRING_LITERAL, "optargs ::= args");
		table.setEntry("optargs", TokenType.RPAREN, "optargs ::= ε");

		// args
		table.setEntry("args", TokenType.ID, "args ::= assign args'");
		table.setEntry("args", TokenType.LPAREN, "args ::= assign args'");
		table.setEntry("args", TokenType.INT_LITERAL, "args ::= assign args'");
		table.setEntry("args", TokenType.OP_MINUS, "args ::= assign args'");
		table.setEntry("args", TokenType.OP_NOT, "args ::= assign args'");
		table.setEntry("args", TokenType.REAL_LITERAL, "args ::= assign args'");
		table.setEntry("args", TokenType.STRING_LITERAL,
				"args ::= assign args'");
		table.setEntry("args", TokenType.BOOL_LITERAL, "args ::= assign args'");

		// args'
		table.setEntry("args'", TokenType.RPAREN, "agrs' ::= ε");
		table.setEntry("args'", TokenType.OP_COMMA, "args' ::= OP_COMMA args");

		// basic
		table.setEntry("basic", TokenType.INT_TYPE, "basic ::=  INT_TYPE");
		table.setEntry("basic", TokenType.REAL_TYPE, "basic ::= REAL_TYPE");
		table.setEntry("basic", TokenType.STRING_TYPE, "basic ::=  STRING_TYPE");
		table.setEntry("basic", TokenType.BOOL_TYPE, "basic ::=  BOOL_TYPE");
	}

}
