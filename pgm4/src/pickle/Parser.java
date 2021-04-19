package pickle;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Pattern;

public class Parser{
    public Scanner scan;
    public Precedence precedence;
    public SymbolTable symbolTable;
    public String sourceFileNm;
    public StorageManager smStorage;

    public boolean bShowExpr;
    public boolean bShowAssign;
    public boolean bShowStmt;


    Parser(Scanner scan, StorageManager storageManager, SymbolTable symbolTable, Precedence precedence) {
        this.scan = scan;
        this.symbolTable = symbolTable;
        this.sourceFileNm = scan.sourceFileNm;
        this.smStorage = storageManager;
        this.precedence = precedence;

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
        String funcName = scan.currentToken.tokenStr;
        ResultValue res = null;
        String line = "";
        Token prevToken = null;
        while(!scan.currentToken.tokenStr.equals(";")) {
            res = expr(true);
            line = res.value + " ";
            prevToken = scan.currentToken;
            scan.getNext();
            while (scan.currentToken.tokenStr.equals(")")) {
                scan.getNext();
            }
            if (scan.currentToken.primClassif.equals(Classif.EOF)) {
                error("Missing ';'");
            }
            if (scan.currentToken.primClassif != Classif.SEPARATOR) {
                error("Missing separator");
            }
        }
        /*if(!prevToken.tokenStr.equals(")") && scan.nextToken.primClassif != Classif.EOF) {
            error("Func %s missing closing paren", funcName);
        }*/
        System.out.println(line);
        return res;
    }

