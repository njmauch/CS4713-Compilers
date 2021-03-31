package pickle;

import java.util.ArrayList;
import java.util.Stack;

public class Parser{
    public Scanner scan;
    public SymbolTable symbolTable;
    public String sourceFileNm;
    public StorageManager smStorage;

    public boolean bShowExpr;
    public boolean bShowAssign;
    public boolean bShowStmt;


    Parser(Scanner scan, StorageManager storageManager, SymbolTable symbolTable) {
        this.scan = scan;
        this.symbolTable = symbolTable;
        this.sourceFileNm = scan.sourceFileNm;
        this.smStorage = storageManager;

        this.bShowExpr = false;
        this.bShowAssign = false;
        this.bShowStmt = false;
    }

    public void getNext() throws Exception {
        while (! scan.getNext().isEmpty()) {
            if (scan.currentToken.primClassif == Classif.EOF) {
                return;
            }
            if (scan.currentToken.primClassif.equals(Classif.OPERAND)) {
                assigmentStmt();
            }
            else if ((scan.currentToken.primClassif == Classif.CONTROL) && (scan.currentToken.subClassif == SubClassif.DECLARE)){
                declareStmt();
            }
            else if (scan.currentToken.primClassif.equals(Classif.FUNCTION)) {
                functionStmt();
            } else if (scan.currentToken.primClassif.equals(Classif.CONTROL)) {
                controlStmt();
            } else if (scan.currentToken.primClassif.equals(Classif.OPERATOR)) {
                error("Can't start with operator", scan.currentToken);
            }
            else {
                error("Unknown token", scan.currentToken);
            }
        }
    }

    private void skipTo(String tokenStr) throws Exception {
        while(!scan.getNext().equals(tokenStr));
    }

    public void error(String fmt, Object... varArgs) throws Exception
    {
        String diagnosticTxt = String.format(fmt, varArgs);
        throw new ParserException(Scanner.iSourceLineNr, diagnosticTxt, this.sourceFileNm);
    }

    private ResultValue statements (boolean bExec) throws Exception {
        ResultValue res = new ResultValue();

        while (! scan.getNext().isEmpty()){
            scan.getNext();
            if (scan.currentToken.primClassif == Classif.EOF){
                return res;
            }

            //Assign Value;
            if (scan.currentToken.primClassif == Classif.OPERAND){
                assigmentStmt();
            }
            else if ((scan.currentToken.primClassif == Classif.CONTROL) && (scan.currentToken.subClassif == SubClassif.END)){
                res.type = SubClassif.END;
                res.terminatingStr = scan.currentToken.tokenStr;
                return res;
            }
            else if ((scan.currentToken.primClassif == Classif.CONTROL) && (scan.currentToken.subClassif == SubClassif.DECLARE)){
                declareStmt();
            }
            else if (scan.currentToken.primClassif == Classif.CONTROL) {
                controlStmt();
            }
            else if (scan.currentToken.primClassif == Classif.FUNCTION){
                functionStmt();
            }
            else if (scan.currentToken.primClassif == Classif.OPERATOR) {
                error("Can't start with operator");
            }
            else {
                error("Invalid token");
            }
        }
        return res;
    }

    private void functionStmt () throws Exception {
        if(scan.currentToken.subClassif == SubClassif.BUILTIN) {
            if(scan.currentToken.tokenStr.equals("print")) {
                print();
            }
            else {
                error("No function found with name %s", scan.currentToken.tokenStr);
            }
        }
    }

    private void print() throws Exception {
        scan.getNext();
        StringBuilder printStr = new StringBuilder();
        int parenCount = 0;
        if(! scan.currentToken.tokenStr.equals("(")) {
            error("Missing open paren");
        }
        parenCount++;
        scan.getNext();
        while(parenCount > 0) {

            if(scan.nextToken.tokenStr.equals(";")) {
                error("No closing paren found");
            }
            if(scan.nextToken.tokenStr.equals(")")) {
                parenCount--;
            }
            else if (scan.nextToken.tokenStr.equals("(")) {
                parenCount++;
            }
            printStr.append(scan.currentToken.tokenStr);
            scan.getNext();
        }
        System.out.println(printStr.toString());
        scan.getNext();
    }

    public ResultValue controlStmt() throws Exception {
        ResultValue res = new ResultValue();
        while (true) {
            scan.getNext();

            if (scan.currentToken.primClassif == Classif.EOF) {
                return res;
            }
            if ((scan.currentToken.primClassif == Classif.CONTROL) && (scan.currentToken.subClassif == SubClassif.FLOW)) {
                if (scan.currentToken.tokenStr.equals("if")) {
                    ifStmt(true);
                    break;
                } else if (scan.currentToken.tokenStr.equals("while")) {
                    whileStmt(true);
                    break;
                }
            }
        }
        res.terminatingStr = scan.getNext();
        return res;
    }

