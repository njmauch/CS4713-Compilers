package pickle;


import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

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

    public static ResultValue uMinus(Parser parser, ResultValue resO1) throws Exception {
        ResultValue resValue = null;

        if(resO1.type == SubClassif.INTEGER) {
            int intResult = -1* (Integer.parseInt(resO1.value));
            resValue = new ResultValue(resO1.type, Integer.toString(intResult), Structure.PRIMITIVE);
        }
        else if(resO1.type == SubClassif.FLOAT) {
            double dResult = -1.0 * (Double.parseDouble(resO1.value));
            resValue = new ResultValue(resO1.type, Double.toString(dResult), Structure.PRIMITIVE);
        }
        else {
            parser.error(resO1.value + " is not numeric");
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

    public static ResultValue not(Parser parser, ResultValue resO1) throws Exception {
        ResultValue res = new ResultValue();
        if(resO1.type.equals(SubClassif.STRING) || resO1.type.equals(SubClassif.BOOLEAN)) {
            if(resO1.value.equals("T")) {
                res.value = "F";
            }
            else{
                res.value = "T";
            }
            res.type = SubClassif.BOOLEAN;
            res.structure = Structure.PRIMITIVE;
        }
        else {
            parser.error("Can't coerce to boolean: %s", resO1.value);
        }
        return res;
    }

    public static ResultValue or(Parser parser, ResultValue resO1, ResultValue resO2) throws Exception {
        ResultValue resultValue = new ResultValue();
        resultValue.type = SubClassif.BOOLEAN;
        if(resO1.type == SubClassif.BOOLEAN || resO1.type == SubClassif.STRING) {
            resO2.value = castBoolean(parser, resO2);
            if(resO1.value.equals("T") || resO2.value.equals("T")){
                resultValue.value = "T";
            }
            else {
                resultValue.value = "F";
            }
        }
        else {
            parser.error("Unable to convert %s to boolean", resO1.value);
        }
        return resultValue;
    }

    public static boolean validDate(Parser parser, ResultValue date) throws Exception {
        int daysOfMonth[] = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int iYear = Integer.parseInt(date.value.substring(0, 4));
        int iMonth = Integer.parseInt(date.value.substring(5, 7));
        int iDay = Integer.parseInt(date.value.substring(8));

        if(date.value.substring(0, 4).contains("-")) {
            parser.error("Invalid year format, needs to be 4 digits: %d", iYear);
        }
        if(iMonth < 1 || iMonth > 12) {
            parser.error("Invalid month: %d", iMonth);
        }
        if(iDay < 1 || iDay > daysOfMonth[iMonth-1]) {
            parser.error("Invalid day of the month: %d", iDay);
        }
        if(iDay == 29 && iMonth == 2) {
            if(iYear % 4 == 0 && (iYear % 100 != 0 || iYear % 400 == 0)) {
                return true;
            }
            else {
                parser.error("Invalid day of the month since it is not a leap year: %d", iYear);
            }
        }
        return true;
    }

    public static int DateToJulian(ResultValue date) {
        int iCountDays;
        int iYear = Integer.parseInt(date.value.substring(0, 4));
        int iMonth = Integer.parseInt(date.value.substring(5, 7));
        int iDay = Integer.parseInt(date.value.substring(8));

        if(iMonth > 2) {
            iMonth -= 3;
        }
        else {
            iMonth += 9;
            iYear -= 1;
        }
        iCountDays = 365 * iYear + iYear / 4 - iYear / 100 + iYear / 400 + (iMonth * 306 + 5) / 10 + (iDay);
        return iCountDays;
    }

    public static ResultValue dateAdj(Parser parser, ResultValue date, int days) throws Exception {
        if(!validDate(parser, date)) {
            parser.error("Invalid date format: %s", date.value);
        }

        int iYear = Integer.parseInt(date.value.substring(0, 4));
        int iMonth = Integer.parseInt(date.value.substring(5, 7));
        int iDay = Integer.parseInt(date.value.substring(8));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = new GregorianCalendar(iYear, iMonth-1, iDay);
        calendar.add(Calendar.DAY_OF_MONTH, days);

        return new ResultValue(SubClassif.DATE, simpleDateFormat.format(calendar.getTime()), Structure.PRIMITIVE, ";");
    }

    public static ResultValue dateDiff(Parser parser, ResultValue date1, ResultValue date2) throws Exception {
        int iDate1;
        int iDate2;

        if(!validDate(parser, date1)) {
            parser.error("Invalid date format: %s", date1.value);
        }
        if(!validDate(parser, date2)) {
            parser.error("Invalid date format: %s", date2.value);
        }
        iDate1 = DateToJulian(date1);
        iDate2 = DateToJulian(date2);

        return new ResultValue(SubClassif.DATE, "" + (iDate1 - iDate2), Structure.PRIMITIVE, ";");
    }

    public static ResultValue dateAge(Parser parser, ResultValue date1, ResultValue date2) throws Exception {
        int iYear1 = Integer.parseInt(date1.value.substring(0, 4));
        int iMonth1 = Integer.parseInt(date1.value.substring(5, 7));
        int iDay1 = Integer.parseInt(date1.value.substring(8));

        int iYear2 = Integer.parseInt(date2.value.substring(0, 4));
        int iMonth2 = Integer.parseInt(date2.value.substring(5, 7));
        int iDay2 = Integer.parseInt(date2.value.substring(8));

        int iDifference = iYear1 - iYear2;

        if(!validDate(parser, date1)) {
            parser.error("Invalid date format: %s", date1.value);
        }
        if(!validDate(parser, date2)) {
            parser.error("Invalid date format: %s", date2.value);
        }

        if(date1.value.compareTo(date2.value) < 0) {
            if(iMonth2 == iMonth1) {
                if(iDay2 < iDay1) {
                    iDifference++;
                }
            }
            if(iMonth2 < iMonth1) {
                iDifference++;
            }
        }
        else if(date1.value.compareTo(date2.value) > 0) {
            if(iMonth2 == iMonth1) {
                if(iDay2 > iDay1) {
                    iDifference--;
                }
            }
            if(iMonth2 > iMonth2) {
                iDifference--;
            }
        }
        else {
            iDifference = 0;
        }
        return new ResultValue(SubClassif.DATE, "" + iDifference, Structure.PRIMITIVE, ";");
    }
}
