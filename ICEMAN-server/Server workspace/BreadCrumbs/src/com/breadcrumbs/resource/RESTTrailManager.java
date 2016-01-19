package com.breadcrumbs.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;

import com.breadcrumbs.models.Trail;
import com.breadcrumbs.models.UserService;

@Path("/TrailManager")
public class RESTTrailManager {
	
	/* The problem heere is that 
	 * I need to know the last trailSaved. How does the client know this?
	 * I need to be able to save the last
	 * */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/SaveTrailPoints")
	public String SaveTrailPoints(String TrailPoints) {
		JSONObject TrailPointsJSON = new JSONObject(TrailPoints);
		Trail trail = new Trail();
		System.out.println("Saved these trails: " + TrailPoints);
		return trail.SaveJSONOfTrailPoints(TrailPointsJSON);
	}
	
	@GET
	@Path("/GetAllTrailsForAUser/{UserId}")
	public String GetAllTrailsForAUser(@PathParam("UserId") String UserId) {
		Trail trail = new Trail();
		return trail.GetAllTrailsForAUser(Integer.parseInt(UserId));
	}
	
	@GET
	@Path("/GetAllTrailPoints/{TrailId}")
	public String GetAllTrailPoints(@PathParam("TrailId") String TrailId) {
		Trail trail = new Trail();
		return trail.GetAllTrailPointsForATrail(TrailId);
	}
	
	// Returns path to the trail we want to load.
	/**
	 * @param TrailId The trail we want to give a cover photo
	 * @param ImageId The Image we are going to display as a cover photo. 
	 * 		  The image a user selects to display as a cover photo must be part of their trail,
	 * 		  So it will always exist. This means we can just save the reference to the image 
	 * 		  and find it that way.
	 * @return
	 */
	@GET
	@Path("/SetCoverPhotoForTrail/{TrailId}/{ImageId}")
	public String SetCoverPhotoForTrailByGivingItThePhotoId(@PathParam("TrailId") String TrailId,
															@PathParam("ImageId") String ImageId) {
		Trail trail = new Trail();
		trail.SetCoverPhoto(TrailId, ImageId);
		return "Success";
	}
	
	// Add a view to a trail.
	@GET
	@Path("/AddTrailView/{TrailId}") 
	public String AddTrailView(@PathParam("TrailId") String TrailId) {
		Trail trail = new Trail();
		trail.AddViewForATrail(TrailId);
		return "Success";
	}
	
	//Get the number of views for a trail
	@GET
	@Path("/GetTrailViews/{TrailId}")
	public String GetTrailViews(@PathParam("TrailId") String TrailId) {		
		Trail trail = new Trail();
		return trail.GetNumberOfViewsForATrail(TrailId);
	}
	
	@GET
	@Path("/UserLikesTrail/{UserId}/{TrailId}") 
	public String UserLikesTrail(@PathParam("UserId") String UserId,
									@PathParam("TrailId") String TrailId) {
		
		Trail trail = new Trail();
		trail.AddLike(UserId, TrailId);
		return "Success";
	}
	
	@GET
	@Path("/GetLikesForTrail/{TrailId}") 
	public String GetNumberOfLikesForATrail(@PathParam("TrailId") String TrailId) {
		Trail trail = new Trail();
		return trail.GetNumberOfLikesForAnEntity(TrailId);
	}
	
	@GET
	@Path("/GetNumberOfCrumbsForATrail/{TrailId}")
	public String GetNumberOfCrumbsForATrail(@PathParam("TrailId") String TrailId) {
		Trail trail = new Trail();
		return trail.GetNumberOfCrumbsForATrail(TrailId);
	}
	
	@GET
	@Path("/GetNumberOfTrailsAUserOwns/{UserId}") 
	public String GetNumberOfTrailsAUserOwns(@PathParam("UserId") String UserId) {
		UserService user = new UserService();
		return user.GetNumberOfTrailsAUserOwns(UserId);
	}
	
	@GET
	@Path("GetAllSavedCrumbIdsForATrail/{TrailId}")
	public String GetAllCrumbIdsForAUser(@PathParam("TrailId") String TrailId) {
		UserService userService = new UserService();
		Trail trailService = new Trail();
		return trailService.GetAllCrumbIdsForATrail(TrailId);
	}
	
	/*
	 * This needs to be all trail ids for a user
	 */
	@GET
	@Path("GetAllTrailIds") 
	public String GetAllTrailIds() {
		Trail trail = new Trail();
		return trail.GetAllTrailIds();
	}
	
	@GET 
	@Path("GetBaseDetailsForATrail/{TrailId}")
	public String GetBaseDetailsForATrail(@PathParam("TrailId") String TrailId) {
		// Fetch each individual detail for the trail
		Trail trail = new Trail();
		return trail.GetSimpleDetailsForATrail(TrailId);
	}
	
	@GET
	@Path("DeleteAllTrails") 
	public void DeleteAllTrails() {
		//USE WITH CAUTION _ THIS SHOULD BE REMOVED BEFORE A PROPER RELEASE
	}
		
	
}
