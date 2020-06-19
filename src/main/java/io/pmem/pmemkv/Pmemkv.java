package io.pmem.pmemkv;

import io.pmem.pmemkv.KeyValueCallback;
import io.pmem.pmemkv.KeyCallback;
import io.pmem.pmemkv.ValueCallback;

public interface Pmemkv<KeyT, ValueT> {

    public void stop();
    public boolean stopped();
    public void getKeys(KeyCallback<KeyT> callback);
    public void getKeysAbove(KeyT key, KeyCallback<KeyT> callback);
    public void getKeysBelow(KeyT key, KeyCallback<KeyT>  callback);
    public void getKeysBetween(KeyT key1, KeyT key2, KeyCallback<KeyT> callback);
    public long countAll();
    public long countAbove(KeyT key);
    public long countBelow(KeyT key);
    public long countBetween(KeyT key1, KeyT key2);
    public void getAll(KeyValueCallback<KeyT, ValueT> callback);
    public void getAbove(KeyT key, KeyValueCallback<KeyT, ValueT>  callback);
    public void getBelow(KeyT key, KeyValueCallback<KeyT, ValueT>  callback);
    public void getBetween(KeyT key1, KeyT key2, KeyValueCallback<KeyT, ValueT>  callback);
    public boolean exists(KeyT key);
    public void get(KeyT key, ValueCallback<ValueT> callback);
    public ValueT getCopy(KeyT key);
    public void put(KeyT key, ValueT value);
    public boolean remove(KeyT key);
}

