package testShit;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import javax.imageio.ImageIO;
import javax.ws.rs.PathParam;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.apache.commons.codec.binary.*;
import org.apache.lucene.util.IOUtils;

import com.breadcrumbs.database.DBMaster;
import com.breadcrumbs.models.Trail;
import com.breadcrumbs.resource.RetrieveData;

public class GETtest {
	private RetrieveData retrieve;

	@Before
	public void setUp() throws Exception {
		retrieve = new RetrieveData();

	}

	@After
	public void tearDown() throws Exception {
		//retrieve.Obliterate(); // Get rid of everything
	}

	@Test
	public void TestGetAllCrumbIdsReturnsData() {

		String id = retrieve.CreateNewUser("Josiah", "7873", "23", "M", "1");
		String trailId = retrieve.SaveTrail("OOOHRAA", "just testing yo", id);
		String crumbId = retrieve.SaveCrumb("testing123", id, trailId, "-36.8", "174.5", "icon", ".jpg", "1", "Greenlane", "Auckland", "New Zealand", "time");
		assertTrue(Integer.parseInt(crumbId) > Integer.parseInt(trailId));
		Trail trails = new Trail();
		try {
			JSONObject jsonResponse = new JSONObject(trails.GetAllTrailsForAUser(Integer.parseInt(id)));
			assertEquals(jsonResponse.length() > 0, true);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test(expected=org.neo4j.graphdb.NotFoundException.class)
	public void TestThatDeleteDeletesNodeAndItsRelationship() {
		String id = retrieve.CreateNewUser("Josiah", "7873", "23", "M", "1");
		String trailId = retrieve.SaveTrail("OOOHRAA", "just testing yo", id);
		String crumbId = retrieve.SaveCrumb("testing123", id, trailId, "-36.8", "174.5", "icon", ".jpg", "1", "Greenlane", "Auckland", "New Zealand", "time");
		assertTrue(Integer.parseInt(crumbId) > Integer.parseInt(trailId));
		Trail trails = new Trail();
		trails.DeleteNodeAndRelationship(trailId);
		DBMaster dbMaster = DBMaster.GetAnInstanceOfDBMaster();
		int intId = Integer.valueOf(trailId);
		Node node = dbMaster.RetrieveNode(intId);
	}

//	@Test
//	public void TestGetAllCrumbsReturnsData() {
//
//		String id = retrieve.CreateNewUser("Josiah", "7873", "23", "M");
//		String trailId = retrieve.SaveTrail("OOOHRAA", "just testing yo", id);
//		String crumbId = retrieve.SaveCrumb("testing123", id, trailId, "-36.8", "174.5", "icon", ".jpg");
//		assertTrue(Integer.parseInt(crumbId) > Integer.parseInt(trailId));
//
//		try {
//			JSONObject jsonResponse = new JSONObject(retrieve.GetAllCrumbsForATrail(trailId));
//			assertEquals(jsonResponse.length() > 0, true);
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	@Test
//	public void TestICanSaveGPSPointsToTheDataBaseMultipleTimes() {
//		String id = retrieve.CreateNewUser("Josiah", "7873", "23", "M");
//		String trailId = retrieve.SaveTrail("OOOHRAA", "just testing yo", id);
//		try {
//			assertTrue(retrieve.GetAllTrails().length() > 0);
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		// Build GPS POiNTS
//	}
//
//	//public static JSONObject GPS_POINTS = new JSONObject("{'Index:0':{'latitude':-36.887355,'longitude':174.7893846,'userId':'66','timeStamp':'1112\/2015 19:55:14','trailId':'67','index':20,'next':21},'Index:1':{'latitude':-36.887355,'longitude':174.7893846,'userId':'66','timeStamp':'11\/12\/2015 19:55:34','trailId':'67','index':21,'next':22}'";
//
//	//}}')
//
//	/*
//	*/
//
//	@Test
//	public void TestThatUserCanBeCreated() {
//		// Warning - this will create a user in the db as it is not inside a
//		// transsaction scope.
//		int userId = Integer.parseInt(retrieve.CreateNewUser("Josiah", "7873", "23", "M"));
//		try {
//			assertTrue(retrieve.GetUser(userId).length() > 0);
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
//
//	@Test
//	public void TestThatTrailCanBeCreated() {
//		String id = retrieve.CreateNewUser("Josiah", "7873", "23", "M");
//		String trailId = retrieve.SaveTrail("OOOHRAA", "just testing yo", id);
//		try {
//			assertTrue(retrieve.GetAllTrails().length() > 0);
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	@Test
//	public void TestThatCrumbCanBeAddedToTrail() {
//		String id = retrieve.CreateNewUser("Josiah", "7873", "23", "M");
//		String trailId = retrieve.SaveTrail("OOOHRAA", "just testing yo", id);
//		String crumbId = retrieve.SaveCrumb("testing123", id, trailId, "-36.8", "174.5", "icon", ".jpg");
//		assertTrue(Integer.parseInt(crumbId) > Integer.parseInt(trailId));
//	}
//
//	@Test
//	public void TestGetAllTrailsReturnsData() {
//		String id = retrieve.CreateNewUser("Josiah", "7873", "23", "M");
//		String trailId = retrieve.SaveTrail("OOOHRAA", "just testing yo", id);
//		try {
//			JSONObject jsonResponse = new JSONObject(retrieve.GetAllTrailsForAUser("6"));
//			assertEquals(jsonResponse.length() > 0, true);
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

}
