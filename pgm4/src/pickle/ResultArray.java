package pickle;

import java.util.ArrayList;

enum ArrayStructure {
    FIXED_ARRAY,
    UNBOUNDED_ARRAY
}

public class ResultArray extends ResultValue implements Cloneable {

    String value;
    ArrayList<ResultArray> array;
    ArrayStructure structure;
    int declaredSize;
    int lastPopulated;

    public ResultArray(String value, ArrayList array, SubClassif type, ArrayStructure structure, int lastPopulated, int declaredSize)
    {
        super(type, value, String.valueOf(structure), ";");
        this.value = value;
        this.array = array;
        this.type = type;
        this.lastPopulated = lastPopulated;
        this.declaredSize = declaredSize;
    }

    public ResultArray clone() throws CloneNotSupportedException {
        ResultArray res = (ResultArray) super.clone();
        return res;
    }
}
