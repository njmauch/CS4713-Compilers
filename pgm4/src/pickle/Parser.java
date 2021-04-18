package pickle;


import java.util.ArrayList;
import java.util.regex.Pattern;

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

    private void skipTo(String tokenStr) throws Exception {
        while (! scan.currentToken.tokenStr.equals(tokenStr) && scan.currentToken.primClassif != Classif.EOF)
            scan.getNext();
    }

    public void error(String fmt, Object... varArgs) throws Exception
    {
        String diagnosticTxt = String.format(fmt, varArgs);
        throw new ParserException(Scanner.iSourceLineNr, diagnosticTxt, this.sourceFileNm);
    }

    public ResultValue statement (boolean bExec) throws Exception {
        scan.getNext();

        if(scan.currentToken.primClassif.equals(Classif.EOF)) {
            return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, "");
        }
        else if(scan.currentToken.primClassif.equals(Classif.CONTROL)) {
            if(scan.currentToken.subClassif.equals(SubClassif.DECLARE)) {
                return declareStmt(bExec);
            }
            else if(scan.currentToken.subClassif.equals(SubClassif.FLOW)) {
                switch (scan.currentToken.tokenStr) {
                    case "if":
                        return ifStmt(bExec);
                    case "while":
                        return whileStmt(bExec);
                    case "for":
                        return forStmt(bExec);
                }
            }
            else if(scan.currentToken.subClassif.equals(SubClassif.END)) {
                return new ResultValue(SubClassif.END, "", Structure.PRIMITIVE, scan.currentToken.tokenStr);
            }
            else {
                error("Invalid control variable %s", scan.currentToken.tokenStr);
            }
        }
        else if(scan.currentToken.primClassif.equals(Classif.OPERAND)) {
            return assignmentStmt(bExec);
        }
        else if(scan.currentToken.primClassif.equals(Classif.FUNCTION)) {
            return functionStmt(bExec);
        }
        return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, scan.currentToken.tokenStr);
    }

    public ResultValue statements(Boolean bExec, String termStr) throws Exception {
        ResultValue res = statement(bExec);
        while(! termStr.contains(res.terminatingStr)) {
            res = statement(bExec);
        }
        return res;
    }

    private ResultValue functionStmt (boolean bExec) throws Exception {
        ResultValue res = null;
        if(scan.currentToken.subClassif == SubClassif.BUILTIN) {
            if(!bExec) {
                skipTo(";");
                res = new ResultValue(SubClassif.BUILTIN, "", Structure.PRIMITIVE, scan.currentToken.tokenStr);
            }
            else if(scan.currentToken.tokenStr.equals("print")) {
                res = print();
            }
            else if(scan.currentToken.tokenStr.equals("LENGTH")) {
                res = Utility.LENGTH(scan.currentToken.tokenStr);
            }
            else if(scan.currentToken.tokenStr.equals("SPACES")) {
                res = Utility.SPACES(scan.currentToken.tokenStr);
            }
            else if(scan.currentToken.tokenStr.equals("ELEM")) {
                res = Utility.ELEM(this, (ResultArray)smStorage.getValue(scan.currentToken.tokenStr));
            }
            else if(scan.currentToken.tokenStr.equals("MAXELEM")) {
                res = Utility.MAXELEM((ResultArray)smStorage.getValue(scan.currentToken.tokenStr));
            }
            else {
                error("No function found with name %s", scan.currentToken.tokenStr);
            }
        }
        return res;
    }

    private ResultValue print() throws Exception {
        scan.getNext();
        ResultValue res;
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
        System.out.println(printStr);
        scan.getNext();
        return new ResultValue(SubClassif.BUILTIN, "", Structure.PRIMITIVE, scan.currentToken.tokenStr);
    }

    private ResultValue whileStmt(boolean bExec) throws Exception {
        ResultValue res;
        Token tempToken;

        tempToken = scan.currentToken;
        if (bExec) {
            ResultValue resCond = evalCond();
            while(resCond.value.equals("T")) {
                res = statements(true, "endwhile");
                if(! res.terminatingStr.equals("endwhile")){
                    error("Expected endwhile for while beggining line %s, got %s", tempToken.iSourceLineNr, res.value);
                }
                scan.setPosition(tempToken);
                scan.getNext();
                resCond = evalCond();
            }
            res = statements(false, "endwhile");
        }
        else {
            skipTo(":");
            res = statements(false, "endwhile");
        }
        if(! res.terminatingStr.equals("endwhile")) {
            error("Expected endwhile for while beggining line %s", tempToken.iSourceLineNr);
        }
        if(! scan.nextToken.tokenStr.equals(";")) {
            error("Expected ; after endwhile");
        }
        return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, ";");
    }

    private ResultValue declareStmt(boolean bExec) throws Exception {
        ResultValue res;
        Structure structure = Structure.PRIMITIVE;

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
        res = new ResultValue(dclType, variableStr, structure);

        if(bExec) {

            Token tempToken = scan.currentToken;
            if (scan.nextToken.tokenStr.equals("[")) {
                scan.getNext();

                structure = Structure.FIXED_ARRAY;
                if (scan.nextToken.tokenStr.equals("]")) {
                    scan.getNext();
                    if (scan.nextToken.tokenStr.equals(";")) {
                        error("Can't declare array without length");
                    } else if (scan.nextToken.tokenStr.equals("=")) {
                        scan.getNext();
                        symbolTable.putSymbol(variableStr, new SymbolTable.STIdentifier(variableStr
                                , tempToken.primClassif, tempToken.subClassif, dclType, structure));
                        smStorage.insertValue(variableStr, new ResultArray(tempToken.tokenStr, dclType, structure));
                        return declareArray(bExec, variableStr, dclType, 0);
                    } else {
                        error("Invalid symbol: %s", scan.nextToken.tokenStr);
                    }
                } else if (scan.nextToken.primClassif != Classif.OPERATOR) {
                    if (scan.nextToken.subClassif.equals(SubClassif.IDENTIFIER)) {
                        if (smStorage.getValue(scan.nextToken.tokenStr) == null) {
                            error("%s is not defined", scan.nextToken.tokenStr);
                        }
                    }
                    Token leftToken = scan.currentToken;
                    skipTo("]");
                    scan.setPosition(leftToken);
                    int dclLength = Integer.parseInt(Utility.castInt(this, expr()));
                    if (dclLength < 0) {
                        error("Array size must be positive");
                    }
                    //right bracket
                    scan.getNext();
                    //get = or ;
                    scan.getNext();
                    if (scan.currentToken.tokenStr.equals(";")) {
                        symbolTable.putSymbol(variableStr, new SymbolTable.STIdentifier(variableStr, tempToken.primClassif, tempToken.subClassif, dclType, Structure.FIXED_ARRAY));
                        ArrayList<ResultValue> tempArrayList = new ArrayList<>();
                        for (int j = 0; j < dclLength; j++) {
                            tempArrayList.add(null);
                        }
                        smStorage.insertValue(variableStr, new ResultArray(tempToken.tokenStr, tempArrayList, dclType, structure, 0, dclLength));
                        return new ResultValue(SubClassif.DECLARE, "", Structure.FIXED_ARRAY, scan.currentToken.tokenStr);
                    } else if (scan.currentToken.tokenStr.equals("=")) {
                        symbolTable.putSymbol(variableStr, new SymbolTable.STIdentifier(variableStr, tempToken.primClassif, tempToken.subClassif, dclType, Structure.FIXED_ARRAY));
                        ArrayList<ResultValue> tempArrayList = new ArrayList<>();
                        for (int j = 0; j < dclLength; j++) {
                            tempArrayList.add(null);
                        }
                        smStorage.insertValue(variableStr, new ResultArray(tempToken.tokenStr, tempArrayList, dclType, structure, 0, dclLength));
                        return declareArray(bExec, variableStr, dclType, dclLength);
                    } else {
                        error("Expected = or ; and got: %s", scan.currentToken.tokenStr);
                    }
                } else {
                    error("Invalid length: %s", scan.nextToken.tokenStr);
                }
            } else {
                if (scan.nextToken.tokenStr.equals("]")) {
                    error("Missing [ with ]");
                }
                symbolTable.putSymbol(variableStr, new SymbolTable.STIdentifier(variableStr,
                        scan.currentToken.primClassif, scan.currentToken.subClassif, dclType, structure));
                smStorage.insertValue(variableStr, new ResultValue(dclType, "", structure));
            }
        }
        if(scan.nextToken.tokenStr.equals("=")) {
            return assignmentStmt(bExec);
        }
        else if(scan.nextToken.tokenStr.equals("[")){
            scan.getNext();
            structure = Structure.FIXED_ARRAY;
            if (scan.nextToken.tokenStr.equals("]")) {
                scan.getNext();

                if (scan.nextToken.tokenStr.equals(";"))
                    error("Can't declary array without length");
                else if (scan.nextToken.tokenStr.equals("="))
                {
                    scan.getNext();
                    return declareArray(bExec, variableStr, dclType, 0);
                }
                else
                    error("Expected = or ; and got: %s", scan.nextToken.tokenStr);
            }
            int length = Integer.parseInt(Utility.castInt(this, expr()));
            if (length < 0) {
                error("Array size must be positive");
            }

            scan.getNext();
            scan.getNext();
            if (scan.currentToken.tokenStr.equals(";")) {
                return new ResultValue(SubClassif.DECLARE, "", Structure.FIXED_ARRAY, scan.currentToken.tokenStr);
            }
            else if (scan.currentToken.tokenStr.equals("=")) {
                return declareArray(bExec, variableStr, dclType, length);
            }
            else {
                error("Expected = or ; and got: %s", scan.nextToken.tokenStr);
            }
        }
        else if (scan.nextToken.primClassif == Classif.OPERATOR) {
            error("Can't perform declare before being initialized: %s", scan.nextToken.tokenStr);
        }
        else if(! scan.getNext().equals(";")) {
            error("Declare statment not terminated");
        }
        return new ResultValue(SubClassif.DECLARE,"",  Structure.PRIMITIVE, scan.currentToken.tokenStr);
    }

    public ResultValue assignmentStmt(boolean bExec) throws Exception {
        ResultValue res;
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
                if (bExec) {
                    resO2 = expr();
                    res = assign(variableStr, resO2);
                    res.terminatingStr = scan.nextToken.tokenStr;
                    return res;
                }
                else {
                    skipTo(";");
                    break;
                }
            case "-=":
                if (bExec) {
                    resO2 = expr();
                    nOp2 = new Numeric(this, resO2, "-=", "2nd Operand");
                    resO1 = this.smStorage.getValue(variableStr);
                    nOp1 = new Numeric(this, resO1, "-=", "1st Operand");
                    res = assign(variableStr, Utility.subtraction(this, nOp1, nOp2));
                    return res;
                }
                else {
                    skipTo(";");
                    break;
                }
            case "+=":
                if (bExec) {
                    resO2 = expr();
                    nOp2 = new Numeric(this, resO2, "-=", "2nd Operand");
                    resO1 = this.smStorage.getValue(variableStr);
                    nOp1 = new Numeric(this, resO1, "-=", "1st Operand");
                    res = assign(variableStr, Utility.addition(this, nOp1, nOp2));
                    return res;
                }
                else {
                    skipTo(";");
                    break;
                }
            default:
                error("expected assignment operator %s", scan.currentToken.tokenStr);
        }
        return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, scan.currentToken.tokenStr);
    }

    private ResultValue expr() throws Exception{
        ResultValue res;
        Numeric nOp1 = null;
        Numeric nOp2;

        while (true) {
            if (scan.currentToken.primClassif.equals(Classif.OPERATOR)) {
                if (! scan.currentToken.tokenStr.equals("-")) {
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
                assert nOp1 != null;
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
                    res = new ResultValue(scan.currentToken.subClassif, scan.currentToken.tokenStr, Structure.PRIMITIVE, "");
                    break;
                }
                else if (scan.currentToken.subClassif.equals(SubClassif.BOOLEAN) || scan.currentToken.subClassif.equals(SubClassif.STRING)) {
                    res = new ResultValue(scan.currentToken.subClassif, scan.currentToken.tokenStr, Structure.PRIMITIVE);
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

    private ResultValue ifStmt(Boolean bExec) throws Exception {
        int saveLineNr = scan.currentToken.iSourceLineNr;
        ResultValue resCond;

        if (bExec) {
            resCond = evalCond();
            if (resCond.value.equals("T")) {
                resCond = statements(true, "endif else");
                if (resCond.terminatingStr.equals("else")) {
                    if (!scan.getNext().equals(":")) {
                        error("expected a ‘:’after ‘else’");
                    }
                    resCond = statements(false, "endif");
                }
            } else {
                resCond = statements(false, "endif else");
                if (resCond.terminatingStr.equals("else")) {
                    if (!scan.getNext().equals(":")) {
                        error("expected a ‘:’after ‘else’");
                    }
                    resCond = statements(true, "endif");
                }
            }
        }
        else {
            skipTo(":");
            resCond = statements(false, "endif else");
            if (resCond.terminatingStr.equals("else")) {
                if (!scan.getNext().equals(":")) {
                    error("expected a ‘:’after ‘else’");
                }
                resCond = statements(false, "endif");
            }
        }

        return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, ";");
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

    public ResultValue forStmt(Boolean bExec) throws Exception {
        ResultValue res;
        Token tempToken;

        if (bExec) {
            tempToken = scan.currentToken;
            scan.getNext();

            if (scan.currentToken.subClassif != SubClassif.IDENTIFIER) {
                error("Unexpected variable found: %s", scan.currentToken.tokenStr);
            }

            if(scan.nextToken.tokenStr.equals("=")) {
                int cv, limit, incr;
                String stringCV = scan.currentToken.tokenStr;
                if(smStorage.getValue(stringCV) == null) {
                    smStorage.insertValue(stringCV, new ResultValue(SubClassif.INTEGER, "", Structure.PRIMITIVE, "to"));
                }

                cv = Integer.parseInt(assignmentStmt(true).value);

                if(!scan.getNext().equals("to")){
                    error("Expected end variable but found: %s", scan.currentToken.tokenStr);
                }

                limit = Integer.parseInt(expr().value);

                if(scan.getNext().equals("by")){
                    incr = Integer.parseInt(expr().value);
                    scan.getNext();
                }
                else {
                    incr = 1;
                }

                if(!scan.currentToken.tokenStr.equals(":")) {
                    error("Expected ':' after for statment");
                }
                for(int i = cv; i < limit; i+= incr) {
                    res = statements(true, "endfor");

                    if (!res.terminatingStr.equals("endfor")) {
                        if(!scan.nextToken.tokenStr.equals(";")) {
                            error("Expected 'endfor;' at end of for stmt");
                        }
                    }

                    res = smStorage.getValue(stringCV);
                    res.value = "" + (Integer.parseInt(res.value) + incr);
                    smStorage.insertValue(stringCV, res);

                    scan.setPosition(tempToken);
                    skipTo(":");
                }
            }
            else if (scan.nextToken.tokenStr.equals("in")) {
                String tempStr = scan.currentToken.tokenStr;
                String object;

                scan.getNext();

                if(scan.currentToken.primClassif != Classif.OPERAND) {
                    error("Expected variable but found: %s", scan.currentToken.tokenStr);
                }

                res = expr();

                if(!scan.getNext().equals(":")) {
                    error("Expected ':' at end of for statement");
                }

                if(res.structure == Structure.FIXED_ARRAY) {
                    ResultArray array = (ResultArray)smStorage.getValue(res.value);

                    ArrayList<ResultValue> resultList = array.array;

                    if (smStorage.getValue(tempStr) == null) {
                        smStorage.insertValue(tempStr, new ResultValue(array.type, "", Structure.PRIMITIVE, "in"));
                    }

                    for(ResultValue value : resultList) {
                        if(value == null) {
                            continue;
                        }

                        res = smStorage.getValue(tempStr);
                        res.value = "" + value.value;
                        smStorage.insertValue(tempStr, res);
                        res = statements(true, "endfor");

                        if (!res.terminatingStr.equals("endfor")) {
                            if(!scan.nextToken.tokenStr.equals(";")) {
                                error("Expected 'endfor;' and end of for loop");
                            }
                        }

                        scan.setPosition(tempToken);
                        skipTo(":");
                    }
                }
                else {
                    object = res.value;

                    smStorage.insertValue(tempStr, new ResultValue(SubClassif.STRING, "", Structure.PRIMITIVE, "in"));

                    for (char ch : object.toCharArray()){
                        res = smStorage.getValue(tempStr);
                        res.value = "" + ch;
                        smStorage.insertValue(tempStr, res);
                        res = statements(true, "endfor");

                        if (!res.terminatingStr.equals("endfor")) {
                            if(!scan.nextToken.tokenStr.equals(";")) {
                                error("Expected 'endfor;' and end of for loop");
                            }
                        }

                        scan.setPosition(tempToken);
                        skipTo(":");
                    }
                }
            }
            else if(scan.currentToken.tokenStr.equals("from")) {
                String stringCV = scan.currentToken.tokenStr;
                String str, delim;
                String stringM[];

                if(smStorage.getValue(stringCV) == null) {
                    smStorage.insertValue(stringCV, new ResultValue(SubClassif.INTEGER, "", Structure.PRIMITIVE, "to"));
                }

                scan.getNext();

                if(scan.nextToken.primClassif != Classif.OPERAND) {
                    error("Expected variable but got: %s", scan.nextToken.tokenStr);
                }

                res = expr();

                if(res.structure != Structure.PRIMITIVE) {
                    error("Invalid type for for tokenizer");
                }

                str = res.value;

                if(!scan.getNext().equals("by")) {
                    error("Missing by for delimiter");
                }

                delim = expr().value;

                if(!scan.getNext().equals(":")) {
                    error("Missing ':' and end of for stmt");
                }

                stringM = str.split(Pattern.quote(delim));

                smStorage.insertValue(stringCV, new ResultValue(SubClassif.STRING, "", Structure.PRIMITIVE, "from"));

                for(String s : stringM) {
                    res = smStorage.getValue(stringCV);
                    res.value = "" + s;
                    smStorage.insertValue(stringCV, res);
                    res = statements(true, "endfor");

                    if (!res.terminatingStr.equals("endfor")) {
                        if(!scan.nextToken.tokenStr.equals(";")) {
                            error("Expected 'endfor;' and end of for loop");
                        }
                    }

                    scan.setPosition(tempToken);
                    skipTo(":");
                }
            }
            else {
                error("Invalid control seperator: %s, expected '=', 'in', or 'from'", scan.currentToken.tokenStr);
            }
        }
        else {
            skipTo(":");
        }
        res = statements(false, "endfor");

        if (!res.terminatingStr.equals("endfor")) {
            if(!scan.nextToken.tokenStr.equals(";")) {
                error("Expected 'endfor;' and end of for loop");
            }
        }

        return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, ";");
    }

    public ResultArray declareArray(boolean bExec, String variableStr, SubClassif type, int declared) throws Exception {
        ResultValue resExpr = new ResultValue();
        ResultArray resultArray;
        ArrayList<ResultValue> exprValue = new ArrayList<>();
        int populated = 1;
        Token tempToken = scan.currentToken;

        if(bExec) {
            while(!resExpr.terminatingStr.equals(";") && !scan.nextToken.tokenStr.equals(";")) {
                resExpr = expr();
                if ((resExpr.structure == Structure.FIXED_ARRAY) || (resExpr.structure == Structure.UNBOUNDED_ARRAY)) {
                    if (populated != 1) {
                        error("Can only have one value as an array in value list");
                        scan.setPosition(tempToken);
                        return assignArrayStmt(variableStr, type, declared);
                    }
                    scan.getNext();
                    populated++;
                    if (type == SubClassif.INTEGER) {
                        resExpr.value = Utility.castInt(this, resExpr);
                        resExpr.type = SubClassif.INTEGER;
                        exprValue.add(resExpr);
                    } else if (type == SubClassif.FLOAT) {
                        resExpr.value = Utility.castFloat(this, resExpr);
                        resExpr.type = SubClassif.FLOAT;
                        exprValue.add(resExpr);
                    } else if (type == SubClassif.BOOLEAN) {
                        resExpr.value = Utility.castBoolean(this, resExpr);
                        resExpr.type = SubClassif.BOOLEAN;
                        exprValue.add(resExpr);
                    } else if (type == SubClassif.STRING) {
                        resExpr.type = SubClassif.STRING;
                        exprValue.add(resExpr);
                    } else {
                        error("Invalid assign type: %s", variableStr);
                    }
                }
            }
            if(declared != -1 && exprValue.size() > declared) {
                declared = exprValue.size();
            }
            if(declared == -1) {
                resultArray = new ResultArray(variableStr, exprValue, type, Structure.UNBOUNDED_ARRAY, --populated, declared);
            }
            else {
                resultArray = new ResultArray(variableStr, exprValue, type, Structure.FIXED_ARRAY, --populated, declared);
            }

            while(resultArray.array.size() < declared) {
                resultArray.array.add(null);
            }
            smStorage.insertValue(variableStr, resultArray);
            return resultArray;
        }
        else {
            skipTo(";");
        }
        return new ResultArray( "", SubClassif.VOID, Structure.PRIMITIVE, scan.currentToken.tokenStr);
    }

    private ResultArray assignArrayStmt(String variableStr, SubClassif type, int declared) throws Exception {
        ResultValue resExpr;
        ResultArray resultArray = new ResultArray();
        int populated = 1;
        if(scan.nextToken.primClassif == Classif.OPERAND) {
            ResultArray resultArray1 = (ResultArray) smStorage.getValue(variableStr);
            ResultValue resultValue = expr();

            if(resultArray1 == null) {
                error("Variable not defined: %s", variableStr);
            }
            if(resultValue == null) {
                error("Variable not defined: %s", scan.nextToken.tokenStr);
            }
            assert resultArray1 != null;
            if(resultArray1.structure.equals(Structure.UNBOUNDED_ARRAY)) {
                assert resultValue != null;
                if (resultValue.structure.equals(Structure.PRIMITIVE)) {
                    error("Can't assign scalar to unbounded array");
                }
            }
            assert resultValue != null;
            if(resultValue.structure.equals(Structure.PRIMITIVE)) {
                for(int i = 0; i < resultArray1.declaredSize; i++) {
                    if(type.equals(SubClassif.INTEGER)) {
                        resExpr = resultValue.clone();
                        resExpr.value = Utility.castInt(this, resExpr);
                        resExpr.type = SubClassif.INTEGER;
                        resultArray1.array.set(i, resExpr);
                    }
                    else if(type.equals(SubClassif.FLOAT)) {
                        resExpr = resultValue.clone();
                        resExpr.value = Utility.castFloat(this, resExpr);
                        resExpr.type = SubClassif.FLOAT;
                        resultArray1.array.set(i, resExpr);
                    }
                    else if(type.equals(SubClassif.BOOLEAN)) {
                        resExpr = resultValue.clone();
                        resExpr.value = Utility.castBoolean(this, resExpr);
                        resExpr.type = SubClassif.BOOLEAN;
                        resultArray1.array.set(i, resExpr);
                    }
                    else if(type.equals(SubClassif.STRING)) {
                        resExpr = resultValue.clone();
                        resExpr.type = SubClassif.FLOAT;
                        resultArray1.array.set(i, resExpr);
                    }
                    else {
                        error("Invalid assign type: %s", variableStr);
                    }
                }
                if(!scan.nextToken.tokenStr.equals(";")) {
                    error("Can only have one argument when using array to scalar assignment");
                }
                if(declared == -1) {
                    resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.UNBOUNDED_ARRAY, populated, declared);
                }
                else {
                    resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.FIXED_ARRAY, populated, declared);
                }
            }
            else if(resultValue.structure == Structure.FIXED_ARRAY || resultValue.structure == Structure.UNBOUNDED_ARRAY) {
                ResultArray resultArray2 = (ResultArray) resultValue;
                int iDclLength = resultArray1.declaredSize;
                int iPopLength = resultArray2.declaredSize;

                if(declared != -1 && iDclLength < iPopLength) {
                    iPopLength = iDclLength;
                }
                for(int i = 0; i < iPopLength; i++) {
                    if(type.equals(SubClassif.INTEGER)) {
                        resExpr = resultArray2.array.get(i).clone();
                        resExpr.value = Utility.castInt(this, resExpr);
                        resExpr.type = SubClassif.INTEGER;
                        if (declared == -1) {
                            if (resultArray1.array == null) {
                                resultArray1.array = new ArrayList<>();
                            }
                            if (resultArray1.array.size() <= i) {
                                resultArray1.array.add(i, null);
                            }
                        }
                        resultArray1.array.set(i, resExpr);
                    }
                    else if(type.equals(SubClassif.FLOAT)) {
                        resExpr = resultArray2.array.get(i).clone();
                        resExpr.value = Utility.castFloat(this, resExpr);
                        resExpr.type = SubClassif.FLOAT;
                        if (declared == -1) {
                            if (resultArray1.array == null) {
                                resultArray1.array = new ArrayList<>();
                            }
                            if (resultArray1.array.size() <= i) {
                                resultArray1.array.add(i, null);
                            }
                        }
                        resultArray1.array.set(i, resExpr);

                    }
                    else if(type.equals(SubClassif.BOOLEAN)) {
                        resExpr = resultArray2.array.get(i).clone();
                        resExpr.value = Utility.castBoolean(this, resExpr);
                        resExpr.type = SubClassif.BOOLEAN;
                        if (declared == -1) {
                            if (resultArray1.array == null) {
                                resultArray1.array = new ArrayList<>();
                            }
                            if (resultArray1.array.size() <= i) {
                                resultArray1.array.add(i, null);
                            }
                        }
                        resultArray1.array.set(i, resExpr);
                    }
                    else if(type.equals(SubClassif.STRING)) {
                        resExpr = resultArray2.array.get(i).clone();
                        resExpr.type = SubClassif.STRING;
                        if (declared == -1) {
                            if (resultArray1.array == null) {
                                resultArray1.array = new ArrayList<>();
                            }
                            if (resultArray1.array.size() <= i) {
                                resultArray1.array.add(i, null);
                            }
                        }
                        resultArray1.array.set(i, resExpr);
                    }
                    else {
                        error("Invalid assign type: %s", variableStr);
                    }
                }
                for(ResultValue tempRes : resultArray1.array) {
                    if(tempRes != null) {
                        populated++;
                    }
                }
                if (!scan.nextToken.tokenStr.equals(";")) {
                    error("Can only have one argument when using array to array assignment");
                }
                if(declared == -1) {
                    resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.UNBOUNDED_ARRAY, populated, declared);
                }
                else {
                    resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.FIXED_ARRAY, populated, declared);
                }
                smStorage.insertValue(variableStr, resultArray);
                return resultArray;
            }
        }
        else{
            error("Expected operand: %s", scan.nextToken.tokenStr);
        }
        return resultArray;
    }
}