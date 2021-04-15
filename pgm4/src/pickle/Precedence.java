package pickle;

import java.util.HashMap;

public class Precedence {
    private HashMap<String, Integer> tokenPrecedence;
    private HashMap<String, Integer> stackPrecedence;

    public void Precedence () {
        this.tokenPrecedence = new HashMap<>();
        this.stackPrecedence = new HashMap<>();

        tokenPrecedence.put("and", 4);
        tokenPrecedence.put("or", 4);
        tokenPrecedence.put("not", 5);
        tokenPrecedence.put("IN", 6);
        tokenPrecedence.put("NOTIN", 6);
        tokenPrecedence.put("<", 6);
        tokenPrecedence.put(">", 6);
        tokenPrecedence.put("<=", 6);
        tokenPrecedence.put(">=", 6);
        tokenPrecedence.put("==", 6);
        tokenPrecedence.put("!=", 6);
        tokenPrecedence.put("#", 7);
        tokenPrecedence.put("+", 8);
        tokenPrecedence.put("-", 8);
        tokenPrecedence.put("*", 9);
        tokenPrecedence.put("/", 9);
        tokenPrecedence.put("^", 11);
        tokenPrecedence.put("u-", 12);
        tokenPrecedence.put("(", 15);

        stackPrecedence.put("and", 4);
        stackPrecedence.put("or", 4);
        stackPrecedence.put("not", 5);
        stackPrecedence.put("IN", 6);
        stackPrecedence.put("NOTIN", 6);
        stackPrecedence.put("<", 6);
        stackPrecedence.put(">", 6);
        stackPrecedence.put("<=", 6);
        stackPrecedence.put(">=", 6);
        stackPrecedence.put("==", 6);
        stackPrecedence.put("!=", 6);
        stackPrecedence.put("#", 7);
        stackPrecedence.put("+", 8);
        stackPrecedence.put("-", 8);
        stackPrecedence.put("*", 9);
        stackPrecedence.put("/", 9);
        stackPrecedence.put("^", 10);
        stackPrecedence.put("u-", 12);
        stackPrecedence.put("(", 2);
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
