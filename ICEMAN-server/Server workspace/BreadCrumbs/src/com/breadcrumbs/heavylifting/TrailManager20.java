package com.breadcrumbs.heavylifting;

import Statics.StaticValues;
import com.breadcrumbs.database.DBMaster;
import com.breadcrumbs.models.Event;
import com.breadcrumbs.models.Location;
import com.breadcrumbs.models.Polyline;
import com.breadcrumbs.models.TrailMetadata;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;

import org.json.JSONObject;
import org.neo4j.graphdb.Node;

public class TrailManager20 {

    private DBMaster dbm;
    
    public TrailManager20() {
        dbm = DBMaster.GetAnInstanceOfDBMaster();
    }
    
    public void SaveMetadata(TrailMetadata metadata, int trailId) {    
        ArrayList<Event> events = metadata.GetEvents();
        Iterator<Event> eventIterator = events.iterator();
        while (eventIterator.hasNext()) {
            Event next = eventIterator.next();
            saveEvent(next, trailId);
        }
    }
    
    // Get all the metadata out of the db.
    public String FetchMetadataFromTrail(String trailId) {
        String cypherQuery = "MATCH (event:Event) WHERE event.TrailId = '"+trailId+"' RETURN event";
        String result = dbm.ExecuteCypherQueryJSONStringReturn(cypherQuery);
        return result;      
    }
    
    private void saveEvent(Event event, int trailId) {
        // Decide if walking or driving
        Hashtable<String, Object> keysAndItems = new Hashtable<String, Object>();
        keysAndItems.put("Id", event.GetId());
        keysAndItems.put("Latitude", event.GetLocation().GetLatitude());
        keysAndItems.put("Longitude", event.GetLocation().GetLongitude());
        if (event.GetPolyline() != null) {
            keysAndItems.put("PolylineString", event.GetPolyline().EncodedPolyline);
            keysAndItems.put("PolylineIsEncoded", event.GetPolyline().IsEncoded);
            keysAndItems.put("TransportMethod", event.GetPolyline().TransportMethod);
        }
        
        
        keysAndItems.put("EventType", event.GetType());
        keysAndItems.put("TrailId", trailId);

        
        // It will not always be a crumb.
        int eventId = dbm.SaveNode(keysAndItems, DBMaster.myLabels.Event);   
        Node crumb = dbm.RetrieveNode(eventId);
        Node trail = dbm.RetrieveNode(trailId);
        dbm.CreateRelationship(crumb, trail, DBMaster.myRelationships.Part_Of);	
    }
    
