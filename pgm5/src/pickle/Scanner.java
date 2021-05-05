package pickle;

import java.io.File;
import java.util.ArrayList;

public class Scanner {

    public String sourceFileNm;
    public static ArrayList<String> sourceLineM;
    public SymbolTable symbolTable;
    public static char[] textCharM;
    public static int iSourceLineNr;
    public int iColPos;
    public Token currentToken;
    public Token nextToken;
    public boolean bShowToken;

    private final static String whiteSpace = " \t\n";
    private final static String delimiters = " \t;:()\'\"=!<>+-*/[]#,^\n~{}";
    private final static String strOperators = "#<>=!^*/+-";
    private final static String operators = "+-*/<>!=#^";
    private final static String separators = "():;~[],{}";

    public Scanner(String sourceFileNm, SymbolTable symbolTable) throws Exception {
        this.sourceFileNm = sourceFileNm;
        this.symbolTable = symbolTable;
        sourceLineM = new ArrayList<>();
        this.bShowToken = false;

        //Scanner to read input file
        java.util.Scanner scanner = new java.util.Scanner(new File(sourceFileNm));

        //Insert every line of file in to a String array list
        while (scanner.hasNextLine()) {
            sourceLineM.add(scanner.nextLine());
        }

        //Set Column and Line number to 0
        iSourceLineNr = 0;
        this.iColPos = 0;

        //Get the first line from the array and set it as character array
        textCharM = sourceLineM.get(iSourceLineNr).toCharArray();

        this.currentToken = new Token();
        this.nextToken = new Token();
        getNextToken();
    }

    public String getNext() throws Exception {

        currentToken = nextToken;

        if(bShowToken) {
            System.out.println("...");
            currentToken.printToken();
        }

        getNextToken();

        return currentToken.tokenStr;
    }

