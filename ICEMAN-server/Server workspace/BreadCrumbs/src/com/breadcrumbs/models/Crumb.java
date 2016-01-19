package com.breadcrumbs.models;


import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.NClob;
import java.util.Hashtable;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.neo4j.graphdb.Node;

import com.breadcrumbs.database.*;
import com.breadcrumbs.database.DBMaster.myLabels;
import com.breadcrumbs.database.DBMaster.myRelationships;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.BodyPartEntity;
public class Crumb {
	
	private DBMaster dbm;
	public String AddCrumb(String chat, String userId, String trailId, String latitude, String longitude, String icon, String extension, String placeId, String suburb, String city, String country, String timeStamp) {
		Trail trailManager = new Trail();
		//create node using node controller
		Hashtable<String, Object> keysAndItems = new Hashtable<String, Object>();
		keysAndItems.put("Chat", chat);
		keysAndItems.put("UserId", userId);
		keysAndItems.put("TrailId", trailId);
		keysAndItems.put("Latitude", latitude);
		keysAndItems.put("Longitude", longitude);
		keysAndItems.put("Icon", icon);
		keysAndItems.put("Extension", extension);
		keysAndItems.put("PlaceId", placeId);
		keysAndItems.put("Suburb", suburb);
		keysAndItems.put("City", city);
		keysAndItems.put("Country", country);
		keysAndItems.put("TimeStamp", timeStamp);

		dbm = DBMaster.GetAnInstanceOfDBMaster();
		int crumbId = dbm.SaveNode(keysAndItems, com.breadcrumbs.database.DBMaster.myLabels.Crumb);	
		Node crumb = dbm.RetrieveNode(crumbId);
		Node trail = dbm.RetrieveNode(Integer.parseInt(trailId));

		// Set the cover photo.
		if (Integer.parseInt(trailManager.GetNumberOfCrumbsForATrail(trailId)) <=1) {
			trailManager.SetCoverPhoto(trailId, Integer.toString(crumbId));
		}
		
		
		dbm.CreateRelationship(crumb, trail, myRelationships.Part_Of);	
		return String.valueOf(crumbId);
	}
	
	public void ConvertAndSaveVideo(InputStream stream, String crumbId) {
		// get first body part (index 0)                
        File targetFile = new File("/usr/share/tomcat7/webapps/images/"+crumbId+".mp4");
        byte[] buffer = new byte[1024];
        OutputStream outputStream;
		try {
			outputStream = new FileOutputStream(targetFile);
            int length;             
            while((length = stream.read(buffer)) > 0)
            	outputStream.write(buffer, 0, length);           
            outputStream.close(); 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * 	why is this not being called?
	 */
	public void ConvertAndSaveImage(String uploadImageString, String crumbId) {
		RenderedImage image = null;
        byte[] imageByte;
        try {
        	String serverAddress = "/var/lib/tomcat7/webapps/images/";
        	String localAddress = "C:/Users/jek40/iceman/ICEMANv0.5/ICEMAN-server/Server workspace/.metadata/.plugins/org.eclipse.wst.server.core/tmp0/wtpwebapps/BreadCrumbs/images/";//"C:/Users/aDirtyCanvas/workspace/BreadCrumbs/WebContent/images/";
            imageByte =  Base64.decodeBase64(uploadImageString);          
            FileOutputStream imageOutFile = new FileOutputStream(
            		serverAddress+crumbId+".jpg");
            
            imageOutFile.write(imageByte);
            	
            imageOutFile.close();
            
            // Now we need to create the thumbnail
            InputStream in = new ByteArrayInputStream(imageByte);
			BufferedImage img = ImageIO.read(in);
			Image scaledImg = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
			BufferedImage thumbnail = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
			thumbnail.createGraphics().drawImage(scaledImg,0,0,null);
			ImageIO.write(thumbnail, "jpg", new File(serverAddress + crumbId+"T.jpg"));

			
			
            Trail trail = new Trail();
            // Now test if we need to save a cover photo for a trail
            //trail.Set
            
            //It's common courtesy to shut the door on the way out...
		} catch (IOException e) {
 
			e.printStackTrace();
		}
            //- this badboy with the crumbId so we can find it again
    
	}

	public String GetLatitudeAndLongitude(String crumbId) {
		dbm = DBMaster.GetAnInstanceOfDBMaster();
		String latitude = dbm.GetStringPropertyFromNode(crumbId, "Latitude");
		String longitude = dbm.GetStringPropertyFromNode(crumbId, "Longitude");
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("Latitude", latitude);
		jsonResponse.put("Longitude", longitude);
		return jsonResponse.toString();
	}
	
}
