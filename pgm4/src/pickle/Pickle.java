/*
  This is a simple driver for the first programming assignment.
  Command Arguments:
      java Pickle arg1
             arg1 is the pickle source file name.
  Output:
      Prints each token in a table.
  Notes:
      1. This creates a SymbolTable object which doesn't do anything
         for this first programming assignment.
      2. This uses the student's Scanner class to get each token from
         the input file.  It uses the getNext method until it returns
         an empty string.
      3. If the Scanner raises an exception, this driver prints 
         information about the exception and terminates.
      4. The token is printed using the Token::printToken() method.
 */
package pickle;

public class Pickle
{
    public static void main(String[] args) throws Exception     {
        SymbolTable symbolTable = new SymbolTable();
        StorageManager storeManager = new StorageManager();
        Precedence precedence = new Precedence();
        try {

            Scanner scan = new Scanner(args[0], symbolTable);
            Parser parser = new Parser(scan, storeManager, symbolTable, precedence);
            ResultValue res;
            while (scan.currentToken.primClassif != Classif.EOF) {
                res = parser.statement(true);
                if(res.type.equals(SubClassif.END)) {
                    parser.error("Invalid token %s", res.terminatingStr);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}