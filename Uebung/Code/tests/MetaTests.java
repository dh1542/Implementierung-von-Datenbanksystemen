import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.NoSuchElementException;

import idb.block.BlockFile;
import idb.buffer.DBBuffer;

import idb.construct.Util;

import idb.meta.Metadata;
import idb.meta.FileCache;

import idb.module.Module;

import idb.datatypes.NamedCombinedRecord;

import idb.record.KeyRecordFile;
import idb.record.DirectRecordFile;
import idb.datatypes.TID;
import idb.datatypes.DBString;


public class MetaTests {
	protected Metadata m;
	protected String[] p;
	protected DBBuffer dbbuf;
	protected FileCache fc;

	@BeforeEach
	public void setup() throws IOException {
		dbbuf = Util.generateClockBuffer(4096, 250);
//		dbbuf = Util.generateSimpleBuffer(4096);
		p = new String[6];
		for(int i=0; i < p.length; ++i){
			File f = File.createTempFile("meta", null);
			p[i] = f.getCanonicalPath();
		}
		fc = new FileCache(dbbuf);
		m = Util.createMetadata(dbbuf, fc, p);
	}

	@AfterEach
	public void tearDown() {
		fc.close();
		for (int i=0; i < p.length; ++i){
			File f = new File(p[i]);
			f.delete();
		}
	}

	@Test
	public void hasRelation() {
		m.addRelation("Student", List.of("Name", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Male"),
				List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT,
					NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL), "MatrNr", 4096);
		assertTrue(m.hasRelation("Student"));
		assertFalse(m.hasRelation("Stxxudent"));
		m.dropRelation("Student");
		assertFalse(m.hasRelation("Student"));
	}

