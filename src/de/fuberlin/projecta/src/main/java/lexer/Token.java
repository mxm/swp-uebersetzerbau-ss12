package lexer;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public
@Data
class Token implements IToken {

	private TokenType type;

	private Object attribute;

	private int lineNumber;
	private int offset;

	@Override
	public String toString() {
		return "<" + type + ", " + attribute + ", " + lineNumber + ", "
				+ offset + ">";
	}

}
