package pickle;

import java.util.HashMap;

public class StorageManager {
    HashMap<String, ResultValue> sManager;
    public StorageManager() {
        sManager = new HashMap<String, ResultValue>();
    }

    public ResultValue getValue(String key) {
        if(!sManager.containsKey(key))
            return null;

        return sManager.get(key);
    }

    public void insertValue(String key, ResultValue value) {
        sManager.put(key, value);
    }
}
