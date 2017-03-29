package nsa.datawave.data.hash;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.junit.Before;
import org.junit.Test;

public class SnowflakeUIDTest {
    
    UIDBuilder<UID> builder;
    
    private String data = "20100901: the quick brown fox jumped over the lazy dog";
    private String data2 = "20100831: the quick brown fox jumped over the lazy dog";
    
    @Before
    public void setup() {
        Configuration conf = new Configuration();
        conf.set(UIDConstants.CONFIG_UID_TYPE_KEY, SnowflakeUID.class.getSimpleName());
        conf.set(UIDConstants.CONFIG_MACHINE_ID_KEY, "" + SnowflakeUID.MAX_MACHINE_ID);
        builder = UID.builder(conf);
    }
    
    @Test
    public void testConstructors() throws ParseException {
        // Verify the Snowflake numerical value is too big (> 96 bits)
        Exception result1 = null;
        try {
            new SnowflakeUID(BigInteger.ONE.shiftLeft(97), 16);
        } catch (IllegalArgumentException e) {
            result1 = e;
        }
        assertNotNull(result1);
        
        // Test null copy constructor
        SnowflakeUID uid = new SnowflakeUID(null);
        String result3 = uid.toString();
        assertNotNull(result3);
        
        // Test empty constructor
        uid = new SnowflakeUID();
        assertTrue(uid.compare(new SnowflakeUID(BigInteger.ONE, 10), uid) < 0);
        assertTrue(uid.getMachineId() < 0);
        assertTrue(uid.getNodeId() < 0);
        assertTrue(uid.getPollerId() < 0);
        assertTrue(uid.getThreadId() < 0);
        assertTrue(uid.getTimestamp() < 0);
        assertNotEquals(false, uid.hashCode());
        assertEquals(SnowflakeUID.DEFAULT_RADIX, uid.getRadix());
        assertEquals("null", uid.getShardedPortion());
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void testBuilder() throws ParseException {
        // Test no-arg based builder
        SnowflakeUIDBuilder result1 = (SnowflakeUIDBuilder) ((UIDBuilder) SnowflakeUID.builder());
        assertNotNull(result1);
        assertNotNull(result1.toString());
        assertNull(((SnowflakeUID) result1.newId()).getSnowflake());
        assertNull(result1.newId(0).getSnowflake());
        assertNull(result1.newId(System.currentTimeMillis(), 0).getSnowflake());
        assertNull(result1.newId(System.currentTimeMillis()).getSnowflake());
        
        // Test node, poller, sequence ID based builder
        SnowflakeUIDBuilder result2 = SnowflakeUID.builder(255, 63, 63);
        assertNotNull(result2);
        result2.setRadix(6);
        assertEquals(6, result2.newId().getRadix());
        
        // Test timestamp and machine ID-based builder
        String dateAsString = "2015-12-08T09:40:58.444-0500";
        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(dateAsString);
        long timestamp = date.getTime();
        
        SnowflakeUIDBuilder result3 = SnowflakeUID.builder(timestamp, ((255 << 12) + (63 << 6) + 63));
        assertNotNull(result3);
        SnowflakeUID result4 = result3.newId();
        assertNotNull(result4);
        // We want to make sure SnowflakeUIDBuilder ignores the supplied timestamp
        assertNotEquals(UID.extractTimeOfDay(new Date(timestamp)), result4.getTime());
        
        // Test timestamp and machine ID-based builder, but specify a different sequence ID
        SnowflakeUIDBuilder result5 = SnowflakeUID.builder(timestamp, ((255 << 12) + (63 << 6) + 63));
        assertNotNull(result5);
        SnowflakeUID result6 = result5.newId(timestamp, 100);
        assertNotNull(result6);
        assertNotEquals(timestamp, result6.getTimestamp());
        assertEquals(100, result6.getSequenceId());
        
        // Test timestamp validation
        Exception result7 = null;
        try {
            new SnowflakeUIDBuilder((SnowflakeUID.MAX_TIMESTAMP + 1), 0, 0, 0, 0);
        } catch (IllegalArgumentException e) {
            result7 = e;
        }
        assertNotNull(result7);
        
        // Test sequence ID validation
        Exception result8 = null;
        try {
            new SnowflakeUIDBuilder(System.currentTimeMillis(), 0, 0, 0, SnowflakeUID.MAX_SEQUENCE_ID + 1);
        } catch (IllegalArgumentException e) {
            result8 = e;
        }
        assertNotNull(result8);
        
        // Test node ID validation
        Exception result9 = null;
        try {
            new SnowflakeUIDBuilder(System.currentTimeMillis(), SnowflakeUID.MAX_NODE_ID + 1, 0, 0, 0);
        } catch (IllegalArgumentException e) {
            result9 = e;
        }
        assertNotNull(result9);
        
        // Test poller ID validation
        Exception result10 = null;
        try {
            new SnowflakeUIDBuilder(System.currentTimeMillis(), 0, SnowflakeUID.MAX_POLLER_ID + 1, 0, 0);
        } catch (IllegalArgumentException e) {
            result10 = e;
        }
        assertNotNull(result10);
        
        // Test thread ID validation
        Exception result11 = null;
        try {
            new SnowflakeUIDBuilder(System.currentTimeMillis(), 0, 0, SnowflakeUID.MAX_THREAD_ID + 1, 0);
        } catch (IllegalArgumentException e) {
            result11 = e;
        }
        assertNotNull(result11);
        
        // Test negative sequence ID
        SnowflakeUIDBuilder result12 = SnowflakeUID.builder(timestamp, ((255 << 12) + (63 << 6) + 63));
        assertNotNull(result12);
        SnowflakeUID result13 = result12.newId(timestamp, -1);
        assertNotNull(result13);
        assertNotEquals(timestamp, result13.getTimestamp());
        assertEquals(0, result13.getSequenceId());
        
        // Test negative machine ID
        Exception result14 = null;
        try {
            SnowflakeUID.builder(timestamp, -1);
        } catch (IllegalArgumentException e) {
            result14 = e;
        }
        assertNotNull(result14);
    }
    
    @Test
    public void testDecomposition() throws ParseException {
        // Use a fixed timestamp
        String dateAsString = "2015-12-08T09:40:58.444-0500";
        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(dateAsString);
        long timestamp = date.getTime();
        
        // Construct a Snowflake UUID
        SnowflakeUIDBuilder builder = SnowflakeUID.builder(timestamp, 10, 20, 30, 1111);
        SnowflakeUID result1 = builder.newId("1", "2", "3");
        
        // Get the component parts
        String result2 = result1.getBaseUid();
        long result3 = result1.getTimestamp();
        int result4 = result1.getNodeId();
        int result5 = result1.getPollerId();
        int result6 = result1.getThreadId();
        int result7 = result1.getMachineId();
        int result8 = result1.getSequenceId();
        String result9 = result1.getExtra();
        
        // Validate results
        assertEquals("0a51e000457.1.2.3", result1.toString().substring(11));
        assertEquals("0a51e000457", result2.substring(11));
        assertNotEquals(timestamp, result3);
        assertEquals(10, result4);
        assertEquals(20, result5);
        assertEquals(30, result6);
        assertEquals(((10 << 12) + (20 << 6) + 30), result7);
        assertEquals(1111, result8);
        assertEquals("1.2.3", result9);
    }
    
    @Test
    public void testDecompositionConstructedWithMachineID() throws ParseException {
        // Use a fixed timestamp
        String dateAsString = "2015-12-08T09:40:58.444-0500";
        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(dateAsString);
        long timestamp = date.getTime();
        
        // Construct a Snowflake UUID
        int machineId = (30 << 12) + (20 << 6) + 10;
        SnowflakeUIDBuilder builder = SnowflakeUID.builder(timestamp, machineId, 9999);
        SnowflakeUID result1 = builder.newId("1", "2", "3");
        
        // Get the component parts
        String result2 = result1.getBaseUid();
        long result3 = result1.getTimestamp();
        int result4 = result1.getNodeId();
        int result5 = result1.getPollerId();
        int result6 = result1.getThreadId();
        int result7 = result1.getMachineId();
        int result8 = result1.getSequenceId();
        String result9 = result1.getExtra();
        
        // Validate results
        assertEquals("1e50a00270f.1.2.3", result1.toString().substring(11));
        assertEquals("1e50a00270f", result2.toString().substring(11));
        assertNotEquals(timestamp, result3);
        assertEquals(30, result4);
        assertEquals(20, result5);
        assertEquals(10, result6);
        assertEquals(((30 << 12) + (20 << 6) + 10), result7);
        assertEquals(9999, result8);
        assertEquals("1.2.3", result9);
    }
    
    @Test
    public void testTimestampAndSequenceRollover() {
        long startingTimestamp = System.currentTimeMillis();
        int startingSequence = SnowflakeUID.MAX_SEQUENCE_ID - 1;
        SnowflakeUIDBuilder builder = SnowflakeUID.builder(startingTimestamp, 10, 10, 10, startingSequence);
        SnowflakeUID[] uids = new SnowflakeUID[3];
        for (int i = 0; i < uids.length; i++) {
            uids[i] = builder.newId();
        }
        
        startingTimestamp = uids[0].getTimestamp();
        assertEquals(startingTimestamp, uids[0].getTimestamp()); // Initial timestamp
        assertEquals(startingSequence, uids[0].getSequenceId()); // Initial sequence ID
        
        assertEquals(startingTimestamp, uids[1].getTimestamp()); // Same timestamp
        assertEquals(startingSequence + 1, uids[1].getSequenceId()); // Incremented sequence ID
        
        assertEquals(startingTimestamp + 1, uids[2].getTimestamp()); // Incremented timestamp to next millisecond
        assertEquals(0, uids[2].getSequenceId()); // Rolled over sequence ID to zero
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testParsing() {
        // Test parsing similar to the HashUID test
        UID a = builder.newId();
        UID b = UID.parse(a.toString());
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.compareTo(b) == 0);
        assertTrue(a.compare(a, b) == 0);
        a = builder.newId("blabla");
        b = UID.parse(a.toString());
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.compareTo(b) == 0);
        assertTrue(a.compare(a, b) == 0);
        
        // Test realistic SnowflakeUID parsing
        long timestamp = 1449585658444L;
        String uidString = builder.newId(new Date(timestamp)).toString();
        SnowflakeUID uid = SnowflakeUID.parse(uidString);
        assertNull(uid.getOptionPrefix());
        // Snowflake should not accept timestamp seeds due to risk of collision
        assertNotEquals(timestamp, uid.getTimestamp());
        assertEquals(SnowflakeUID.MAX_MACHINE_ID, uid.getMachineId());
        assertEquals(SnowflakeUID.MAX_NODE_ID, uid.getNodeId());
        assertEquals(SnowflakeUID.MAX_POLLER_ID, uid.getPollerId());
        assertEquals(SnowflakeUID.MAX_THREAD_ID, uid.getThreadId());
        assertEquals(2, uid.getSequenceId());
        assertNull(uid.getExtra());
        assertEquals(uidString, uid.toString());
        
        // Test SnowflakeUID parsing with a specified sequence ID and an appended value
        uidString = uidString + ".something_extra";
        uid = UID.parse(uidString);
        assertNull(uid.getOptionPrefix());
        assertNotEquals(timestamp, uid.getTimestamp());
        assertEquals(SnowflakeUID.MAX_MACHINE_ID, uid.getMachineId());
        assertEquals(SnowflakeUID.MAX_NODE_ID, uid.getNodeId());
        assertEquals(SnowflakeUID.MAX_POLLER_ID, uid.getPollerId());
        assertEquals(SnowflakeUID.MAX_THREAD_ID, uid.getThreadId());
        assertEquals(2, uid.getSequenceId());
        assertEquals("something_extra", uid.getExtra());
        assertEquals(uidString, uid.toString());
        
        // Test parseBase() of UID constructed with a raw BigInteger from a timestamp + extras
        uid = new SnowflakeUID(BigInteger.valueOf(timestamp).shiftLeft(44), 16, "1.2.3", "4");
        SnowflakeUID result1 = SnowflakeUID.parseBase(uid.toString());
        assertTrue(uid.toString().endsWith("1.2.3.4"));
        assertTrue(uid.toString().startsWith(result1.toString()));
        
        // Test parse of null string
        Exception result2 = null;
        try {
            SnowflakeUID.parse(null, 16, 0);
        } catch (IllegalArgumentException e) {
            result2 = e;
        }
        assertNotNull(result2);
    }
    