	/*
	 * Purpose of this method is to process the meta data that we recieve. We want to sort out the bad/unnessesary gps points.
	 * We want to use this method to "clean" the data that we get.
	 * 
	 * @param metadata 
	 * This is basically the description of the entire trail. It contains all the gps points, as well as latitude/longitude of 
	 * every place of interest (where we stay etc), as well as activity at the time etc. This should hopefully not be too much chatter.
            
	 * 
	 * We want to change this information into the road plots (as based on google maps directions) as well as 
	 */
	public TrailMetadata ProcessMetadata(JSONObject metadataObject) {
		ArrayList<Event> events = new ArrayList<Event>();
                ArrayList<Location> gpsWaypoints = new ArrayList<Location>();
                ArrayList<Location> eventLocations = new ArrayList<Location>();
                Polyline eventPolyline = null;
		// At the moment I am just converting each gps point into a trail point, but I want to change that later	
		Iterator<String> keys = metadataObject.keys();
                Location lastLocation = null;
                Location currentLocation = null;
                int count = 0;
		while (keys.hasNext()) {
                        keys.next(); // Just to move through the list
			String next = Integer.toString(count); //We cannot use next, because it seems to pull it out in some random order.
                        JSONObject node = metadataObject.getJSONObject(next);
			String type = node.getString("type");
                         if (type.equals("1")) {
                            String latitude = node.getString("latitude");
                            String longitude = node.getString("longitude");
                            Location location = new Location(latitude, longitude);
                            gpsWaypoints.add(location);
                        } else {
                             // We want to update the location to a new one so that we can draw a line between 
                            if (currentLocation != null ) {
                                lastLocation = new Location(currentLocation.GetLatitude(), currentLocation.GetLongitude());
                            }
                            String transportMethod = node.getString("driving_method");
                            String latitude = node.getString("latitude");
                            String longitude = node.getString("longitude");
                            String eventId = node.getString("eventId");
                            
                            // Create the polyline between this and the last event we processed.
                            currentLocation = new Location(Double.parseDouble(latitude), Double.parseDouble(longitude));
                            int transport_method = Integer.parseInt(transportMethod);
                            if (lastLocation != null) {
                                eventPolyline = FetchPathBetweenEventsWithWaypoints(lastLocation, currentLocation, transport_method, gpsWaypoints);
                                gpsWaypoints.clear();
                            }

                            // Create an event, and add it to our list of events
                            eventLocations.add(currentLocation);
                            Event event = new Event(currentLocation, Integer.parseInt(type),eventPolyline, Integer.parseInt(eventId) ); 
                            events.add(event);
                        }
                        count+= 1;
                       
		}
                // Not sure If I am going to add the polyline to the trail.
                // Build the overview polyline. Im not sure we will use this in the future but this is the simplest way to get it working now.
           /*     String overviewPolylineString = null;
                if (eventLocations.size() > 1) {
                    overviewPolylineString = FetchTotalTrailPath(eventLocations);
                }
                
                // Create TrailMetadata object.
                Polyline polyline = new Polyline(overviewPolylineString, 0, -1, Statics.StaticValues.DRIVING, 1);*/
                TrailMetadata trailMetadata = new TrailMetadata(events, null);
                
		return trailMetadata;
	}
        
        
        public Polyline FetchPathBetweenEvents(Location location1, Location location2, int transportMethod) {
            // Fetch the path between the two locations from google. Build a polyline out of that.
            int isEncoded = 0;
            String encodedString = "";
           
            if (transportMethod == Statics.StaticValues.DRIVING) {
                // Fetch polyline from google.
                encodedString = getGoogleDirectionsWithNoWaypoints(location1, location2);
                if (encodedString == null) {
                    isEncoded = 1;
                    // Build our own custom encoded polyline just by matching the points.
                    encodedString = location1.toString() + "|" + location2.toString();
                }
            } else if (transportMethod == StaticValues.WALKING) {
                // Do simple 
                isEncoded = 1;
                // Build our own custom encoded polyline just by matching the points.
                encodedString = location1.toString() + "|" + location2.toString();
            }           
            Polyline result = new Polyline(encodedString, 1,0, transportMethod, isEncoded);
            return result;
        }
        
        public Polyline FetchPathBetweenEventsWithWaypoints(Location location1, Location location2, 
            int transportMethod, ArrayList<Location> waypoints) {
            // Fetch the path between the two locations from google. Build a polyline out of that.
            int isEncoded = 0;
            String encodedString = "";
           
            if (transportMethod == Statics.StaticValues.DRIVING) {
                // Fetch polyline from google.
                encodedString = getGoogleDirectionsWithWaypoints(location1, location2, waypoints);
                // This is the catch to check if encoding the string using the google api failed.
                if (encodedString == null) {
                    isEncoded = 1;
                    // Build our own custom encoded polyline just by matching the points.
                    encodedString = location1.toString() + "|" + location2.toString();
                }
            } else if (transportMethod == StaticValues.WALKING) {
                // Do simple 
                isEncoded = 1;
                // Build our own custom encoded polyline just by matching the points.
                encodedString = location1.toString() + "|" + location2.toString();
            }
            
            Polyline result = new Polyline(encodedString, 1,0, transportMethod, isEncoded);
            return result;
        }
        
        private String getGoogleDirectionsWithNoWaypoints(Location location1, Location location2) {
            ArrayList<Location> locationList = new ArrayList();
            
            String urlString = buildUrl(locationList, location1, location2);
            JSONObject jsonResponse = fetchDirectionsFromGoogle(urlString);
            if (jsonResponse.getJSONArray("routes").length() == 0) {
                return null;
            }
            
            JSONObject routes = jsonResponse.getJSONArray("routes").getJSONObject(0);
            JSONObject overviewPolyline = routes.getJSONObject("overview_polyline");
            String encodedPolyline = overviewPolyline.getString("points");
            
            return encodedPolyline;
        }
        
