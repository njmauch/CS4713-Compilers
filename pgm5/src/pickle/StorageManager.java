package pickle;

import java.util.HashMap;

public class StorageManager {
    HashMap<String, ResultValue> storageManager;
    public StorageManager() {
        storageManager = new HashMap<>();
    }

    public ResultValue getValue(String key) {
        if(!storageManager.containsKey(key))
            return null;

        return storageManager.get(key);
    }

    public void insertValue(String key, ResultValue value) {
        storageManager.put(key, value);
    }
}
