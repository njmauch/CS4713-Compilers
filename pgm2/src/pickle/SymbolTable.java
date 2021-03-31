package pickle;
import java.util.HashMap;

public class SymbolTable {
    public HashMap<String, STEntry> ST;

    //Code for STEntry
    class STEntry {
        String sTSymbol;
        Classif primClassif;
        public STEntry(String s, Classif pci){
            sTSymbol = s;
            primClassif = pci;
        }
    }

    public class STControl extends STEntry{

        SubClassif subClassif;
        public STControl(String s, Classif pci, SubClassif sci)
        {
            super(s, pci);
            this.subClassif = sci;
        }
    }

    public class STIdentifier extends STEntry{
        SubClassif subClassif;
        public STIdentifier(String s, Classif pci, SubClassif sci)
        {
            super(s, pci);
            this.subClassif = sci;
        }
    }

    public class STFunction extends STEntry{
        SubClassif subClassif;
        public STFunction(String s, Classif pci, SubClassif sci)
        {
            super(s, pci);
            this.subClassif = sci;
        }
    }

    public SymbolTable(){
        ST = new HashMap<String, STEntry>();
        initGlobal();
    }

    public void putSymbol(String s, STEntry e){
        ST.put(s,e);
    }
    public STEntry getSymbol(String s){
        if (ST.containsKey(s)){
            return ST.get(s);
        }
        return null;
    }

    private void initGlobal()
    {
        this.putSymbol("<=", new STEntry("<=", Classif.OPERATOR));
        this.putSymbol(">=", new STEntry(">=", Classif.OPERATOR));
        this.putSymbol("!=", new STEntry("!=", Classif.OPERATOR));
        this.putSymbol("==", new STEntry("==", Classif.OPERATOR));

        this.putSymbol("^", new STEntry("^", Classif.OPERATOR));
        this.putSymbol("+", new STEntry("+", Classif.OPERATOR));
        this.putSymbol("-", new STEntry("-", Classif.OPERATOR));
        this.putSymbol("*", new STEntry("*", Classif.OPERATOR));
        this.putSymbol("/", new STEntry("/", Classif.OPERATOR));
        this.putSymbol("<", new STEntry("<", Classif.OPERATOR));
        this.putSymbol(">", new STEntry(">", Classif.OPERATOR));
        this.putSymbol("!", new STEntry("!", Classif.OPERATOR));
        this.putSymbol("=", new STEntry("=", Classif.OPERATOR));
        this.putSymbol("#", new STEntry("#", Classif.OPERATOR));

        this.putSymbol("and", new STEntry("and", Classif.OPERATOR));
        this.putSymbol("or", new STEntry("or", Classif.OPERATOR));
        this.putSymbol("not", new STEntry("not", Classif.OPERATOR));
        this.putSymbol("in", new STEntry("in", Classif.OPERATOR));
        this.putSymbol("notin", new STEntry("notin", Classif.OPERATOR));

        this.putSymbol("Int", new STIdentifier("Int", Classif.CONTROL, SubClassif.DECLARE));
        this.putSymbol("Float", new STIdentifier("Float", Classif.CONTROL, SubClassif.DECLARE));
        this.putSymbol("String", new STIdentifier("String", Classif.CONTROL, SubClassif.DECLARE));
        this.putSymbol("Bool", new STIdentifier("Bool", Classif.CONTROL, SubClassif.DECLARE));

        this.putSymbol("if", new STControl("if", Classif.CONTROL, SubClassif.FLOW));
        this.putSymbol("endif", new STControl("endif", Classif.CONTROL, SubClassif.END));
        this.putSymbol("else", new STControl("else", Classif.CONTROL, SubClassif.END));
        this.putSymbol("for", new STControl("for", Classif.CONTROL, SubClassif.FLOW));
        this.putSymbol("endfor", new STControl("endfor", Classif.CONTROL, SubClassif.END));
        this.putSymbol("while", new STControl("while", Classif.CONTROL, SubClassif.FLOW));
        this.putSymbol("endwhile", new STControl("endwhile", Classif.CONTROL, SubClassif.END));

        this.putSymbol("print", new STFunction("print", Classif.FUNCTION, SubClassif.BUILTIN));
    }
}
