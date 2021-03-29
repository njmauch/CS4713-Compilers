package pickle;

public class Utility {

    public static ResultValue addition(Parser parser, Numeric nOp1, Numeric nOp2) {
        ResultValue resValue = null;

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue + nOp2.integerValue;
            resValue = new ResultValue(Integer.toString(intResult), nOp1.type);
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue + nOp2.doubleValue;
            resValue = new ResultValue(Double.toString(dResult), nOp1.type);
        }
        else{
            System.err.printf(nOp1.strValue + " / " + nOp2.strValue + " are not numeric\n");
        }
        return resValue;
    }

    public static ResultValue subtraction(Parser parser, Numeric nOp1, Numeric nOp2) {
        ResultValue resValue = null;

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue - nOp2.integerValue;
            resValue = new ResultValue(Integer.toString(intResult), nOp1.type);
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue - nOp2.doubleValue;
            resValue = new ResultValue(Double.toString(dResult), nOp1.type);
        }
        else{
            System.err.printf(nOp1.strValue + " / " + nOp2.strValue + " are not numeric\n");
        }
        return resValue;
    }

    public static ResultValue multiplication(Parser parser, Numeric nOp1, Numeric nOp2) {
        ResultValue resValue = null;

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue * nOp2.integerValue;
            resValue = new ResultValue(Integer.toString(intResult), nOp1.type);
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue * nOp2.doubleValue;
            resValue = new ResultValue(Double.toString(dResult), nOp1.type);
        }
        else{
            System.err.printf(nOp1.strValue + " / " + nOp2.strValue + " are not numeric\n");
        }
        return resValue;
    }

    public static ResultValue division(Parser parser, Numeric nOp1, Numeric nOp2) {
        ResultValue resValue = null;

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue / nOp2.integerValue;
            resValue = new ResultValue(Integer.toString(intResult), nOp1.type);
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue / nOp2.doubleValue;
            resValue = new ResultValue(Double.toString(dResult), nOp1.type);
        }
        else{
            System.err.printf(nOp1.strValue + " / " + nOp2.strValue + " are not numeric\n");
        }
        return resValue;
    }

    public static ResultValue exponential(Parser parser, Numeric nOp1, Numeric nOp2) {
        ResultValue resValue = null;

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = (int) Math.pow(nOp1.integerValue, nOp2.integerValue);
            resValue = new ResultValue(Integer.toString(intResult), nOp1.type);
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = Math.pow(nOp1.doubleValue, nOp2.doubleValue);
            resValue = new ResultValue(Double.toString(dResult), nOp1.type);
        }
        else{
            System.err.printf(nOp1.strValue + " / " + nOp2.strValue + " are not numeric\n");
        }
        return resValue;
    }

    public static ResultValue negative(Parser parser, Numeric nOp) {
        ResultValue resValue = null;
        if(nOp.type == SubClassif.INTEGER) {
            int intResult = -nOp.integerValue;
            resValue = new ResultValue(Integer.toString(intResult), nOp.type);
        }
        else if(nOp.type == SubClassif.FLOAT) {
            double dResult = -nOp.doubleValue;
            resValue = new ResultValue(Double.toString(dResult), nOp.type);
        }
        else {
            System.err.printf(nOp.strValue + " is not numeric\n");
        }
        return resValue;
    }


}
