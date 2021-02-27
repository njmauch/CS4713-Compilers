package pickle;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class Scanner {

    public String sourceFileNm = "";
    public static ArrayList<String> sourceLineM;
    public SymbolTable symbolTable;
    public static char[] textCharM;
    public static int iSourceLineNr;
    public int iColPos;
    public Token currentToken;
    public Token nextToken;

    private final static String whiteSpace = " \t\n";
    private final static String delimiters = " \t;:()\'\"=!<>+-*/[]#,^\n";
    private final static String strOperators = "<>=!^";
    private final static String operators = "+-*/<>!=#^";
    private final static String separators = "():;[],";

    public Scanner(String sourceFileNm, SymbolTable symbolTable) throws Exception, FileNotFoundException {
        this.sourceFileNm = sourceFileNm;
        this.symbolTable = symbolTable;
        sourceLineM = new ArrayList<String>();

        //Scanner to read input file
        java.util.Scanner scanner = new java.util.Scanner(new File(sourceFileNm));

        //Insert every line of file in to a String array list
        while (scanner.hasNextLine()) {
            sourceLineM.add(scanner.nextLine());
        }

        //Set Column and Line number to 0
        iSourceLineNr = 0;
        iColPos = 0;

        //Get the first line from the array and set it as character array
        textCharM = sourceLineM.get(iSourceLineNr).toCharArray();

        //Prepare tokens
        currentToken = new Token();
        nextToken = new Token();

        //Gets next token
        nextToken.tokenStr = getNext();
    }

    public String getNext() throws Exception {

        currentToken = nextToken;
        this.nextToken = getNextToken();

        return currentToken.tokenStr;
    }

    private Token getNextToken() throws Exception {
        int iBeginTokenPos;
        int iEndTokenPos;
        Token tempToken = new Token();

        //If new line print out the line
        if (iColPos == 0) {
            //If the current line is empty (whitespace), print out lines until it reaches non empty line
            if (sourceLineM.get(iSourceLineNr).isEmpty()) {
                while (sourceLineM.get(iSourceLineNr).isEmpty()) {
                    System.out.printf("   %d %s\n", iSourceLineNr + 1, sourceLineM.get(iSourceLineNr));
                    iSourceLineNr++;
                }
            }
            //Prints out line with tokens
            System.out.printf("   %d %s\n", iSourceLineNr + 1, sourceLineM.get(iSourceLineNr));
        }

        while (true) {
            if (iColPos + 1 >= textCharM.length) {
                iSourceLineNr++;
                if (iSourceLineNr >= sourceLineM.size()) {
                    tempToken.tokenStr = "";
                    tempToken.primClassif = Classif.EOF;
                    tempToken.iSourceLineNr = iSourceLineNr;
                    return tempToken;
                }
                else if (separators.indexOf(textCharM[iColPos]) > -1) {
                    tempToken.primClassif = Classif.SEPARATOR;
                    tempToken.tokenStr = new String(textCharM, iColPos, 1);
                    textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                    iColPos = 0;
                    return tempToken;
                }
            } else {
                if ((whiteSpace.indexOf(textCharM[iColPos]) > -1) && iColPos < textCharM.length) {
                    iColPos++;
                } else if (((iColPos + 1) < textCharM.length) && (textCharM[iColPos] == '/') && (textCharM[iColPos + 1] == '/')) {
                    iColPos = textCharM.length;
                } else {
                    break;
                }
            }
        }

        //Set beginning of token index to the current column position
        iBeginTokenPos = iColPos;

        while (delimiters.indexOf(textCharM[iColPos]) == -1) {
            iColPos++;
            //If the end of the array break out of loop
            if (iColPos == textCharM.length) {
                break;
            }
        }

        //Checking if token is string
        if (textCharM[iBeginTokenPos] == '\'' || textCharM[iBeginTokenPos] == '\"') {
            createStringToken(tempToken);
            return tempToken;
        }

        if (iBeginTokenPos == iColPos) {
            if (strOperators.indexOf(textCharM[iColPos]) > -1) {
                if (textCharM[iColPos + 1] == '=') {
                    tempToken.primClassif = Classif.OPERATOR;
                    tempToken.tokenStr = new String(textCharM, iBeginTokenPos, 2);
                    return tempToken;
                } else if (operators.indexOf(textCharM[iColPos]) > -1) {
                    tempToken.primClassif = Classif.OPERATOR;
                    tempToken.tokenStr = new String(textCharM, iBeginTokenPos, 1);
                    return tempToken;
                }
            }
        }


        iEndTokenPos = iColPos;
        tempToken.tokenStr = new String(textCharM, iBeginTokenPos, iEndTokenPos - iBeginTokenPos);
        tempToken.iColPos = iBeginTokenPos;
        tempToken.iSourceLineNr = iSourceLineNr;

        SymbolTable.STEntry entryResult = symbolTable.getSymbol(tempToken.tokenStr);

        if (entryResult != null) {
            tempToken.primClassif = entryResult.primClassif;

            if (entryResult instanceof SymbolTable.STControl) {
                tempToken.subClassif = ((SymbolTable.STControl) entryResult).subClassif;
                tempToken.tokenStr = new String(textCharM, iBeginTokenPos, iEndTokenPos - iBeginTokenPos);
                return tempToken;
            } else if (entryResult instanceof SymbolTable.STFunction) {
                tempToken.subClassif = ((SymbolTable.STFunction) entryResult).subClassif;
                tempToken.tokenStr = new String(textCharM, iBeginTokenPos, iEndTokenPos - iBeginTokenPos);
                return tempToken;
            } else if (entryResult instanceof SymbolTable.STIdentifier) {
                tempToken.subClassif = ((SymbolTable.STIdentifier) entryResult).subClassif;
                tempToken.tokenStr = new String(textCharM, iBeginTokenPos, iEndTokenPos - iBeginTokenPos);
                return tempToken;
            }
        } else {
            createOperandToken(tempToken);
            return tempToken;
        }
        return tempToken;
    }

    private void createStringToken(Token strToken) throws Exception {
        strToken.primClassif = Classif.OPERAND;
        strToken.subClassif = SubClassif.STRING;
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
    }

    private void createOperandToken(Token operandToken) throws Exception {
        int bIsFloat = 0;
        StringBuilder tempStr = new StringBuilder();

        operandToken.primClassif = Classif.OPERAND;
        if (textCharM[operandToken.iColPos] >= '0' && textCharM[operandToken.iColPos] <= '9') {
            for (char c : textCharM) {
                if (delimiters.indexOf(textCharM[iColPos]) > -1) {
                    break;
                }
                if (c == '.') {
                    //If bIsFloat is true, that means second decimal has been found so raise exception
                    if (bIsFloat == 1) {
                        System.err.print(String.format("Line: %d Invalid number format: '%s', File: %s\n", iSourceLineNr + 1, "token" /*tokenStr*/, sourceFileNm));
                        throw new Exception();
                    }
                    //First decimal found so set float flag to true
                    else {
                        tempStr.append('.');
                        bIsFloat = 1;
                    }
                }
                //If encountering a non numeric character in the number raise exception
                else if (!(Character.isDigit(c))) {
                    System.err.print(String.format("Line: %d Invalid number format: '%s', File: %s\n", iSourceLineNr + 1, "token" /*tokenStr*/, sourceFileNm));
                    throw new Exception();
                }
                tempStr.append(c);
            }
            if (bIsFloat == 1) {
                operandToken.subClassif = SubClassif.FLOAT;
            } else {
                operandToken.subClassif = SubClassif.INTEGER;
            }
        } else if ((operandToken.tokenStr.equals("T")) || (operandToken.tokenStr.equals("F"))) {
            operandToken.subClassif = SubClassif.BOOLEAN;
        } else {
            operandToken.subClassif = SubClassif.IDENTIFIER;
            return;
        }
        operandToken.tokenStr = tempStr.toString();
    }
}

