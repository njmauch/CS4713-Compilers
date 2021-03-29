package pickle;

import java.io.IOException;

public class Parser{
    public Scanner scan;
    public SymbolTable symbolTable;
    public String sourceFileNm;
    public StorageManager storageManager;
    public Token currentParseToken;


    Parser(Scanner scan, SymbolTable symbolTable)
    {
        this.scan = scan;
        this.symbolTable = symbolTable;
        this.sourceFileNm = scan.sourceFileNm;
        this.storageManager = new StorageManager();
    }

    public void beginParse () throws Exception {
        scan.getNext();
        while(scan.currentToken.primClassif != Classif.EOF) {
            statements(true);
        }
    }
    public void nextParseToken() throws Exception {
        if(scan.getNext().isEmpty()) {
            currentParseToken = null;
            return;
        }
        currentParseToken = scan.currentToken;
    }
    private void functionStmt () throws Exception {
        if(scan.currentToken.subClassif == SubClassif.BUILTIN) {
            if(scan.currentToken.tokenStr.equals("print")) {
                printParse();
            }
            else {
                error("No function found with name %s", scan.currentToken.tokenStr);
            }
        }
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

    public void controlStmt() throws Exception {
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
            statements(false);
        }
        if (!scan.currentToken.tokenStr.equals("endwhile")) {
            error("Expected endwhile");
        }
        if (!scan.currentToken.tokenStr.equals(";")) {
            error("Expected ';' after endwhile ");
        }
    }
    public void assigmentStmt() throws Exception {
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
                resO1 = this.storageManager.getValue(variableStr);
                nOp1 = new Numeric(this, resO1, "-=", "1st Operand");
                res = assign(variableStr, Utility.subtraction(this, nOp1, nOp2));
                break;
            case "+=":
                resO2 = expr();
                nOp2 = new Numeric(this, resO2, "+=", "2nd Operand");
                resO1 = this.storageManager.getValue(variableStr);
                nOp1 = new Numeric(this, resO1, "+=", "1st Operand");
                res = assign(variableStr, Utility.addition(this, nOp1, nOp2));
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
        ResultValue temp;
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
        ResultValue res02;
        ResultValue res = new ResultValue();
        String opStr;

        if(scan.currentToken.primClassif != Classif.OPERATOR) {
            res01 = expr();
        }

        opStr = scan.currentToken.tokenStr;

        scan.getNext();
        res02 = expr();

        if(opStr.equals(">")) {
            res = Utility.greaterThan(scan, res01, res02);
        }
        else if (opStr.equals("<")) {
            res = Utility.lessThan(scan, res01, res02);
        }
        else if (opStr.equals(">=")) {
            res = Utility.greaterThanOrEqual(scan, res01, res02);
        }
        else if (opStr.equals("<=")) {
            res = Utility.lessThanOrEqual(scan, res01, res02);
        }
        else if (opStr.equals("==")) {
            res = Utility.equal(scan, res01, res02);
        }
        else if (opStr.equals("!=")) {
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
                case IDENTIFIER:
                    res = storageManager.getValue(scan.currentToken.tokenStr);
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
        storageManager.insertValue(variableStr, res);

        return res;
    }

    public void declareStmt() throws Exception {
        System.out.println("ENTER declareParse()");
        String dclType = currentParseToken.tokenStr;

        // Scan variable name
        nextParseToken();
        String variableName = currentParseToken.tokenStr;
        SymbolTable.STControl dclTypeEntry = (SymbolTable.STControl) symbolTable.getSymbol(dclType);
        if (dclTypeEntry.primClassif != Classif.CONTROL || dclTypeEntry.subClassif != SubClassif.DECLARE) {
            error("Declaration error.  Incorrect symbol \"%s\" from token string \"%s\"", dclTypeEntry.symbol, dclType);
        }

        // Scan operator or semicolon
        nextParseToken();
        if(currentParseToken.tokenStr.equals("=")){
            nextParseToken();
            if((currentParseToken.primClassif != Classif.OPERAND) || (scan.currentToken.subClassif == SubClassif.IDENTIFIER)){
                //TODO: do error return here.
                System.out.println("Error");
            }
            nextParseToken();
        }

        Classif variablePrimClassif = Classif.EMPTY;
        SubClassif variableSubClassif = SubClassif.EMPTY;
        SubClassif variableType = SubClassif.EMPTY;
        String structure = "";

        if(dclType.equals("Int")) {
            variablePrimClassif = Classif.OPERAND;
            variableSubClassif = SubClassif.IDENTIFIER;
            variableType = SubClassif.INTEGER;
            structure = "primitive";
        }
        if(dclType.equals("Float")) {
            variablePrimClassif = Classif.OPERAND;
            variableSubClassif = SubClassif.IDENTIFIER;
            variableType = SubClassif.FLOAT;
            structure = "primitive";
        }
        if(dclType.equals("String")) {
            variablePrimClassif = Classif.OPERAND;
            variableSubClassif = SubClassif.IDENTIFIER;
            variableType = SubClassif.STRING;
            structure = "unbound array";
        }
        if(dclType.equals("Bool")) {
            variablePrimClassif = Classif.OPERAND;
            variableSubClassif = SubClassif.IDENTIFIER;
            variableType = SubClassif.BOOLEAN;
            structure = "primitive";
        }

        SymbolTable.STIdentifier declareEntry = symbolTable.new STIdentifier(variableName, variablePrimClassif, variableSubClassif, variableType, structure);

        symbolTable.putSymbol(variableName, declareEntry);
        // Check variable is declared
        if(!symbolTable.ht.containsKey(variableName)) {
            error("Declaration error.  \"%s\" not found in Symbol Table", variableName);
        }
        // Check semicolon
        if(!scan.currentToken.tokenStr.equals(";")){
            error("syntax error. Expected \";\" at \"%s\"", scan.currentToken.tokenStr);
        }

        SymbolTable.STIdentifier verifyEntry = (SymbolTable.STIdentifier) symbolTable.ht.get(variableName);
        System.out.println("SYMBOL TABLE ENTRY");
        System.out.println(verifyEntry.symbol);
        System.out.println(verifyEntry.primClassif);
        System.out.println(verifyEntry.subClassif);
        System.out.println(verifyEntry.dclType);
        System.out.println(verifyEntry.structure);
        System.out.println();

        ResultValue res = new ResultValue(declareEntry.dclType, "undefined", declareEntry.structure, "undefined");

        storageManager.insertValue(declareEntry.symbol, res);
        ResultValue testRes = storageManager.getValue(declareEntry.symbol);

        System.out.println("STORAGE MANAGER ENTRY");
        System.out.println(testRes.type);
        System.out.println(testRes.value);
        System.out.println(testRes.structure);
        System.out.println(testRes.terminatingStr);

    }

    public void error(String fmt, Object... varArgs) throws Exception
    {
        String diagnosticTxt = String.format(fmt, varArgs);
        throw new ParserException(Scanner.iSourceLineNr, diagnosticTxt, this.sourceFileNm);
    }

    private void printParse() throws Exception {
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
            printStr.append(scan.nextToken.tokenStr);
            scan.getNext();
        }
        System.out.println(printStr.toString());
    }
}