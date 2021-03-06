package com.teamunemployment.breadcrumbs.database.Models;

import android.support.annotation.Nullable;

import com.teamunemployment.breadcrumbs.database.DatabaseController;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by jek40 on 18/05/2016.
 */
public class TrailSummaryModel {

    private JSONObject summaryJSON;
    public TrailSummaryModel(JSONObject jsonObject) {
        this.summaryJSON = jsonObject;
    }

    /**
     * Get the duration for a locally stored trail.
     * @return The number of days since the start of this trail.
     */
    public int GetDaysDuration() {
        String datatimeString = "";
        if (summaryJSON == null) {
            return 0;
        }
        if (summaryJSON.has("StartDate")) {
            try {
                datatimeString = summaryJSON.getString("StartDate");

                return 0;
            } catch (JSONException e) {
                // Should never get here because of if check.
                e.printStackTrace();
            }
        }

        // Going to have to deal with this.
        return 0;
    }


    /**
     * Get the cover photo for a trail (from local database)
     * @return the id of the cover photo.
     */
    @Nullable
    public String GetCoverPhoto() {
        // IF set, return the set value
        if (summaryJSON.has("CoverPhotoId")) {
            try {
                return summaryJSON.getString("CoverPhotoId");
            } catch (JSONException e) {
                // I am pretty sure that we can never reach here, because of the check above.
                e.printStackTrace();
            }
        }
        // If we have nothing, then we need to go about dealing with this problem some other way.
        return null;
    }

    /**
     * Get the name of a trip from local database.
     * @return The trip name, or a default name if not yet set.
     */
    public String GetTripName() {
        if (summaryJSON.has("TrailName")) {
            try {
                return summaryJSON.getString("TrailName");
            } catch (JSONException ex) {
                // Should never get here.
                ex.printStackTrace();
            }
        }

        return null;
    }

    public Date GetLastUpdate() {
        if (summaryJSON.has("LastUpdate")) {
            try {
                String lastUpdate = summaryJSON.getString("LastUpdate");
                Date dateTime = new Date(lastUpdate);
                return dateTime;
            } catch (JSONException e) {
                // Should never get here because of if check.
                e.printStackTrace();
            }
        }
        return null;
    }
}
