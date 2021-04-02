package pickle;


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
                controlStmt(true);
            } else if (scan.currentToken.primClassif.equals(Classif.OPERATOR)) {
                error("Can't start with operator", scan.currentToken);
            }
            else {
                error("Unknown token %s", scan.currentToken.tokenStr);
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

        scan.getNext();
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
                controlStmt(true);
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
        ResultValue res = null;
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
            if(scan.currentToken.tokenStr.equals(",")){
                scan.getNext();
            }
            if(scan.nextToken.tokenStr.equals(")")) {
                parenCount--;
            }
            else if (scan.nextToken.tokenStr.equals("(")) {
                parenCount++;
            }
            res = expr();
            printStr.append(res.value);
            printStr.append(" ");
            if(scan.nextToken.tokenStr.equals(")")) {
                parenCount--;
            }
            scan.getNext();
        }
        System.out.println(printStr.toString());
        scan.getNext();
    }

    public ResultValue controlStmt(boolean bExec) throws Exception {
        ResultValue res = new ResultValue();
        while (true) {
            //scan.getNext();

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

        switch (scan.currentToken.tokenStr) {
            case "Int" -> dclType = SubClassif.INTEGER;
            case "Float" -> dclType = SubClassif.FLOAT;
            case "String" -> dclType = SubClassif.STRING;
            case "Bool" -> dclType = SubClassif.BOOLEAN;
            default -> error("Unknown declare type %s", scan.currentToken.tokenStr);
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

        //SymbolTable.STEntry stEntry = symbolTable.getSymbol(variableStr);

        symbolTable.putSymbol(variableStr, new SymbolTable.STIdentifier(variableStr, Classif.OPERAND, SubClassif.IDENTIFIER, dclType,"primitive"));

        return res;

    }

    public ResultValue assigmentStmt() throws Exception {
        ResultValue res = new ResultValue();
        Numeric nOp2;
        Numeric nOp1;
        if(scan.currentToken.subClassif != SubClassif.IDENTIFIER) {
            error("Expected a variable for the target assignment %s", scan.currentToken.tokenStr);
        }
        String variableStr = scan.currentToken.tokenStr;
        scan.getNext();
        if(scan.currentToken.primClassif != Classif.OPERATOR) {
            error("expected assignment operator %s", scan.currentToken.tokenStr);
        }
        String operatorStr = scan.currentToken.tokenStr;
        scan.getNext();

        ResultValue resO2;
        ResultValue resO1;

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
                res = assign(variableStr, Utility.subtraction(this, nOp1, nOp2));
                break;
            case "+=":
                resO2 = expr();
                nOp2 = new Numeric(this, resO2, "-=", "2nd Operand");
                resO1 = this.smStorage.getValue(variableStr);
                nOp1 = new Numeric(this, resO1, "-=", "1st Operand");
                res = assign(variableStr, Utility.addition(this, nOp1, nOp2));
            default:
                error("expected assignment operator");
        }
        if(!scan.getNext().equals(";")) {
            error("Must end in ';'");
        }
        return res;
    }

    private ResultValue expr() throws Exception{
        ResultValue res = new ResultValue();
        Numeric nOp1 = null;
        Numeric nOp2;

        while (true) {
            if (scan.currentToken.primClassif.equals(Classif.OPERATOR)) {
                //if (! scan.currentToken.tokenStr.equals("-")) {
                    //break;
                //}
                if (scan.currentToken.primClassif != Classif.OPERAND && ! scan.currentToken.tokenStr.equals("-")) {
                    error("Expected operand %s", scan.currentToken.tokenStr);
                }
                scan.getNext();
                if (scan.currentToken.subClassif.equals(SubClassif.IDENTIFIER)) {
                    SymbolTable.STEntry stEntry = this.symbolTable.getSymbol(scan.currentToken.tokenStr);
                    if (stEntry.primClassif.equals(Classif.EMPTY)) {
                        error("Symbol not found: %s", scan.currentToken.tokenStr);
                    }
                    if (stEntry.primClassif != Classif.OPERAND) {
                        error("Expected Operand: %s", scan.currentToken.tokenStr);
                    }
                    res = this.smStorage.getValue(stEntry.symbol);
                    if (res.type.equals(SubClassif.FLOAT) || res.type.equals(SubClassif.INTEGER)) {
                        nOp1 = new Numeric(this, res, "-", "unary minus");
                    }
                } else if (scan.currentToken.subClassif.equals(SubClassif.FLOAT) || scan.currentToken.subClassif.equals(SubClassif.INTEGER)) {
                    ResultValue resTemp = new ResultValue(scan.currentToken.subClassif, scan.currentToken.tokenStr);
                    nOp1 = new Numeric(this, resTemp, "-", "Unary minus");
                } else {
                    error("Need numeric value %s", scan.currentToken.tokenStr);
                }
                res = Utility.uMinus(this, nOp1);
                break;
            }
            else if (scan.currentToken.primClassif.equals(Classif.OPERAND)) {
                if(scan.currentToken.subClassif.equals(SubClassif.IDENTIFIER)) {
                    SymbolTable.STEntry stEntry = this.symbolTable.getSymbol(scan.currentToken.tokenStr);
                    if (stEntry.primClassif.equals(Classif.EMPTY)) {
                        error("Symbol not found: %s", scan.currentToken.tokenStr);
                    }
                    if (stEntry.primClassif != Classif.OPERAND) {
                        error("Expected Operand: %s", scan.currentToken.tokenStr);
                    }
                    res = this.smStorage.getValue(stEntry.symbol);
                    break;
                }
                else if(scan.currentToken.subClassif.equals(SubClassif.INTEGER) || scan.currentToken.subClassif.equals(SubClassif.FLOAT)) {
                    res = new ResultValue(scan.currentToken.subClassif, scan.currentToken.tokenStr, "primitive", "");
                    break;
                }
                else if (scan.currentToken.subClassif.equals(SubClassif.BOOLEAN) || scan.currentToken.subClassif.equals(SubClassif.STRING)) {
                    res = new ResultValue(scan.currentToken.subClassif, scan.currentToken.tokenStr, "primitive");
                    return res;
                }
            }
        }
        if(scan.nextToken.primClassif != Classif.SEPARATOR) {
            scan.getNext();
            if (scan.currentToken.primClassif != Classif.OPERATOR) {
                error("Invalid token %s", scan.currentToken.tokenStr);
            }

            if (scan.currentToken.tokenStr.equals(">") || scan.currentToken.tokenStr.equals("<") || scan.currentToken.tokenStr.equals(">=") ||
                    scan.currentToken.tokenStr.equals("<=") || scan.currentToken.tokenStr.equals("==") || scan.currentToken.tokenStr.equals("!=") ||
                    scan.currentToken.tokenStr.equals("and") || scan.currentToken.tokenStr.equals("or") || scan.currentToken.tokenStr.equals("not")) {
                return res;
            }

            String opStr = scan.currentToken.tokenStr;

            scan.getNext();

            ResultValue resO2 = expr();

            if (res.type != SubClassif.FLOAT && res.type != SubClassif.INTEGER) {
                error("Expected numeric value: %s", res.value);
            }

            nOp1 = new Numeric(this, res, scan.currentToken.tokenStr, "1st operand");
            nOp2 = new Numeric(this, resO2, scan.currentToken.tokenStr, "2nd Operand");

            switch (opStr) {
                case "+" -> res = Utility.addition(this, nOp1, nOp2);
                case "-" -> res = Utility.subtraction(this, nOp1, nOp2);
                case "/" -> res = Utility.division(this, nOp1, nOp2);
                case "*" -> res = Utility.multiplication(this, nOp1, nOp2);
                case "^" -> res = Utility.exponential(this, nOp1, nOp2);
                default -> error("Invalid operator: %s", scan.currentToken.tokenStr);
            }
        }
        return res;
    }

    private ResultValue evalCond() throws Exception {
        scan.getNext();

        ResultValue resO1 = null;
        ResultValue resO2;
        ResultValue res = new ResultValue();
        String opStr;

        if(scan.currentToken.primClassif != Classif.OPERATOR) {
            resO1 = expr();
        }

        opStr = scan.currentToken.tokenStr;

        scan.getNext();
        resO2 = expr();

        switch (opStr) {
            case ">" -> res = Utility.greaterThan(this, resO1, resO2);
            case "<" -> res = Utility.lessThan(this, resO1, resO2);
            case ">=" -> res = Utility.greaterThanOrEqual(this, resO1, resO2);
            case "<=" -> res = Utility.lessThanOrEqual(this, resO1, resO2);
            case "==" -> res = Utility.equal(this, resO1, resO2);
            case "!=" -> res = Utility.notEqual(this, resO1, resO2);
            default -> error("Bad compare token");
        }
        return res;
    }

    void ifStmt(Boolean bExec) throws Exception {
        int saveLineNr = scan.currentToken.iSourceLineNr;
        ResultValue resCond;
        ResultValue resTemp;
        if (bExec) {
            resCond = evalCond();
            if (resCond.value.equals("T")) {
                resTemp = statements(true);
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
                resTemp = statements(false);
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
            resTemp = statements(false);
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


    private ResultValue assign(String variableStr, ResultValue res) throws Exception {
        switch (res.type) {
            case INTEGER -> {
                res.value = Utility.castInt(this, res);
                res.type = SubClassif.INTEGER;
            }
            case FLOAT -> {
                res.value = Utility.castFloat(this, res);
                res.type = SubClassif.FLOAT;
            }
            case BOOLEAN -> {
                res.value = Utility.castBoolean(this, res);
                res.type = SubClassif.BOOLEAN;
            }
            case STRING -> res.type = SubClassif.STRING;
            default -> error("Assign type is incompatible");
        }
        smStorage.insertValue(variableStr, res);

        return res;
    }


}