    public void whileStmt(boolean bExec) throws Exception {
        ResultValue res;
        Token tempToken;

        tempToken = scan.currentToken;
        if (bExec) {
            ResultValue res01 = expr();
            if (!scan.currentToken.tokenStr.equals(":")) {
                error("Expected ':' after while");
            }
            if (res01.type != SubClassif.BOOLEAN) {
                error("Expected boolean");
            }
            while (res01.value.equals("T")) {
                res = statements(true);
                if (!res.terminatingStr.equals("endwhile")) {
                    error("No endwhile found");
                }
                if (!scan.getNext().equals(";")) {
                    error("Expected ';' after endwhile");
                }
                scan.setPosition(tempToken);
                res01 = expr();
            }
        } else {
            skipTo(":");
            res = statements(false);
        }
        if (!scan.currentToken.tokenStr.equals("endwhile")) {
            error("Expected endwhile");
        }
        if (!scan.currentToken.tokenStr.equals(";")) {
            error("Expected ';' after endwhile ");
        }
    }

    private ResultValue declareStmt() throws Exception {
        ResultValue res;

        SubClassif dclType = SubClassif.EMPTY;

        if(scan.currentToken.tokenStr.equals("Int")) {
            dclType = SubClassif.INTEGER;
        }
        else if (scan.currentToken.tokenStr.equals("Float")) {
            dclType = SubClassif.FLOAT;
        }
        else if (scan.currentToken.tokenStr.equals("String")) {
            dclType = SubClassif.STRING;
        }
        else if (scan.currentToken.tokenStr.equals("Bool")) {
            dclType = SubClassif.BOOLEAN;
        }
        else {
            error("Unknown declare type %s", scan.currentToken.tokenStr);
        }
        scan.getNext();

        if((scan.currentToken.primClassif != Classif.OPERAND) || (scan.currentToken.subClassif != SubClassif.IDENTIFIER))  {
            error("Expected variable for target %s", scan.currentToken.tokenStr);
        }

        String variableStr = scan.currentToken.tokenStr;
        res = new ResultValue(dclType, variableStr, "primitive");

        if(scan.getNext().equals("=")){
            res = expr();
        }
        if(! scan.currentToken.tokenStr.equals(";")) {
            error("Expected ';' at end of statement");
        }

        SymbolTable.STEntry stEntry = symbolTable.getSymbol(variableStr);

        symbolTable.putSymbol(variableStr, new SymbolTable.STIdentifier(variableStr, Classif.OPERAND, dclType, dclType, "primitive"));

        return res;

    }

    public ResultValue assigmentStmt() throws Exception {
        ResultValue res = new ResultValue();
        if(scan.currentToken.subClassif != SubClassif.IDENTIFIER) {
            error("Expected a variable for the target assignment %s", scan.currentToken.tokenStr);
        }
        String variableStr = scan.currentToken.tokenStr;
        scan.getNext();
        if(scan.currentToken.primClassif != Classif.OPERATOR) {
            error("expected assignment operator %s", scan.currentToken.tokenStr);
        }
        scan.getNext();
        String operatorStr = scan.currentToken.tokenStr;
        ResultValue resO2;
        ResultValue resO1;

        switch(operatorStr) {
            case "=":
                resO2 = expr();
                res = assign(variableStr, resO2);
                break;
            case "-=":
                resO2 = expr();
                resO1 = this.smStorage.getValue(variableStr);
                res = assign(variableStr, Utility.subtraction(this, resO1, resO2));
                break;
            case "+=":
                resO2 = expr();
                resO1 = this.smStorage.getValue(variableStr);
                res = assign(variableStr, Utility.addition(this, resO1, resO2));
            default:
                error("expected assignment operator");
        }
        return res;
    }

    private ResultValue expr() throws Exception{
        ResultValue res = null;
        Token popToken = null;

        ArrayList<Token> outStack = new ArrayList<Token>();
        Stack<Token> stack = new Stack<Token>();
        Stack<ResultValue> resultStack = new Stack<ResultValue>();

        scan.getNext();

        return res;
    }

    private ResultValue products() throws Exception
    {
        ResultValue res = operand();                    // Rule 3
        ResultValue temp = new ResultValue();
        while (scan.currentToken.tokenStr.equals("*") ) // * from rule 4.1
        {
            scan.getNext();
            if (scan.currentToken.primClassif != Classif.OPERAND)
                error("Within expression, expected operand.  Found: '%s'"
                        , scan.currentToken.tokenStr);
            temp = operand();                           // Rule 4.1
            res = Utility.multiplication(this, res, temp);
        }
        return res;
    }

