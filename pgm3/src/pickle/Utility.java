package pickle;

public class Utility {

    public static ResultValue addition(Parser parser, ResultValue resO2, ResultValue resO1) throws Exception {
        ResultValue resValue = null;
        Numeric nOp2;
        Numeric nOp1;

        nOp2 = new Numeric(parser, resO2, "+=", "2nd Operand");
        nOp1 = new Numeric(parser, resO1, "+=", "1st Operand");

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue + nOp2.integerValue;
            resValue = new ResultValue(nOp1.type, Integer.toString(intResult), "primitive");
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue + nOp2.doubleValue;
            resValue = new ResultValue( nOp1.type, Double.toString(dResult), "primitive");
        }
        else{
            System.err.print(nOp1.strValue + " / " + nOp2.strValue + " are not numeric\n");
        }
        return resValue;
    }

    public static ResultValue subtraction(Parser parser, ResultValue resO2, ResultValue resO1) throws Exception {
        ResultValue resValue = null;
        Numeric nOp2;
        Numeric nOp1;

        nOp2 = new Numeric(parser, resO2, "-=", "2nd Operand");
        nOp1 = new Numeric(parser, resO1, "-=", "1st Operand");

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue - nOp2.integerValue;
            resValue = new ResultValue(nOp1.type, Integer.toString(intResult), "primitive");
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue - nOp2.doubleValue;
            resValue = new ResultValue(nOp1.type, Double.toString(dResult), "primitive");
        }
        else{
            System.err.print(nOp1.strValue + " / " + nOp2.strValue + " are not numeric\n");
        }
        return resValue;
    }

    public static ResultValue multiplication(Parser parser, ResultValue resO2, ResultValue resO1) throws Exception {
        ResultValue resValue = null;
        Numeric nOp2;
        Numeric nOp1;

        nOp2 = new Numeric(parser, resO2, "*", "2nd Operand");
        nOp1 = new Numeric(parser, resO1, "*", "1st Operand");

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue * nOp2.integerValue;
            resValue = new ResultValue(nOp1.type, Integer.toString(intResult), "primitive");
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue * nOp2.doubleValue;
            resValue = new ResultValue(nOp1.type,Double.toString(dResult),  "primitive");
        }
        else{
            System.err.print(nOp1.strValue + " / " + nOp2.strValue + " are not numeric\n");
        }
        return resValue;
    }

    public static ResultValue division(Parser parser, ResultValue resO2, ResultValue resO1) throws Exception {
        ResultValue resValue = null;
        Numeric nOp2;
        Numeric nOp1;

        nOp2 = new Numeric(parser, resO2, "/", "2nd Operand");
        nOp1 = new Numeric(parser, resO1, "/", "1st Operand");

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue / nOp2.integerValue;
            resValue = new ResultValue( nOp1.type, Integer.toString(intResult), "primitive");
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue / nOp2.doubleValue;
            resValue = new ResultValue(nOp1.type, Double.toString(dResult), "primitive");
        }
        else{
            System.err.print(nOp1.strValue + " / " + nOp2.strValue + " are not numeric\n");
        }
        return resValue;
    }

    public static ResultValue exponential(Parser parser, ResultValue resO2, ResultValue resO1) throws Exception {
        ResultValue resValue = null;
        Numeric nOp2;
        Numeric nOp1;

        nOp2 = new Numeric(parser, resO2, "^", "2nd Operand");
        nOp1 = new Numeric(parser, resO1, "^", "1st Operand");


        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = (int) Math.pow(nOp1.integerValue, nOp2.integerValue);
            resValue = new ResultValue(nOp1.type, Integer.toString(intResult), "primitive");
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = Math.pow(nOp1.doubleValue, nOp2.doubleValue);
            resValue = new ResultValue(nOp1.type, Double.toString(dResult), "primitive");
        }
        else{
            System.err.print(nOp1.strValue + " / " + nOp2.strValue + " are not numeric\n");
        }
        return resValue;
    }

    public static ResultValue negative(Parser parser, ResultValue res) throws Exception {
        ResultValue resValue = null;
        Numeric nOp;

        nOp = new Numeric(parser, res, "U-", "1st Operand");

        if(nOp.type == SubClassif.INTEGER) {
            int intResult = -nOp.integerValue;
            resValue = new ResultValue(nOp.type, Integer.toString(intResult), "primitive");
        }
        else if(nOp.type == SubClassif.FLOAT) {
            double dResult = -nOp.doubleValue;
            resValue = new ResultValue(nOp.type, Double.toString(dResult), "primitive");
        }
        else {
            System.err.print(nOp.strValue + " is not numeric\n");
        }
        return resValue;
    }

    public static ResultValue lessThan(Parser parser, ResultValue resO2, ResultValue resO1){
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");
        return res;
    }

    public static ResultValue greaterThan(Parser parser, ResultValue resO2, ResultValue resO1){
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");
        return res;
    }

    public static ResultValue greaterThanOrEqual(Parser parser, ResultValue resO2, ResultValue resO1){
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");
        return res;
    }

    public static ResultValue lessThanOrEqual(Parser parser, ResultValue resO2, ResultValue resO1){
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");
        return res;
    }

    public static ResultValue equal(Parser parser, ResultValue resO2, ResultValue resO1){
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");
        return res;
    }

    public static ResultValue notEqual(Parser parser, ResultValue resO2, ResultValue resO1){
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");
        return res;
    }

    public static String castFloat(Parser parser, ResultValue res) throws Exception{
        double dResult;

        try {
            dResult = Double.parseDouble(res.value);
            return Double.toString(dResult);
        }
        catch (Exception e){
            parser.error("Can not convert to double: %s", res.value);
            return null;
        }
    }

    public static String castInt(Parser parser, ResultValue res) throws Exception{
        int intResult;

        try {
            intResult = Integer.parseInt(res.value);
            return Integer.toString(intResult);
        }
        catch (Exception e) {
            try {
                intResult = (int) Double.parseDouble(res.value);
                return Integer.toString(intResult);
            } catch (Exception e2) {
                parser.error("Unable to convert to integer: %s", res.value);
                return null;
            }
        }
    }

    public static String castBoolean(Parser parser, ResultValue res) throws Exception {
        if(res.value.equals("T")) {
            return "T";
        }
        else if (res.value.equals("F")) {
            return "F";
        }
        else {
            parser.error("Unable to convert to boolean: %s", res.value);
        }
        return null;
    }
}
