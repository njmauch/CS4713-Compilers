package pickle;


import java.lang.reflect.Array;

public class Utility {

    public static ResultValue addition(Parser parser, Numeric nOp1, Numeric nOp2) throws Exception {
        ResultValue resValue = null;


        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue + nOp2.integerValue;
            resValue = new ResultValue(nOp1.type, Integer.toString(intResult), Structure.PRIMITIVE);
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue + nOp2.doubleValue;
            resValue = new ResultValue( nOp1.type, Double.toString(dResult), Structure.PRIMITIVE);
        }
        else{
            parser.error(nOp1.strValue + " / " + nOp2.strValue + " are not numeric");
        }
        return resValue;
    }

    public static ResultValue subtraction(Parser parser, Numeric nOp1, Numeric nOp2) throws Exception {
        ResultValue resValue = null;

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue - nOp2.integerValue;
            resValue = new ResultValue(nOp1.type, Integer.toString(intResult), Structure.PRIMITIVE);
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue - nOp2.doubleValue;
            resValue = new ResultValue(nOp1.type, Double.toString(dResult), Structure.PRIMITIVE);
        }
        else{
            parser.error(nOp1.strValue + " / " + nOp2.strValue + " are not numeric");
        }
        return resValue;
    }

    public static ResultValue multiplication(Parser parser, Numeric nOp1, Numeric nOp2) throws Exception {
        ResultValue resValue = null;

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue * nOp2.integerValue;
            resValue = new ResultValue(nOp1.type, Integer.toString(intResult), Structure.PRIMITIVE);
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue * nOp2.doubleValue;
            resValue = new ResultValue(nOp1.type,Double.toString(dResult),  Structure.PRIMITIVE);
        }
        else{
            parser.error(nOp1.strValue + " / " + nOp2.strValue + " are not numeric");
        }
        return resValue;
    }

    public static ResultValue division(Parser parser, Numeric nOp1, Numeric nOp2) throws Exception {
        ResultValue resValue = null;

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = nOp1.integerValue / nOp2.integerValue;
            resValue = new ResultValue( nOp1.type, Integer.toString(intResult), Structure.PRIMITIVE);
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = nOp1.doubleValue / nOp2.doubleValue;
            resValue = new ResultValue(nOp1.type, Double.toString(dResult), Structure.PRIMITIVE);
        }
        else{
            parser.error(nOp1.strValue + " / " + nOp2.strValue + " are not numeric");
        }
        return resValue;
    }

    public static ResultValue exponential(Parser parser, Numeric nOp1, Numeric nOp2) throws Exception {
        ResultValue resValue = null;

        if(nOp1.type == SubClassif.INTEGER) {
            int intResult = (int) Math.pow(nOp1.integerValue, nOp2.integerValue);
            resValue = new ResultValue(nOp1.type, Integer.toString(intResult), Structure.PRIMITIVE);
        }
        else if(nOp1.type == SubClassif.FLOAT) {
            double dResult = Math.pow(nOp1.doubleValue, nOp2.doubleValue);
            resValue = new ResultValue(nOp1.type, Double.toString(dResult), Structure.PRIMITIVE);
        }
        else{
            parser.error(nOp1.strValue + " / " + nOp2.strValue + " are not numeric");
        }
        return resValue;
    }

    public static ResultValue uMinus(Parser parser, Numeric nOp) throws Exception {
        ResultValue resValue = null;

        if(nOp.type == SubClassif.INTEGER) {
            int intResult = -nOp.integerValue;
            resValue = new ResultValue(nOp.type, Integer.toString(intResult), Structure.PRIMITIVE);
        }
        else if(nOp.type == SubClassif.FLOAT) {
            double dResult = -nOp.doubleValue;
            resValue = new ResultValue(nOp.type, Double.toString(dResult), Structure.PRIMITIVE);
        }
        else {
            parser.error(nOp.strValue + " is not numeric");
        }
        return resValue;
    }

    public static ResultValue lessThan(Parser parser, ResultValue resO1, ResultValue resO2) throws Exception {
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");

        if(resO1.type.equals(SubClassif.INTEGER)) {
            String intResult = Utility.castInt(parser, resO2);
            int intOp1 = Integer.parseInt(resO1.value);
            int intOp2 = Integer.parseInt(intResult);
            if (intOp1 < intOp2) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.FLOAT)) {
            String dResult = Utility.castFloat(parser, resO2);
            double dOp1 = Double.parseDouble(resO1.value);
            double dOp2 = Double.parseDouble(dResult);
            if (dOp1 < dOp2) {
                res.value = "T";
            } else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.STRING)) {
            int strComp = resO1.value.compareTo(resO2.value);
            if(strComp < 0) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.BOOLEAN)){
            parser.error("Cannot not perform compare on Boolean %s", resO1.value);
        }
        else {
            parser.error("Uknown type: %s", resO1.type);
        }
        return res;
    }

    public static ResultValue greaterThan(Parser parser, ResultValue resO1, ResultValue resO2) throws Exception {
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");

        if(resO1.type.equals(SubClassif.INTEGER)) {
            String intResult = Utility.castInt(parser, resO2);
            int intOp1 = Integer.parseInt(resO1.value);
            int intOp2 = Integer.parseInt(intResult);
            if (intOp1 > intOp2) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.FLOAT)) {
            String dResult = Utility.castFloat(parser, resO2);
            double dOp1 = Double.parseDouble(resO1.value);
            double dOp2 = Double.parseDouble(dResult);
            if (dOp1 > dOp2) {
                res.value = "T";
            } else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.STRING)) {
            int strComp = resO1.value.compareTo(resO2.value);
            if(strComp > 0) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.BOOLEAN)){
            parser.error("Cannot not perform compare on Boolean %s", resO1.value);
        }
        else {
            parser.error("Uknown type: %s", resO1.type);
        }
        return res;
    }

    public static ResultValue greaterThanOrEqual(Parser parser, ResultValue resO1, ResultValue resO2) throws Exception {
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");

        if(resO1.type.equals(SubClassif.INTEGER)) {
            String intResult = Utility.castInt(parser, resO2);
            int intOp1 = Integer.parseInt(resO1.value);
            int intOp2 = Integer.parseInt(intResult);
            if (intOp1 >= intOp2) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.FLOAT)) {
            String dResult = Utility.castFloat(parser, resO2);
            double dOp1 = Double.parseDouble(resO1.value);
            double dOp2 = Double.parseDouble(dResult);
            if (dOp1 >= dOp2) {
                res.value = "T";
            } else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.STRING)) {
            int strComp = resO1.value.compareTo(resO2.value);
            if(strComp >= 0) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.BOOLEAN)){
            parser.error("Cannot not perform compare on Boolean %s", resO1.value);
        }
        else {
            parser.error("Uknown type: %s", resO1.type);
        }
        return res;
    }

    public static ResultValue lessThanOrEqual(Parser parser, ResultValue resO1, ResultValue resO2) throws Exception {
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");

        if(resO1.type.equals(SubClassif.INTEGER)) {
            String intResult = Utility.castInt(parser, resO2);
            int intOp1 = Integer.parseInt(resO1.value);
            int intOp2 = Integer.parseInt(intResult);
            if (intOp1 <= intOp2) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.FLOAT)) {
            String dResult = Utility.castFloat(parser, resO2);
            double dOp1 = Double.parseDouble(resO1.value);
            double dOp2 = Double.parseDouble(dResult);
            if (dOp1 <= dOp2) {
                res.value = "T";
            } else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.STRING)) {
            int strComp = resO1.value.compareTo(resO2.value);
            if(strComp <= 0) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.BOOLEAN)){
            parser.error("Cannot not perform compare on Boolean %s", resO1.value);
        }
        else {
            parser.error("Uknown type: %s", resO1.type);
        }
        return res;
    }

    public static ResultValue equal(Parser parser, ResultValue resO1, ResultValue resO2) throws Exception {
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");

        if(resO1.type.equals(SubClassif.INTEGER)) {
            String intResult = Utility.castInt(parser, resO2);
            int intOp1 = Integer.parseInt(resO1.value);
            int intOp2 = Integer.parseInt(intResult);
            if (intOp1 == intOp2) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.FLOAT)) {
            String dResult = Utility.castFloat(parser, resO2);
            double dOp1 = Double.parseDouble(resO1.value);
            double dOp2 = Double.parseDouble(dResult);
            if (dOp1 == dOp2) {
                res.value = "T";
            } else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.STRING)) {
            int strComp = resO1.value.compareTo(resO2.value);
            if(strComp == 0) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.BOOLEAN)){
            parser.error("Cannot not perform compare on Boolean %s", resO1.value);
        }
        else {
            parser.error("Uknown type: %s", resO1.type);
        }
        return res;
    }

    public static ResultValue notEqual(Parser parser, ResultValue resO1, ResultValue resO2) throws Exception {
        ResultValue res = new ResultValue(SubClassif.BOOLEAN, "");

        if(resO1.type.equals(SubClassif.INTEGER)) {
            String intResult = Utility.castInt(parser, resO2);
            int intOp1 = Integer.parseInt(resO1.value);
            int intOp2 = Integer.parseInt(intResult);
            if (intOp1 != intOp2) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.FLOAT)) {
            String dResult = Utility.castFloat(parser, resO2);
            double dOp1 = Double.parseDouble(resO1.value);
            double dOp2 = Double.parseDouble(dResult);
            if (dOp1 != dOp2) {
                res.value = "T";
            } else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.STRING)) {
            int strComp = resO1.value.compareTo(resO2.value);
            if(strComp != 0) {
                res.value = "T";
            }
            else {
                res.value = "F";
            }
        }
        else if(resO1.type.equals(SubClassif.BOOLEAN)){
            parser.error("Cannot not perform compare on Boolean %s", resO1.value);
        }
        else {
            parser.error("Uknown type: %s", resO1.type);
        }
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

    public static ResultValue LENGTH(String str) {
        ResultValue res;
        if(str == null) {
            res = new ResultValue(SubClassif.INTEGER, "0", Structure.PRIMITIVE);
        }
        else {
            res = new ResultValue(SubClassif.INTEGER, String.valueOf(str.length()), Structure.PRIMITIVE);
        }
        return res;
    }

    public static ResultValue SPACES(String str) {
        ResultValue res = null;
        int i;
        if(str == null) {
            res = new ResultValue(SubClassif.BOOLEAN, "T", Structure.PRIMITIVE);
        }
        else {
            char charArray[] = str.toCharArray();
            res = new ResultValue(SubClassif.BOOLEAN, "F", Structure.PRIMITIVE);
            for (i = 0; i < charArray.length; i++) {
                if (charArray[i] != ' ') {
                    return res;
                }
            }
            res.value = "T";
        }
        return res;
    }

    public static ResultValue ELEM(Parser parser, ResultArray array) throws Exception {
        if (array.structure != Structure.FIXED_ARRAY) {
            parser.error("Invalid argument to ELEM()");
        }
        if (array == null) {
            return new ResultValue(SubClassif.INTEGER, "0", Structure.PRIMITIVE);
        }

        if(array.lastPopulated == 0)
            return new ResultValue(SubClassif.INTEGER, String.valueOf(array.lastPopulated), Structure.PRIMITIVE);
        else
            return new ResultValue(SubClassif.INTEGER, String.valueOf((array.lastPopulated + 1)), Structure.PRIMITIVE);
    }

    public static ResultValue MAXELEM(ResultArray array) {
        ResultValue res = null;
        res = new ResultValue(SubClassif.INTEGER, String.valueOf(array.declaredSize), Structure.PRIMITIVE);
        return res;
    }

    public static ResultValue concat(Parser parser, ResultValue resO1, ResultValue resO2) {
        ResultValue res = new ResultValue(SubClassif.STRING, (resO1.value + resO2.value), Structure.PRIMITIVE);
        return res;
    }
}