    private ResultValue evalCond() throws Exception {
        scan.getNext();

        ResultValue resO1 = null;
        ResultValue resO2 = null;
        ResultValue res = new ResultValue();
        String opStr;
        Token opToken;

        if(scan.currentToken.primClassif != Classif.OPERATOR) {
            resO1 = expr();
        }

        opStr = scan.currentToken.tokenStr;
        opToken = scan.currentToken;

        scan.getNext();
        resO2 = expr();

        if(opStr.equals(">")) {
            res = Utility.greaterThan(this, resO1, resO2);
        }
        else if (opStr.equals("<")) {
            res = Utility.lessThan(this, resO1, resO2);
        }
        else if (opStr.equals(">=")) {
            res = Utility.greaterThanOrEqual(this, resO1, resO2);
        }
        else if (opStr.equals("<=")) {
            res = Utility.lessThanOrEqual(this, resO1, resO2);
        }
        else if (opStr.equals("==")) {
            res = Utility.equal(this, resO1, resO2);
        }
        else if (opStr.equals("!=")) {
            res = Utility.notEqual(this, resO1, resO2);
        }
        else {
            error("Bad compare token");
        }
        return res;
    }

    void ifStmt(Boolean bExec) throws Exception {
        int saveLineNr = scan.currentToken.iSourceLineNr;
        if (bExec) {
            ResultValue resCond = evalCond();
            if (resCond.value.equals("T")) {
                ResultValue resTemp = statements(true);
                if (resTemp.terminatingStr.equals("else")) {
                    if (!scan.getNext().equals(":")) {
                        error("expected a ‘:’after ‘else’");
                    }
                    resTemp = statements(false);
                }
                if (!resTemp.terminatingStr.equals("endif")) {
                    error("expected a ‘endif’ for an ‘if’");
                }
                if (!scan.getNext().equals(";")) {
                    error("expected a ‘;’after ‘endif’");
                }
            } else {
                ResultValue resTemp = statements(false);
                if (resTemp.terminatingStr.equals("else")) {
                    if (!scan.getNext().equals(":")) {
                        error("expected a ‘:’after ‘else’");
                    }
                    resTemp = statements(true);
                }
                if (!resTemp.terminatingStr.equals("endif")) {
                    error("expected a ‘endif’ for an ‘if’");
                }
                if (!scan.getNext().equals(";")) {
                    error("expected a ‘;’after ‘endif’");
                }
            }
        }
        else {
            skipTo(":");
            ResultValue resTemp = statements(false);
            if (resTemp.terminatingStr.equals("else")) {
                if (!scan.getNext().equals(":")) {
                    error("expected a ‘:’after ‘else’");
                }
                resTemp = statements(false);
            }
            if(!resTemp.terminatingStr.equals("endif")) {
                error("expected a ‘endif’ for an ‘if’");
            }
            if (!scan.getNext().equals(";")) {
                error("expected a ‘;’after ‘endif’");
            }
        }
    }



    private ResultValue operand() throws Exception
    {
        ResultValue res;
        if (scan.currentToken.primClassif == Classif.OPERAND)
        {
            switch (scan.currentToken.subClassif)
            {
                case IDENTIFIER:
                    res = smStorage.getValue(scan.currentToken.tokenStr);
                    scan.getNext();
                    return res;
                case INTEGER:
                case FLOAT:
                case STRING:
                case BOOLEAN:
                    res = scan.currentToken.toResult(this);  // Rule 5.1
                    scan.getNext();                     // nextToken is operator or sep
                    return res;
            }

        }
        error("Within operand, found: '%s'"
                , scan.currentToken.tokenStr);
        return null; // fake for Java compiler
    }


    private ResultValue assign(String variableStr, ResultValue res) throws Exception {
        switch(res.type) {
            case INTEGER:
                res.value = Utility.castInt(this, res);
                res.type = SubClassif.INTEGER;
                break;
            case FLOAT:
                res.value = Utility.castFloat(this, res);
                res.type = SubClassif.FLOAT;
                break;
            case BOOLEAN:
                res.value = Utility.castBoolean(this, res);
                res.type = SubClassif.BOOLEAN;
                break;
            case STRING:
                res.type = SubClassif.STRING;
                break;
            default:
                error("Assign type is incompatible");
        }
        smStorage.insertValue(variableStr, res);

        return res;
    }


}