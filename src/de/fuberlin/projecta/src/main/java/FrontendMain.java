import java.io.File;

import lexer.ILexer;
import lexer.Lexer;
import lexer.io.FileCharStream;
import lexer.io.ICharStream;
import lexer.io.StringCharStream;
import parser.ISyntaxTree;
import parser.Parser;
import parser.ParserException;
import semantic.analysis.SemanticAnalyzer;
import utils.IOUtils;

public class FrontendMain {

	static void readStdin() {
		String data = IOUtils.readMultilineStringFromStdin();
		run(new StringCharStream(data));
	}

	static void readFile(String path) {
		File sourceFile = new File(path);
		if (!sourceFile.exists()) {
			System.out.println("File does not exist.");
			return;
		}

		if (!sourceFile.canRead()) {
			System.out.println("File is not readable");
		}

		assert (sourceFile.exists());
		assert (sourceFile.canRead());

		run(new FileCharStream(path));
	}

	static void run(ICharStream stream) {
		ILexer lexer = new Lexer(stream);
		Parser parser = new Parser(lexer);
		try {
			parser.parse();
		} catch (ParserException e) {
			e.printStackTrace();
			System.err.println("Parser failed.");
			return;
		}
		
		ISyntaxTree tree = parser.getParseTree();
		parser.printParseTree();
		
		SemanticAnalyzer analyzer = new SemanticAnalyzer(tree);
		analyzer.analyze();
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Reading from stdin. Exit with new line and Ctrl+D.");
			readStdin();
		} else if (args.length == 1) {
			final String path = args[0];
			readFile(path);
		} else {
			System.out.println("Wrong number of parameters.");
		}

	}
}
