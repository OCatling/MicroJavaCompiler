/*  MicroJava Parser (HM 06-12-28)
    ================
*/
package MJ;

import java.util.*;
import MJ.SymTab.*;
import MJ.CodeGen.*;

public class Parser {
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
	private static final String[] name = { // token names for error messages
		"none", "identifier", "number", "char constant", "+", "-", "*", "/", "%",
		"==", "!=", "<", "<=", ">", ">=", "=", ";", ",", ".", "(", ")",
		"[", "]", "{", "}", "class", "else", "final", "if", "new", "print",
		"program", "read", "return", "void", "while", "eof"
		};

	private static Token t;			// current token (recently recognized)
	private static Token la;		// lookahead token
	private static int sym;			// always contains la.kind
	public  static int errors;  // error counter
	private static int errDist;	// no. of correctly recognized tokens since last error

	private static BitSet exprStart, statStart, statSync, statSeqFollow, declSync, declFollow;

	//------------------- auxiliary methods ----------------------
	private static void scan() {
		t = la;
		la = Scanner.next();
		sym = la.kind;
		errDist++;
		/*
		System.out.print("line " + la.line + ", col " + la.col + ": " + name[sym]);
		if (sym == ident) System.out.print(" (" + la.string + ")");
		if (sym == number || sym == charCon) System.out.print(" (" + la.val + ")");
		System.out.println();*/
	}

	private static void check(int expected) {
		if (sym == expected) scan();
		else error(name[expected] + " expected");
	}

	public static void error(String msg) { // syntactic error at token la
		if (errDist >= 3) {
			System.out.println("-- line " + la.line + " col " + la.col + ": " + msg);
			errors++;
		}
		errDist = 0;
	}

	//-------------- parsing methods (in alphabetical order) -----------------

	// "program" ident {ConstDecl | ClassDecl | VarDecl} '{' {MethodDecl} '}'.
	private static void Program() {
            check(program_);
            check(ident);
            Tab.openScope();
            for(;;){
                if(sym == class_) ClassDecl();
                else if(sym == final_) ConstDecl();
                else if(sym == ident) VarDecl();
                else if (sym == lbrace || sym == eof) break;
                else {
                    error("invalid declaration");
                    while (!declSync.get(sym)) scan();
                    errDist = 0;
                }
            }
            check(lbrace);
            while(sym == ident || sym == void_) MethodDecl();
            check(rbrace);
            Tab.dumpScope(Tab.curScope.locals);
            Tab.closeScope();
	}
        
        // "final" Type ident "=" (number | charConst) ";"
        private static void ConstDecl(){
            check(final_);
            Struct type = Type(); // Get Type Identifier 
            check(ident);
            Tab.insert(Obj.Con, t.string, type); // Insert New Symbol to Table
            check(assign);
            if(sym == number) scan();
            else if(sym == charCon) scan();
            else error("Invalid Initilisation");
            check(semicolon);
        }
        
        // Type ident {"," ident } ";"
        private static void VarDecl(){
            Struct type = Type(); // Get Type Identifier 
            check(ident);
            Tab.insert(Obj.Var, t.string, type); // Insert New Symbol to Table
            while(sym == comma){ 
                scan();
                check(ident); 
                Tab.insert(Obj.Var, t.string, type); // Insert New Symbol to Table 
                
            } 
            check(semicolon);
        }
        
        // = "class" ident "{" {VarDecl} "}"
        private static void ClassDecl(){
            check(class_);
            Struct type;
            check(ident);
            check(lbrace);
            Tab.openScope();
            while(sym == ident) VarDecl();
            check(rbrace);
            Tab.dumpScope(Tab.curScope.locals);
            Tab.closeScope();
        }
        
        //(Type | "void") ident "(" [FormPars] ")" {VarDecl} Block.
        private static void MethodDecl(){
            Tab.openScope();
            if(sym == void_) {
                scan();
                Tab.insert(Obj.Meth, t.string, Tab.noObj.type);
            }
            else if(sym == ident) {
                Struct type = Type(); // Get Type Identifier
                Tab.insert(Obj.Meth, t.string, type); // Insert New Symbol to Tabless
            }
            else error("Type Or Void Expected");
            check(ident);
            check(lpar);
            if(sym == ident) FormPars();
            check(rpar);
            while(sym == ident) VarDecl();
            Block();
            Tab.closeScope();
        }
        
        // Type ident {"," Type ident}
        private static void FormPars(){
            Struct type = Type(); // Get Type Identifier
            Tab.insert(Obj.Type, t.string, type); // Insert New Symbol to Tabless
            check(ident);
            while(sym == comma) { 
                scan(); 
                type = Type(); // Get Type Identifier
                check(ident); 
                Tab.insert(Obj.Type, t.string, type); // Insert New Symbol to Tabless
            }
        }
        