	@Test
	public void dropRelation() {
		m.addRelation("Student", List.of("Name", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Male"),
				List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT,
					NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL), "MatrNr", 4096);
		assertThrows(NoSuchElementException.class, () -> m.dropRelation("Studd"));
		m.dropRelation("Student");
	}
	@Test
	public void unbalancedTypesNamesRelation() {
		assertThrows(IllegalArgumentException.class ,() -> m.addRelation("Student", List.of("FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Male"),
				List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT,
					NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL), "MatrNr", 4096));
	}
	@Test
	public void illegalPrimaryKeyRelation() {
		assertThrows(IllegalArgumentException.class ,() -> m.addRelation("Student", List.of("Name", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Male"),
				List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT,
					NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL), "MatrNr2", 4096));
	}
	@Test
	public void illegalNameRelation() {
		assertThrows(IllegalArgumentException.class ,() -> m.addRelation("Student", List.of("Name", "First_Name", "MatrNr", "Subject", "PostalCode", "City", "Email", "Male"),
				List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT,
					NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL), "MatrNr", 4096));
	}
	@Test
	public void negativeSizeRelation() {
		assertThrows(IllegalArgumentException.class ,() -> m.addRelation("Student", List.of("Name", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Male"),
				List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT,
					NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL), "MatrNr", -4096));
	}
	@Test
	public void hasSurrogateRelation() {
		m.addRelation("Student", List.of("Name", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Male"),
				List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT,
					NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL), null, 4096);
		assertTrue(m.hasRelation("Student"));
		assertFalse(m.hasRelation("Stxxudent"));
		m.dropRelation("Student");
	}
	@Test
	public void doubleRelation() {
		m.addRelation("Student", List.of("Name", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Male"),
				List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT,
					NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL), "MatrNr", 4096);
		assertThrows(IllegalStateException.class ,() -> m.addRelation("Student", List.of("FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Male"),
				List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT,
					NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL), "MatrNr", 4096));
		m.dropRelation("Student");
	}

	@Test
	public void getType() {
		createIDB09Relation();
		try{
			assertEquals(NamedCombinedRecord.Type.STRING, m.getType("Student", "FirstName"));
			assertEquals(NamedCombinedRecord.Type.BOOL, m.getType("Student", "Male"));
			assertEquals(NamedCombinedRecord.Type.INT, m.getType("Results", "Result"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void getTypeInvRel() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.getType("Rslts", "Male"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void getTypeInvCol() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.getType("Results", "Male"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void hasIndexInvRel() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.hasIndex("Rslts", "Male"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void hasIndexInvCol() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.hasIndex("Results", "Male"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void hasIndexPrimary() {
		createIDB09Relation();
		try{
			assertTrue(m.hasIndex("Results", "_Surrogate"));
			assertTrue(m.hasIndex("Student", "MatrNr"));
			assertFalse(m.hasIndex("Student", "PostalCode"));
			assertFalse(m.hasIndex("Results", "MatrNr"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}
	@Test
	public void getStringIdxInv() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.getStringIndex("Rslts", "Male"));
			assertThrows(NoSuchElementException.class, ()-> m.getStringIndex("Results", "Male"));
			assertThrows(NoSuchElementException.class, ()-> m.getStringIndex("Student", "Name"));
			assertThrows(IllegalStateException.class, ()-> m.getStringIndex("Student", "MatrNr"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}
	@Test
	public void getBoolIdxInv() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.getBoolIndex("Rslts", "Male"));
			assertThrows(NoSuchElementException.class, ()-> m.getBoolIndex("Results", "Male"));
			assertThrows(NoSuchElementException.class, ()-> m.getBoolIndex("Student", "Male"));
			assertThrows(IllegalStateException.class, ()-> m.getBoolIndex("Student", "MatrNr"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}
	@Test
	public void getIntIdxInv() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.getIntIndex("Rslts", "Male"));
			assertThrows(NoSuchElementException.class, ()-> m.getIntIndex("Results", "Male"));
			assertThrows(NoSuchElementException.class, ()-> m.getIntIndex("Student", "PostalCode"));
			m.getIntIndex("Student", "MatrNr");
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void getColumns() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.getColumns("Rslts"));
			assertEquals(Set.of("MatrNr", "Date", "_Surrogate", "Result", "PrfNr"), new HashSet<>(m.getColumns("Results")));
			assertEquals(Set.of("MatrNr", "Name", "FirstName", "Subject", "City", "PostalCode", "Email", "Male"), new HashSet<>(m.getColumns("Student")));
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void getPrimaryKey() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.getPrimaryKey("Rslts"));
			assertEquals("_Surrogate", m.getPrimaryKey("Results"));
			assertEquals("MatrNr", m.getPrimaryKey("Student"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void getNCR() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.getNCR("Rslts"));
			NamedCombinedRecord resultsNCR = m.getNCR("Results");
			assertThrows(NoSuchElementException.class, () -> resultsNCR.getType("FirstName"));
			assertEquals(NamedCombinedRecord.Type.STRING, resultsNCR.getType("Date"));
			assertEquals(NamedCombinedRecord.Type.INT, resultsNCR.getType("Result"));
			assertEquals(NamedCombinedRecord.Type.INT, resultsNCR.getType("PrfNr"));
			assertEquals(NamedCombinedRecord.Type.INT, resultsNCR.getType("MatrNr"));
			assertEquals(NamedCombinedRecord.Type.INT, resultsNCR.getType("_Surrogate"));
			NamedCombinedRecord studentNCR = m.getNCR("Student");
			assertThrows(NoSuchElementException.class, () -> studentNCR.getType("_Surrogate"));
			assertEquals(NamedCombinedRecord.Type.STRING, studentNCR.getType("Name"));
			assertEquals(NamedCombinedRecord.Type.STRING, studentNCR.getType("FirstName"));
			assertEquals(NamedCombinedRecord.Type.STRING, studentNCR.getType("Subject"));
			assertEquals(NamedCombinedRecord.Type.STRING, studentNCR.getType("City"));
			assertEquals(NamedCombinedRecord.Type.STRING, studentNCR.getType("Email"));
			assertEquals(NamedCombinedRecord.Type.INT, studentNCR.getType("PostalCode"));
			assertEquals(NamedCombinedRecord.Type.INT, studentNCR.getType("MatrNr"));
			assertEquals(NamedCombinedRecord.Type.BOOL, studentNCR.getType("Male"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void getTID() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.getTIDFile("Rslts"));
			assertNotNull(m.getTIDFile("Results"));
			assertNotNull(m.getTIDFile("Student"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void createHash() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.createIndex("Rslts", "Male"));
			assertThrows(NoSuchElementException.class, ()-> m.createIndex("Results", "Male"));
			m.createIndex("Student", "PostalCode");
			assertThrows(IllegalStateException.class, ()-> m.createIndex("Student", "PostalCode"));
			assertThrows(IllegalStateException.class, ()-> m.createIndex("Student", "MatrNr"));
			assertTrue(m.hasIndex("Student", "PostalCode"));
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void loadTest() throws Exception {
		createIDB09Relation();
		try{
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Wukas", "FirstName"), Util.namedCombinedRecordFrom(21161,"MatrNr"), Util.namedCombinedRecordFrom("Angewandte Philosophie", "Subject"), Util.namedCombinedRecordFrom(91058, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("wukas.legmann@fau.de", "Email"), Util.namedCombinedRecordFrom("Legmann", "Name")));
			m.createIndex("Student", "FirstName");
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Wilbert", "FirstName"), Util.namedCombinedRecordFrom(45731,"MatrNr"), Util.namedCombinedRecordFrom("EEI", "Subject"), Util.namedCombinedRecordFrom(91058, "PostalCode"), Util.namedCombinedRecordFrom("Tennnenlohe", "City"), Util.namedCombinedRecordFrom("wilbert@linner.de", "Email"), Util.namedCombinedRecordFrom("Linner", "Name")));
			KeyRecordFile<TID, DBString> idx = m.getStringIndex("Student", "FirstName");
			TID ign = new TID(0, 0);
			assertEquals(1, idx.size(new DBString("Wukas"), ign));
			assertEquals(1, idx.size(new DBString("Wilbert"), ign));
			assertEquals(0, idx.size(new DBString("Tobias"), ign));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Heinrich", "FirstName"), Util.namedCombinedRecordFrom(12345,"MatrNr"), Util.namedCombinedRecordFrom("Sport", "Subject"), Util.namedCombinedRecordFrom(96050, "PostalCode"), Util.namedCombinedRecordFrom("Bamberg", "City"), Util.namedCombinedRecordFrom("hini@pumper.de", "Email"), Util.namedCombinedRecordFrom("Pumpernickel", "Name")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(12345, "MatrNr"), Util.namedCombinedRecordFrom("21.09.1994", "Date"), Util.namedCombinedRecordFrom(13, "Result"), Util.namedCombinedRecordFrom(1, "PrfNr")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Albert", "FirstName"), Util.namedCombinedRecordFrom(23451,"MatrNr"), Util.namedCombinedRecordFrom("Physik", "Subject"), Util.namedCombinedRecordFrom(90443, "PostalCode"), Util.namedCombinedRecordFrom("Nürnberg", "City"), Util.namedCombinedRecordFrom("ali1stein@gmail.com", "Email"), Util.namedCombinedRecordFrom("Einstein", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Albert", "FirstName"), Util.namedCombinedRecordFrom(34512,"MatrNr"), Util.namedCombinedRecordFrom("Mathematik", "Subject"), Util.namedCombinedRecordFrom(91055, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("albert.brudzewo@fau.de", "Email"), Util.namedCombinedRecordFrom("de Brudzewo", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(false, "Male"), Util.namedCombinedRecordFrom("Elisa", "FirstName"), Util.namedCombinedRecordFrom(88150,"MatrNr"), Util.namedCombinedRecordFrom("Archäologie", "Subject"), Util.namedCombinedRecordFrom(91052, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("elisa.michael@robotics-erlangen.de", "Email"), Util.namedCombinedRecordFrom("Micheal", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(false, "Male"), Util.namedCombinedRecordFrom("Marie", "FirstName"), Util.namedCombinedRecordFrom(45123,"MatrNr"), Util.namedCombinedRecordFrom("Physik", "Subject"), Util.namedCombinedRecordFrom(91052, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("mary.c@yahoo.de", "Email"), Util.namedCombinedRecordFrom("Curie", "Name")));
			m.createIndex("Student", "City");
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Micheal", "FirstName"), Util.namedCombinedRecordFrom(22239,"MatrNr"), Util.namedCombinedRecordFrom("Maschinenbau", "Subject"), Util.namedCombinedRecordFrom(91052, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("m.stadelmann@fau.de", "Email"), Util.namedCombinedRecordFrom("Stadelmann", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(false, "Male"), Util.namedCombinedRecordFrom("Prinzesssin Eleonora", "FirstName"), Util.namedCombinedRecordFrom(22489,"MatrNr"), Util.namedCombinedRecordFrom("Informations- & Kommunikationstechnik", "Subject"), Util.namedCombinedRecordFrom(90513, "PostalCode"), Util.namedCombinedRecordFrom("Zirndorf", "City"), Util.namedCombinedRecordFrom("eli@von-brabant.com", "Email"), Util.namedCombinedRecordFrom("von Brabant", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Tilly", "FirstName"), Util.namedCombinedRecordFrom(32168,"MatrNr"), Util.namedCombinedRecordFrom("Mos - Mäusefangen ohne Stuhlverlassen", "Subject"), Util.namedCombinedRecordFrom(91054, "PostalCode"), Util.namedCombinedRecordFrom("Buckenhof", "City"), Util.namedCombinedRecordFrom("tilly.stubentieger@informatik.studium.fau.de", "Email"), Util.namedCombinedRecordFrom("Tieger", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(false, "Male"), Util.namedCombinedRecordFrom("Margaret", "FirstName"), Util.namedCombinedRecordFrom(51234,"MatrNr"), Util.namedCombinedRecordFrom("Informatik", "Subject"), Util.namedCombinedRecordFrom(90443, "PostalCode"), Util.namedCombinedRecordFrom("Nürnberg", "City"), Util.namedCombinedRecordFrom("ham.ma@hotmail.com", "Email"), Util.namedCombinedRecordFrom("Hamilton", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(false, "Male"), Util.namedCombinedRecordFrom("Marie", "FirstName"), Util.namedCombinedRecordFrom(54321,"MatrNr"), Util.namedCombinedRecordFrom("Chemie", "Subject"), Util.namedCombinedRecordFrom(96052, "PostalCode"), Util.namedCombinedRecordFrom("Bamberg", "City"), Util.namedCombinedRecordFrom("marie.lavoisier@studium.fau.de", "Email"), Util.namedCombinedRecordFrom("Lavoisier", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(false, "Male"), Util.namedCombinedRecordFrom("Margaret", "FirstName"), Util.namedCombinedRecordFrom(43215,"MatrNr"), Util.namedCombinedRecordFrom("Chemie", "Subject"), Util.namedCombinedRecordFrom(90763, "PostalCode"), Util.namedCombinedRecordFrom("Nürnberg", "City"), Util.namedCombinedRecordFrom("cavendish.ma@icloud.com", "Email"), Util.namedCombinedRecordFrom("Cavendish", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Albrecht", "FirstName"), Util.namedCombinedRecordFrom(32154,"MatrNr"), Util.namedCombinedRecordFrom("Mathematik", "Subject"), Util.namedCombinedRecordFrom(90408, "PostalCode"), Util.namedCombinedRecordFrom("Nürnberg", "City"), Util.namedCombinedRecordFrom("me@duerer.de", "Email"), Util.namedCombinedRecordFrom("Dürer", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(false, "Male"), Util.namedCombinedRecordFrom("Jade", "FirstName"), Util.namedCombinedRecordFrom(21543,"MatrNr"), Util.namedCombinedRecordFrom("Informatik", "Subject"), Util.namedCombinedRecordFrom(96052, "PostalCode"), Util.namedCombinedRecordFrom("Bamberg", "City"), Util.namedCombinedRecordFrom("jade@myspace.com", "Email"), Util.namedCombinedRecordFrom("Raymond", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(false, "Male"), Util.namedCombinedRecordFrom("Grace", "FirstName"), Util.namedCombinedRecordFrom(15432,"MatrNr"), Util.namedCombinedRecordFrom("Informatik", "Subject"), Util.namedCombinedRecordFrom(91058, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("g.h@navy.gov", "Email"), Util.namedCombinedRecordFrom("Hopper", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Alan", "FirstName"), Util.namedCombinedRecordFrom(13524,"MatrNr"), Util.namedCombinedRecordFrom("Informatik", "Subject"), Util.namedCombinedRecordFrom(90449, "PostalCode"), Util.namedCombinedRecordFrom("Nürnberg", "City"), Util.namedCombinedRecordFrom("616c616e@547572696e670d0a.uk", "Email"), Util.namedCombinedRecordFrom("Turing", "Name")));
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Tobias", "FirstName"), Util.namedCombinedRecordFrom(32486,"MatrNr"), Util.namedCombinedRecordFrom("Maschinenbau", "Subject"), Util.namedCombinedRecordFrom(91052, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("koch.tobias@fau", "Email"), Util.namedCombinedRecordFrom("Köch", "Name")));

			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(12345, "MatrNr"), Util.namedCombinedRecordFrom("29.09.1994", "Date"), Util.namedCombinedRecordFrom(23, "Result"), Util.namedCombinedRecordFrom(2, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(23451, "MatrNr"), Util.namedCombinedRecordFrom("29.04.1904", "Date"), Util.namedCombinedRecordFrom(33, "Result"), Util.namedCombinedRecordFrom(3, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(23451, "MatrNr"), Util.namedCombinedRecordFrom("29.09.1904", "Date"), Util.namedCombinedRecordFrom(43, "Result"), Util.namedCombinedRecordFrom(4, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(23451, "MatrNr"), Util.namedCombinedRecordFrom("29.01.1905", "Date"), Util.namedCombinedRecordFrom(27, "Result"), Util.namedCombinedRecordFrom(4, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(23451, "MatrNr"), Util.namedCombinedRecordFrom("14.03.1904", "Date"), Util.namedCombinedRecordFrom(20, "Result"), Util.namedCombinedRecordFrom(5, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(23451, "MatrNr"), Util.namedCombinedRecordFrom("17.03.1904", "Date"), Util.namedCombinedRecordFrom(17, "Result"), Util.namedCombinedRecordFrom(10, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(34512, "MatrNr"), Util.namedCombinedRecordFrom("17.03.1445", "Date"), Util.namedCombinedRecordFrom(27, "Result"), Util.namedCombinedRecordFrom(6, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(34512, "MatrNr"), Util.namedCombinedRecordFrom("18.03.1445", "Date"), Util.namedCombinedRecordFrom(20, "Result"), Util.namedCombinedRecordFrom(7, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(34512, "MatrNr"), Util.namedCombinedRecordFrom("20.03.1445", "Date"), Util.namedCombinedRecordFrom(17, "Result"), Util.namedCombinedRecordFrom(8, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(34512, "MatrNr"), Util.namedCombinedRecordFrom("20.07.1445", "Date"), Util.namedCombinedRecordFrom(10, "Result"), Util.namedCombinedRecordFrom(9, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(45123, "MatrNr"), Util.namedCombinedRecordFrom("29.09.1904", "Date"), Util.namedCombinedRecordFrom(13, "Result"), Util.namedCombinedRecordFrom(4, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(45123, "MatrNr"), Util.namedCombinedRecordFrom("14.03.1904", "Date"), Util.namedCombinedRecordFrom(20, "Result"), Util.namedCombinedRecordFrom(5, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(45123, "MatrNr"), Util.namedCombinedRecordFrom("17.03.1904", "Date"), Util.namedCombinedRecordFrom(33, "Result"), Util.namedCombinedRecordFrom(10, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(51234, "MatrNr"), Util.namedCombinedRecordFrom("14.01.1924", "Date"), Util.namedCombinedRecordFrom(27, "Result"), Util.namedCombinedRecordFrom(13, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(51234, "MatrNr"), Util.namedCombinedRecordFrom("17.05.1924", "Date"), Util.namedCombinedRecordFrom(33, "Result"), Util.namedCombinedRecordFrom(12, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(54321, "MatrNr"), Util.namedCombinedRecordFrom("19.05.1774", "Date"), Util.namedCombinedRecordFrom(33, "Result"), Util.namedCombinedRecordFrom(14, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(54321, "MatrNr"), Util.namedCombinedRecordFrom("14.05.1775", "Date"), Util.namedCombinedRecordFrom(37, "Result"), Util.namedCombinedRecordFrom(15, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(54321, "MatrNr"), Util.namedCombinedRecordFrom("12.05.1775", "Date"), Util.namedCombinedRecordFrom(33, "Result"), Util.namedCombinedRecordFrom(16, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(54321, "MatrNr"), Util.namedCombinedRecordFrom("10.04.1775", "Date"), Util.namedCombinedRecordFrom(33, "Result"), Util.namedCombinedRecordFrom(17, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(32154, "MatrNr"), Util.namedCombinedRecordFrom("10.02.1500", "Date"), Util.namedCombinedRecordFrom(13, "Result"), Util.namedCombinedRecordFrom(18, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(32154, "MatrNr"), Util.namedCombinedRecordFrom("14.02.1500", "Date"), Util.namedCombinedRecordFrom(17, "Result"), Util.namedCombinedRecordFrom(19, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(32154, "MatrNr"), Util.namedCombinedRecordFrom("14.02.1500", "Date"), Util.namedCombinedRecordFrom(20, "Result"), Util.namedCombinedRecordFrom(20, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(32154, "MatrNr"), Util.namedCombinedRecordFrom("14.03.1500", "Date"), Util.namedCombinedRecordFrom(33, "Result"), Util.namedCombinedRecordFrom(21, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(32154, "MatrNr"), Util.namedCombinedRecordFrom("14.02.1501", "Date"), Util.namedCombinedRecordFrom(40, "Result"), Util.namedCombinedRecordFrom(22, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(21543, "MatrNr"), Util.namedCombinedRecordFrom("14.02.1980", "Date"), Util.namedCombinedRecordFrom(40, "Result"), Util.namedCombinedRecordFrom(23, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(21543, "MatrNr"), Util.namedCombinedRecordFrom("14.07.1980", "Date"), Util.namedCombinedRecordFrom(43, "Result"), Util.namedCombinedRecordFrom(24, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(21543, "MatrNr"), Util.namedCombinedRecordFrom("24.02.1981", "Date"), Util.namedCombinedRecordFrom(23, "Result"), Util.namedCombinedRecordFrom(24, "PrfNr")));

			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(13524, "MatrNr"), Util.namedCombinedRecordFrom("24.02.1933", "Date"), Util.namedCombinedRecordFrom(23, "Result"), Util.namedCombinedRecordFrom(25, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(13524, "MatrNr"), Util.namedCombinedRecordFrom("12.03.1933", "Date"), Util.namedCombinedRecordFrom(33, "Result"), Util.namedCombinedRecordFrom(26, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(13524, "MatrNr"), Util.namedCombinedRecordFrom("10.03.1933", "Date"), Util.namedCombinedRecordFrom(30, "Result"), Util.namedCombinedRecordFrom(27, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(13524, "MatrNr"), Util.namedCombinedRecordFrom("13.09.1933", "Date"), Util.namedCombinedRecordFrom(27, "Result"), Util.namedCombinedRecordFrom(28, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(13524, "MatrNr"), Util.namedCombinedRecordFrom("10.09.1933", "Date"), Util.namedCombinedRecordFrom(20, "Result"), Util.namedCombinedRecordFrom(29, "PrfNr")));
			m.insert("Results", List.of("MatrNr", "Date", "Result", "PrfNr"), List.of(Util.namedCombinedRecordFrom(13542, "MatrNr"), Util.namedCombinedRecordFrom("19.09.1933", "Date"), Util.namedCombinedRecordFrom(20, "Result"), Util.namedCombinedRecordFrom(30, "PrfNr")));

			m.addBoolColumn("Student", "Valid", true);

		// This query is:
		// A := Scan(1)
		// B := Scan(2)
		// C := Cross(A, B, "Stud", "Results")
		// D := Store(C)
		// E := Sel(D, Stud.MatrNR == Results.MatrNr)
		// F := Group(E, List.of(min("Results.Result"), count()), "Stud.Name", "Stud.First Name", "Stud.Subject", "Stud.City", "Stud.Email")
		// G := Sel(F, count() >= 3)
		// H := GEN(G, x -> namedCombinedRecordFrom(x.getString("Stud.First Name") + x.getString("Stud.Name"), "Full Name"))
		// K := REN(H, x -> x.startsWith("Stud.") ? x.split(".")[1] : x)
		// I := PROJ(K, "min(Results.Result)", "Full Name", "Subject", "City", "Email");
		// L := Order(I, "min(Results.Result)", "Full Name");
		//
		// Results:
		// 12345 has to few results
		// 23451 : 1.7
		// 34512 : 1.0
		// 45123: 1.3
		// 51234 has to few results
		// 54321 : 3.3
		// 43215 has no results
		// 32154 : 1.3
		// 21543 : 2.3
		// 15432 has no results
		// 13524 : 2.0
	// (1): Name: String, First Name: String, MatrNR: Int, Subject: String, PostalCode: Int, City: String, Email: String, Male: Bool
	// (2): MatrNr: Int, PrfNr: Int, Date: String, Result: Int
		Module a = Util.generateTableScan(m.getTIDFile("Student").view(), m.getNCR("Student"));
		Module b = Util.generateTableScan(m.getTIDFile("Results").view(), m.getNCR("Results"));
		Module c = Util.generateCross(a, b, "Stud", "Results");
		Module d = Util.generateStore(c, dbbuf);
		Module e = Util.generateSel(d, x -> x.getInt("Stud.MatrNr") == x.getInt("Results.MatrNr"));
		Module f = Util.generateGroup(e, List.of(Util.count(), Util.min("Results.Result")), "Stud.Name", "Stud.FirstName", "Stud.Subject", "Stud.City", "Stud.Email", "Stud.Valid");
		Module g = Util.generateSel(f, x -> x.getInt("count()") >= 3);
		Module h = Util.generateGen(g, x -> Util.namedCombinedRecordFrom(x.getString("Stud.FirstName")+" "+x.getString("Stud.Name"), "Full Name"));
		Module km = Util.generateRename(h, x -> x.startsWith("Stud.") ? x.split("\\.")[1] : x);
		Module k = Util.generateStore(km, dbbuf);
		Module i = Util.generateProj(k, "min(Results.Result)", "Full Name", "Subject", "City", "Email", "Valid");
		Module l = Util.generateOrder(i, true, "min(Results.Result)", "Full Name");
		NamedCombinedRecord ncr = l.pull();
		assertNotNull(ncr); // de Brudzewo
		assertEquals(10, ncr.getInt("min(Results.Result)"));
		assertEquals("Albert de Brudzewo", ncr.getString("Full Name"));
		assertEquals("Mathematik", ncr.getString("Subject"));
		assertEquals("Erlangen", ncr.getString("City"));
		assertEquals("albert.brudzewo@fau.de", ncr.getString("Email"));
		assertTrue(ncr.getBool("Valid"));
		ncr = l.pull(); //Albrecht Dürer
		assertNotNull(ncr);
		assertEquals(13, ncr.getInt("min(Results.Result)"));
		assertEquals("Albrecht Dürer", ncr.getString("Full Name"));
		assertEquals("Mathematik", ncr.getString("Subject"));
		assertEquals("Nürnberg", ncr.getString("City"));
		assertEquals("me@duerer.de", ncr.getString("Email"));
		assertTrue(ncr.getBool("Valid"));
		ncr = l.pull(); //Marie Curie
		assertNotNull(ncr);
		assertEquals(13, ncr.getInt("min(Results.Result)"));
		assertEquals("Marie Curie", ncr.getString("Full Name"));
		assertEquals("Physik", ncr.getString("Subject"));
		assertEquals("Erlangen", ncr.getString("City"));
		assertEquals("mary.c@yahoo.de", ncr.getString("Email"));
		assertTrue(ncr.getBool("Valid"));
		ncr = l.pull(); //Albert Einstein
		assertNotNull(ncr);
		assertEquals(17, ncr.getInt("min(Results.Result)"));
		assertEquals("Albert Einstein", ncr.getString("Full Name"));
		assertEquals("Physik", ncr.getString("Subject"));
		assertEquals("Nürnberg", ncr.getString("City"));
		assertEquals("ali1stein@gmail.com", ncr.getString("Email"));
		assertTrue(ncr.getBool("Valid"));
		ncr = l.pull(); //Alan Turing
		assertNotNull(ncr);
		assertEquals("Alan Turing", ncr.getString("Full Name"));
		assertEquals(20, ncr.getInt("min(Results.Result)"));
		assertEquals("Informatik", ncr.getString("Subject"));
		assertEquals("Nürnberg", ncr.getString("City"));
		assertEquals("616c616e@547572696e670d0a.uk", ncr.getString("Email"));
		assertTrue(ncr.getBool("Valid"));
		ncr = l.pull(); //Jade Raymond
		assertNotNull(ncr);
		assertEquals(23, ncr.getInt("min(Results.Result)"));
		assertEquals("Jade Raymond", ncr.getString("Full Name"));
		assertEquals("Informatik", ncr.getString("Subject"));
		assertEquals("Bamberg", ncr.getString("City"));
		assertEquals("jade@myspace.com", ncr.getString("Email"));
		assertTrue(ncr.getBool("Valid"));
		ncr = l.pull(); //Marie Lavoisier
		assertNotNull(ncr);
		assertEquals(33, ncr.getInt("min(Results.Result)"));
		assertEquals("Marie Lavoisier", ncr.getString("Full Name"));
		assertEquals("Chemie", ncr.getString("Subject"));
		assertEquals("Bamberg", ncr.getString("City"));
		assertEquals("marie.lavoisier@studium.fau.de", ncr.getString("Email"));
		assertTrue(ncr.getBool("Valid"));
		assertNull(l.pull());
		assertNull(l.pull());
		assertNull(l.pull());
		l.close();
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void insertExceptios() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.insert("Rslts", List.of("Male"), List.of(Util.namedCombinedRecordFrom(false, "Male"))));
			assertThrows(IllegalArgumentException.class, ()-> m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(false, "Male"))));
			// Surrogate
			assertThrows(IllegalArgumentException.class, ()-> m.insert("Results", List.of("MatNr", "Date", "Result", "PrfNr", "_Surrogate"), List.of(Util.namedCombinedRecordFrom(21161, "MatNr"), Util.namedCombinedRecordFrom("21.09.2019", "Date"), Util.namedCombinedRecordFrom(23,"Result"), Util.namedCombinedRecordFrom(198541, "PrfNr"), Util.namedCombinedRecordFrom(4, "_Surrogate"))));
			assertThrows(IllegalArgumentException.class, ()-> m.insert("Results", List.of("MatNr", "Date", "Result"), List.of(Util.namedCombinedRecordFrom(21161, "MatNr"), Util.namedCombinedRecordFrom("21.09.2019", "Date"), Util.namedCombinedRecordFrom(23,"Result"))));
			// CustomSurrogate
			assertThrows(IllegalArgumentException.class, ()-> m.insert("Results", List.of("MatNr", "Result", "PrfNr", "_Surrogate"), List.of(Util.namedCombinedRecordFrom(21161, "MatNr"), Util.namedCombinedRecordFrom(23,"Result"), Util.namedCombinedRecordFrom(198541, "PrfNr"), Util.namedCombinedRecordFrom(4, "_Surrogate"))));
			// invalid AttrName
			assertThrows(NoSuchElementException.class, ()-> m.insert("Student", List.of("Mane", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Mane"), Util.namedCombinedRecordFrom("Wukas", "FirstName"), Util.namedCombinedRecordFrom(21161,"MatrNr"), Util.namedCombinedRecordFrom("Angewandte Philosophie", "Subject"), Util.namedCombinedRecordFrom(91058, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("wukas.legmann@fau.de", "Email"), Util.namedCombinedRecordFrom("Legmann", "Name"))));
			assertThrows(NoSuchElementException.class, ()-> m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Mane"), Util.namedCombinedRecordFrom("Wukas", "FirstName"), Util.namedCombinedRecordFrom(21161,"MatrNr"), Util.namedCombinedRecordFrom("Angewandte Philosophie", "Subject"), Util.namedCombinedRecordFrom(91058, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("wukas.legmann@fau.de", "Email"), Util.namedCombinedRecordFrom("Legmann", "Name"))));

			// type-mismatch
			assertThrows(IllegalStateException.class, ()-> m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Wukas", "FirstName"), Util.namedCombinedRecordFrom(21161,"MatrNr"), Util.namedCombinedRecordFrom("Angewandte Philosophie", "Subject"), Util.namedCombinedRecordFrom(91058, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("wukas.legmann@fau.de", "Email"), Util.namedCombinedRecordFrom(29872, "Name"))));

			//correct
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Wukas", "FirstName"), Util.namedCombinedRecordFrom(21161,"MatrNr"), Util.namedCombinedRecordFrom("Angewandte Philosophie", "Subject"), Util.namedCombinedRecordFrom(91058, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("wukas.legmann@fau.de", "Email"), Util.namedCombinedRecordFrom("Legmann", "Name")));

			//duplicated PK
			assertThrows(IllegalStateException.class, ()-> m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Wukas", "FirstName"), Util.namedCombinedRecordFrom(21161,"MatrNr"), Util.namedCombinedRecordFrom("Angewandte Philosophie", "Subject"), Util.namedCombinedRecordFrom(91058, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("wukas.legmann@fau.de", "Email"), Util.namedCombinedRecordFrom("Legmann", "Name"))));
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void addIntColumn() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.addIntColumn("Rslts", "Male", 2));
			assertThrows(IllegalArgumentException.class, ()-> m.addIntColumn("Results", "PrfNr", 2));
			m.addIntColumn("Student", "DDay", 1);
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name", "DDay"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Wukas", "FirstName"), Util.namedCombinedRecordFrom(21161,"MatrNr"), Util.namedCombinedRecordFrom("Angewandte Philosophie", "Subject"), Util.namedCombinedRecordFrom(91058, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("wukas.legmann@fau.de", "Email"), Util.namedCombinedRecordFrom("Legmann", "Name"), Util.namedCombinedRecordFrom(6, "DDay")));
			m.addIntColumn("Student", "Days", 1);
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void addStringColumn() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.addStringColumn("Rslts", "Male", "2"));
			assertThrows(IllegalArgumentException.class, ()-> m.addStringColumn("Results", "PrfNr", "2"));
			m.addStringColumn("Student", "DDay", "1");
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name", "DDay"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Wukas", "FirstName"), Util.namedCombinedRecordFrom(21161,"MatrNr"), Util.namedCombinedRecordFrom("Angewandte Philosophie", "Subject"), Util.namedCombinedRecordFrom(91058, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("wukas.legmann@fau.de", "Email"), Util.namedCombinedRecordFrom("Legmann", "Name"), Util.namedCombinedRecordFrom("6.6.1944", "DDay")));
			m.addStringColumn("Student", "Days", "1");
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	@Test
	public void addBoolColumn() {
		createIDB09Relation();
		try{
			assertThrows(NoSuchElementException.class, ()-> m.addBoolColumn("Rslts", "Male", true));
			assertThrows(IllegalArgumentException.class, ()-> m.addBoolColumn("Results", "PrfNr", true));
			m.addBoolColumn("Student", "DDay", true);
			m.insert("Student", List.of("Male", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Name", "DDay"), List.of(Util.namedCombinedRecordFrom(true, "Male"), Util.namedCombinedRecordFrom("Wukas", "FirstName"), Util.namedCombinedRecordFrom(21161,"MatrNr"), Util.namedCombinedRecordFrom("Angewandte Philosophie", "Subject"), Util.namedCombinedRecordFrom(91058, "PostalCode"), Util.namedCombinedRecordFrom("Erlangen", "City"), Util.namedCombinedRecordFrom("wukas.legmann@fau.de", "Email"), Util.namedCombinedRecordFrom("Legmann", "Name"), Util.namedCombinedRecordFrom(false, "DDay")));
			m.addBoolColumn("Student", "Days", false);
		}
		finally {
			tearDownIDB09Relation();
		}
	}

	protected void createIDB09Relation() {
		m.addRelation("Results", List.of("MatrNr", "PrfNr", "Date", "Result"), List.of(NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT), null, 4096);
		m.addRelation("Student", List.of("Name", "FirstName", "MatrNr", "Subject", "PostalCode", "City", "Email", "Male"),
				List.of(NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.INT,
					NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.STRING, NamedCombinedRecord.Type.BOOL), "MatrNr", 4096);
	}

	private void tearDownIDB09Relation() {
		m.dropRelation("Student");
		m.dropRelation("Results");
	}
}