        private String getGoogleDirectionsWithWaypoints(Location location1, Location location2, ArrayList<Location> waypoints) {
            
            String urlString = buildUrl(waypoints, location1, location2);
            JSONObject jsonResponse = fetchDirectionsFromGoogle(urlString);
            if (jsonResponse.getJSONArray("routes").length() == 0) {
                return null;
            }
            
            JSONObject routes = jsonResponse.getJSONArray("routes").getJSONObject(0);
            JSONObject overviewPolyline = routes.getJSONObject("overview_polyline");
            String encodedPolyline = overviewPolyline.getString("points");
            
            return encodedPolyline;
        }
        
        public String FetchTotalTrailPath(List<Location> locationList, Location origin, Location destination) {
            // Create a string to grab the polyline from google Directions api
            String urlString = buildUrl(locationList, origin, destination);
            
            // Fetch the data using the url that we have built.
            JSONObject jsonResponse = fetchDirectionsFromGoogle(urlString); // We get a lot of data from this response, some of it is pretty cool so we should look at using more than we are now.
            JSONObject routes = jsonResponse.getJSONObject("routes");
            JSONObject overviewPolyline = routes.getJSONObject("overview_polyline");
            String encodedPolyine = overviewPolyline.getString("points");
            return encodedPolyine;
        }
        
        /*
            Method requests directions from the google directions api, routing the 
            route through the given locations.
        */
        private String buildUrl(List<Location> locationList, Location origin, Location destination) {
            String baseUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=";//-44.9439635,168.8379247&destination=-43.4721,170.017685&key=AIzaSyC3zYs82SyMaMlj2Xbss9b51FuoWJEVF-E"
            
            // Fetch the destination and origin of our trail.
            
            String oLatString = Double.toString(origin.GetLatitude());
            String oLonString = Double.toString(origin.GetLongitude());
            String dLatString = Double.toString(destination.GetLatitude());
            String dLonString = Double.toString(destination.GetLongitude());

            baseUrl = baseUrl.concat(oLatString + "," + oLonString + "&destination="+dLatString + "," + dLonString);
          
            // Iterate through each of the points. Make the first the origin,
            // the last the destination and the rest of them the waypoints.
            if (locationList.size() > 0) {
                boolean first = true;
                baseUrl = baseUrl.concat("&waypoints=");
                Iterator<Location> locationIterator = locationList.iterator();
                while (locationIterator.hasNext()) {
                    Location location = locationIterator.next();
                    Double latitude = location.GetLatitude();
                    Double longitude = location.GetLongitude();
                    String latitudeString = Double.toString(latitude);
                    String longitudeString = Double.toString(longitude);
                    if (!first) {
                    	baseUrl = baseUrl.concat("|"+latitudeString + "," + longitudeString);
                    }
                   
                    first = false;
                }
            }
            baseUrl = baseUrl.concat("&key=AIzaSyC3zYs82SyMaMlj2Xbss9b51FuoWJEVF-E");
            return baseUrl;
        }
        
        private JSONObject fetchDirectionsFromGoogle(String urlString) {
            try {
                // Create the connection
                URL url = new URL(urlString);
                URLConnection yc = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
                String inputLine;

                StringBuilder response = new StringBuilder(); 
                while ((inputLine = in.readLine()) != null)  {
                    System.out.println(inputLine);
                    response.append(inputLine);
                    response.append('\r');                    
                }
                
                in.close();
                JSONObject jsonResponse = new JSONObject(response.toString());
                return jsonResponse;

                
            } catch (MalformedURLException ex) {
                Logger.getLogger(TrailManager20.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TrailManager20.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return null;
        }
        
        // This may cost a bit of memory Im not too sure.
        private class PathResultObject {
            public final boolean encodedOrNot;
            public final String polylineString;
            
            public PathResultObject(boolean encodedOrNot, String polyString) {
                this.encodedOrNot = encodedOrNot;
                this.polylineString = polyString;
            }
        }
}


//AIzaSyC3zYs82SyMaMlj2Xbss9b51FuoWJEVF-E