        // ident ["[" "]"]
        private static Struct Type(){
            check(ident);
            Obj obj = Tab.find(t.string);
            Struct type = obj.type;
            if(sym == lbrack) { 
                scan(); 
                check(rbrack); 
                type = new Struct(Struct.Arr, type);
            }
            return type;
        }
        
        // "{" {Statement} "}"
        private static void Block(){
            check(lbrace);
            while(sym != rbrace && sym != eof){ Statement(); }
            check(rbrace);
        }
        
        // = Designator ("=" Expr | ActPars) ";"
        // | "if" "(" Condition ")" Statement ["else" Statement]
        // | "while" "(" Condition ")" Statement
        // | "return" [Expr] ";"
        // | "read" "(" Designator ")" ";"
        // | "print" "(" Expr ["," number] ")" ";"
        // | Block
        // | ";"
        private static void Statement(){
            if(!statStart.get(sym)){
                error("Invalid Start Of Statement");
                while(!statSync.get(sym)) scan();
                if (sym == semicolon) scan();
                errDist = 0;
            }
            
            // Check Designator Statement
            if(sym == ident){
                Designator();
                if(sym == assign) { scan(); Expr(); }
                else if(sym == lbrace) ActPars();
                else error("Assignment Operator or Parameters Expected");
                check(semicolon);
            
            // Check If Statement
            } else if(sym == if_){
                scan();
                check(lpar);
                Condition();
                check(rpar);
                Statement();
                if(sym == else_){ scan(); Statement();}
            
            // Check While Loop Statement
            } else if( sym == while_) {
                scan();
                check(lpar);
                Condition();
                check(rpar);
                Statement();
            
            // Check Return Statement
            } else if(sym == return_) {
                scan();
                if(exprStart.get(sym)) Expr();
                else error("No Return Value Given");
                check(semicolon);
            
            // Check Read Statement
            } else if(sym == read_){
                scan();
                check(lpar);
                Designator();
                check(rpar);
                check(semicolon);
            
            // Check Print Statement
            } else if(sym == print_){
                scan();
                check(lpar);
                Expr();
                if(sym == comma) {scan(); check(number);}
                check(rpar);
                check(semicolon);
            
            // Check Block
            } else if(sym == lbrace){
                Block();
            
            // Check End Of Statement
            } else if(sym == semicolon){
                scan();
            }
            
            
            
        }
        
        // "(" [ Expr {"," Expr} ] ")"
        private static void ActPars(){
            check(lbrace);
            if(exprStart.get(sym)) {
                Expr();
                while(sym == comma){
                    scan();
                    Expr();
                }
            }
            check(rbrace);
        }
        
        // Expr Relop Expr
        private static void Condition(){
            Operand operandX, operandY;
            operandX = Expr();
            Code.load(operandX);
            int operator = Relop();
            operandY = Expr();
            Code.load(operandY);
            if(!operandX.type.compatibleWith(operandY.type)) 
                error("Type Mismatch");
            if(operandX.type.isRefType() && operator != Code.eq 
                    && operator != Code.ne) 
                error("Invalid Compare");
        }
        
        // "==" | "!=" | ">" | ">=" | "<" | "<="
        private static int Relop(){
            if(sym == eql) {
                scan();
                return Code.eq;
            }
            else if(sym == neq) {
                scan();
                return Code.
            }
            else if(sym == gtr) scan();
            else if(sym == geq) scan();
            else if(sym == lss) scan();
            else if(sym == leq) scan();
            else error("Invalid Opperand");
        }
        
        // ["-"] Term {Addop Term}.
        private static Operand Expr(){
            Operand operandX, operandY; int op;
            if(sym == minus) scan();
            operandX = Term();
            if(operandX.type == Tab.intType){
                if(operandX.kind == Operand.Con){
                   operandX.val = -operandX.val;                
                } else {
                    Code.load(operandX);
                    Code.put(Code.neg);
                }
            } else error("Operand must be of type int"); 
            op = Addop();
            while(sym == plus || sym == minus){
                op = Addop();
                operandY = Term();
                Code.load(operandX);
                Code.load(operandY);
            }
            return operandX;
        }
        
        // Factor {Mulop Factor} 
        private static Operand Term(){
            Operand operandY, operandX = Factor(); 
            int operator;
            scan();
            while(sym == times || sym == slash || sym == rem){ 
                operator = Mulop();
                Code.load(operandX);
                operandY = Factor();
                Code.load(operandY);
                if(operandX.type != Tab.intType || operandY.type != Tab.intType) 
                    error("Operands must be of type int");
                Code.put(operator);
            }
            return operandX;
        } // END OF Term
        
