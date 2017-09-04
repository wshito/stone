package chap3;
import stone.*;

/**
 * 第3章の字句解析器クラス{@link stone.Lexer}の動作テストクラス．
 */
public class LexerRunner {
    public static void main(String[] args) throws ParseException {
        Lexer l = new Lexer(new CodeDialog());
        for (Token t; (t = l.read()) != Token.EOF; )
            System.out.println("=> " + t.getText());
    }
}
