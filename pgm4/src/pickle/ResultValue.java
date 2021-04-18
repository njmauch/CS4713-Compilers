package pickle;

enum Structure {
    PRIMITIVE,
    FIXED_ARRAY,
    UNBOUNDED_ARRAY
}

public class ResultValue {
    public SubClassif type;
    public String value;
    public Structure structure;
    public String terminatingStr;

    public ResultValue(SubClassif type, String value, Structure structure, String terminatingStr) {
        this.type = type;
        this.value = value;
        this.structure = structure;
        this.terminatingStr = terminatingStr;
    }

    public ResultValue(SubClassif type, String value) {
        this.type = type;
        this.value = value;
        this.structure = Structure.PRIMITIVE;
        this.terminatingStr = ";";
    }

    public ResultValue(SubClassif type, String value, Structure structure) {
        this.type = type;
        this.value = value;
        this.structure = structure;
        this.terminatingStr = ";";
    }

    public ResultValue() {
        this.type = SubClassif.EMPTY;
        this.value = "";
        this.structure = Structure.PRIMITIVE;
        this.terminatingStr = "";
    }

    public ResultValue clone() throws CloneNotSupportedException {
        ResultValue res = (ResultValue) super.clone();
        return res;
    }
}
