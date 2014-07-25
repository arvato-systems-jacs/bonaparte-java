package de.jpaw.bonaparte.hazelcast.test;

import java.io.IOException;
import java.util.Map;

import org.testng.annotations.Test;

import com.hazelcast.config.Config;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.nio.serialization.SerializationService;
import com.hazelcast.nio.serialization.SerializationServiceBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import de.jpaw.bonaparte.pojos.hazeltest.DSTest;
import de.jpaw.util.ByteUtil;

@Test
public class SerializationTest {

    private void dotstDs(DSTest obj) throws IOException {
        System.out.println("SUB DS");
        
        Config cfg = new Config();
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
        Map<Integer, DSTest> testMap = instance.getMap("dstest");
        testMap.put(1, obj);
        
        // now obtain the raw data behind it
        SerializationService ss = new SerializationServiceBuilder().setUseNativeByteOrder(true).build();
        final Data data1 = ss.toData(obj);

        ObjectDataOutput out = ss.createObjectDataOutput(1024);
        data1.writeData(out);
        byte[] bytes1 = out.toByteArray();
        
        ObjectDataOutput out2 = ss.createObjectDataOutput(1024);
        out2.writeObject(obj);
        byte[] bytes2 = out2.toByteArray();
        System.out.println("Size 1 is " + bytes1.length + ", size 2 is " + bytes2.length);
        System.out.println("buff 2 is\n" + ByteUtil.dump(bytes2, 0));
    }
    
    public void testDataSerializable() throws Exception {
        System.out.println("Test DS");
        
        DSTest tmp = new DSTest();
        tmp.hello = "Hello, world";
        tmp.num = 18;
        tmp.short1 = "0";
        tmp.short2 = "A";
        tmp.hello2 = "alallalallalong... alallalallalong... ding dong!";
        
        dotstDs(tmp);
    }

}
