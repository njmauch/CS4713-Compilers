package pickle;

public class Numeric {
    public int integerValue;
    public double doubleValue;
    public String strValue;
    public SubClassif type;

    public Numeric(Parser parser, ResultValue resO1, String operand, String operandNumber) {
        if(resO1.value == null) {
            System.err.printf(operandNumber + " of " + operand + " wasn't initialized\n");
        }
        try {
            integerValue = Integer.parseInt(resO1.value);
            doubleValue = (double)integerValue;
            strValue = Integer.toString(integerValue);
            type = SubClassif.INTEGER;
        } catch (Exception e) {
            try {
                doubleValue = Double.parseDouble(resO1.value);
                integerValue = (int) doubleValue;
                strValue = Double.toString(doubleValue);
                String last;
                last = strValue.substring(strValue.length() - 2, strValue.length());
                if(last.charAt(0) == '.')
                    strValue += "0";
                type = SubClassif.FLOAT;
            } catch (Exception e2) {
                System.err.printf(operandNumber + " of " + operand + " isn't valid numeric\n");
            }
        }
    }
}
