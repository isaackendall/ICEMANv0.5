package com.teamunemployment.breadcrumbs.client.Maps;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.ui.IconGenerator;
import com.google.repacked.kotlin.jvm.internal.DoubleCompanionObject;
import com.teamunemployment.breadcrumbs.ActivityRecognition.ActivityController;
import com.teamunemployment.breadcrumbs.AsyncWorkers.GenericAsyncWorker;
import com.teamunemployment.breadcrumbs.BackgroundServices.UploadTrailService;
import com.teamunemployment.breadcrumbs.BreadcrumbsActivityAPI;
import com.teamunemployment.breadcrumbs.BreadcrumbsLocationAPI;
import com.teamunemployment.breadcrumbs.Dialogs.IDialogCallback;
import com.teamunemployment.breadcrumbs.Dialogs.SimpleMaterialDesignDialog;
import com.teamunemployment.breadcrumbs.Location.SimpleGps;
import com.teamunemployment.breadcrumbs.Models;
import com.teamunemployment.breadcrumbs.Network.LoadBalancer;
import com.teamunemployment.breadcrumbs.Network.NetworkConnectivityManager;
import com.teamunemployment.breadcrumbs.Network.ServiceProxy.AsyncDataRetrieval;
import com.teamunemployment.breadcrumbs.Network.ServiceProxy.HTTPRequestHandler;
import com.teamunemployment.breadcrumbs.PreferencesAPI;
import com.teamunemployment.breadcrumbs.R;
import com.teamunemployment.breadcrumbs.RandomUsefulShit.Utils;
import com.teamunemployment.breadcrumbs.caching.TextCaching;
import com.teamunemployment.breadcrumbs.client.Animations.SimpleAnimations;
import com.teamunemployment.breadcrumbs.client.Cards.CrumbCardDataObject;
import com.teamunemployment.breadcrumbs.client.ImageChooser.TrailCoverImageSelector;
import com.teamunemployment.breadcrumbs.data.TripPath;
import com.teamunemployment.breadcrumbs.data.source.SyncManager;
import com.teamunemployment.breadcrumbs.data.source.TripDataSource;
import com.teamunemployment.breadcrumbs.database.DatabaseController;
import com.teamunemployment.breadcrumbs.database.Models.LocalTrailModel;
import com.teamunemployment.breadcrumbs.database.Models.TrailSummaryModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import mehdi.sakout.fancybuttons.FancyButton;

/**
 * Created by jek40 on 22/05/2016.
 */
public class LocalMap extends MapViewer {
    private PreferencesAPI preferencesAPI;
    private String localTrailId;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private BreadcrumbsLocationAPI locationAPI;
    private BreadcrumbsActivityAPI activityAPI;
    private LatLng lastPoint;

    public final static int DELETED_CRUMBS = 4435;