    private ResultValue whileStmt(boolean bExec) throws Exception {
        ResultValue res;
        Token tempToken;

        tempToken = scan.currentToken;
        if (bExec) {
            ResultValue resCond = expr(false);
            while(resCond.value.equals("T")) {
                res = statements(true, "endwhile");
                if(! res.terminatingStr.equals("endwhile")){
                    error("Expected endwhile for while beggining line %s, got %s", tempToken.iSourceLineNr, res.value);
                }
                scan.setPosition(tempToken);
                scan.getNext();
                resCond = expr(false);
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
                    int dclLength = Integer.parseInt(Utility.castInt(this, expr(false)));
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
            int length = Integer.parseInt(Utility.castInt(this, expr(false)));
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
        SubClassif type = SubClassif.EMPTY;
        Numeric nOp2;
        Numeric nOp1;
        int iIndex = 0;
        int iIndex2 = 0;
        boolean bIndex = false;
        ResultValue resO2;
        ResultValue resO1 = null;
        if(bExec) {
            try{
                type = smStorage.getValue(scan.currentToken.tokenStr).type;
            }
            catch(Exception e) {
                error("Variable has not yet been declared: %s", scan.currentToken.tokenStr);
            }
        }
        if(scan.currentToken.subClassif != SubClassif.IDENTIFIER) {
            error("Expected a variable for the target assignment %s", scan.currentToken.tokenStr);
        }
        String variableStr = scan.currentToken.tokenStr;
        res = smStorage.getValue(variableStr);
        scan.getNext();
        if(res == null && bExec){
            error("%s required to be declared", variableStr);
        }

        if(scan.currentToken.tokenStr.equals("[")){
            iIndex = Integer.parseInt(Utility.castInt(this, expr(false)));
            scan.getNext();
            scan.getNext();
            bIndex = true;
        }

        if(scan.currentToken.primClassif != Classif.OPERATOR) {
            error("Expected operator but got: %s", scan.currentToken.tokenStr);
        }

        if(scan.currentToken.tokenStr.equals("=")) {
            if (bExec) {
                if (res.structure.equals(Structure.PRIMITIVE)) {
                    if (bIndex == false) {
                        resO1 = assign(variableStr, expr(false));
                        if (scan.currentToken.primClassif != Classif.OPERAND) {
                            scan.getNext();
                        }
                    } else {
                        resO2 = expr(false);
                        String strValue = smStorage.getValue(variableStr).value;
                        if (iIndex == -1) {
                            iIndex = strValue.length() - 1;
                        }
                        if (iIndex > strValue.length() - 1) {
                            error("Index %s out of bounds", iIndex);
                        }
                        String tempValue;
                        if (iIndex2 == 0) {
                            tempValue = strValue.substring(0, iIndex) + resO2.value + strValue.substring(iIndex + 1);
                        } else {
                            tempValue = strValue.substring(0, iIndex) + resO2.value;
                        }
                        ResultValue finalRes = new ResultValue(SubClassif.STRING, tempValue);
                        resO1 = assign(variableStr, finalRes);
                    }
                    return resO1;
                } else if (res.structure.equals(Structure.FIXED_ARRAY) || res.structure.equals(Structure.UNBOUNDED_ARRAY)) {
                    if (bIndex == false) {
                        resO1 = assignArrayStmt(variableStr, type, ((ResultArray) res).declaredSize);
                    } else {
                        if (res.structure != Structure.UNBOUNDED_ARRAY && iIndex >= ((ResultArray) res).declaredSize) {
                            error("Index %d out of bounds", iIndex);
                        }
                        if (iIndex < 0) {
                            if (((ResultArray) res).declaredSize != -1) {
                                iIndex += ((ResultArray) res).declaredSize;
                            } else {
                                iIndex += ((ResultArray) res).lastPopulated;
                            }
                        }
                        if (((ResultArray) res).declaredSize == -1) {
                            while (iIndex >= ((ResultArray) res).array.size()) {
                                ((ResultArray) res).array.add(null);
                            }
                        }
                        ResultValue tempRes = expr(false);
                        resO1 = assignIndex(variableStr, type, iIndex, tempRes);
                    }
                    return resO1;
                } else {
                    error("Invalid structure type on %s", res.value);
                }
            } else {
                skipTo(";");
            }
        }
        else if(scan.currentToken.tokenStr.equals("+=")) {
            if (bExec) {
                if (res.structure.equals(Structure.PRIMITIVE)) {
                    if (!bIndex) {
                        nOp2 = new Numeric(this, expr(false), "+=", "2nd operand");
                        nOp1 = new Numeric(this, res, "+=", "1st Operand");
                        ResultValue resTemp = Utility.addition(this, nOp1, nOp2);
                        resO1 = assign(variableStr, resTemp);
                        if (scan.currentToken.primClassif != Classif.OPERAND) {
                            scan.getNext();
                        }
                    } else {
                        resO2 = expr(false);
                        String strValue = smStorage.getValue(variableStr).value;
                        if (iIndex == -1) {
                            iIndex = strValue.length() - 1;
                        }
                        if (iIndex > strValue.length() - 1) {
                            error("Index %s out of bounds", iIndex);
                        }
                        String tempValue;
                        if (iIndex2 == 0) {
                            tempValue = strValue.substring(0, iIndex) + resO2.value + strValue.substring(iIndex + 1);
                        } else {
                            tempValue = strValue.substring(0, iIndex) + resO2.value;
                        }
                        ResultValue finalRes = new ResultValue(SubClassif.STRING, tempValue);
                        resO1 = assign(variableStr, finalRes);
                    }
                    return resO1;
                } else if (res.structure.equals(Structure.FIXED_ARRAY) || res.structure.equals(Structure.UNBOUNDED_ARRAY)) {
                    if (!bIndex) {
                        error("Can't perform += on array");
                    } else {
                        if (res.structure != Structure.UNBOUNDED_ARRAY && iIndex >= ((ResultArray) res).declaredSize) {
                            error("Index %d out of bounds", iIndex);
                        }
                        if (iIndex < 0) {
                            if (((ResultArray) res).declaredSize != -1) {
                                iIndex += ((ResultArray) res).declaredSize;
                            } else {
                                iIndex += ((ResultArray) res).lastPopulated;
                            }
                        }
                        if (((ResultArray) res).declaredSize == -1) {
                            while (iIndex >= ((ResultArray) res).array.size()) {
                                ((ResultArray) res).array.add(null);
                            }
                        }
                        ResultValue tempRes = expr(false);
                        ResultValue tempRes2 = ((ResultArray) res).array.get(iIndex);
                        nOp2 = new Numeric(this, tempRes, "+=", "2nd Operand");
                        nOp1 = new Numeric(this, tempRes2, "+=", "1st Operand");
                        tempRes = Utility.addition(this, nOp2, nOp1);
                        resO1 = assignIndex(variableStr, type, iIndex, tempRes);
                    }
                    return resO1;
                } else {
                    error("Invalid structure type on %s", res.value);
                }
            } else {
                skipTo(";");
            }
        }
        else if (scan.currentToken.tokenStr.equals("-=")) {
            if (bExec) {
                if (res.structure.equals(Structure.PRIMITIVE)) {
                    if (!bIndex) {
                        nOp2 = new Numeric(this, expr(false), "+=", "2nd operand");
                        nOp1 = new Numeric(this, res, "+=", "1st Operand");
                        ResultValue resTemp = Utility.addition(this, nOp1, nOp2);
                        resO1 = assign(variableStr, resTemp);
                        if (scan.currentToken.primClassif != Classif.OPERAND) {
                            scan.getNext();
                        }
                    } else {
                        resO2 = expr(false);
                        String strValue = smStorage.getValue(variableStr).value;
                        if (iIndex == -1) {
                            iIndex = strValue.length() - 1;
                        }
                        if (iIndex > strValue.length() - 1) {
                            error("Index %s out of bounds", iIndex);
                        }
                        String tempValue;
                        if (iIndex2 == 0) {
                            tempValue = strValue.substring(0, iIndex) + resO2.value + strValue.substring(iIndex + 1);
                        } else {
                            tempValue = strValue.substring(0, iIndex) + resO2.value;
                        }
                        ResultValue finalRes = new ResultValue(SubClassif.STRING, tempValue);
                        resO1 = assign(variableStr, finalRes);
                    }
                    return resO1;
                } else if (res.structure.equals(Structure.FIXED_ARRAY) || res.structure.equals(Structure.UNBOUNDED_ARRAY)) {
                    if (!bIndex) {
                        error("Can't perform += on array");
                    } else {
                        if (res.structure != Structure.UNBOUNDED_ARRAY && iIndex >= ((ResultArray) res).declaredSize) {
                            error("Index %d out of bounds", iIndex);
                        }
                        if (iIndex < 0) {
                            if (((ResultArray) res).declaredSize != -1) {
                                iIndex += ((ResultArray) res).declaredSize;
                            } else {
                                iIndex += ((ResultArray) res).lastPopulated;
                            }
                        }
                        if (((ResultArray) res).declaredSize == -1) {
                            while (iIndex >= ((ResultArray) res).array.size()) {
                                ((ResultArray) res).array.add(null);
                            }
                        }
                        ResultValue tempRes = expr(false);
                        ResultValue tempRes2 = ((ResultArray) res).array.get(iIndex);
                        nOp2 = new Numeric(this, tempRes, "-=", "2nd Operand");
                        nOp1 = new Numeric(this, tempRes2, "-=", "1st Operand");
                        tempRes = Utility.subtraction(this, nOp2, nOp1);
                        resO1 = assignIndex(variableStr, type, iIndex, tempRes);
                    }
                    return resO1;
                } else {
                    error("Invalid structure type on %s", res.value);
                }
            } else {
                skipTo(";");
            }
        } else {
        error("Expected assignment operator but got %s", scan.currentToken.tokenStr);
        }
        return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, scan.currentToken.tokenStr);
    }

    private ResultValue expr(Boolean inFunc) throws Exception{
        Stack outStack = new Stack<ResultValue>();
        Stack stack = new Stack<Token>();
        Token popped;
        ResultValue res, resValue1, resValue2;
        boolean bFound;
        boolean bCategory = false;

        if(scan.nextToken.tokenStr.equals(";")) {
            error("Expected operand");
        }
        if(scan.currentToken.primClassif == Classif.FUNCTION && (scan.currentToken.tokenStr.equals("print"))) {
            scan.getNext();
        }
        if(scan.currentToken.primClassif != Classif.FUNCTION || scan.currentToken.tokenStr.equals("print")){
            scan.getNext();
        }
        Token prevToken = scan.nextToken;
        while(scan.currentToken.primClassif.equals(Classif.OPERAND)
                || scan.currentToken.primClassif.equals(Classif.OPERATOR)
                || scan.currentToken.primClassif.equals(Classif.FUNCTION)
                || "()".contains(scan.currentToken.tokenStr)) {
            if (scan.currentToken.primClassif.equals(Classif.EOF)) {
                error("Missing separator");
            }
            switch (scan.currentToken.primClassif) {
                case OPERAND:
                    if (bCategory) {
                        error("Unexpected operand, instead got: %s", scan.currentToken.tokenStr);
                    }
                    resValue1 = getOperand();
                    outStack.push(resValue1);
                    bCategory = true;
                    break;
                case OPERATOR:
                    if (bCategory == false && !scan.currentToken.tokenStr.equals("-")) {
                        error("Unexpected operator, instead got: %s", scan.currentToken.tokenStr);
                    }
                    if(scan.currentToken.tokenStr.equals("-")) {
                        if (prevToken.primClassif == Classif.OPERATOR || prevToken.tokenStr.equals(",") || prevToken.tokenStr.equals("(")) {
                            if (scan.nextToken.primClassif == Classif.OPERAND || scan.nextToken.tokenStr.equals("(")) {
                                stack.push(new Token("u-"));
                            } else {
                                error("Unexpected operator, instead got: %s", scan.nextToken.tokenStr);
                            }
                        }
                    } else {
                            while (!stack.empty()) {
                                if (getPrecedence(scan.currentToken, false) < getPrecedence((Token) stack.peek(), true)) {
                                    break;
                                } else if (!stack.empty()) {
                                    popped = (Token) stack.pop();
                                    resValue1 = (ResultValue) outStack.pop();
                                    if (popped.tokenStr.equals("u-")) {
                                        res = evalCond(resValue1, new ResultValue(), "u-");
                                        outStack.push(res);
                                    } else if (popped.tokenStr.equals("not")) {
                                        res = evalCond(new ResultValue(), (ResultValue) outStack.pop(), "not");
                                        outStack.push(res);
                                    } else {
                                        resValue2 = (ResultValue) outStack.pop();
                                        res = evalCond(resValue1, resValue2, popped.tokenStr);
                                    }
                                    outStack.push(res);
                                }
                            }
                            stack.push(scan.currentToken);
                    }
                    bCategory = false;

                case FUNCTION:
                    if (bCategory == true) {
                        error("Missing separator, instead got: %s", scan.currentToken.tokenStr);
                    }
                    stack.push(scan.currentToken);
                    if (scan.nextToken.tokenStr.equals("(")) {
                        scan.getNext();
                    } else {
                        error("Function statement needs '(' to start with: Func %s", scan.currentToken.tokenStr);
                    }
                    break;

                case SEPARATOR:
                    switch (scan.currentToken.tokenStr) {
                        case "(":
                            stack.push(scan.currentToken);
                            break;
                        case ")":
                            if (inFunc && scan.nextToken.tokenStr.equals(";")) {
                                break;
                            }
                            bFound = false;
                            while (!stack.empty()) {
                                popped = (Token) stack.pop();
                                if (popped.tokenStr.equals("(") || popped.primClassif.equals(Classif.FUNCTION)) {
                                    bFound = true;
                                    if (popped.primClassif.equals(Classif.FUNCTION)) {
                                        ResultValue tempRes = (ResultValue) outStack.pop();
                                        outStack.push(builtInFunctions(popped, tempRes));
                                    }
                                    break;
                                } else if (popped.tokenStr.equals("u-")) {
                                    resValue1 = (ResultValue) outStack.pop();
                                    res = evalCond(resValue1, new ResultValue(), "u-");
                                    outStack.push(res);
                                } else {
                                    resValue1 = (ResultValue) outStack.pop();
                                    resValue2 = (ResultValue) outStack.pop();
                                    res = evalCond(resValue1, resValue2, popped.tokenStr);
                                    outStack.push(res);
                                }
                            }
                            if (!bFound) {
                                error("Expected left paren");
                            }
                            break;
                    }
            }
            prevToken = scan.currentToken;
            scan.getNext();
        }
        if(scan.currentToken.subClassif.equals(SubClassif.DECLARE)){
            error("Missing separator");
        }
        while(!stack.empty()){
            popped = (Token)stack.pop();
            if(popped.tokenStr.equals("(")) {
                error("Unmatched right paren");
            }
            else if(popped.tokenStr.equals("u-")) {
                resValue1 = (ResultValue) outStack.pop();
                res = evalCond(resValue1, new ResultValue(), "u-");
                outStack.push(res);
            } else if (popped.tokenStr.equals("not")) {
                res = evalCond(new ResultValue(), (ResultValue) outStack.pop(), "not");
                outStack.push(res);
            }
            else {
                if(popped.primClassif.equals(Classif.FUNCTION)) {
                    error("Function %s missing right paren", popped.tokenStr);
                }
                resValue1 = (ResultValue) outStack.pop();
                if(outStack.empty()) {
                    error("Expected operand, instead stack is empty");
                }
                resValue2 = (ResultValue) outStack.pop();
                res = evalCond(resValue1, resValue2, popped.tokenStr);
                outStack.push(res);
            }
        }
        res = (ResultValue) outStack.pop();
        scan.setPosition(prevToken);
        res.terminatingStr = scan.nextToken.tokenStr;
        return res;
    }

    private ResultValue evalCond(ResultValue resO1, ResultValue resO2, String opStr) throws Exception {
        ResultValue res = new ResultValue();
        Numeric nOp1;
        Numeric nOp2;

        switch (opStr) {
            case "+":
                nOp2 = new Numeric(this, resO2, "+", "2nd operand");
                nOp1 = new Numeric(this, resO1, "+", "1st operand");
                res = Utility.addition(this, nOp1, nOp2);
                break;
            case "-":
                nOp2 = new Numeric(this, resO2, "-", "2nd operand");
                nOp1 = new Numeric(this, resO1, "-", "1st operand");
                res = Utility.subtraction(this, nOp1, nOp2);
                break;
            case "*":
                nOp2 = new Numeric(this, resO2, "*", "2nd operand");
                nOp1 = new Numeric(this, resO1, "*", "1st operand");
                res = Utility.multiplication(this, nOp1, nOp2);
                break;
            case "/":
                nOp2 = new Numeric(this, resO2, "/", "2nd operand");
                nOp1 = new Numeric(this, resO1, "/", "1st operand");
                res = Utility.division(this, nOp1, nOp2);
                break;
            case "^":
                nOp2 = new Numeric(this, resO2, "^", "2nd operand");
                nOp1 = new Numeric(this, resO1, "^", "1st operand");
                res = Utility.exponential(this, nOp1, nOp2);
                break;
            case ">":
                res = Utility.greaterThan(this, resO1, resO2);
                break;
            case "<":
                res = Utility.lessThan(this, resO1, resO2);
                break;
            case ">=":
                res = Utility.greaterThanOrEqual(this, resO1, resO2);
                break;
            case "<=":
                res = Utility.lessThanOrEqual(this, resO1, resO2);
                break;
            case "==":
                res = Utility.equal(this, resO1, resO2);
                break;
            case "!=":
                res = Utility.notEqual(this, resO1, resO2);
                break;
            case "u-":
                nOp1 = new Numeric(this, resO1, "u-", "Unary Minus");
                res = Utility.uMinus(this, nOp1);
                break;
            case "#":
                res = Utility.concat(this, resO1, resO2);
                break;
            default:
                error("Bad compare token");
                break;
        }
        return res;
    }

    private ResultValue ifStmt(Boolean bExec) throws Exception {
        int saveLineNr = scan.currentToken.iSourceLineNr;
        ResultValue resCond;

        if (bExec) {
            resCond = expr(false);
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

                limit = Integer.parseInt(expr(false).value);

                if(scan.getNext().equals("by")){
                    incr = Integer.parseInt(expr(false).value);
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

                res = expr(false);

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

                res = expr(false);

                if(res.structure != Structure.PRIMITIVE) {
                    error("Invalid type for for tokenizer");
                }

                str = res.value;

                if(!scan.getNext().equals("by")) {
                    error("Missing by for delimiter");
                }

                delim = expr(false).value;

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
                resExpr = expr(false);
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

    public ResultValue assignIndex(String variableStr, SubClassif type, int iIndex, ResultValue resIndex) throws Exception{
        ResultValue resultValue = new ResultValue();
        ResultArray resultArray = null;
        int populated = 0;

        if(scan.nextToken.primClassif.equals(Classif.SEPARATOR)) {
            ResultArray resultArray1 = (ResultArray) smStorage.getValue(variableStr);
            ResultValue resultValue1 = resIndex;
            if (!scan.nextToken.tokenStr.equals(";")) {
                error("Missing ;");
            }
            if (resultArray1 == null) {
                error("Variable %s not in scope", variableStr);
            }
            if (resultValue1 == null) {
                error("Operand %s not in scope", scan.nextToken.tokenStr);
            }
            if (resultValue1.structure.equals(Structure.PRIMITIVE)) {
                if (type.equals(SubClassif.INTEGER)) {
                    resultValue = resultValue1.clone();
                    resultValue.value = Utility.castInt(this, resultValue);
                    resultValue.type = SubClassif.INTEGER;
                    resultArray1.array.set(iIndex, resultValue);
                } else if (type.equals(SubClassif.FLOAT)) {
                    resultValue = resultValue1.clone();
                    resultValue.value = Utility.castFloat(this, resultValue);
                    resultValue.type = SubClassif.FLOAT;
                    resultArray1.array.set(iIndex, resultValue);
                }
                if (type.equals(SubClassif.BOOLEAN)) {
                    resultValue = resultValue1.clone();
                    resultValue.value = Utility.castBoolean(this, resultValue);
                    resultValue.type = SubClassif.BOOLEAN;
                    resultArray1.array.set(iIndex, resultValue);
                }
                if (type.equals(SubClassif.STRING)) {
                    resultValue = resultValue1.clone();
                    resultValue.type = SubClassif.STRING;
                    resultArray1.array.set(iIndex, resultValue);
                }
            } else {
                error("Can't assign structure into index", resultValue1.structure);
            }
            for (ResultValue resTemp : resultArray1.array) {
                if (resTemp != null) {
                    populated++;
                }
            }
            if (resultArray1.declaredSize == -1) {
                resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.UNBOUNDED_ARRAY, populated, resultArray.declaredSize);
            } else {
                resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.FIXED_ARRAY, populated, resultArray.declaredSize);
            }
            smStorage.insertValue(variableStr, resultArray);
        }
        else {
            scan.nextToken.printToken();
            error("Can't asssign %s into index", scan.nextToken.tokenStr);
        }

        return null;
    }

    private ResultArray assignArrayStmt(String variableStr, SubClassif type, int declared) throws Exception {
        ResultValue resExpr;
        ResultArray resultArray = new ResultArray();
        int populated = 1;
        if(scan.nextToken.primClassif == Classif.OPERAND) {
            ResultArray resultArray1 = (ResultArray) smStorage.getValue(variableStr);
            ResultValue resultValue = expr(false);

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

    public ArrayList<ResultValue> getArray() throws Exception {
        ResultValue resValue = new ResultValue();
        ArrayList<ResultValue> resValueList = new ArrayList<>();
        while (!resValue.terminatingStr.equals(";") && !scan.currentToken.tokenStr.equals(";")){
            scan.getNext();
            resValue = expr(false);
            if(scan.currentToken.subClassif.equals(SubClassif.INTEGER)) {
                resValue.value = Utility.castInt(this, resValue);
                resValue.type = SubClassif.INTEGER;
                resValueList.add(resValue);
            }
            else if(scan.currentToken.subClassif.equals(SubClassif.FLOAT)){
                resValue.value = Utility.castFloat(this, resValue);
                resValue.type = SubClassif.FLOAT;
                resValueList.add(resValue);
            } else if(scan.currentToken.subClassif.equals(SubClassif.BOOLEAN)){
                resValue.value = Utility.castBoolean(this, resValue);
                resValue.type = SubClassif.BOOLEAN;
                resValueList.add(resValue);
            } else if(scan.currentToken.subClassif.equals(SubClassif.STRING)) {
                resValue.type = SubClassif.STRING;
                resValueList.add(resValue);
            } else {
                error("Invalid list type: %s", scan.currentToken.tokenStr);
            }
        }
        return resValueList;
    }

    public ResultValue getOperand() throws Exception {
        Token op = scan.currentToken;
        ResultValue resultValue1;
        ResultValue index = null;
        ResultValue index2 = null;
        if(op.subClassif.equals(SubClassif.IDENTIFIER)) {
            resultValue1 = smStorage.getValue(op.tokenStr);
            if (resultValue1 == null) {
                error("Variable has not been yet declared: %s", op.tokenStr);
            }
        }
        else {
            resultValue1 = new ResultValue(op.subClassif, op.tokenStr);
        }
        if(scan.nextToken.tokenStr.equals("[")) {
            if(resultValue1.structure.equals(Structure.PRIMITIVE) && resultValue1.type != SubClassif.STRING) {
                error("Type can't be indexed");
            }
            ResultValue tempRes = smStorage.getValue(scan.currentToken.tokenStr);
            if(tempRes == null){
                error("Variable has not been yet declared: %s", scan.currentToken.tokenStr);
            }
            String value = scan.currentToken.tokenStr;

            scan.getNext();
            index = expr(false);
            scan.getNext();
            if(tempRes.structure != Structure.PRIMITIVE) {
                if(index2 == null) {
                    ResultArray resultArray1 = (ResultArray) smStorage.getValue(value);
                    if(resultArray1 == null){
                        error("Variable has not been yet declared: %s", op.tokenStr);
                    }
                    int iIndex = Integer.parseInt(Utility.castInt(this, index));
                    if(iIndex < 0) {
                        if(resultArray1.declaredSize != -1) {
                            iIndex += ((ResultArray) resultArray1).declaredSize;
                        }
                        else {
                            iIndex += resultArray1.lastPopulated;
                        }
                    }
                    if(resultArray1.declaredSize != -1 && iIndex >= resultArray1.declaredSize) {
                        error("Trying to reference an index outside of bounds of array");
                    }
                    else if(resultArray1.declaredSize == -1 && resultArray1.array.get(iIndex) == null) {
                        error("Index %d has not been initialized", iIndex);
                    }
                    else if(resultArray1.array.get(iIndex) == null) {
                        error("Index %d has not been initialized", iIndex);
                    }
                    resultValue1 = resultArray1.array.get(iIndex);
                }
            }
            else {
                if(index2 == null) {
                    resultValue1 = smStorage.getValue(value);
                    if(resultValue1 == null){
                        error("Variable has not been yet declared: %s", op.tokenStr);
                    }
                    String strVal = resultValue1.value;
                    if(index.value.equals("-1")) {
                        index.value = String.valueOf(resultValue1.value.length() - 1);
                    }
                    else if(Integer.valueOf(index.value) < 0) {
                        index.value = String.valueOf(strVal.length() + Integer.valueOf(index.value));
                    }
                    if(strVal.length() -1 < Integer.valueOf(index.value)) {
                        error("Index %d out of bounds for array %s", Integer.valueOf(index.value), strVal);
                    }
                    char ch = strVal.charAt((Integer.parseInt(Utility.castInt(this, index))));
                    resultValue1 = new ResultValue(SubClassif.STRING, String.valueOf(ch));
                }
                else {
                    resultValue1 = smStorage.getValue(value);
                    String strVal = resultValue1.value;
                    strVal = strVal.substring((Integer.parseInt(Utility.castInt(this, index))), (Integer.parseInt(Utility.castInt(this, index2))));
                    resultValue1 = new ResultValue(SubClassif.STRING, strVal);
                }
            }
        }
        return resultValue1;
    }

    private ResultValue builtInFunctions(Token funcName, ResultValue parm) throws Exception {
        ResultValue res = null;
        String value = "";
        SubClassif type = SubClassif.BUILTIN;
        if(funcName.tokenStr.equals("LENGTH")) {
            res = Utility.LENGTH(parm.value);
        }
        else if(funcName.tokenStr.equals("SPACES")) {
            res = Utility.SPACES(parm.value);
        }
        else if(funcName.tokenStr.equals("ELEM")) {
            ResultArray array = (ResultArray) parm;
            res = Utility.ELEM(this, array);
        }
        else if(funcName.tokenStr.equals("MAXELEM")) {
            ResultArray array = (ResultArray) parm;
            res = Utility.MAXELEM(array);
        }
        return res;
    }

    public int getPrecedence(Token operator, Boolean inStack) {
        int prec;
        if(inStack) {
            prec = precedence.getStackPrecedence(operator.tokenStr);
        }
        else {
            prec = precedence.getTokenPrecedence(operator.tokenStr);
        }
        return prec;
    }


}