    @Test
    public void testEquals() {
        long timestamp = 1449585658444L;
        String uidString = builder.newId(new Date(timestamp)).toString();
        UID a = SnowflakeUID.parse(uidString);
        UID b = SnowflakeUID.parse(uidString);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.getExtra() == null);
        a = SnowflakeUID.parse(uidString + ".blabla.blabla.blabla");
        b = SnowflakeUID.parse(uidString + ".blabla.blabla.blabla");
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.getExtra().equals("blabla.blabla.blabla"));
    }
    
    @Test
    public void testDifference() {
        long timestamp = 1449585658444L;
        UID a = builder.newId(data.getBytes(), new Date(timestamp));
        UID b = builder.newId(data2.getBytes(), new Date(timestamp));
        assertTrue(!a.equals(b));
        assertTrue(!b.equals(a));
        a = builder.newId(data.getBytes(), new Date(timestamp), "blabla.blabla.blabla.blabla.blabla.blabla.blabla");
        b = builder.newId(data2.getBytes(), new Date(timestamp), "blabla.blabla.blabla.blabla.blabla.blabla.blabla");
        assertTrue(!a.equals(b));
        assertTrue(!b.equals(a));
        a = builder.newId(data.getBytes(), new Date(timestamp), "blabla", "blabla", "blabla", "blabla", "blabla", "blabla", "blabla");
        b = builder.newId(data2.getBytes(), new Date(timestamp), "blebla", "blabla", "blabla", "blabla", "blabla", "blabla", "blabla");
        assertTrue(!a.equals(b));
        assertTrue(!b.equals(a));
    }
    
    @Test
    public void testComparisons() {
        UID a = builder.newId(data.getBytes());
        UID b = builder.newId(data2.getBytes());
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        a = builder.newId(data.getBytes(), "blabla.blabla");
        b = builder.newId(data2.getBytes(), "blabla.blabla");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        a = builder.newId(data.getBytes(), "blabla.blabla");
        b = builder.newId(data2.getBytes(), "blebla.blabla");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }
    
    @Test
    public void testParse() {
        UID a = builder.newId();
        UID b = UID.parse(a.toString());
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.compareTo(b) == 0);
        assertTrue(a.compare(a, b) == 0);
        a = builder.newId("blabla");
        b = UID.parse(a.toString());
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.compareTo(b) == 0);
        assertTrue(a.compare(a, b) == 0);
    }
    
    @Test
    public void testWritable() throws IOException {
        UID a = builder.newId(data.getBytes(), (Date) null);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        a.write(out);
        out.close();
        
        UID b = builder.newId();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream in = new DataInputStream(bais);
        b.readFields(in);
        in.close();
        baos.close();
        
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        
        a = builder.newId(data.getBytes(), (Date) null);
        
        baos = new ByteArrayOutputStream();
        out = new DataOutputStream(baos);
        a.write(out);
        out.close();
        
        b = builder.newId();
        bais = new ByteArrayInputStream(baos.toByteArray());
        in = new DataInputStream(bais);
        b.readFields(in);
        in.close();
        baos.close();
        
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        
        a = builder.newId(data.getBytes(), "blabla");
        
        baos = new ByteArrayOutputStream();
        out = new DataOutputStream(baos);
        a.write(out);
        out.close();
        
        b = new SnowflakeUID() {};
        bais = new ByteArrayInputStream(baos.toByteArray());
        in = new DataInputStream(bais);
        b.readFields(in);
        in.close();
        baos.close();
        
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
    }
}