    @Override
    public void DoBottomSheetClick() {
        switch (BOTTOM_SHEET_STATE) {
            case EDIT_MODE:
                setUpReadOnlyMode();
                break;
            case READ_ONLY_MODE:
                setUpEditMode();
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (preferencesAPI == null) {
            preferencesAPI = new PreferencesAPI(context);
        }
        drawLocalTrailOnMap();
        String coverId = preferencesAPI.GetCurrentTrailCoverPhoto();
        setListenerForTrackingToggle();
        ZoomOnMyLocation();
        if (coverId != null) {
            setCoverPhoto(coverId);
        }
    }

    private void setListenerForTrackingToggle() {
        SwitchCompat switchCompat = (SwitchCompat) findViewById(R.id.toggle_tracking);
        if (preferencesAPI == null) {
            preferencesAPI = new PreferencesAPI(context);
        }
        switchCompat.setChecked(preferencesAPI.isTrackingEnabledByUser());
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Start listening
                    startTracking();
                }
                if (!isChecked) {
                    stopTracking();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void SetUpDetailsItems() {
        findViewById(R.id.settings_my_trail).setVisibility(View.VISIBLE);
        findViewById(R.id.toggle_tracking).setVisibility(View.VISIBLE);
        findViewById(R.id.publish_trail).setVisibility(View.VISIBLE);
        findViewById(R.id.view_count).setVisibility(View.GONE);
        findViewById(R.id.number_of_views).setVisibility(View.GONE);

        setPublishClickListener();
    }

    private void setPublishClickListener() {
        FancyButton fancyButton = (FancyButton) findViewById(R.id.publish_trail);
        fancyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPublish();
            }
        });
    }

    private void startPublishingNotification() {
        int id = 8;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setContentTitle("Upload Trip").setContentText("Upload in progress").setSmallIcon(R.drawable.ic_backup_white_24dp);
        notificationBuilder.setProgress(0,0, true);
        notificationManager.notify(id, notificationBuilder.build());
    }

    private void doPublish() {
        // Create the shell to hold our simple dialog.
        final SimpleMaterialDesignDialog materialDesignDialog = SimpleMaterialDesignDialog.Build(context);
        if (preferencesAPI == null) {
           preferencesAPI = new PreferencesAPI(context);
        }
        // Callback for the action button on the dialog.
        IDialogCallback onAccept = new IDialogCallback() {
            @Override
            public void DoCallback() {
                // First we need to build our notification.
                // Send save Request to server
                int serverTrailId = preferencesAPI.GetServerTrailId();
                if (serverTrailId == -1) {
                    boolean result = handleFirstTimePublishing();
                    if (result) {
                        //toggleButton(publishFab);
                    }
                } else {
                    boolean result = publishTrail(Integer.toString(serverTrailId));
                    if (result) {
                        //toggleButton(publishFab);
                    }
                }
            databaseController.SetLastUpdate(Integer.toString(preferencesAPI.GetLocalTrailId()), Long.toString(System.currentTimeMillis()));
            //setLastUpdate(DateTime.now());
            }
        };

        // Make the dialog look and work the way we want
        materialDesignDialog.SetTitle("Publish Trip")
                .SetTextBody("This will publish your trip and make it visible to the world.")
                .SetActionWording("Publish")
                .SetCallBack(onAccept);

        // Show the dialog.
        materialDesignDialog.Show();
    }



    /**
     * Push the changes for a trail to the server
     * @param trailId
     * @return The trail publish service was successfully launched.
     */
    private boolean publishTrail(String trailId) {
        String trailName = fetchCurrentTrailName();

        // This is the use case where the trail title has no data. This is an issue an we cannot save
        if (trailName.isEmpty()) {
            return false;
        }

        int serverTrailId = preferencesAPI.GetServerTrailId();
        if (serverTrailId == -1) {
            // Issue, it should have this at this point
            return false;
        }

        startPublishingNotification();
        // Save our name locally for future reference.
        preferencesAPI.SaveTrailNameString(trailName);

        // Save the trail name to the server too.
        HTTPRequestHandler httpRequestHandler = new HTTPRequestHandler();
        httpRequestHandler.SaveNodeProperty(Integer.toString(serverTrailId), "TrailName", trailName, context);

        Intent uploadTrailService = new Intent(context, UploadTrailService.class);
        context.startService(uploadTrailService);

        return true;
    }

    /**
     * Handle the use case where we have not saved our trail yet. This involves creating the trail
     * and saving the Id that gets returned.
     */
    private boolean handleFirstTimePublishing() {
        String trailName = fetchCurrentTrailName();

        // This is the use case where the trail title has no data. This is an issue an we cannot save
        if (trailName.isEmpty()) {
            return false;
        }
        startPublishingNotification();
        String userId = preferencesAPI.GetUserId();

        String url = MessageFormat.format("{0}/rest/login/saveTrail/{1}/{2}/{3}",
                LoadBalancer.RequestServerAddress(),
                trailName,
                " ",
                userId);

        // Save
        Log.d("UPLOAD", "Attempting to create a new Trail with url: " + url);
        url = url.replaceAll(" ", "%20");

        AsyncDataRetrieval asyncDataRetrieval = new AsyncDataRetrieval(url, new AsyncDataRetrieval.RequestListener() {
            @Override
            public void onFinished(String result) throws JSONException {
                //result is the id of the trail we just saved.
                preferencesAPI.SaveCurrentServerTrailId(Integer.parseInt(result));
                publishTrail(result);

            }
        }, this);
        asyncDataRetrieval.execute();
        return true;
    }

    private JSONObject fetchCurrentLocationNode() {
        SimpleGps simpleGps = new SimpleGps(context);
        Location location = simpleGps.GetInstantLocation();

        if (location != null) {
            JSONObject response = new JSONObject();

            try {
                response.put(Models.Crumb.LATITUDE, location.getLatitude());
                response.put(Models.Crumb.LONGITUDE, location.getLongitude());
                response.put("LastActivity", Integer.toString(0));
                response.put("CurrentActivity", Integer.toString(0));
                response.put("Granularity", Integer.toString(0));
                return response;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void initDatabaseController() {
        if (databaseController == null) {
            databaseController = new DatabaseController(context);
        }
    }

    private void doLocalLoad() {
        // Check if we have a database controller - if not this method creates it.
        initDatabaseController();

        // We will use this multiple times - so get it now as a member variable.
        trailId = Integer.toString(preferencesAPI.GetLocalTrailId());

        // Build trail path from our database
        buildTrailPathFromLocalDB();

        // Grab the images and show them.
        displayCrumbsFromLocal();

        // The user is looking at their own trail, which should draw to where they are, so we zoom onto them.
        //zoomOnMyLocation();
    }

    private void addMeMarker() {
        SimpleGps simpleGps = new SimpleGps(context);
        Location location = simpleGps.GetInstantLocation();
        if (location == null) {
            return;
        }
        IconGenerator iconFactory = new IconGenerator(context);
        iconFactory.setColor(Color.CYAN);
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon("You"))).
                position(position).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

        mMap.addMarker(markerOptions);
    }

    private void buildTrailPathFromLocalDB() {
        List<LatLng> list = new ArrayList<>();
        // Fetch from repository (Pretend now, but we will fetch from the repository later).
        JSONObject metadata = databaseController.fetchMetadataFromDB(trailId, false);

        // Do draw with map tasks.
        Iterator<String> metadataKeys = metadata.keys();
        while (metadataKeys.hasNext()) {
            String next = metadataKeys.next();
            try {
                JSONObject node = metadata.getJSONObject(next);
                String latitude = node.getString("latitude");
                String longitude = node.getString("longitude");
                LatLng point = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                list.add(point);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
        // Need to load local trails here too.
       // DrawPolyline(list);
    }

    private void displayCrumbsFromLocal() {
       // JSONObject crumbsJson = databaseController.GetAllCrumbs(trailId);

        //DisplayCrumbsFromLocalDatabase(crumbsJson);
    }

    public void DisplayCrumbsFromLocalDatabase(JSONObject crumbs) {
        // This creates the async request with a callback method of what I want completed when the
        try {
            // Get the crumb title ??? why do i do this? last crumb?
            Iterator<String> keys = crumbs.keys();
            JSONObject next = null;

            // Create a trail.
            mapDisplayManager.clusterManager.clearItems();
            mapDisplayManager.GetDataObjects().clear();
            int counter = 0;
            while (keys.hasNext()) {
                // The next node to get data from and Draw.
                String id = Integer.toString(counter);
                next = crumbs.getJSONObject(id);

                mapDisplayManager.DrawLocalCrumbFromJson(next);
                mapDisplayManager.clusterManager.cluster();
                counter += 1;
            }
            // Now that we are done, we want to set the focus to the last crumb added

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            Log.e("BC.MAP", "A JsonException was thrown during the display process for a crumb." +
                    "This probably means that you are missing a field on your json. Stacktrace follows");
            e.printStackTrace();
        }
        Log.i("MAP", "Finished Loading crumbs and displaying them on the map");
    }

    private String fetchCurrentTrailName() {
        EditText trailTitle = (EditText) findViewById(R.id.trail_title_input);
        Editable currentTitleEditable = trailTitle.getText();
        if (currentTitleEditable == null) {
            trailTitle.setError("Trip title must not be blank.");
            return "";
        }

        if (currentTitleEditable.toString().isEmpty()) {
            trailTitle.setError("Trip title must not be blank.");
            return "";
        }

        return currentTitleEditable.toString();
    }

    /**
     *	Define how the bottom sheet fab works in edit mode.
     */
    private void setUpEditMode() {
        BOTTOM_SHEET_STATE = EDIT_MODE;

        Activity act = (Activity) context;
        EditText trailName = (EditText) act.findViewById(R.id.trail_title_input);
        trailName.setEnabled(true);

        TextView tellThemToSelectACoverPhoto = (TextView) act.findViewById(R.id.cover_photo_prompt);
        SimpleAnimations.FadeInView(tellThemToSelectACoverPhoto);
        tellThemToSelectACoverPhoto.setOnClickListener(selectACoverPhoto());
        bottomSheetFab.setBackgroundTintList(ColorStateList.valueOf((context.getResources().getColor(R.color.good_to_go))));
        bottomSheetFab.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_accept));
    }

    private View.OnClickListener selectACoverPhoto() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               launchCoverPhotoSelector();
            }
        };
    }

    /**
     * Set read only mode.
     */
    private void setUpReadOnlyMode() {
        BOTTOM_SHEET_STATE = READ_ONLY_MODE;

        Activity act = (Activity) context;
        EditText trailName = (EditText) act.findViewById(R.id.trail_title_input);
        trailName.setEnabled(false);


        TextView tellThemToSelectACoverPhoto = (TextView) act.findViewById(R.id.cover_photo_prompt);
        SimpleAnimations.FadeOutView(tellThemToSelectACoverPhoto);

        // Save trailName and trailId.
        Editable trailNameEditable = trailName.getText();
        if (trailNameEditable != null) {
            saveTrailName(trailNameEditable.toString());

            TextView trailTitle = (TextView) findViewById(R.id.bottom_sheet_trail_title);
            trailTitle.setText(trailNameEditable.toString());
        }

        // Set the trail
        bottomSheetFab.setBackgroundTintList(ColorStateList.valueOf((context.getResources().getColor(R.color.white))));
        bottomSheetFab.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_edit));
    }

    // save our trail name.
    private void saveTrailName(String trailName) {
        databaseController.SaveTrailName(trailName, preferencesAPI.GetLocalTrailId());
    }

    @Override
    public void SetBaseDetailsForATrail(String trailId) {
        // Load details
        if (databaseController == null) {
            databaseController = new DatabaseController(context);
        }

        if (preferencesAPI == null) {
            preferencesAPI = new PreferencesAPI(context);
        }
        if (trailId == null) {
            trailId = Integer.toString(preferencesAPI.GetLocalTrailId());
        }
        setCoverPhoto(preferencesAPI.GetCurrentTrailCoverPhoto());

        TrailSummaryModel trailSummaryModel = new TrailSummaryModel(databaseController.GetTrailSummary(trailId));
        LocalTrailModel localTrailModel = new LocalTrailModel(databaseController.GetAllCrumbs(Integer.toString(preferencesAPI.GetLocalTrailId())));

        TextView days = (TextView) bottomSheet.findViewById(R.id.duration_details);
        days.setText(trailSummaryModel.GetDaysDuration() + " Days");
//        TextView pois = (TextView) bottomSheet.findViewById(R.id.number_of_crumbs_details);
//        pois.setText(localTrailModel.getNumberOfPOI() + " Points of Interest");

        TextView videos = (TextView) bottomSheet.findViewById(R.id.number_of_videos_details);
        videos.setText(localTrailModel.getNumberOfVideos() + " Videos");

        TextView photosTextView = (TextView) bottomSheet.findViewById(R.id.number_of_photos_details);
        photosTextView.setText(localTrailModel.getNumberOfPhotos() + " Photos");

        EditText trailName = (EditText) bottomSheet.findViewById(R.id.trail_title_input);
        trailName.setText(trailSummaryModel.GetTripName());

        TextView trailTitle = (TextView) findViewById(R.id.bottom_sheet_trail_title);
        String name = trailSummaryModel.GetTripName();
        if (name == null || name.isEmpty()) {
            name = "...";
        }
        trailTitle.setText(name);
    }

    private void drawLocalTrailOnMap() {
        if (databaseController == null) {
            databaseController = new DatabaseController(context);
        }

        if (preferencesAPI == null) {
            preferencesAPI = new PreferencesAPI(context);
        }

        TextCaching textCaching = new TextCaching(context);
        String index = textCaching.FetchCachedText("TrailActivityIndex");
        if (index == null) {
            index = "0";
        }
        SyncManager syncManager = new SyncManager();
        syncManager.Sync(getLoadTripPathCallback(), preferencesAPI.GetLocalTrailId(),databaseController, Integer.parseInt(index));
    }

    private TripDataSource.LoadTripPathCallback getLoadTripPathCallback() {
        return new TripDataSource.LoadTripPathCallback() {
            @Override
            public void onTripPathLoaded(TripPath tripPath) {
                DrawPath(tripPath);
            }
        };
    }

    @Override
    public void StartCrumbDisplay(String trailId) {
        // Set up our crumbs

        boolean amConnected = NetworkConnectivityManager.IsNetworkAvailable(context);

        if (amConnected) {
            //processServerSide()
        }
        final String localTrailId = Integer.toString(preferencesAPI.GetLocalTrailId());
        GenericAsyncWorker.IGenericAsync overrides = new GenericAsyncWorker.IGenericAsync() {
            @Override
            public JSONObject backgroundTasks() {
                return databaseController.GetAllCrumbs(localTrailId);
            }

            @Override
            public void postExecute(JSONObject jsonObject) {
                Log.d(TAG, "Starting display crumbs from local. Time = " + System.currentTimeMillis());
                DisplayCrumbsFromLocalDatabase(jsonObject);
                Log.d(TAG, "Finished display crumbs from local. Time = " + System.currentTimeMillis());
            }
        };
        GenericAsyncWorker genericAsyncWorker = new GenericAsyncWorker(overrides);
        genericAsyncWorker.execute();
    }

    @Override
    public void GetAndDisplayTrailOnMap(String trailId) {
        // Get and display our trail
    }

    @Override
    public void SetUpBottomSheetFab() {
        bottomSheetFab.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        bottomSheetFab.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_edit));

        bottomSheetFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DoBottomSheetClick();
            }
        });
    }

    /**
     * Launch a grid view from which we can select the
     */
    private void launchCoverPhotoSelector() {
        Intent selectCoverPhotoIntent = new Intent(context, TrailCoverImageSelector.class);
        startActivityForResult(selectCoverPhotoIntent, 155);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 155) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Recieved result for saving cover photo.");

                // The user picked an image. We need to set this image.
                String id = data.getStringExtra("Id");
                setCoverPhoto(id);
                setUpReadOnlyMode();
            }
        }

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                // We have deleted some crumbs, so we need to remove them from our list of data objects and refresh the map.
                ArrayList<CrumbCardDataObject> deleted = data.getParcelableArrayListExtra("DeletedCrumbs");

                deleteAndRefresh(deleted);

            }
        }
    }

    private void deleteAndRefresh(ArrayList<CrumbCardDataObject> deletedItems) {
        trailId = Integer.toString(preferencesAPI.GetLocalTrailId());
        JSONObject crumbs = databaseController.GetAllCrumbs(trailId);
        DisplayCrumbsFromLocalDatabase(crumbs);
    }

    private void setCoverPhoto(String id) {
        if (id == null) {
            return;
        }
        ImageView coverPhoto = (ImageView) findViewById(R.id.trail_cover_photo);
        if (id.endsWith("L")) {
            id =  id.substring(0, id.length()-1);
            Glide.with(context).load(Utils.FetchLocalPathToImageFile(id)).centerCrop().placeholder(Color.GRAY).crossFade().into(coverPhoto);
        } else {
            Glide.with(context).load(LoadBalancer.RequestCurrentDataAddress() + "/images/"+id+".jpg").centerCrop().placeholder(Color.GRAY).crossFade().into(coverPhoto);
        }
    }

    private void ZoomOnMyLocation() {
        if (mMap != null) {
            FetchMyLocation();
        }
    }

    @Override
    public void setCollapsedToolbarState() {
        super.setCollapsedToolbarState();
        ZoomOnMyLocation();
    }

    // Stop the activity listener that toggles the tracking
    private void stopTracking() {
        preferencesAPI.SetUserTracking(false);
        ActivityController activityController = new ActivityController(context);
        activityController.StopListening();
    }

    // Start the activity listener that triggers the gps tracking.
    private void startTracking() {
        preferencesAPI.SetUserTracking(true);
        ActivityController activityController = new ActivityController(context);
        activityController.StartListenting();
    }


}
