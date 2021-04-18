package pickle;

import java.util.ArrayList;

public class ResultArray extends ResultValue implements Cloneable {

    String value;
    ArrayList<ResultValue> array;
    int declaredSize;
    int lastPopulated;

    public ResultArray(String value, ArrayList array, SubClassif type, Structure structure, int lastPopulated, int declaredSize)
    {
        super(type, value, structure, ";");
        this.value = value;
        this.array = array;
        this.type = type;
        this.lastPopulated = lastPopulated;
        this.declaredSize = declaredSize;
    }

    public ResultArray(String value, SubClassif type, Structure structure) {
        super(type, value, structure, ";");
        this.value = value;
        this.type = type;
        this.structure = structure;
    }

    public ResultArray(String value, SubClassif type, Structure structure, String terminatingStr){
        super(type, value, structure, terminatingStr);
    }

    public ResultArray(){
        super(SubClassif.EMPTY, "", Structure.PRIMITIVE, ";");
        this.value = "";
        this.type = SubClassif.EMPTY;
        this.structure = Structure.PRIMITIVE;
    }

    public ResultArray clone() throws CloneNotSupportedException {
        ResultArray res = (ResultArray) super.clone();
        return res;
    }
}