        // Designator [ActPars]
        // | number
        // | charConst
        // | "new" ident ["[" Expr "]"]
        // | "(" Expr ")".
        private static Operand Factor(){
            Operand operand = new Operand(Tab.noObj);
            Obj object;
            if(sym == ident) { 
                scan();
                operand = Designator();
                if(sym == lpar) {
                    scan();
                    if(operand.kind == Operand.Meth){
                        ActPars();
                    }
                }
            } 
            else if(sym == number) {
                scan();
                operand.val = t.val;
                Code.load(operand);
                operand.type = Tab.intType;
            }
            else if(sym == charCon) {
                scan();
                operand.val = t.val;
                Code.load(operand);
                operand.type = Tab.charType;
            }
            else if(sym == new_) { 
                scan();
                check(ident);
                object = Tab.find(t.string);
                Struct type = object.type;
                if(sym == lbrack){ 
                    scan(); 
                    // Checks array contains int
                    if(Expr().type != Tab.intType) error("Must Be An Int");
                    check(rbrack); 
                    type = new Struct(Struct.Arr, type);
                    Code.put(Code.newarray);
                    if(type.elemType == Tab.charType) Code.put(0);
                    else Code.put(1);
                } else {
                    if(type.kind != Struct.Class) error("Illegal Inistialisation");
                    Code.put(Code.new_); 
                    Code.put2(type.fields.nPars);
                }
                operand.kind = Operand.Stack;
                operand.type = type;
            }
            else if(sym == lpar) { 
                scan(); 
                operand = Expr();
                check(rpar); }  
            else error("Invalid Factor");
            return operand;
        } // END OF Factor
        
        // ident {"." ident | "[" Expr "]"}
        private static Operand Designator(){
            check(ident);
            Obj obj = Tab.find(t.string); // Find the object
            Operand x = new Operand(obj);
            for(;;){
                if(sym == period){
                    scan(); 
                    check(ident);
                    if(x.type.kind == Struct.Class){
                        Code.load(x);
                        obj = Tab.findField(t.string, x.type); // Find field
                        x.kind = Operand.Fld;
                        x.adr = obj.adr;
                        x.type = obj.type;
                    
                    }  else error("Invalid field access"); 
                
                }  else if(sym == lbrack){ 
                    scan(); 
                    Expr(); 
                    if(x.type.kind == Struct.Arr){
                        Code.load(x);
                        x.kind = Operand.Elem;
                        x.adr = obj.adr;
                        x.type = obj.type; 
                       check(rbrack);
                    } else error("Invalid Array Access");
                
                } else break;
            
            } // END OF for Loop
            return x;
        } // END OF Designator
        
        // "+" | "-"
        private static int Addop(){
            int returnVal;
            if(sym == plus) {
                scan();
                returnVal = Code.add;
            }
            else if(sym == minus) {
                scan();
                returnVal = Code.sub;
            }
            else {
                error("Expecting + or -");
                returnVal = Code.trap;
            }
            return returnVal;
        }
        
        // "*" | "/" | "%"
        private static int Mulop(){
            int returnVal;
            if(sym == times) {
                scan();
                returnVal = Code.mul;
            }
            else if(sym == slash) {
                scan();
                returnVal = Code.div;
            }
            else if(sym == rem){
                scan();
                returnVal = Code.rem;
            }
            else {
                error("Expecting *, / or %");
                returnVal = Code.trap;
            }
            return returnVal;
        }

	public static void parse() {
		// initialize symbol sets
                Tab.init();
                Code.init();
                
		BitSet s;
		s = new BitSet(64); exprStart = s;
		s.set(ident); s.set(number); s.set(charCon); s.set(new_); s.set(lpar); s.set(minus);

		s = new BitSet(64); statStart = s;
		s.set(ident); s.set(if_); s.set(while_); s.set(read_);
		s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);

		s = new BitSet(64); statSync = s;
		s.set(eof); s.set(if_); s.set(while_); s.set(read_);
		s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);

		s = new BitSet(64); statSeqFollow = s;
		s.set(rbrace); s.set(eof);

		s = new BitSet(64); declSync = s;
		s.set(final_); s.set(ident); s.set(class_);
		s.set(lbrace); s.set(eof);

		// start parsing
		errors = 0; errDist = 3;
		scan();
		Program();
		if (sym != eof) error("end of file found before end of program");
	}

}