    private void getNextToken() throws Exception {
        int iBeginTokenPos;
        int iEndTokenPos;
        nextToken = new Token();

        if ((iSourceLineNr + 1) > sourceLineM.size()) {
            nextToken.tokenStr = "";
            nextToken.primClassif = Classif.EOF;
            nextToken.iSourceLineNr = iSourceLineNr;
            return;
        }
        //If new line print out the line
        if (iColPos == 0) {
            //If the current line is empty (whitespace), print out lines until it reaches non empty line
            if (sourceLineM.get(iSourceLineNr).isEmpty()) {
                //System.out.printf("   %d %s\n", iSourceLineNr + 1, sourceLineM.get(iSourceLineNr));
                iSourceLineNr++;
                if (iSourceLineNr < sourceLineM.size()) {
                    textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                }
                iColPos = 0;
                getNextToken();
                return;
            }
            //Prints out line with tokens
            //System.out.printf("   %d %s\n", iSourceLineNr + 1, sourceLineM.get(iSourceLineNr));
        }


        while (true) {
            if (iColPos + 1 >= textCharM.length) {
                if (separators.indexOf(textCharM[iColPos]) > -1) {
                    nextToken.primClassif = Classif.SEPARATOR;
                    nextToken.tokenStr = new String(textCharM, iColPos, 1);
                    nextToken.iSourceLineNr = iSourceLineNr;
                    nextToken.iColPos = iColPos;
                    iSourceLineNr++;
                    if (iSourceLineNr < sourceLineM.size()) {
                        textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                    }
                    iColPos = 0;
                    return;
                }
                else if (++iSourceLineNr >= sourceLineM.size()) {
                    nextToken.tokenStr = "";
                    nextToken.primClassif = Classif.EOF;
                    nextToken.iSourceLineNr = iSourceLineNr;
                    return;
                }
                else {
                    textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                    iColPos = 0;
                    getNextToken();
                    return;
                }
            } else {
                if ((whiteSpace.indexOf(textCharM[iColPos]) > -1) && iColPos < textCharM.length) {
                    iColPos++;
                } else if (((iColPos + 1) < textCharM.length) && (textCharM[iColPos] == '/') && (textCharM[iColPos + 1] == '/')) {
                    iSourceLineNr++;
                    if (iSourceLineNr < sourceLineM.size()) {
                        textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                    }
                    iColPos = 0;
                    getNextToken();
                    return;
                } else {
                    break;
                }
            }
        }

        //Set beginning of token index to the current column position
        iBeginTokenPos = iColPos;

        if (textCharM[iColPos] >= '0' && textCharM[iColPos] <= '9') {
            nextToken.iSourceLineNr = iSourceLineNr;
            createOperandToken(nextToken);
            if(iColPos >= textCharM.length) {
                nextToken.iSourceLineNr = iSourceLineNr;
                nextToken.iColPos = iColPos;
                iSourceLineNr++;
                textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                iColPos = 0;
            }
            return;
        }

        while (delimiters.indexOf(textCharM[iColPos]) == -1) {
            iColPos++;
            //If the end of the array break out of loop
            if (iColPos == sourceLineM.get(iSourceLineNr).length()) {
                break;
            }
        }


        //Checking if token is string
        if (textCharM[iBeginTokenPos] == '\'' || textCharM[iBeginTokenPos] == '\"') {
            nextToken.iSourceLineNr = iSourceLineNr;
            createStringToken(nextToken);
            if(iColPos >= textCharM.length) {
                nextToken.iSourceLineNr = iSourceLineNr;
                iSourceLineNr++;
                textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                iColPos = 0;
            }
            return;
        }

        if (iBeginTokenPos == iColPos) {
            if (strOperators.indexOf(textCharM[iColPos]) > -1) {
                if (textCharM[iColPos + 1] == '=') {
                    nextToken.iColPos = iColPos;
                    nextToken.primClassif = Classif.OPERATOR;
                    nextToken.tokenStr = new String(textCharM, iBeginTokenPos, 2);
                    nextToken.iSourceLineNr = iSourceLineNr;
                    iColPos +=2;
                    return;
                } else if (operators.indexOf(textCharM[iColPos]) > -1) {
                    nextToken.iColPos = iColPos;
                    nextToken.primClassif = Classif.OPERATOR;
                    nextToken.tokenStr = new String(textCharM, iBeginTokenPos, 1);
                    nextToken.iSourceLineNr = iSourceLineNr;
                    iColPos++;
                    return;
                }
            }
        }


        iEndTokenPos = iColPos;
        nextToken.tokenStr = new String(textCharM, iBeginTokenPos, iEndTokenPos - iBeginTokenPos);
        nextToken.iColPos = iBeginTokenPos;
        nextToken.iSourceLineNr = iSourceLineNr;

        SymbolTable.STEntry entryResult = symbolTable.getSymbol(nextToken.tokenStr);

        if (entryResult != null) {
            nextToken.primClassif = entryResult.primClassif;

            if (entryResult instanceof SymbolTable.STControl) {
                nextToken.subClassif = ((SymbolTable.STControl) entryResult).subClassif;
                nextToken.tokenStr = new String(textCharM, iBeginTokenPos, iEndTokenPos - iBeginTokenPos);
                if(iColPos >= textCharM.length) {
                    nextToken.iSourceLineNr = iSourceLineNr;
                    iSourceLineNr++;
                    textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                    iColPos = 0;
                }
                return;
            } else if (entryResult instanceof SymbolTable.STFunction) {
                nextToken.subClassif = ((SymbolTable.STFunction) entryResult).subClassif;
                nextToken.tokenStr = new String(textCharM, iBeginTokenPos, iEndTokenPos - iBeginTokenPos);
                if(iColPos >= textCharM.length) {
                    nextToken.iSourceLineNr = iSourceLineNr;
                    iSourceLineNr++;
                    textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                    iColPos = 0;
                }
                return;
            } else if (entryResult instanceof SymbolTable.STIdentifier) {
                nextToken.subClassif = ((SymbolTable.STIdentifier) entryResult).subClassif;
                nextToken.tokenStr = new String(textCharM, iBeginTokenPos, iEndTokenPos - iBeginTokenPos);
                if(iColPos >= textCharM.length) {
                    nextToken.iSourceLineNr = iSourceLineNr;
                    iSourceLineNr++;
                    textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                    iColPos = 0;
                }
                return;
            }
        } else if (separators.contains(nextToken.tokenStr)) {
            nextToken.primClassif = Classif.SEPARATOR;
            nextToken.tokenStr = new String(textCharM, iBeginTokenPos, 1);
            nextToken.iSourceLineNr = iSourceLineNr;
            iColPos++;
            if(iColPos >= textCharM.length) {
                iSourceLineNr++;
                textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                iColPos = 0;
            }
            return;
        }
        else {
            createOperandToken(nextToken);
            if(iColPos >= textCharM.length) {
                iColPos = 0;
                nextToken.iSourceLineNr = iSourceLineNr;
                iSourceLineNr++;
                textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                iColPos = 0;
            }
            return;
        }
    }

