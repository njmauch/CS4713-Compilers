package pickle;

import java.io.*;
import java.util.*;

public class Scanner {

    public String sourceFileNm = "";
    public static ArrayList<String> sourceLineM;
    public SymbolTable symbolTable;
    public static char[] textCharM;
    public static int iSourceLineNr;
    public int iColPos;
    public Token currentToken;
    public Token nextToken;

    private final static String delimiters = " \t;:()\'\"=!<>+-*/[]#,^\n";
    private final static String operators = "+-*/<>!=#^";
    private final static String separators = "():;[],";

    public Scanner(String sourceFileNm, SymbolTable symbolTable) throws Exception, FileNotFoundException {
        this.sourceFileNm = sourceFileNm;
        symbolTable = symbolTable;
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
        int iBeginTokenPos;
        int iEndTokenPos;
        String tokenStr = "";

        currentToken = nextToken;

        //If the column equals the size of the line reset column number to 0 and add 1 to line number.
        if(iColPos == sourceLineM.get(iSourceLineNr).length()){
            iColPos = 0;
            iSourceLineNr++;
            //If the line number equals the number of lines in the file
            if(iSourceLineNr >= sourceLineM.size()) {
                //Set token to ""
                nextToken.tokenStr = tokenStr;
                //Set class as EOF
                nextToken.primClassif = Classif.EOF;
                //Set line number
                nextToken.iSourceLineNr = iSourceLineNr;
                nextToken.iColPos = iColPos;
                //Print the EOF token
                nextToken.printToken();
                return tokenStr;
            }
        }

        //If new line print out the line
        if(iColPos == 0) {
            //If the current line is empty (whitespace), print out lines until it reaches non empty line
            if (sourceLineM.get(iSourceLineNr).isEmpty()) {
                while (sourceLineM.get(iSourceLineNr).isEmpty()) {
                    System.out.print(String.format("   %d %s\n", iSourceLineNr + 1, sourceLineM.get(iSourceLineNr)));
                    iSourceLineNr++;
                }
            }
            //Prints out line with tokens
            System.out.print(String.format("   %d %s\n", iSourceLineNr + 1, sourceLineM.get(iSourceLineNr)));
        }

        //Sets the char array to the current line
        textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
        //Goes through whitespace and ignores it
        while(textCharM[iColPos] == ' ') {
            //If whitespace goes to end of line go to the next line
            if (iColPos == sourceLineM.get(iSourceLineNr).length()) {
                iSourceLineNr++;
                textCharM = sourceLineM.get(iSourceLineNr).toCharArray();
                iColPos = 0;
            }
            //Move column position for each whitespace
            else {
                iColPos++;
            }
        }

        //Set beginning of token index to the current column position
        iBeginTokenPos = iColPos;

        //Move through token until a delimiter is found
        while(delimiters.indexOf(textCharM[iColPos]) == -1) {
            iColPos++;
            //If the end of the array break out of loop
            if (iColPos == sourceLineM.get(iSourceLineNr).length()){
                break;
            }
        }

        if(iBeginTokenPos == iColPos) {
            //Checking if token is string
            if(textCharM[iBeginTokenPos] == '\'' || textCharM[iBeginTokenPos] == '"') {
                iColPos++;
                //Goes through until matching single or double quote is found and skips properly escaped characters
                while(textCharM[iColPos] != textCharM[iBeginTokenPos] || textCharM[iColPos-1] == '\\') {
                    iColPos++;
                    //If reaches end of line throw error that no matching quotation was found.
                    if(iColPos >= textCharM.length) {
                        System.err.print(String.format("Line: %d Missing ending quotation '%s', File: %s\n", iSourceLineNr + 1, sourceLineM.get(iSourceLineNr), sourceFileNm));
                        throw new Exception();
                    }
                }
                //Set token as string
                iEndTokenPos = iColPos;
                //Create new string stoken without quotation marks
                tokenStr = new String(textCharM, iBeginTokenPos + 1, (iEndTokenPos - 1) - iBeginTokenPos);
                nextToken = new Token(tokenStr);
                //Set class and sub class
                nextToken.primClassif = Classif.OPERAND;
                nextToken.subClassif = SubClassif.STRING;
                nextToken.iSourceLineNr = iSourceLineNr;
                nextToken.iColPos = iBeginTokenPos;
            }
            else {
                //If single character token then it is a seperator
                tokenStr = new String(textCharM, iBeginTokenPos, 1);
                nextToken = new Token(tokenStr);
                nextToken.iSourceLineNr = iSourceLineNr;
                nextToken.iColPos = iBeginTokenPos;
                setTokenClass(tokenStr);
            }
            iColPos++;
        }
        else {
            //Any other type of token
            iEndTokenPos = iColPos;
            tokenStr = new String(textCharM, iBeginTokenPos, iEndTokenPos - iBeginTokenPos);
            nextToken = new Token(tokenStr);
            nextToken.iColPos = iBeginTokenPos;
            nextToken.iSourceLineNr = iSourceLineNr;
            setTokenClass(tokenStr);
        }

        //Return the token string
        return tokenStr;
    }

    //Set the token class
    private void setTokenClass(String tokenStr) throws Exception {
        //Booleans to keep track if float or int
        int bIsFloat = 0;
        int bIsInt = 0;
        char[] tokenCharsM = tokenStr.toCharArray();


        //Check if token is separator and set class
        if(separators.indexOf(tokenCharsM[0]) > -1) {
            nextToken.primClassif = Classif.SEPARATOR;
        }
        //Check if token is operator and set class
        else if(operators.indexOf(tokenCharsM[0]) > -1) {
            nextToken.primClassif = Classif.OPERATOR;
        }
        //Check if the character is alpha, set class as a operand/variable
        else if (Character.isAlphabetic(tokenCharsM[0])) {
            nextToken.primClassif = Classif.OPERAND;
            nextToken.subClassif = SubClassif.IDENTIFIER;
        }
        else {
            //Token starts with a digit
            bIsInt = 1;
            //Loops through each character in number
            for(int i = 0; i < tokenCharsM.length; i++) {
                //if a decimal is found
                if(tokenCharsM[i] == '.') {
                    //If bIsFloat is true, that means second decimal has been found so raise exception
                    if(bIsFloat == 1) {
                        System.err.print(String.format("Line: %d Invalid number format: '%s', File: %s\n", iSourceLineNr + 1, tokenStr, sourceFileNm));
                        throw new Exception();
                    }
                    //First decimal found so set float flag to true
                    else {
                        bIsFloat = 1;
                    }
                }
                //If encountering a non numeric character in the number raise exception
                else if(!(Character.isDigit(tokenCharsM[i]))) {
                    System.err.print(String.format("Line: %d Invalid numbuer format: '%s', File: %s\n", iSourceLineNr + 1, tokenStr, sourceFileNm));
                    throw new Exception();
                }
            }
        }
        //If float flag is true set class to float
        if(bIsFloat == 1){
            nextToken.primClassif = Classif.OPERAND;
            nextToken.subClassif = SubClassif.FLOAT;
        }
        //If int flag is true and float flag is false set class to integer
        else if(bIsInt == 1) {
            nextToken.primClassif = Classif.OPERAND;
            nextToken.subClassif = SubClassif.INTEGER;
        }
    }
}

