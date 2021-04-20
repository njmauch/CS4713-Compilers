package pickle;

import java.util.HashMap;

public class Precedence {
    HashMap<String, Integer> tokenPrecedence;
    HashMap<String, Integer> stackPrecedence;

    public Precedence () {
        tokenPrecedence = new HashMap<>();
        stackPrecedence = new HashMap<>();
        initGlobal();
    }

    private void initGlobal() {
        this.tokenPrecedence.put("and", 4);
        this.tokenPrecedence.put("or", 4);
        this.tokenPrecedence.put("not", 5);
        this.tokenPrecedence.put("IN", 6);
        this.tokenPrecedence.put("NOTIN", 6);
        this.tokenPrecedence.put("<", 6);
        this.tokenPrecedence.put(">", 6);
        this.tokenPrecedence.put("<=", 6);
        this.tokenPrecedence.put(">=", 6);
        this.tokenPrecedence.put("==", 6);
        this.tokenPrecedence.put("!=", 6);
        this.tokenPrecedence.put("#", 7);
        this.tokenPrecedence.put("+", 8);
        this.tokenPrecedence.put("-", 8);
        this.tokenPrecedence.put("*", 9);
        this.tokenPrecedence.put("/", 9);
        this.tokenPrecedence.put("^", 11);
        this.tokenPrecedence.put("u-", 12);
        this.tokenPrecedence.put("(", 15);

        this.stackPrecedence.put("and", 4);
        this.stackPrecedence.put("or", 4);
        this.stackPrecedence.put("not", 5);
        this.stackPrecedence.put("IN", 6);
        this.stackPrecedence.put("NOTIN", 6);
        this.stackPrecedence.put("<", 6);
        this.stackPrecedence.put(">", 6);
        this.stackPrecedence.put("<=", 6);
        this.stackPrecedence.put(">=", 6);
        this.stackPrecedence.put("==", 6);
        this.stackPrecedence.put("!=", 6);
        this.stackPrecedence.put("#", 7);
        this.stackPrecedence.put("+", 8);
        this.stackPrecedence.put("-", 8);
        this.stackPrecedence.put("*", 9);
        this.stackPrecedence.put("/", 9);
        this.stackPrecedence.put("^", 10);
        this.stackPrecedence.put("u-", 12);
        this.stackPrecedence.put("(", 2);
    }

    public int getTokenPrecedence(String key) {
        if(!tokenPrecedence.containsKey(key))
            return -1;

        return tokenPrecedence.get(key);
    }

    public int getStackPrecedence(String key) {
        if(!stackPrecedence.containsKey(key))
            return -1;

        return stackPrecedence.get(key);
    }
}
