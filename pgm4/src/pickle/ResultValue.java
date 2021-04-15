package pickle;

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

    public ResultValue(SubClassif type, String value) {
        this.type = type;
        this.value = value;
        this.structure = "";
        this.terminatingStr = ";";
    }

    public ResultValue(SubClassif type, String value, String structure) {
        this.type = type;
        this.value = value;
        this.structure = structure;
        this.terminatingStr = ";";
    }

    public ResultValue() {
        this.type = SubClassif.EMPTY;
        this.value = "";
        this.structure = "";
        this.terminatingStr = "";
    }

}
