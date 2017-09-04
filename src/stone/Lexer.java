package stone;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正規表現を利用した字句解析器クラス．{@link Lexer#read() read()}でトークンを取り出し
 * {@link Lexer#peek(int) peek()}で先読みする．
 * <p>
 * <u>使い方</u>
 * <pre>
 * Lexer l = new Lexer(new BufferedReader(new FileReader("sourcefileName"));
 * for (Token t; (t = l.read()) != Token.EOF; ) {
 *     System.out.println(t.getText());
 * }
 * </pre>
 *
 */
public class Lexer {
	/**
	 * Group 1が行頭空白文字を除いた文字列にマッチ．Group 2がコメントに，3が整数リテラルに，4が文字列リテラルにそれぞれマッチする．
	 * Group 1内で2から4にマッチしていなければ識別子を取り出せる．
	 */
    public static String regexPat
    // \s*は0個以上の空白文字にマッチ．その後ろに，((コメント)|(整数リテラル)|(文字列リテラル)|識別子)?
    // 文字列リテラルは，"(pat1|pat2|pat3|pat4)*"にマッチ
    //    pat1はエスケープされたダブルクォーテーション「\\"」にマッチ
    //    pat2はエスケープ2つのエスケープ「\\\\」にマッチ
    //    pat3は改行のエスケープ「\\n」にマッチ
    //    pat4はダブルクォーテーション以外の1文字にマッチ
    // 識別子内の「\|\|」は「||」とマッチ．「\p{Punct}」は任意の記号文字1文字とマッチ
    // Group 1が全体のカッコ，先頭の文字列を取り除いた部分．
    // Group 2がコメント，3が整数リテラル，4が文字列リテラル，Group 1内で2から4にマッチしていなければ識別子
        = "\\s*((//.*)|([0-9]+)|(\"(\\\\\"|\\\\\\\\|\\\\n|[^\"])*\")"
          + "|[A-Z_a-z][A-Z_a-z0-9]*|==|<=|>=|&&|\\|\\||\\p{Punct})?";
    
    private Pattern _pattern = Pattern.compile(regexPat);
    private ArrayList<Token> _queue = new ArrayList<Token>();
    
    /** 読み込むべきソース行がまだ残っていれば<code>true</code>． */
    private boolean _hasMore;
    
    private LineNumberReader _reader;

    /**
     * コンストラクタ．
     * @param r
     */
    public Lexer(Reader r) {
        _hasMore = true;
        _reader = new LineNumberReader(r);
    }
    
    /**
     * ソースコードからトークンを1個読み出して返す．ソースコードの終わりに達した場合は{@link Token#EOF Token.EOF}を返す．
     * 次に{@link Lexer#read() read()}を読み出した時は，次のトークンが返る．
     * 
     * @return
     * @throws ParseException
     */
    public Token read() throws ParseException {
        if (_fillQueue(0)) // ソースコードからトークンを1個読み込む．
            return _queue.remove(0); // キューの先頭のトークンを読み出して削除する．
        else
            return Token.EOF;
    }
    
    /**
     * ソースコードの現在位置から<code>i</code>番目のトークンをpeekする．<code>i</code>は0から開始．
     * 
     * @param i
     * @return
     * @throws ParseException
     */
    public Token peek(int i) throws ParseException {
        if (_fillQueue(i)) // ソースコードからトークンを<code>i+1</code>個読み込む．
            return _queue.get(i); // peakはキューから読み出したトークンを削除しない！
        else
            return Token.EOF; 
    }
    
    //================== Private or Protected ==========================
    
    /**
     * 引数で与えられた数プラス1のトークン数がキューに格納されるまでソース行を読み続け<code>true</code>
     * を返す．もし，読み込むべきトークンがなければ<code>false</code>を返す．
     * <p>
     * このメソッドが<code>true</code>を返せば，<code>i</code>番目のトークンがキューにあることが
     * 保証される．
     * 
     * @param i 読み込み終了するトークン数マイナス1
     * @return
     * @throws ParseException
     */
    private boolean _fillQueue(int i) throws ParseException {
        while (i >= _queue.size())
            if (_hasMore)
                p_readLine();
            else
                return false;
        return true;
    }
    
    /**
     * 1行読み込み，その行のトークンを全て<code>ArrayList</code>のキューに格納する． 
     * @throws ParseException
     */
    protected void p_readLine() throws ParseException {
        String line;
        try {
            line = _reader.readLine();
        } catch (IOException e) {
            throw new ParseException(e);
        }
        if (line == null) {
            _hasMore = false;
            return;
        }
        int lineNo = _reader.getLineNumber();
        Matcher matcher = _pattern.matcher(line);
        matcher.useTransparentBounds(true).useAnchoringBounds(false);
        int pos = 0;
        int endPos = line.length();
        while (pos < endPos) {
            matcher.region(pos, endPos);
            if (matcher.lookingAt()) {
                p_addToken(lineNo, matcher);
                pos = matcher.end();
            }
            else
                throw new ParseException("bad token at line " + lineNo);
        }
        _queue.add(new IdToken(lineNo, Token.EOL));
    }
    
    /**
     * 正規表現にマッチした文字列を取り出しトークンを切り出す．
     * 
     * @param lineNo
     * @param matcher
     */
    protected void p_addToken(int lineNo, Matcher matcher) {
        String m = matcher.group(1);
        if (m != null) // if not a space
            if (matcher.group(2) == null) { // if not a comment, コメントは無視
                Token token;
                if (matcher.group(3) != null) // if 整数リテラル
                    token = new NumToken(lineNo, Integer.parseInt(m));
                else if (matcher.group(4) != null) // if 文字列リテラル
                    token = new StrToken(lineNo, p_toStringLiteral(m));
                else
                    token = new IdToken(lineNo, m); // 識別子
                _queue.add(token);
            }
    }
    
    /**
     * 文字列リテラルのエスケープを取り除き，元の文字列を復元する．
     * 
     * @param s
     * @return
     */
    protected String p_toStringLiteral(String s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length() - 1;
        for (int i = 1; i < len; i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < len) {
                int c2 = s.charAt(i + 1);
                if (c2 == '"' || c2 == '\\')
                    c = s.charAt(++i);
                else if (c2 == 'n') {
                    ++i;
                    c = '\n';
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    protected static class NumToken extends Token {
        private int value;

        protected NumToken(int line, int v) {
            super(line);
            value = v;
        }
        public boolean isNumber() { return true; }
        public String getText() { return Integer.toString(value); }
        public int getNumber() { return value; }
    }

    protected static class IdToken extends Token {
        private String text; 
        protected IdToken(int line, String id) {
            super(line);
            text = id;
        }
        public boolean isIdentifier() { return true; }
        public String getText() { return text; }
    }

    protected static class StrToken extends Token {
        private String literal;
        StrToken(int line, String str) {
            super(line);
            literal = str;
        }
        public boolean isString() { return true; }
        public String getText() { return literal; }
    }
}
