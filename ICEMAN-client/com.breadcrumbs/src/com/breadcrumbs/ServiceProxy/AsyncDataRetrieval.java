package com.breadcrumbs.ServiceProxy;

import android.os.AsyncTask;

import com.breadcrumbs.client.FragmentMaster;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class AsyncDataRetrieval extends AsyncTask<String, Integer, String> {
		private JSONObject json = new JSONObject();
		private String url;
        // jsonResult was losing a variable some how, so im testing with this to see what happens.
        private String localResponse = null;
		
		// This is the interface for the callback method to use.
		public interface RequestListener {
			public void onFinished(String result);
		}

		// Our callback instance
		private RequestListener requestListener;
		
		// Pass in the callback and the url to our constructor.
		public AsyncDataRetrieval(String url, RequestListener requestListener){
			this.url = url;
			this.requestListener = requestListener;
		}
		
		@Override
		protected String doInBackground(String... urls) {
			return SendDataRequest(url);
		}
		
		@Override
	    protected void onPostExecute(String jsonResult) {
			if (requestListener != null) {
				requestListener.onFinished(this.localResponse);
			}
	    }
		
		/* 
		 * This is our method we use to retrieve data using get (or store data.)
		 * Remember - GET is pretty shit, I need to do more research
		 */
		public String SendDataRequest(String url) {
			HttpRequestBase request = null;
			DefaultHttpClient httpClient = new DefaultHttpClient();
			 String stringResponse ="";
			try {
				 HttpGet httpGet = new HttpGet(url);
				 HttpResponse httpResponse = httpClient.execute(httpGet);
		    	    //No fucking clue what this magic does
				 HttpEntity httpEntity = httpResponse.getEntity();
		    	  stringResponse = EntityUtils.toString(httpEntity);
		    	  localResponse = stringResponse;
		    	    //Turn our JSON string into actual JSON
			} catch (IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
                e.printStackTrace();
            }
			return stringResponse;
		}

        public String PostDataToServer(String urlString) {
            URL url = null;
            try {
                url = new URL(urlString);
                URLConnection connection = url.openConnection();
                //connection.
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return"";
        }
	}
	
         

      

