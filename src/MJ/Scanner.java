/* MicroJava Scanner (HM 06-12-28)
   =================
*/
package MJ;
import java.io.*;
import java.util.Arrays;

public class Scanner {
	private static final char eofCh = '\u0080';
	private static final char eol = '\n';
	private static final int  // token codes
		none      = 0,
		ident     = 1,
		number    = 2,
		charCon   = 3,
		plus      = 4,
		minus     = 5,
		times     = 6,
		slash     = 7,
		rem       = 8,
		eql       = 9,
		neq       = 10,
		lss       = 11,
		leq       = 12,
		gtr       = 13,
		geq       = 14,
		assign    = 15,
		semicolon = 16,
		comma     = 17,
		period    = 18,
		lpar      = 19,
		rpar      = 20,
		lbrack    = 21,
		rbrack    = 22,
		lbrace    = 23,
		rbrace    = 24,
		class_    = 25,
		else_     = 26,
		final_    = 27,
		if_       = 28,
		new_      = 29,
		print_    = 30,
		program_  = 31,
		read_     = 32,
		return_   = 33,
		void_     = 34,
		while_    = 35,
		eof       = 36;
	private static final String key[] = { // sorted list of keywords
		"class", "else", "final", "if", "new", "print",
		"program", "read", "return", "void", "while"
	};
	private static final int keyVal[] = {
		class_, else_, final_, if_, new_, print_,
		program_, read_, return_, void_, while_
	};

	private static char ch;			// lookahead character
	public  static int col;			// current column
	public  static int line;		// current line
	private static int pos;			// current position from start of source file
	private static Reader in;  	// source file reader
	private static char[] lex;	// current lexeme (token string)

	//----- ch = next input character
	private static void nextCh() {
		try {
			ch = (char)in.read(); col++; pos++;
			if (ch == eol) {line++; col = 0;}
			else if (ch == '\uffff') ch = eofCh;
		} catch (IOException e) {
			ch = eofCh;
		}
	}

	//--------- Initialize scanner
	public static void init(Reader r) {
		in = new BufferedReader(r);
		lex = new char[64];
		line = 1; col = 0;
		nextCh();
	}

	//---------- Return next input token
	public static Token next() {
		while(ch <= ' ') nextCh();          // Skip Blanks
                Token token = new Token();          // Creates New Token
                token.line = line; token.col = col; // Inits char locale
                
                switch(ch){
                    // Alphabet
                    case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                        case 'g': case 'h': case 'i': case 'j': case 'k':
                        case 'l': case 'm': case 'n': case 'o': case 'p':
                        case 'q': case 'r': case 's': case 't': case 'u':
                        case 'v': case 'w': case 'x': case 'y': case 'z':
                    case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                        case 'G': case 'H': case 'I': case 'J': case 'K':
                        case 'L': case 'M': case 'N': case 'O': case 'P':
                        case 'Q': case 'R': case 'S': case 'T': case 'U':
                        case 'V': case 'W': case 'X': case 'Y': case 'Z':
                            readName(token); break;
                    // Numbers
                    case '0': case '1': case '2': case '3': case '4': case '5': 
                        case '6': case '7': case '8': case '9':
                            readNumber(token); break;
                    // Break characters
                    case ';': nextCh(); token.kind = semicolon; break;
                    case '.': nextCh(); token.kind = period; break;
                    case ',': nextCh(); token.kind = comma; break;
                    case eofCh: token.kind = eof; break;
                    // Comparison
                    case '=':
                        nextCh();
                        if(ch == '='){ nextCh(); token.kind = eql; }
                        else{ token.kind = assign; }
                        break;
                    case '<':
                        nextCh();
                        if(ch == '='){ nextCh(); token.kind = leq; }
                        else{ token.kind = lss; }
                        break;
                    case '>':
                        nextCh();
                        if(ch == '='){ nextCh(); token.kind = geq; }
                        else{ token.kind = gtr; } 
                        break;
                    case '!':
                        nextCh();
                        if(ch == '='){ nextCh(); token.kind = neq; }
                        else {
                            token.kind = none; 
                        }
                        break;
                    // Operators
                    case '+': nextCh(); token.kind = plus; break;
                    case '-': nextCh(); token.kind = minus; break;
                    case '*': nextCh(); token.kind = times; break;
                    case '%': nextCh(); token.kind = rem; break;
                    case '/':                               // Comment or Divide
                        nextCh(); 
                        if(ch == '/'){
                            do nextCh();
                            while(ch != '\n' || ch != eofCh);
                            token = next();
                        
                        } else { token.kind = slash; } 
                        break;
                    // Brackets / Paraenthesis / Braces
                    case '(': nextCh(); token.kind = lpar; break;
                    case ')': nextCh(); token.kind = rpar; break;
                    case '{': nextCh(); token.kind = lbrace; break;
                    case '}': nextCh(); token.kind = rbrace; break;
                    case '[': nextCh(); token.kind = lbrack; break;
                    case ']': nextCh(); token.kind = rbrack; break;
                    // Default
                    default: nextCh(); token.kind = none; break;
                } // END OF switch 
            return token;   
	} // END OF next

        private static void readName(Token token) {
            // Build The String
            StringBuilder sb = new StringBuilder(); 
            while(Character.isLetterOrDigit(ch)){ sb.append(ch); nextCh(); };
            // Checks If String Is A Keyword
            int index = Arrays.binarySearch(key, sb.toString());
            
            // If it isn't a keyword: kind == identifer
            if(index == -1) token.kind = ident;
            // else if it is a keyword: kind == keyword
            else token.kind = keyVal[index];
            
        } // END OF readName
        
        private static void readNumber(Token token) {
            // Build The Number
            StringBuilder sb = new StringBuilder();
            while(Character.isDigit(ch)){  sb.append(ch); nextCh(); }
                
            // Convert String to integer
            try{
                token.val = Integer.parseInt(sb.toString());
            } catch (NumberFormatException e){
                System.out.println("Overflow");
            }
            token.kind = number;
            
        } // END OF readNumber
        
        private static void readCharCon(Token token){
            // Build The String
            StringBuilder sb = new StringBuilder();
            do{ sb.append(ch); nextCh();}
            while(ch != '"');
            if(sb.length() > 2 || sb.charAt(0) != '\\') System.out.println("Not Constant");
            
        } // END OF readCharCon
}