    public void setPosition(Token token) throws Exception {
        this.iSourceLineNr = token.iSourceLineNr;
        this.iColPos = token.iColPos;
        textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
        //currentToken = new Token();
        //nextToken = new Token();
        this.getNext();
        this.getNext();
    }
    private void createStringToken(Token strToken) throws Exception {
        strToken.primClassif = Classif.OPERAND;
        strToken.subClassif = SubClassif.STRING;
        strToken.iSourceLineNr = iSourceLineNr;
        strToken.iColPos = iColPos;
        char strTokenDelim = textCharM[iColPos];
        StringBuilder tempStr = new StringBuilder();
        iColPos++;

        while (true) {
            if (iColPos >= textCharM.length) {
                System.err.printf("String literal must be on the same line, begins line: %d and column: %d\n", iSourceLineNr, iColPos);
                throw new Exception();
            } else if (textCharM[iColPos] == strTokenDelim) {
                iColPos++;
                break;
            } else if (textCharM[iColPos] == '\\' && iColPos < textCharM.length - 1) {
                switch (textCharM[iColPos + 1]) {
                    case 't' -> {
                        tempStr.append('\t');
                        iColPos += 2;
                    }
                    case 'n' -> {
                        tempStr.append('\n');
                        iColPos += 2;
                    }
                    case '\'' -> {
                        tempStr.append('\'');
                        iColPos += 2;
                    }
                    case '"' -> {
                        tempStr.append('\"');
                        iColPos += 2;
                    }
                    case '\\' -> {
                        tempStr.append('\\');
                        iColPos += 2;
                    }
                    default -> {
                        System.err.printf("Invalid escape attempt, line: %d and column: %d\n", iSourceLineNr, iColPos);
                        throw new Exception();
                    }
                }
            } else {
                tempStr.append(textCharM[iColPos]);
                iColPos++;
            }
        }

        strToken.tokenStr = tempStr.toString();
        if(strToken.tokenStr.matches("^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]$")) {
            strToken.subClassif = SubClassif.DATE;
        }
    }

    private void createOperandToken(Token operandToken) throws Exception {
        int bIsFloat = 0;
        StringBuilder tempStr = new StringBuilder();
        operandToken.iColPos = iColPos;
        operandToken.primClassif = Classif.OPERAND;
        if (textCharM[iColPos] >= '0' && textCharM[iColPos] <= '9') {
            for (int i = iColPos; ;i++) {
                if (delimiters.indexOf(textCharM[i]) > -1) {
                    break;
                }
                iColPos++;
                if (textCharM[i] == '.') {
                    //If bIsFloat is true, that means second decimal has been found so raise exception
                    if (bIsFloat == 1) {
                        System.err.printf("Line: %d Invalid number format: '%s', File: %s\n", iSourceLineNr + 1, "token" /*tokenStr*/, sourceFileNm);
                        throw new Exception();
                    }
                    //First decimal found so set float flag to true
                    else {
                        tempStr.append('.');
                        bIsFloat = 1;
                        continue;
                    }
                }
                //If encountering a non numeric character in the number raise exception
                else if (!(Character.isDigit(textCharM[i]))) {
                    System.err.printf("Line: %d Invalid number format: '%s', File: %s\n", iSourceLineNr + 1, "token" /*tokenStr*/, sourceFileNm);
                    throw new Exception();
                }
                tempStr.append(textCharM[i]);
            }
            if (bIsFloat == 1) {
                operandToken.tokenStr = tempStr.toString();
                operandToken.subClassif = SubClassif.FLOAT;
                return;
            } else {
                operandToken.tokenStr = tempStr.toString();
                operandToken.subClassif = SubClassif.INTEGER;
                return;
            }
        } else if ((operandToken.tokenStr.equals("T")) || (operandToken.tokenStr.equals("F"))) {
            operandToken.subClassif = SubClassif.BOOLEAN;
            nextToken.iColPos--;
            return;
        } else {
            operandToken.subClassif = SubClassif.IDENTIFIER;
            return;
        }
    }
}

