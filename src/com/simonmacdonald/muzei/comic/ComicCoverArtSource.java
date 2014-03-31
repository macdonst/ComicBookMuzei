/*
 * Copyright (c) 2014 Simon MacDonald
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.simonmacdonald.muzei.comic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.simonmacdonald.muzei.comic.ComicCoverService.Cover;
import com.simonmacdonald.muzei.comic.comicvine.Comic;
import com.simonmacdonald.muzei.comic.comicvine.Volume;
import com.squareup.okhttp.OkHttpClient;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RestAdapter.LogLevel;
import retrofit.RetrofitError;

public class ComicCoverArtSource extends RemoteMuzeiArtSource {
    private static final String BY_LINE = "Data provided by ComicVine. © 2014 ComicVine";
	private static final String TAG = "ComicCoverArtSource";
    private static final String SOURCE_NAME = "ComicCoverArtSource";
    private static final int MAX_ID = 399735;

    public ComicCoverArtSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        if (Utils.isDownloadOnlyOnWifi(this) && !Utils.isWifiConnected(this)) {
            scheduleUpdate(System.currentTimeMillis() + Utils.getRefreshRate(this));
            return;
    	}
    	
        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;
        
        final Context that = this;
//        RestAdapter restAdapter = new RestAdapter.Builder()
//                .setEndpoint("https://mighty-harbor-8598.herokuapp.com/")
//                .setLogLevel(LogLevel.FULL)
//                .setRequestInterceptor(new RequestInterceptor() {
//                    @Override
//                    public void intercept(RequestFacade request) {
//                        request.addQueryParam("consumer_key", Config.CONSUMER_KEY);
//                    }
//                })
//                .setErrorHandler(new ErrorHandler() {
//                    @Override
//                    public Throwable handleError(RetrofitError retrofitError) {
//                    	if (retrofitError != null && retrofitError.getResponse() != null) {
//                            int statusCode = retrofitError.getResponse().getStatus();
//                            if (retrofitError.isNetworkError()
//                                    || (500 <= statusCode && statusCode < 600)) {
//                                return new RetryException();
//                            }
//                    	}
//                        scheduleUpdate(System.currentTimeMillis() + Utils.getRefreshRate(that));
//                        return retrofitError;
//                    }
//                })
//                .build();
//
//        ComicCoverService service = restAdapter.create(ComicCoverService.class);
//        Cover response = service.getRandomCover();

        Volume volume = fetchVolume(796);
        
        Log.d(TAG, "firstIssue = " + volume.getFirstIssue());
        Log.d(TAG, "lastIssue = " + volume.getLastIssue());
        
        Comic comic = fetchComic(796, volume.getFirstIssue(), volume.getLastIssue());
                
		if (!isValidImage(comic.getImageUrl())) {
			throw new RetryException();
		}
        
        String token;
        if (currentToken == null) {
        	token = "1";
        } else {
        	token = "" + (Integer.valueOf(currentToken) + 1) ;
        }

        Log.d(TAG, "title = " + comic.getTitle());
//        Log.d(TAG, "author = " + author);
        Log.d(TAG, "imageUrl = " + comic.getImageUrl());
        Log.d(TAG, "detailsUrl = " + comic.getDetailsUrl());
        
        publishArtwork(new Artwork.Builder()
                .title(comic.getTitle())
                .byline(BY_LINE)
                .imageUri(Uri.parse(comic.getImageUrl()))
                .token(token)
                .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(comic.getDetailsUrl())))
                .build());
        
        scheduleUpdate(System.currentTimeMillis() + Utils.getRefreshRate(this));
    }

	private boolean isValidImage(String imageUrl) throws RetryException {
		OkHttpClient client = new OkHttpClient();
		try {
			URL url = new URL(imageUrl);
	        HttpURLConnection connection = client.open(url);
	        String type = connection.getContentType();
	        Log.d(TAG, "Content-Type = " + type);
	        return type.startsWith("image");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return false;
	}
    
    private Volume fetchVolume(int id) {
        String strUrl = "http://www.comicvine.com/api/volume/4050-" + id + 
        	"/?api_key=" + API.COMICVINE_KEY + "&format=json&field_list=first_issue,last_issue,name";
        JsonObject obj =  makeComicVineApiRequest(strUrl);
        int firstIssue = obj.getAsJsonObject("results").getAsJsonObject()
        		.getAsJsonObject("first_issue").getAsJsonObject()
        		.getAsJsonPrimitive("issue_number").getAsInt();
        int lastIssue = obj.getAsJsonObject("results").getAsJsonObject()
        		.getAsJsonObject("last_issue").getAsJsonObject()
        		.getAsJsonPrimitive("issue_number").getAsInt();
        return new Volume(id, firstIssue, lastIssue);
    }

	private Comic fetchComic(int volumeId, int firstIssue, int lastIssue) {
        Random random = new Random();
        String strUrl = "http://www.comicvine.com/api/issues/?api_key=85f9c7961d5d6140087c408e510a797438103616&format=json&" + 
        		"filter=issue_number:" + random.nextInt(lastIssue) + ",volume:" + volumeId; 
        JsonObject obj =  makeComicVineApiRequest(strUrl);

        JsonObject volume2 = obj.getAsJsonArray("results").getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("volume").getAsJsonObject();
        JsonObject image = obj.getAsJsonArray("results").getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("image").getAsJsonObject();
        
        String title = volume2.get("name").getAsString() + " #" + 
        		obj.getAsJsonArray("results").getAsJsonArray().get(0).getAsJsonObject().get("issue_number").getAsString();
        String imageUrl = image.get("super_url").getAsString();
        String detailsUrl = volume2.get("site_detail_url").getAsString();

        return new Comic(title, imageUrl, detailsUrl);
	}

	private JsonObject makeComicVineApiRequest(String strUrl) {
		OkHttpClient client = new OkHttpClient();
		URL url;
        String json = "";
		try {
			url = new URL(strUrl);
	        HttpURLConnection connection = client.open(url);
	        InputStream in = null;
	        try {
	        	// Read the response.
	        	in = connection.getInputStream();
	        	byte[] response = readFully(in);
	        	json =  new String(response, "UTF-8");
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
	        	if (in != null)
					try {
						in.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	        }
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        Gson gson = new Gson();
		return gson.fromJson(json, JsonObject.class);
	}

    byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int count; (count = in.read(buffer)) != -1; ) {
          out.write(buffer, 0, count);
        }
        return out.toByteArray();
      }
}

