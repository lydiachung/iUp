package iup;

import static org.junit.Assert.*;
import iup.Version;

import org.joda.time.DateTime;
import org.junit.Test;

public class IUpTest {
	
	@Test
	public void testGetVersion() {
		
		Version expVersion, actVersion;
		
		expVersion = new Version(10, 3, 0, 0, new DateTime(2014, 11, 13, 2, 14, 0));
		actVersion = Version.getVersion("10.3.0.0-20141113.021400");
		
		assertEquals(expVersion, actVersion);
		
		expVersion = new Version(11, 8, 2, 7, new DateTime(2014, 1, 8, 12, 35, 41));
		actVersion = Version.getVersion("11.8.2.7-20140108.123541");
		
		assertEquals(expVersion, actVersion);
		
	}
	
	@Test
	public void testVersionComparable() {
		
		Version version1, version2;
		
		// same
		version1 = new Version(11, 8, 2, 7, new DateTime(2014, 1, 8, 12, 35, 41));
		version2 = new Version(11, 8, 2, 7, new DateTime(2014, 1, 8, 12, 35, 41));
		
		assertEquals(0, version1.compareTo(version2));
		
		// major
		version1 = new Version(10, 8, 2, 7, new DateTime(2014, 1, 8, 12, 35, 41));
		version2 = new Version(11, 8, 2, 7, new DateTime(2014, 1, 8, 12, 35, 41));
		
		assertEquals(-1, version1.compareTo(version2));
		
		// dev
		version1 = new Version(10, 9, 2, 7, new DateTime(2014, 1, 8, 12, 35, 41));
		version2 = new Version(10, 8, 2, 7, new DateTime(2014, 1, 8, 12, 35, 41));
		
		assertEquals(1, version1.compareTo(version2));
		
		// patch
		version1 = new Version(10, 8, 6, 7, new DateTime(2014, 1, 8, 12, 35, 41));
		version2 = new Version(10, 8, 2, 7, new DateTime(2014, 1, 8, 12, 35, 41));
		
		assertEquals(1, version1.compareTo(version2));
		
		// build
		version1 = new Version(10, 8, 6, 1, new DateTime(2014, 1, 8, 12, 35, 41));
		version2 = new Version(10, 8, 6, 7, new DateTime(2014, 1, 8, 12, 35, 41));
		
		assertEquals(-1, version1.compareTo(version2));
		
		// timestamp
		version1 = new Version(10, 8, 6, 1, new DateTime(2014, 1, 8, 10, 35, 41));
		version2 = new Version(10, 8, 6, 1, new DateTime(2014, 1, 8, 12, 35, 41));
		
		assertEquals(-1, version1.compareTo(version2));
		
		version1 = new Version(10, 8, 6, 1, new DateTime(2014, 1, 8, 10, 35, 41));
		version2 = new Version(10, 8, 6, 1, new DateTime(2014, 1, 7, 10, 35, 41));
		
		assertEquals(1, version1.compareTo(version2));
		
	}

}
