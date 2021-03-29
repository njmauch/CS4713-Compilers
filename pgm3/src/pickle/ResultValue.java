package pickle;

import javax.xml.transform.Result;
import java.util.ArrayList;

public class ResultValue {
    public SubClassif type;
    public String value;
    public String structure;
    public String terminatingStr;

    public ResultValue(SubClassif type, String value, String structure, String terminatingStr) {
        this.type = type;
        this.value = value;
        this.structure = structure;
        this.terminatingStr = terminatingStr;
    }

    public ResultValue(SubClassif type, String structure) {
        this.type = type;
        this.value = "";
        this.structure = structure;
        this.terminatingStr = "";
    }

    public ResultValue(String value, SubClassif type) {
        this.type = type;
        this.value = value;
        this.structure = "";
        this.terminatingStr = ";";
    }
    public ResultValue() {
        this.type = SubClassif.EMPTY;
        this.value = "";
        this.structure = "";
        this.terminatingStr = "";
    }

}