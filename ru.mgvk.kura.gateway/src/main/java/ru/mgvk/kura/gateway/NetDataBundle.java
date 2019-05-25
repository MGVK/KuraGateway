package ru.mgvk.kura.gateway;

import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireRecord;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetDataBundle implements Serializable {

    transient int                              wireIndex = 0;
    transient List<Map<String, TypedValue<?>>> records   = new ArrayList<>();


    public NetDataBundle(int wireIndex,
                         List<Map<String, TypedValue<?>>> records) {
        this.wireIndex = wireIndex;
        this.records = records;
    }

    public NetDataBundle(int wireIndex) {
        this.wireIndex = wireIndex;
    }

    public NetDataBundle() {

    }

    public NetDataBundle(List<Map<String, TypedValue<?>>> records) {
        this.records = records;
    }

    public static NetDataBundle readFromStream(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        List<Map<String, TypedValue<?>>> records   = new ArrayList<>();
        int                              wireIndex = stream.readInt();
        int                              mainSize  = stream.readInt();
        for (int i = 0; i < mainSize; i++) {

            Map<String, TypedValue<?>> map     = new HashMap<>();
            int                        mapSize = stream.readInt();
            for (int map_i = 0; map_i < mapSize; map_i++) {
                String     key = (String) stream.readObject();
                TypedValue v   = TypedValues.newTypedValue(stream.readObject());
                map.put(key, v);
            }
            records.add(map);
        }

        return new NetDataBundle(wireIndex, records);

    }

    public NetDataBundle toSerializable(List<WireRecord> records) {
//        StringBuilder b = new StringBuilder();
//        records.forEach(wireRecord -> b.append(wireRecord.getProperties().toString()));
//        data = b.toString();
        records.forEach(wireRecord -> NetDataBundle.this.records.add(wireRecord.getProperties()));
        return this;
    }

    public void writeObjectToStream(java.io.ObjectOutputStream stream)
            throws IOException {
        stream.writeInt(wireIndex);
        stream.writeInt(records.size());

        records.forEach(stringTypedValueMap -> {
            try {
                stream.writeInt(stringTypedValueMap.size());
            } catch (IOException e) {
                e.printStackTrace();
            }
            stringTypedValueMap.forEach((s, typedValue) -> {
                try {
                    stream.writeObject(s);
                    stream.writeObject(typedValue.getValue());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    public List<WireRecord> fromSerializable() {
        List<WireRecord> _records = new ArrayList<>();
        this.records.forEach(record -> _records.add(new WireRecord(record)));
        return _records;
    }


    public int getWireIndex() {
        return wireIndex;
    }

    public void setWireIndex(int wireIndex) {
        this.wireIndex = wireIndex;
    }
}
