package pickle;
import java.util.*;
/**
 * This class represents a token for the Scanner Class.
 */
public class Token
{
    /** string from the source program, possibly modified for literals
     */
    public String tokenStr = "";
    /** Parser uses this to help simplify parsing since many subclasses are
     * combined.  Some values: OPERAND, SEPARATOR, OPERATOR, EMPTY
     */
    public Classif primClassif = Classif.EMPTY;
    /** a sub-classification of a token also used to simplify parsing.
     * Some values for OPERANDs: IDENTIFIER, INTEGER constant, FLOAT constant,
     * STRING constant.
     */
    public SubClassif subClassif = SubClassif.EMPTY;
    /** Line number location in the source file for this token.  Line numbers are
     * * relative to 1.
     */
    public int iSourceLineNr = 0;
    /** Column location in the source file for this token.  column positions are
     * relative to zero.
     */
    public int iColPos = 0;

    public Token(String value)
    {
        this.tokenStr = value;
    }
    public Token()
    {
        this("");   // invoke the other constructor
    }
    /**
     * Prints the primary classification, sub-classification, and token string
     * <p>
     * If the classification is EMPTY, it uses "**garbage**".
     * If the sub-classification is EMPTY, it uses "-".
     */
    public void printToken()
    {
        String primClassifStr;
        String subClassifStr;

        if (primClassif != Classif.EMPTY)
            primClassifStr = primClassif.toString();
        else
            primClassifStr = "**garbage**";

        if (subClassif != SubClassif.EMPTY)
            subClassifStr = subClassif.toString();
        else
            subClassifStr = "-";

        if (primClassif == Classif.OPERAND
                && subClassif == SubClassif.STRING)
        {
            System.out.printf("%-11s %-12s "
                    , primClassifStr
                    , subClassifStr);
            hexPrint(25,tokenStr);
        }
        else
            System.out.printf("%-11s %-12s %s\n"
                    , primClassifStr
                    , subClassifStr
                    , tokenStr);

    }

    public ResultValue toResult(Parser parser) {
        ResultValue res = new ResultValue();
        res.type = this.subClassif;
        res.value = this.tokenStr;
        res.structure = "PRIMITIVE";
        return res;
    }
    public static Token copyToken(Token fromToken) {
        Token toToken = new Token();

        toToken.tokenStr = fromToken.tokenStr;
        toToken.primClassif = fromToken.primClassif;
        toToken.subClassif = fromToken.subClassif;
        toToken.iColPos	= fromToken.iColPos;
        toToken.iSourceLineNr = fromToken.iSourceLineNr;
        /*toToken.precedence	= fromToken.precedence;
        toToken.stackPrecedence = fromToken.stackPrecedence;*/

        return toToken;
    }

    public void hexPrint(int indentLength, String tokenStr) {
        char [] strM = tokenStr.toCharArray();
        int tempLength = tokenStr.length();

        System.out.print("\n");
        for(int i = 0; i < indentLength; i += 1) {
            System.out.print(" ");
        }
        for (int i = 0; i < tempLength; i += 1) {
            System.out.print(String.format("%02X", (int)strM[i]));
        }
        System.out.print("\n");
    }
}