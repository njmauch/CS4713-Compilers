package pickle;

import javax.xml.transform.Result;
import java.io.IOException;

public class Parser{
    public Scanner scan;
    public SymbolTable symbolTable;
    public String sourceFileNm;
    public StorageManager smStorage;

    public class ParseResult{
        SubClassif resultType;
        Token token;
        ParseResult sibling;
    }


    Parser(Scanner scan, SymbolTable symbolTable)
    {
        this.scan = scan;
        this.symbolTable = symbolTable;
        this.sourceFileNm = scan.sourceFileNm;
        this.smStorage = new StorageManager();
    }

    private ResultValue statements (boolean bExec) throws Exception, IOException {
        ResultValue res = new ResultValue();

        while (! scan.getNext().isEmpty()){
            scan.getNext();
            if (scan.currentToken.primClassif == Classif.EOF){
                return res;
            }

            //Assign Value;
            if (scan.currentToken.primClassif == Classif.OPERAND){
                //if (debugStmt()) {
                 //   break;
                //}
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
        ResultValue res = new ResultValue();
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

            if(scan.currentToken.tokenStr.equals(";")) {
                error("No closing paren found");
            }
            if(scan.currentToken.tokenStr.equals(")")) {
                parenCount--;
            }
            else if (scan.currentToken.tokenStr.equals("(")) {
                parenCount++;
            }
            printStr.append(scan.currentToken.tokenStr);
            scan.getNext();
        }
        System.out.println(printStr.toString());
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

    private void declareStmt() throws Exception {
        ResultValue res = new ResultValue();
        String declareStr = scan.currentToken.tokenStr;
        Token declareToken = scan.currentToken;


        if(declareStr == "Int") {
            declareToken.subClassif = SubClassif.INTEGER;
        }
        else if(declareStr == "Float") {
            declareToken.subClassif = SubClassif.FLOAT;
        }
        else if(declareStr == "String") {
            declareToken.subClassif = SubClassif.STRING;
        }
        else if(declareStr == "Bool") {
            declareToken.subClassif = SubClassif.BOOLEAN;
        }
        else {
            declareToken.subClassif = SubClassif.EMPTY;
        }
        scan.getNext();
        if(scan.currentToken.tokenStr.equals("=")) {
            if ((scan.currentToken.primClassif != Classif.OPERAND) || (scan.currentToken.subClassif == SubClassif.IDENTIFIER)) {
                error("Expected variable");
            }
            res = expr();
        }
    }

    ResultValue assigmentStmt() throws Exception {
        ResultValue res = new ResultValue();
        if(scan.currentToken.subClassif != SubClassif.IDENTIFIER) {
            error("Expected a variable for the target assignment", scan.currentToken.tokenStr);
        }
        String variableStr = scan.currentToken.tokenStr;
        scan.getNext();
        if(scan.currentToken.primClassif != Classif.OPERATOR) {
            error("expected assignment operator", scan.currentToken.tokenStr);
        }
        String operatorStr = scan.currentToken.tokenStr;
        ResultValue resO2;
        ResultValue resO1;
        Numeric nOp2;  // numeric value of second operand
        Numeric nOp1;  // numeric value of first operand
        switch(operatorStr) {
            case "=":
                resO2 = expr();
                res = assign(variableStr, resO2);
                break;
            case "-=":
                resO2 = expr();
                nOp2 = new Numeric(this, resO2, "-=", "2nd Operand");
                resO1 = this.smStorage.getValue(variableStr);
                nOp1 = new Numeric(this, resO1, "-=", "1st Operand");
                res = assign(variableStr, Utility.subtraction(this, nOp1, nOp2);
                break;
            case "+=":
                resO2 = expr();
                nOp2 = new Numeric(this, resO2, "+=", "2nd Operand");
                resO1 = this.smStorage.getValue(variableStr);
                nOp1 = new Numeric(this, resO1, "+=", "1st Operand");
                res = assign(variableStr, Utility.addition(this, nOp1, nOp2);
            default:
                error("expected assignment operator");
        }
        return res;
    }

    private ResultValue expr() throws Exception{
        scan.getNext();
        ResultValue res = products();
        ResultValue temp;
        while(scan.currentToken.tokenStr.equals("+")) {
            scan.getNext();
            if(scan.currentToken.primClassif != Classif.OPERAND){
                error("Within expression, expected operand. Found: '%s'", scan.currentToken.tokenStr);
            }
            temp = products();
            res = Utility.addition(this, res, temp);
        }
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

        ResultValue res01 = null;
        ResultValue res02 = null;
        ResultValue res = new ResultValue();
        String opStr;
        Token opToken;

        if(scan.currentToken.primClassif != Classif.OPERATOR) {
            res01 = expr();
        }

        opStr = scan.currentToken.tokenStr;
        opToken = scan.currentToken;

        scan.getNext();
        res02 = expr();

        if(opStr == ">") {
            res = Utility.greaterThan(scan, res01, res02);
        }
        else if (opStr == "<") {
            res = Utility.lessThan(scan, res01, res02);
        }
        else if (opStr == ">=" ) {
            res = Utility.greaterThanOrEqual(scan, res01, res02);
        }
        else if (opStr == "<=" ) {
            res = Utility.lessThanOrEqual(scan, res01, res02);
        }
        else if (opStr == "==" ) {
            res = Utility.equal(scan, res01, res02);
        }
        else if (opStr == "!=" ) {
            res = Utility.notEqual(scan, res01, res02);
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
            if (resCond.value == "T") {
                ResultValue resTemp = executeStatements("T");
                if (resTemp.terminatingStr == "else") {
                    if (scan.getNext() != ":") {
                        error("expected a ‘:’after ‘else’");
                    }
                    resTemp = executeStatments("F");
                }
                if (resTemp.terminatingStr != "endif") {
                    error("expected a ‘endif’ for an ‘if’");
                }
                if (scan.getNext() != ";") {
                    error("expected a ‘;’after ‘endif’");
                }
            } else {
                ResultValue resTemp = executeStatements("F");
                if (resTemp.terminatingStr == "else") {
                    if (scan.getNext() != ":") {
                        error("expected a ‘:’after ‘else’");
                    }
                    resTemp = executeStatments("T");
                }
                if (resTemp.terminatingStr != "endif") {
                    error("expected a ‘endif’ for an ‘if’");
                }
                if (scan.getNext() != ";") {
                    error("expected a ‘;’after ‘endif’");
                }
            }
        }
        else {
            skipTo(":");
            ResultValue resTemp = executeStatements("F");
            if (resTemp.terminatingStr == "else") {
                if (scan.getNext() != ":") {
                    error("expected a ‘:’after ‘else’");
                }
                resTemp = executeStatments("F");
            }
            if(resTemp.terminatingStr != "endif") {
                error("expected a ‘endif’ for an ‘if’");
            }
            if (scan.getNext() != ";") {
                error("expected a ‘;’after ‘endif’");
            }
        }
    }

    private void skipTo(String tokenStr) throws Exception {
        while(!scan.getNext().equals(tokenStr));
    }

    private ResultValue operand() throws Exception
    {
        ResultValue res;
        if (scan.currentToken.primClassif == Classif.OPERAND)
        {
            switch (scan.currentToken.subClassif)
            {
                case SubClassif.IDENTIFIER:
                    res = smStorage.getValue(scan.currentToken.tokenStr);
                    scan.getNext();
                    return res;
                case SubClassif.INTEGER:
                case SubClassif.FLOAT:
                case SubClassif.DATE:
                case SubClassif.STRING:
                case SubClassif.BOOLEAN:
                    res =scan.currentToken.toResult();  // Rule 5.1
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
    public void error(String fmt, Object... varArgs) throws Exception
    {
        String diagnosticTxt = String.format(fmt, varArgs);
        throw new ParserException(this.scan.iSourceLineNr, diagnosticTxt, this.sourceFileNm);
    }

}