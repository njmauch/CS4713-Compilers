package pickle;


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


    /**
     * Parser(Scanner scan, StorageManager storageManager, SymbolTable symbolTable, Precedence precedence)
     * Parser construct that takes scanner, storageManager, symboltable, and precedence hashmap
     * this objects are used throughout the parser class
     * @param scan object that is used to get the next token and navigate through source file
     * @param storageManager object that contains and declared variables and their values and types
     * @param symbolTable object that contains the definition for pickle
     * @param precedence object that contains the precedence of token in stack or as a token
     */
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

    /**
     * void skipTo(String tokenStr) throws Exception
     * Function that skips to the next instance of tokenStr in the source file
     * @param tokenStr variable passed in that we skip to until it is found
     * @throws Exception general error
     */
    private void skipTo(String tokenStr) throws Exception {
        //until tokenStr is found, get the next token
        while (! scan.currentToken.tokenStr.equals(tokenStr) && scan.currentToken.primClassif != Classif.EOF)
            scan.getNext();
    }

    /**
     * void error(String fmt, Object... varArgs) throws Exception
     * Print error when called. handles various formats of errors
     * @param fmt string that contains the error message and variable
     * @param varArgs variable number of arguments used in the string
     * @throws Exception generic exception
     */
    public void error(String fmt, Object... varArgs) throws Exception
    {
        String diagnosticTxt = String.format(fmt, varArgs);
        throw new ParserException(Scanner.iSourceLineNr, diagnosticTxt, this.sourceFileNm);
    }

    /**
     * ResultValue statement (boolean bExec) throws Exception
     * Function that is beginning of parsing through source file.  Based on type of token that
     * is given from scanner, will decide what to do with that token and that line of code
     * in pickle
     * @param bExec boolean to decide if certain code is to be executed or to be skipped
     * @return
     * @throws Exception generic exception
     */
    public ResultValue statement (boolean bExec) throws Exception {
        //get the next token
        scan.getNext();

        //if end of file, return void ResultValue and end parsing the source file
        if(scan.currentToken.primClassif.equals(Classif.EOF)) {
            return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, "");
        }
        //if not EOF, then token is CONTROL/OPERAND/FUNCTION
        else if(scan.currentToken.primClassif.equals(Classif.CONTROL)) {
            //if Primary class is control and sub class is Declare so call declareStmt function
            if(scan.currentToken.subClassif.equals(SubClassif.DECLARE)) {
                return declareStmt(bExec);
            }
            //Primary class is CONTROL, and sub class is FLOW, determine if 'if', 'while' or 'for' statement
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
            //is an end token (endif, endfor, endwhile) so break out and go to next token if exist
            else if(scan.currentToken.subClassif.equals(SubClassif.END)) {
                return new ResultValue(SubClassif.END, "", Structure.PRIMITIVE, scan.currentToken.tokenStr);
            }
            //something went wrong if we get this
            else {
                error("Invalid control variable %s", scan.currentToken.tokenStr);
            }
        }
        //if token is operand, assign the token and call asssignmentStmt
        else if(scan.currentToken.primClassif.equals(Classif.OPERAND)) {
            return assignmentStmt(bExec);
        }
        //Token is function, go to functionStmt method to determine what kind of function
        else if(scan.currentToken.primClassif.equals(Classif.FUNCTION)) {
            return functionStmt(bExec);
        }
        return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, scan.currentToken.tokenStr);
    }

    /**
     * ResultValue statements(Boolean bExec, String termStr) throws Exception
     * This method is utilized by ifStmt, whileStmt, and forStmt
     * It executes from current position all the way up to the terminating str
     * @param bExec boolean to decide if section of code is executed or skipped
     * @param termStr token that we loop through till found
     * @return
     * @throws Exception
     */
    public ResultValue statements(Boolean bExec, String termStr) throws Exception {
        //execute first statement
        ResultValue res = statement(bExec);
        //loop through source file until we reach the terminating string
        while(! termStr.contains(res.terminatingStr)) {
            if((res.terminatingStr.equals("break") || res.terminatingStr.equals("continue")) && bExec) {
                break;
            }
            //execute next statement
            res = statement(bExec);
        }
        return res;
    }

    /**
     * ResultValue functionStmt (boolean bExec) throws Exception
     * This method is to run any builtin functions in pickle.  Currently implemented built in
     * functions include, print(), LENGTH(), SPACES(), ELEM(), MAXELEM();
     *
     * @param bExec decides if code is being executed or being skipped
     * @return
     * @throws Exception
     */
    private ResultValue functionStmt (boolean bExec) throws Exception {
        ResultValue res = null;
        //Checks if built in function, there is possibility of user defined but not implemented in current state
        if(scan.currentToken.subClassif == SubClassif.BUILTIN) {
            //if bExec is false, skip code
            if(!bExec) {
                skipTo(";");
                res = new ResultValue(SubClassif.BUILTIN, "", Structure.PRIMITIVE, scan.currentToken.tokenStr);
            }
            //we are executing function
            //print function, call print method to print statement
            else if(scan.currentToken.tokenStr.equals("print")) {
                res = print();
            }
            //
            else if(scan.currentToken.tokenStr.equals("SPACES") || scan.currentToken.tokenStr.equals("ELEM")
                    || scan.currentToken.tokenStr.equals("MAXELEM") || scan.currentToken.tokenStr.equals("LENGTH")) {
                res = expr(false);
            }
            else if(scan.currentToken.tokenStr.equals("dateDiff") || scan.currentToken.tokenStr.equals("dateAge")
                    || scan.currentToken.tokenStr.equals("dateAdj")) {
                String dateFunction = scan.currentToken.tokenStr;
                ResultValue date1 = expr(true);
                Token prevToken;
                scan.getNext();

                while(scan.currentToken.tokenStr.equals(")")) {
                    scan.getNext();
                }

                ResultValue date2 = expr(true);
                prevToken = scan.currentToken;

                while(scan.currentToken.tokenStr.equals(")")) {
                    scan.getNext();
                }

                switch(dateFunction) {
                    case "dateDiff" -> res = Utility.dateDiff(this, date1, date2);
                    case "dateAge" -> res = Utility.dateAge(this, date1, date2);
                    case "dateAdj" -> res = Utility.dateAdj(this, date1, Integer.parseInt(date2.value));
                }
                if(!prevToken.tokenStr.equals(")") && scan.nextToken.primClassif != Classif.EOF) {
                    error("Missing closing paren");
                }
                scan.setPosition(prevToken);
                return res;
            }
            //function was called but is not a built in or defined function
            else {
                error("No function found with name %s", scan.currentToken.tokenStr);
            }
        }
        return res;
    }

    /**
     * ResultValue print() throws Exception
     * Print function, prints statement to the screen
     * @return
     * @throws Exception
     */
    private ResultValue print() throws Exception {
        //get the name of the function in case of error
        String funcName = scan.currentToken.tokenStr;
        ResultValue res = null;
        //start building string to be printed to screen
        String line = "";
        Token prevToken = null;
        //start building the string
        while(!scan.currentToken.tokenStr.equals(";")) {
            res = expr(true);
            //add the value returned from expr to the string
            if(res.structure.equals(Structure.FIXED_ARRAY) || res.structure.equals(Structure.UNBOUNDED_ARRAY)) {
                ResultArray array = (ResultArray)smStorage.getValue(res.value);
                ArrayList<ResultValue> resultList = array.array;
                for(ResultValue resTemp : resultList) {
                    if(resTemp == null){
                        continue;
                    }
                    line += resTemp.value + " ";
                }
            }
            else {
                line += res.value + " ";
            }
            prevToken = scan.currentToken;
            //get next token
            scan.getNext();
            while (scan.currentToken.tokenStr.equals(")")) {
                scan.getNext();
            }
            //if we reached end of file, then something went wrong
            if (scan.currentToken.primClassif.equals(Classif.EOF)) {
                error("Missing ';'");
            }
            //The end of the line should be ended by a separator
            if (scan.currentToken.primClassif != Classif.SEPARATOR) {
                error("Missing separator");
            }
        }
        if(!prevToken.tokenStr.equals(")") && scan.nextToken.primClassif != Classif.EOF) {
            error("Func %s missing closing paren", funcName);
        }
        //print out the line
        System.out.println(line);
        return res;
    }

    /**
     * ResultValue whileStmt(boolean bExec) throws Exception
     * whileStmt Function executes while statements in pickle
     * @param bExec decides if statements are to be executed or skipped
     * @return
     * @throws Exception
     */
    private ResultValue whileStmt(boolean bExec) throws Exception {
        ResultValue resCond;
        Token tempToken;

        //temp token used to go back to top of while loop to check if condition is still true
        tempToken = scan.currentToken;
        //true, so we are executing statements inside while
        if (bExec) {
            //evaluate expression, determine if condition is true or false
            resCond = expr(false);
            //if condition returned true, execute statements
            while(resCond.value.equals("T")) {
                //run through the statements inside the while loop until "endwhile" is found
                resCond = statements(true, "endwhile");
                if(resCond.terminatingStr.equals("break") || resCond.terminatingStr.equals("continue")) {
                    if(! scan.getNext().equals(";")) {
                        error("Expected ';' after %s", resCond.terminatingStr);
                    }
                    if(resCond.terminatingStr.equals("break")) {
                        break;
                    }
                    else {
                        resCond = statements(false, "endwhile");
                    }
                }
                //if endwhile is not found then print error
                if(! resCond.terminatingStr.equals("endwhile")){
                    error("Expected endwhile for while beggining line %s, got %s", tempToken.iSourceLineNr, resCond.value);
                }
                //go back to beginning of while to recheck condition statement
                scan.setPosition(tempToken);
                //get next token
                //scan.getNext();
                //evaluate condition again to determine if we execute while loop again
                resCond = expr(false);
            }
            //condition returned false so skip ahead to endwhile
            resCond = statements(false, "endwhile");
        }
        //we were told to ignore execution so we skip condition and then skip to endwhile
        else {
            skipTo(":");
            resCond = statements(false, "endwhile");
        }
        //checks to make sure that while statement ends in 'endwhile;'
        if(! resCond.terminatingStr.equals("endwhile")) {
            error("Expected endwhile for while beggining line %s", tempToken.iSourceLineNr);
        }
        if(! scan.nextToken.tokenStr.equals(";")) {
            error("Expected ; after endwhile");
        }
        return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, ";");
    }

    /**
     * ResultValue declareStmt(boolean bExec) throws Exception
     * declareStmt function called when a control declare token is encountered
     * it uses a switch statement to determine type of subclass and assigns it
     * to the next token found as it is the variable name
     * Can also assign a value to declared variable if '=' is found
     * @param bExec boolean to decide if we execute or not
     * @return
     * @throws Exception
     */
    private ResultValue declareStmt(boolean bExec) throws Exception {
        //default set structure type to primitive
        Structure structure = Structure.PRIMITIVE;

        SubClassif dclType = SubClassif.EMPTY;

        //determine what kind of variable is being declared in the source file
        switch (scan.currentToken.tokenStr) {
            case "Int" -> dclType = SubClassif.INTEGER;
            case "Float" -> dclType = SubClassif.FLOAT;
            case "String" -> dclType = SubClassif.STRING;
            case "Bool" -> dclType = SubClassif.BOOLEAN;
            case "Date" -> dclType = SubClassif.DATE;
            default -> error("Unknown declare type %s", scan.currentToken.tokenStr);
        }
        // get the variable name from source file
        scan.getNext();

        //ensure that the next token is a variable name
        if((scan.currentToken.primClassif != Classif.OPERAND) || (scan.currentToken.subClassif != SubClassif.IDENTIFIER))  {
            error("Expected variable for target %s", scan.currentToken.tokenStr);
        }

        //get the name of the variable
        String variableStr = scan.currentToken.tokenStr;

        //we are executing
        if(bExec) {
            //token used to skip to in arrays for assignment
            Token tempToken = scan.currentToken;
            //check if the variable is an array
            if (scan.nextToken.tokenStr.equals("[")) {
                //advance token to the left bracket
                scan.getNext();

                //set structure to fixed array
                structure = Structure.FIXED_ARRAY;
                //if declare statment is var[] then length of array is not declared
                if (scan.nextToken.tokenStr.equals("]")) {
                    //advance token to right bracket
                    scan.getNext();
                    //array can not be declared without either size or list of values
                    if (scan.nextToken.tokenStr.equals(";")) {
                        error("Can't declare array without length");
                    }
                    //we are assigning values to array so insert into symbol table and storage manager
                    else if (scan.nextToken.tokenStr.equals("=")) {
                        //advance the token to the '='
                        scan.getNext();
                        //insert array into the symbol table
                        symbolTable.putSymbol(variableStr, new SymbolTable.STIdentifier(variableStr
                                , tempToken.primClassif, tempToken.subClassif, dclType, structure));
                        //insert array into storage manager
                        smStorage.insertValue(variableStr, new ResultArray(tempToken.tokenStr, dclType, structure));
                        //call declare array to get the values of the array being declared
                        return declareArray(bExec, variableStr, dclType, 0);
                    }
                    //we found something we aren't supposed to
                    else {
                        error("Invalid symbol: %s", scan.nextToken.tokenStr);
                    }
                }
                //the size of array is declared
                else if (scan.nextToken.primClassif != Classif.OPERATOR) {
                    //if we encounter a variable used to declare size, ensure that variable value is declared
                    if (scan.nextToken.subClassif.equals(SubClassif.IDENTIFIER)) {
                        if (smStorage.getValue(scan.nextToken.tokenStr) == null) {
                            error("%s is not defined", scan.nextToken.tokenStr);
                        }
                    }
                    //save the left bracket
                    Token leftToken = scan.currentToken;
                    //checking for right bracket
                    skipTo("]");
                    //go back to the left bracket
                    scan.setPosition(leftToken);
                    //use expression to find the length of the array
                    int dclLength = Integer.parseInt(Utility.castInt(this, expr(false)));
                    //if length is negative, throw error
                    if (dclLength < 0) {
                        error("Array size must be positive");
                    }
                    //right bracket
                    scan.getNext();
                    //get = or ;
                    scan.getNext();
                    //list of values not given for array but array is still declared
                    if (scan.currentToken.tokenStr.equals(";")) {
                        //put the new array in the symbol table
                        symbolTable.putSymbol(variableStr, new SymbolTable.STIdentifier(variableStr, tempToken.primClassif, tempToken.subClassif, dclType, Structure.FIXED_ARRAY));
                        //create the an array of null values based on length declared for array
                        ArrayList<ResultValue> tempArrayList = new ArrayList<>();
                        for (int j = 0; j < dclLength; j++) {
                            tempArrayList.add(null);
                        }
                        //insert the new array into the storagemanager
                        smStorage.insertValue(variableStr, new ResultArray(tempToken.tokenStr, tempArrayList, dclType, structure, 0, dclLength));
                        return new ResultValue(SubClassif.DECLARE, "", Structure.FIXED_ARRAY, scan.currentToken.tokenStr);
                    }
                    //list of values is given for the array to call function declareArray
                    else if (scan.currentToken.tokenStr.equals("=")) {
                        //insert the array into the symbol table
                        symbolTable.putSymbol(variableStr, new SymbolTable.STIdentifier(variableStr, tempToken.primClassif, tempToken.subClassif, dclType, Structure.FIXED_ARRAY));
                        //create a temporary array of null values to be able to insert into storage manager
                        ArrayList<ResultValue> tempArrayList = new ArrayList<>();
                        for (int j = 0; j < dclLength; j++) {
                            tempArrayList.add(null);
                        }
                        //insert into storagemanager
                        smStorage.insertValue(variableStr, new ResultArray(tempToken.tokenStr, tempArrayList, dclType, structure, 0, dclLength));
                        //call on declare array to populate the list of values
                        return declareArray(bExec, variableStr, dclType, dclLength);
                    }
                    //found something bad
                    else {
                        error("Expected = or ; and got: %s", scan.currentToken.tokenStr);
                    }
                } else {
                    error("Invalid length: %s", scan.nextToken.tokenStr);
                }
            }
            //we are not declaring an array
            else {
                //we found a right bracket so bad syntax in source file
                if (scan.nextToken.tokenStr.equals("]")) {
                    error("Missing [ with ]");
                }
                //put the variable into the symbol table
                symbolTable.putSymbol(variableStr, new SymbolTable.STIdentifier(variableStr,
                        scan.currentToken.primClassif, scan.currentToken.subClassif, dclType, structure));
                //put the varibale into the storage manager
                smStorage.insertValue(variableStr, new ResultValue(dclType, "", structure));
            }
        }
        //if '=' is found, call on assignmentStmt to assign variable
        if(scan.nextToken.tokenStr.equals("=")) {
            return assignmentStmt(bExec);
        }
        //we encountered an array
        else if(scan.nextToken.tokenStr.equals("[")){
            //advance to the left bracket
            scan.getNext();
            //setting structure as fixed array
            structure = Structure.FIXED_ARRAY;
            //check if size is declared
            if (scan.nextToken.tokenStr.equals("]")) {
                //size isn't declared so get the right bracket
                scan.getNext();

                //can't declare array without size or list of values
                if (scan.nextToken.tokenStr.equals(";"))
                    error("Can't declare array without length");
                    //we are setting values to the array
                else if (scan.nextToken.tokenStr.equals("="))
                {
                    //get the '=' token
                    scan.getNext();
                    //call declareArray to fill the values into the array
                    return declareArray(bExec, variableStr, dclType, 0);
                }
                //got something wrong
                else
                    error("Expected = or ; and got: %s", scan.nextToken.tokenStr);
            }
            //length of the array is given so get the length
            int length = Integer.parseInt(Utility.castInt(this, expr(false)));
            //if the length is negative, throw error
            if (length < 0) {
                error("Array size must be positive");
            }

            //advance to right bracket
            scan.getNext();
            //advance to either ';' or '='
            scan.getNext();
            //values not given for array but we are still declaring the array
            if (scan.currentToken.tokenStr.equals(";")) {
                return new ResultValue(SubClassif.DECLARE, "", Structure.FIXED_ARRAY, scan.currentToken.tokenStr);
            }
            //list of values was given so we use declareArray to set the array values
            else if (scan.currentToken.tokenStr.equals("=")) {
                return declareArray(bExec, variableStr, dclType, length);
            }
            //we got something wrong
            else {
                error("Expected = or ; and got: %s", scan.nextToken.tokenStr);
            }
        }
        else if (scan.nextToken.primClassif == Classif.OPERATOR) {
            error("Can't perform declare before being initialized: %s", scan.nextToken.tokenStr);
        }
        //check for terminating ; on the statement
        else if(! scan.getNext().equals(";")) {
            error("Declare statment not terminated");
        }
        return new ResultValue(SubClassif.DECLARE,"",  Structure.PRIMITIVE, scan.currentToken.tokenStr);
    }

    /**
     * ResultValue assignmentStmt(boolean bExec) throws Exception
     * assignmentStmt is called whenever we encounter an '='.  Determines the value
     * of the expression after the '=' and assign it to the variable
     * also handles '+=' and '-='
     * @param bExec decides if we are executing or not
     * @return
     * @throws Exception
     */
    public ResultValue assignmentStmt(boolean bExec) throws Exception {
        ResultValue res;
        SubClassif type = SubClassif.EMPTY;
        Numeric nOp2;
        Numeric nOp1;
        int iIndex = 0, iIndex2 = 0;
        boolean bIndex = false;
        ResultValue resO2;
        ResultValue resO1 = null;
        //if we are executing, get the data type of the variable as well as determing if
        //variable is declared
        if(bExec) {
            try{
                type = smStorage.getValue(scan.currentToken.tokenStr).type;
            }
            catch(Exception e) {
                error("Variable has not yet been declared: %s", scan.currentToken.tokenStr);
            }
        }
        //make sure the token is a variable that can be assigned a value
        if(scan.currentToken.subClassif != SubClassif.IDENTIFIER) {
            error("Expected a variable for the target assignment %s", scan.currentToken.tokenStr);
        }
        //get the name of the variable
        String variableStr = scan.currentToken.tokenStr;
        //get the value of the variable from storagemanager
        res = smStorage.getValue(variableStr);

        //ensure that the variable has been declared
        if(res == null && bExec){
            error("%s required to be declared", variableStr);
        }
        //advance token to either right bracket, '=', '-=', or '+='
        scan.getNext();
        //if we encountered array, get the index value given for assignment
        if(scan.currentToken.tokenStr.equals("[")){
            if(scan.nextToken.tokenStr.equals("~")) {
                iIndex = 0;
            }
            else {
                //get the index value inside brackets
                iIndex = Integer.parseInt(Utility.castInt(this, expr(false)));
            }
            //advance to right bracket
            scan.getNext();
            if(scan.currentToken.tokenStr.equals("~")) {
                if(scan.nextToken.tokenStr.equals("]")) {
                    iIndex2 = smStorage.getValue(variableStr).value.length();
                }
                else {
                    iIndex2 = Integer.parseInt(Utility.castInt(this, expr(false)));
                }
                scan.getNext();
            }
            //advance to operator
            scan.getNext();
            //boolean used to say we are assigning value to index of array
            bIndex = true;
        }

        //ensuring token is operator
        if(scan.currentToken.primClassif != Classif.OPERATOR) {
            error("Expected operator but got: %s", scan.currentToken.tokenStr);
        }

        //finding out what kind of operation is being performed
        switch (scan.currentToken.tokenStr) {
            //assigning value
            case "=":
                //we are executing
                if (bExec) {
                    //get the structure or the variable
                    switch (res.structure) {
                        //not an array so we do a simple assignment
                        case PRIMITIVE -> {
                            if (!bIndex) {
                                //call assign to assign the result from the expresion after = to the variable
                                resO1 = assign(variableStr, expr(false));
                                if (scan.currentToken.primClassif != Classif.OPERAND) {
                                    scan.getNext();
                                }
                            }
                            //we have encountered changing a string
                            else {
                                //get the substring that is going into the current string
                                resO2 = expr(false);
                                //get the current value of the variable that is being changed
                                String strValue = smStorage.getValue(variableStr).value;
                                if (iIndex == -1) {
                                    iIndex = strValue.length() - 1;
                                }
                                if (iIndex > strValue.length() - 1) {
                                    error("Index %s out of bounds", iIndex);
                                }
                                //temp string that has index being updated
                                String tempValue;
                                //insert old string up to index, replace index value with given value (resO2), then fill rest of string with rest of old string
                                if(iIndex == 0) {
                                    tempValue = strValue.substring(0, iIndex) + resO2.value + strValue.substring(iIndex + 1);
                                }
                                else {
                                    tempValue = strValue.substring(0, iIndex) + resO2.value + strValue.substring(iIndex2);
                                }
                                //create new ResultValue with new string
                                ResultValue finalRes = new ResultValue(SubClassif.STRING, tempValue);
                                //assign new result to the already existing variable
                                resO1 = assign(variableStr, finalRes);
                            }
                            return resO1;
                        }
                        case FIXED_ARRAY, UNBOUNDED_ARRAY -> {
                            if (!bIndex) {
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
                        }
                        default -> error("Invalid structure type on %s", res.value);
                    }
                }
                //not executing
                else {
                    skipTo(";");
                }
                break;
            //encountered an assign with addition
            case "+=":
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
                            if(iIndex2 == 0) {
                                tempValue = strValue.substring(0, iIndex) + resO2.value + strValue.substring(iIndex + 1);
                            }
                            else {
                                tempValue = strValue.substring(0, iIndex) + resO2.value + strValue.substring(iIndex2);
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
                break;
            case "-=":
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
                            if(iIndex2 == 0) {
                                tempValue = strValue.substring(0, iIndex) + resO2.value + strValue.substring(iIndex + 1);
                            }
                            else {
                                tempValue = strValue.substring(0, iIndex) + resO2.value + strValue.substring(iIndex2);
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
                break;
            default:
                error("Expected assignment operator but got %s", scan.currentToken.tokenStr);
                break;
        }
        return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, scan.currentToken.tokenStr);
    }

    /***
     * private ResultValue expr()
     * This function will run the expression and return the value to called
     * @param
     * @return          - ResultValue - Result after execute the statement
     * @throws Exception
     */
    private ResultValue expr(Boolean inFunc) throws Exception{
        Stack<ResultValue> outStack = new Stack<>();
        Stack<Token> stack = new Stack<>();
        Token popped;								//Poped tokens
        ResultValue res, resValue1, resValue2;		//Setup Return values
        boolean bFound;
        boolean bCategory = false;

        //If the next token is ;, the error, it has to be an OPERAND
        if(scan.nextToken.tokenStr.equals(";")) {
            error("Expected operand");
        }
        //Function print
        if(scan.currentToken.primClassif == Classif.FUNCTION && (scan.currentToken.tokenStr.equals("print") ||
                scan.currentToken.tokenStr.startsWith("date"))) {
            scan.getNext();
        }
        //If not print
        if(scan.currentToken.primClassif != Classif.FUNCTION || scan.currentToken.tokenStr.equals("print")
                || scan.currentToken.tokenStr.startsWith("date")){
            scan.getNext();
        }
        Token prevToken = scan.currentToken;	//Save the current Token

        /*
         * Keep scanning for OPERAND, OPERATOR or FUNCTION
         */
        while(scan.currentToken.primClassif.equals(Classif.OPERAND)
                || scan.currentToken.primClassif.equals(Classif.OPERATOR)
                || scan.currentToken.primClassif.equals(Classif.FUNCTION)
                || "()".contains(scan.currentToken.tokenStr)) {
            //Missing separator
            if (scan.currentToken.primClassif.equals(Classif.EOF)) {
                error("Missing separator");
            }
            switch (scan.currentToken.primClassif) {
                case OPERAND:	//If the current token is OPERAND
                    if (bCategory) {
                        error("Unexpected operand, instead got: %s", scan.currentToken.tokenStr);
                    }
                    resValue1 = getOperand();
                    outStack.push(resValue1);
                    bCategory = true;
                    break;
                case OPERATOR:	//If the current token is OPERATOR
                    //Only vailid for OPERATOR - and not
                    if (!bCategory && !scan.currentToken.tokenStr.equals("-") && !scan.currentToken.tokenStr.equals("not")) {
                        error("Unexpected operator, instead got: %s", scan.currentToken.tokenStr);
                    }

                    switch(scan.currentToken.tokenStr) {
                        //If the OPERATOR is not
                        case "not":
                            if (scan.nextToken.primClassif == Classif.OPERATOR || scan.nextToken.tokenStr.equals(",") || scan.nextToken.tokenStr.equals("(")) {
                                stack.push(scan.currentToken);
                            }
                            break;
                        //If the OPERATOR is -
                        case "-":
                            if (prevToken.primClassif == Classif.OPERATOR || prevToken.tokenStr.equals(",") || prevToken.tokenStr.equals("(")) {
                                if (scan.nextToken.primClassif == Classif.OPERAND || scan.nextToken.tokenStr.equals("(")) {
                                    stack.push(new Token("u-"));
                                } else {
                                    error("Unexpected operator, instead got: %s", scan.nextToken.tokenStr);
                                }
                                break;
                            }
                        default:
                            while (!stack.empty()) {
                                if (getPrecedence(scan.currentToken, false) > getPrecedence((Token) stack.peek(), true)) {
                                    break;
                                } else if (!stack.empty()) {
                                    popped = stack.pop();
                                    resValue1 = outStack.pop();
                                    if (popped.tokenStr.equals("u-")) {
                                        res = evalCond(resValue1, new ResultValue(), "u-");
                                        outStack.push(res);
                                    }
                                    else if (popped.tokenStr.equals("not")) {
                                        res = evalCond(outStack.pop(), new ResultValue(), popped.tokenStr);
                                        res.terminatingStr = popped.tokenStr;
                                    }
                                    else {
                                        resValue2 = outStack.pop();
                                        res = evalCond(resValue2, resValue1, popped.tokenStr);
                                    }
                                    outStack.push(res);
                                }
                            }
                            stack.push(scan.currentToken);
                            break;
                    }
                    bCategory = false;
                    break;
                case FUNCTION:	//If the current token is FUNCTION
                    if (bCategory) {
                        error("Missing separator, instead got: %s", scan.currentToken.tokenStr);
                    }
                    if(scan.currentToken.tokenStr.startsWith("date")) {
                        ResultValue dateResult = functionStmt(true);
                        outStack.push(dateResult);
                        bCategory = true;
                        break;
                    }
                    stack.push(scan.currentToken);
                    if (scan.nextToken.tokenStr.equals("(")) {
                        scan.getNext();
                    } else {
                        error("Function statement needs '(' to start with: Func %s", scan.currentToken.tokenStr);
                    }
                    break;
                case SEPARATOR:	//If the current token is SEPARATOR
                    switch (scan.currentToken.tokenStr) {
                        case "(" -> stack.push(scan.currentToken);        //Push any (
                        case ")" -> {                                    //Pop any )
                            if (inFunc && scan.nextToken.tokenStr.equals(";")) {
                                break;
                            }
                            bFound = false;
                            if(scan.nextToken.tokenStr.equals(")")) {
                                bFound = true;
                            }
                            while (!stack.empty()) {
                                popped = stack.pop();
                                if (popped.tokenStr.equals("(") || popped.primClassif.equals(Classif.FUNCTION)) {
                                    bFound = true;
                                    if (popped.primClassif.equals(Classif.FUNCTION)) {
                                        ResultValue tempRes = outStack.pop();
                                        outStack.push(builtInFunctions(popped, tempRes));
                                    }
                                    break;
                                } else if (popped.tokenStr.equals("u-")) {
                                    resValue1 = outStack.pop();
                                    res = evalCond(resValue1, new ResultValue(), "u-");
                                    outStack.push(res);
                                } else if (popped.tokenStr.equals("not")) {
                                    outStack.push(evalCond(outStack.pop(), new ResultValue(), "not"));
                                } else {
                                    resValue1 = outStack.pop();
                                    resValue2 = outStack.pop();
                                    res = evalCond(resValue2, resValue1, popped.tokenStr);
                                    outStack.push(res);
                                }
                            }
                            if (!bFound) {
                                error("Expected left paren");
                            }
                        }
                    }
            }
            prevToken = scan.currentToken;	//Save current token to pervious token
            scan.getNext();
        }
        //Missing separator
        if(scan.currentToken.subClassif.equals(SubClassif.DECLARE)){
            error("Missing separator");
        }
        //After reaching the end of stack, if the stack is not empty
        while(!stack.empty()){
            popped = stack.pop();
            //Error with ()
            if(popped.tokenStr.equals("(")) {
                error("Unmatched right paren");
            }
            else if(popped.tokenStr.equals("u-")) {
                resValue1 = outStack.pop();
                res = evalCond(resValue1, new ResultValue(), "u-");
                outStack.push(res);
            }
            else if (popped.tokenStr.equals("not")) {
                outStack.push(evalCond(outStack.pop(), new ResultValue(), popped.tokenStr));
            }
            else {
                if(popped.primClassif.equals(Classif.FUNCTION)) {
                    error("Function %s missing right paren", popped.tokenStr);
                }
                resValue1 = outStack.pop();
                if(outStack.empty()) {
                    error("Expected operand, instead stack is empty");
                }
                resValue2 = outStack.pop();
                res = evalCond(resValue2, resValue1, popped.tokenStr);
                outStack.push(res);
            }
        }
        res = outStack.pop();
        scan.setPosition(prevToken);
        res.terminatingStr = scan.nextToken.tokenStr;
        return res;
    }

    /**
     * ResultValue evalCond(ResultValue resO1, ResultValue resO2, String opStr) throws Exception
     * @param resO1 first operand
     * @param resO2 second operand
     * @param opStr operation to be performed with the first and second operand, or possibly just first operand
     * @return ResultValue
     * @throws Exception
     */
    private ResultValue evalCond(ResultValue resO1, ResultValue resO2, String opStr) throws Exception {
        ResultValue res = new ResultValue();
        Numeric nOp1;
        Numeric nOp2;

        switch (opStr) {
            case "+" -> {
                nOp2 = new Numeric(this, resO2, "+", "2nd operand");
                nOp1 = new Numeric(this, resO1, "+", "1st operand");
                res = Utility.addition(this, nOp1, nOp2);
            }
            case "-" -> {
                nOp2 = new Numeric(this, resO2, "-", "2nd operand");
                nOp1 = new Numeric(this, resO1, "-", "1st operand");
                res = Utility.subtraction(this, nOp1, nOp2);
            }
            case "*" -> {
                nOp2 = new Numeric(this, resO2, "*", "2nd operand");
                nOp1 = new Numeric(this, resO1, "*", "1st operand");
                res = Utility.multiplication(this, nOp1, nOp2);
            }
            case "/" -> {
                nOp2 = new Numeric(this, resO2, "/", "2nd operand");
                nOp1 = new Numeric(this, resO1, "/", "1st operand");
                res = Utility.division(this, nOp1, nOp2);
            }
            case "^" -> {
                nOp2 = new Numeric(this, resO2, "^", "2nd operand");
                nOp1 = new Numeric(this, resO1, "^", "1st operand");
                res = Utility.exponential(this, nOp1, nOp2);
            }
            case ">" -> res = Utility.greaterThan(this, resO1, resO2);
            case "<" -> res = Utility.lessThan(this, resO1, resO2);
            case ">=" -> res = Utility.greaterThanOrEqual(this, resO1, resO2);
            case "<=" -> res = Utility.lessThanOrEqual(this, resO1, resO2);
            case "==" -> res = Utility.equal(this, resO1, resO2);
            case "!=" -> res = Utility.notEqual(this, resO1, resO2);
            case "u-" -> {
                res = Utility.uMinus(this, resO1);
            }
            case "#" -> res = Utility.concat(this, resO1, resO2);
            case "not" -> res = Utility.not(this, resO1);
            case "or" -> res = Utility.or(this, resO1, resO2);
            default -> error("Bad compare token");
        }
        return res;
    }

    /**
     * ResultValue ifStmt(Boolean bExec) throws Exception
     * If statement sing inside parser
     * @param bExec	- determind if the function is executed
     * @return ResultValue
     * @throws Exception
     */
    private ResultValue ifStmt(Boolean bExec) throws Exception {
        int saveLineNr = scan.currentToken.iSourceLineNr;
        ResultValue resCond;
        String szTerminatingStr = ";";

        if (bExec) {
            resCond = expr(false);
            if (resCond.value.equals("T")) {
                resCond = statements(true, "endif else");
                if(resCond.terminatingStr.equals("break") || resCond.terminatingStr.equals("continue")) {
                    szTerminatingStr = scan.currentToken.tokenStr;
                    if(! scan.getNext().equals(";")) {
                        error("Expected ';' after %s", resCond.terminatingStr);
                    }
                    resCond = statements(false, "else endif");
                }
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
                    if(resCond.terminatingStr.equals("break") || resCond.terminatingStr.equals("continue")) {
                        szTerminatingStr = scan.currentToken.tokenStr;
                        if(! scan.getNext().equals(";")) {
                            error("Expected ';' after %s", resCond.terminatingStr);
                        }
                        resCond = statements(false, "endif");
                    }
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

        return new ResultValue(SubClassif.VOID, "", Structure.PRIMITIVE, szTerminatingStr);
    }

    /**
     * ResultValue assign(String variableStr, ResultValue res) throws Exception
     * assign and put the variable into the storage management
     * @param variableStr 	string	- string of variable
     * @param res 		  	ResultValue	- value to return
     * @return ResultValue
     * @throws Exception
     */
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

    /**
     * ResultValue forStmt(Boolean bExec) throws Exception
     * forStmt making for working on Parser
     * @param bExec 		Boolean	- determind if for function is executed or not
     * @return ResultValue
     * @throws Exception
     */
    public ResultValue forStmt(Boolean bExec) throws Exception {
        ResultValue res;
        Token tempToken;

        //If function is execute
        if (bExec) {
            tempToken = scan.currentToken;	//Save current token to tempToken
            scan.getNext();					//Get next token

            //Return error if this is IDENTIFIER
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

                    if(res.terminatingStr.equals("break") || res.terminatingStr.equals("continue")) {
                        if(! scan.getNext().equals(";")) {
                            error("Expected ';' after %s", res.terminatingStr);
                        }
                        if(res.terminatingStr.equals("break")) {
                            break;
                        }
                        if(res.terminatingStr.equals("continue")) {
                            res = statements(false, "endfor");
                        }
                    }

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

                if(scan.nextToken.primClassif != Classif.OPERAND) {
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

                        if(res.terminatingStr.equals("break") || res.terminatingStr.equals("continue")) {
                            if(! scan.getNext().equals(";")) {
                                error("Expected ';' after %s", res.terminatingStr);
                            }
                            if(res.terminatingStr.equals("break")) {
                                break;
                            }
                            if(res.terminatingStr.equals("continue")) {
                                res = statements(false, "endfor");
                            }
                        }

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

                        if(res.terminatingStr.equals("break") || res.terminatingStr.equals("continue")) {
                            if(! scan.getNext().equals(";")) {
                                error("Expected ';' after %s", res.terminatingStr);
                            }
                            if(res.terminatingStr.equals("break")) {
                                break;
                            }
                            if(res.terminatingStr.equals("continue")) {
                                res = statements(false, "endfor");
                            }
                        }

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
                String[] stringM;

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

                    if(res.terminatingStr.equals("break") || res.terminatingStr.equals("continue")) {
                        if(! scan.getNext().equals(";")) {
                            error("Expected ';' after %s", res.terminatingStr);
                        }
                        if(res.terminatingStr.equals("break")) {
                            break;
                        }
                        if(res.terminatingStr.equals("continue")) {
                            res = statements(false, "endfor");
                        }
                    }

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
        else {		//If for statement is not execute, skip to :
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

    /**
     * RdeclareArray(boolean bExec, String variableStr, SubClassif type, int declared)
     * Decalare Array
     * @param bExec 		Boolean	- determind if for function is executed or not
     * @param variableStr	String	- String of declare variable
     * @param type			SubClassif	- Array type
     * @param declared		int
     * @return ResultArray 	Return declared Array
     * @throws Exception
     */
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
                    }
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

    /**
     * ResultArray assignIndex(String variableStr, SubClassif type, int iIndex, ResultValue resIndex)
     * This function assign the Index of Array
     * @param variableStr   String  - String of declare variable
     * @param type          SubClassif  - Array type
     * @param iIndex        int - indext of the Array to assign
     * @param resIndex      ResultValue
     * @return ResultArray  Return Array after assigned
     * @throws Exception
     */
    public ResultArray assignIndex(String variableStr, SubClassif type, int iIndex, ResultValue resIndex) throws Exception{
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
                resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.UNBOUNDED_ARRAY, --populated, resultArray1.declaredSize);
            } else {
                resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.FIXED_ARRAY, --populated, resultArray1.declaredSize);
            }
            smStorage.insertValue(variableStr, resultArray);
            return resultArray;
        }
        else {
            scan.nextToken.printToken();
            error("Can't asssign %s into index", scan.nextToken.tokenStr);
        }

        return null;
    }

    /**
     * ResultArray assignArrayStmt(String variableStr, SubClassif type, int declared)
     * This function assign the Index of Array
     * @param variableStr   String  - String of declare variable
     * @param type          SubClassif  - Array type
     * @return ResultArray  Return Array after assigned
     * @throws Exception
     */
    private ResultArray assignArrayStmt(String variableStr, SubClassif type, int declared) throws Exception {
        ResultValue resExpr;
        ResultArray resultArray = new ResultArray();
        int populated = 0;
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
                if (resultValue.structure.equals(Structure.PRIMITIVE)) {
                    error("Can't assign scalar to unbounded array");
                }
            }
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
                        resExpr.type = SubClassif.STRING;
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
                    resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.UNBOUNDED_ARRAY, --populated, declared);
                }
                else {
                    resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.FIXED_ARRAY, --populated, declared);
                }
            }
            else if(resultValue.structure == Structure.FIXED_ARRAY || resultValue.structure == Structure.UNBOUNDED_ARRAY) {
                ResultArray resultArray2 = (ResultArray) resultValue;
                int iDclLength = resultArray1.declaredSize;
                int iPopLength = resultArray2.lastPopulated + 1;

                if(declared != -1 && iDclLength < iPopLength) {
                    iPopLength = iDclLength;
                }
                for(int i = 0; i < iPopLength; i++) {
                    if(type.equals(SubClassif.INTEGER)) {
                        resExpr = resultArray2.array.get(i).clone();
                        resExpr.value = Utility.castInt(this, resExpr);
                        resExpr.type = SubClassif.INTEGER;
                        if (declared != -1) {
                            resultArray1.array.set(i, resExpr);
                        }
                        else {
                            if (resultArray1.array == null) {
                                resultArray1.array = new ArrayList<>();
                            }
                            if (resultArray1.array.size() <= i) {
                                resultArray1.array.add(i, null);
                            }
                            resultArray1.array.set(i, resExpr);
                        }
                    }
                    else if(type.equals(SubClassif.FLOAT)) {
                        resExpr = resultArray2.array.get(i).clone();
                        resExpr.value = Utility.castFloat(this, resExpr);
                        resExpr.type = SubClassif.FLOAT;
                        if (declared != -1) {
                            resultArray1.array.set(i, resExpr);
                        }
                        else {
                            if (resultArray1.array == null) {
                                resultArray1.array = new ArrayList<>();
                            }
                            if (resultArray1.array.size() <= i) {
                                resultArray1.array.add(i, null);
                            }
                            resultArray1.array.set(i, resExpr);
                        }
                    }
                    else if(type.equals(SubClassif.BOOLEAN)) {
                        resExpr = resultArray2.array.get(i).clone();
                        resExpr.value = Utility.castBoolean(this, resExpr);
                        resExpr.type = SubClassif.BOOLEAN;
                        if (declared != -1) {
                            resultArray1.array.set(i, resExpr);
                        }
                        else {
                            if (resultArray1.array == null) {
                                resultArray1.array = new ArrayList<>();
                            }
                            if (resultArray1.array.size() <= i) {
                                resultArray1.array.add(i, null);
                            }
                            resultArray1.array.set(i, resExpr);
                        }

                    }
                    else if(type.equals(SubClassif.STRING)) {
                        resExpr = resultArray2.array.get(i).clone();
                        resExpr.type = SubClassif.STRING;
                        if (declared != -1) {
                            resultArray1.array.set(i, resExpr);
                        }
                        else {
                            if (resultArray1.array == null) {
                                resultArray1.array = new ArrayList<>();
                            }
                            if (resultArray1.array.size() <= i) {
                                resultArray1.array.add(i, null);
                            }
                            resultArray1.array.set(i, resExpr);
                        }

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
                    resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.UNBOUNDED_ARRAY, --populated, declared);
                }
                else {
                    resultArray = new ResultArray(variableStr, resultArray1.array, type, Structure.FIXED_ARRAY, --populated, declared);
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

    /**
     * ResultValue getOperand() throws Exception
     * This ultility function to return element of array, using in expr to get the operand
     * @return ResultArray
     * @throws Exception
     */
    public ResultValue getOperand() throws Exception {
        Token op = scan.currentToken;
        ResultValue resultValue1;
        ResultValue index, index2 = null;
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
            if(scan.nextToken.tokenStr.equals("~")) {
                index = new ResultValue(SubClassif.IDENTIFIER, "0", Structure.PRIMITIVE);
            }
            else {
                index = expr(false);
            }
            if(scan.nextToken.tokenStr.equals("~")) {
                scan.getNext();
                if(scan.nextToken.tokenStr.equals("]")) {
                    index2 = new ResultValue(SubClassif.IDENTIFIER, "-1", Structure.PRIMITIVE);
                }
                else {
                    index2 = expr(false);
                    if(Integer.parseInt(index2.value) < 0) {
                        error("Slice value can not be less than -1");
                    }
                }
            }
            scan.getNext();
            if(tempRes.structure != Structure.PRIMITIVE) {
                if (index2 == null) {
                    ResultArray resultArray1 = (ResultArray) smStorage.getValue(value);
                    if (resultArray1 == null) {
                        error("Variable has not been yet declared: %s", op.tokenStr);
                    }
                    int iIndex = Integer.parseInt(Utility.castInt(this, index));
                    if (iIndex < 0) {
                        if (resultArray1.declaredSize != -1) {
                            iIndex += ((ResultArray) resultArray1).declaredSize;
                        } else {
                            iIndex += resultArray1.lastPopulated;
                        }
                    }

                    if (resultArray1.declaredSize != -1 && iIndex >= resultArray1.declaredSize) {
                        error("Trying to reference an index outside of bounds of array");
                    } else if (resultArray1.declaredSize == -1 && resultArray1.array.get(iIndex) == null) {
                        error("Index %d has not been initialized", iIndex);
                    } else if (resultArray1.array.get(iIndex) == null) {
                        error("Index %d has not been initialized", iIndex);
                    }
                    resultValue1 = resultArray1.array.get(iIndex);
                } else {
                    if (Integer.parseInt(index.value) < 0) {
                        error("Slice index can not be negative");
                    }
                    ArrayList<ResultValue> newArray = new ArrayList<>();
                    ResultArray firstArrayValue = (ResultArray) smStorage.getValue(value);
                    if (firstArrayValue == null) {
                        error("Variable has not been declared: %S", op.tokenStr);
                    }
                    int iIndex1 = (Integer.parseInt(Utility.castInt(this, index)));
                    int iIndex2 = (Integer.parseInt(Utility.castInt(this, index2)));
                    if (iIndex2 == -1) {
                        iIndex2 = firstArrayValue.lastPopulated;
                    }
                    for (int i = iIndex1; i < iIndex2; i++) {
                        newArray.add(firstArrayValue.array.get(i));
                    }
                    ResultArray newArrayValue = new ResultArray("Splice", newArray, firstArrayValue.type, Structure.FIXED_ARRAY, iIndex2 - iIndex1, iIndex2 - iIndex1);
                    return newArrayValue;
                }
                if (resultValue1 == null) {
                    error("Array was never initialized", value);
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
                    if(Integer.parseInt(index.value) < 0) {
                        error("Slice index can't be negative");
                    }
                    resultValue1 = smStorage.getValue(value);
                    String strVal = resultValue1.value;
                    if(index2.value.equals("-1")) {
                        index2.value = String.valueOf(resultValue1.value.length());
                    }
                    strVal = strVal.substring((Integer.parseInt(Utility.castInt(this, index))), (Integer.parseInt(Utility.castInt(this, index2))));
                    resultValue1 = new ResultValue(SubClassif.STRING, strVal);
                }
            }
        }
        return resultValue1;
    }

    private ResultValue builtInFunctions(Token funcName, ResultValue parm) throws Exception {
        ResultValue res = null;
        switch (funcName.tokenStr) {
            case "LENGTH" -> res = Utility.LENGTH(parm.value);
            case "SPACES" -> res = Utility.SPACES(parm.value);
            case "ELEM" -> {
                ResultArray array = (ResultArray) parm;
                res = Utility.ELEM(this, array);
            }
            case "MAXELEM" -> {
                ResultArray array = (ResultArray) parm;
                res = Utility.MAXELEM(array);
            }
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