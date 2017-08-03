package net.sothatsit.blockstore.chunkstore;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class NameStore {
    
    private List<String> names = new ArrayList<>();
    
    public int toInt(String name, boolean create) {
        int index = names.indexOf(name);
        
        if (!create || index >= 0)
            return index;
        
        names.add(name);
        
        return names.size() - 1;
    }
    
    public String fromInt(int name) {
        return names.get(name);
    }
    
    public void write(ObjectOutputStream stream) throws IOException {
        stream.writeInt(names.size());
        
        for(int i = 0; i < names.size(); ++i) {
            stream.writeUTF(names.get(i));
        }
    }
    
    public void read(ObjectInputStream stream) throws IOException {
        int amount = stream.readInt();
        
        for(int i = 0; i < amount; ++i) {
            names.add(stream.readUTF());
        }
    }
    
}
