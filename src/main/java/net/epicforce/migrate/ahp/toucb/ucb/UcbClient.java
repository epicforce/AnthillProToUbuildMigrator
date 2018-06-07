package net.epicforce.migrate.ahp.toucb.ucb;

/*
 * UcbClient.java
 *
 * So the UCB client is almost useless; it just randomly doesn't have
 * functionality that is critical to the migration process.
 *
 * The underlying REST API actually supports everything, though, so
 * this client provides a nice low-ish level access.  It still relies
 * on the UCB client's internals to work, but it uses the low level
 * calls rather than the messed up wrappers.
 *
 * You can use both this library and the UCB one, but note that the
 * objects produced by this library aren't going to directly work with
 * UCB's objects and vise versa.  UCB does have a translation layer,
 * but you risk things being lost in translation by using it.
 *
 * This interface has no training wheels and is subject to breakage
 * based on the whims of the UCB developers.  Though, honestly, there
 * are plenty of things in this app which will be kind of sensitive
 * to UCB changes so its a little late to care about that :)
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.urbancode.commons.util.IO;
import com.urbancode.ubuild.client.rest.HttpHelper;
import com.urbancode.ubuild.client.rest.Login;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import net.epicforce.migrate.ahp.exception.MigrateException;


public class UcbClient extends HttpHelper
{
    /*
     * This URL should probably come from the Urls class, though it
     * doesn't have to be.  The UCB server will be automatically
     * prepended so this is just the path part of the URI.
     */
    protected String url;

    /**
     * Initialize a UcbClient for a certain URL.
     *
     * @param url           URL fragment (path portion), use Urls constants
     */
    public UcbClient(final String url)
    {
        this.url = url;
    }

    /**
     * Change URL.  This is sometimes needed for multi-step processes.
     *
     * @param url    New URL
     */
    public void setUrl(final String url)
    {
        this.url = url;
    }

    /**
     * Get artifact set by ID
     *
     * @param id  the string to load.  Sometimes this works by name as well.
     * @returns a JSONObject
     */
    public JSONObject getById(String id)
           throws UcbException
    {
        return sendGetReturnObject(id);
    }

    /**
     * Get all
     */
    public JSONObject[] getAll()
           throws UcbException
    {
        return sendGetReturnArray(null);
    }

    /**
     * Run an update
     */
    public void update(JSONObject obj)
           throws UcbException
    {
        try {
            sendPut(url, obj.getString("id"), obj.toString());
        } catch(Exception e) {
            try {
                throw new UcbException("Failed to send put " + obj.toString() +
                                       " (id:" + obj.getString("id") + ") " +
                                       "to UCB url " + url, e
                );
            } catch(JSONException sube) {
                throw new UcbException("The object you provided to update has "
                                       + "no id field.", sube
                );
            }
        }
    }

    /**
     * Run a delete
     */
    public void delete(String id)
           throws UcbException
    {
        try {
            sendDelete(url, id);
        } catch(Exception e) {
            throw new UcbException("Failed to send delete " + id +
                                   "to UCB url " + url, e
            );
        }
    }

    /**
     * This method posts a Map of key-values into a standard,
     * run-of-the-mill post.  Not posting JSON, just posting form
     * data.
     *
     * This method post a List of NameValuePair's, which is how the
     * underlying interface works.
     *
     * This is to support forms that use duplicate keys.  Unfortunately
     * I did not predict this use case and I started this implementation
     * with a Map<> style input.  Now I support both.
     *
     * @params posts        List of NameValues to post.
     * @return whatever the server gives us
     * @throws MigrateException on any failure.
     */
    public String sendRawPost(List<NameValuePair> nvpList)
           throws MigrateException
    {
        try {
            // Taken from the UCB HttpHelper
            Login login = Login.getCurrentLogin();
            String postUrl = UriBuilder.fromPath(login.getBaseUrl())
                                       .path(url)
                                       .build(new Object[0])
                                       .toString();

            HttpPost postMethod = new HttpPost(postUrl);

            try {
                postMethod.setHeader("Content-Type",
                                     "application/x-www-form-urlencoded"
                );

                // UCB has just enough CSRF protection to be annoying,
                // not enough to really be any use.
                postMethod.setHeader("Referer", postUrl);

                // Set them
                postMethod.setEntity(EntityBuilder.create()
                                                  .setParameters(nvpList)
                                                  .build()
                );

                // Use the client to ship it
                DefaultHttpClient client = getDefaultHttpClient();
                HttpResponse response = client.execute(postMethod);

                // There is an inexplicably private 'isGoodResponseCode'
                // method.  Fortunately, its easy to replicate
                int responseCode = response.getStatusLine().getStatusCode();;

                if((responseCode < 200) || (responseCode >= 300)) {
                    // Failed
                    throw new MigrateException(
                        "POST to " + postUrl + " failed with response: " +
                        responseCode
                    );
                }

                // Get the Content
                InputStream content = response.getEntity().getContent();
                return IO.readText(content, "UTF-8");
            } finally {
                postMethod.releaseConnection();
            }
        } catch(MigrateException e) {
            throw e;
        } catch(IOException e) {
            throw new MigrateException("General error reaching UCB", e);
        }
    }

    /**
     * This method posts a Map of key-values into a standard,
     * run-of-the-mill post.  Not posting JSON, just posting form
     * data.
     *
     * @param posts         Post map
     * @return whatever the web server gives us (content body wise)
     * @throws MigrateException on any failure
     */
    public String sendRawPost(Map<String, String> posts)
           throws MigrateException
    {
        // Set parameters
        List<NameValuePair> nvpList = new ArrayList<>(posts.size());

        for(Map.Entry<String, String> ent : posts.entrySet()) {
            nvpList.add(
                new BasicNameValuePair(ent.getKey(), ent.getValue())
            );
        }

        return sendRawPost(nvpList);
    }

    /*
     * The following are wrappers over HttpHelper's various methods
     * in order to make them return common JSON objects.  They all use
     * 'url' for the base URL.  Due to the number and simplicity,
     * I'm not going to document them all individually.
     */
    public JSONObject sendPostReturnObject(String postString)
           throws UcbException
    {
        try {
            return new JSONObject(sendPost(url, postString));
        } catch(Exception e) {
            throw new UcbException("Failed to send post " + postString +
                                   "to UCB url " + url, e
            );
        }
    }

    public JSONObject[] sendPostReturnArray(String postString)
           throws UcbException
    {
        try {
            return jsonArrayConvert(
                        new JSONArray(sendPost(url, postString))
            );
        } catch(Exception e) {
            throw new UcbException("Failed to send post " + postString +
                                   "to UCB url " + url, e
            );
        }
    }

    public JSONObject sendPutReturnObject(String id, String postString)
           throws UcbException
    {
        try {
            return new JSONObject(sendPut(url, id, postString));
        } catch(Exception e) {
            throw new UcbException("Failed to send put " + postString +
                                   " (id:" + id + ") " +
                                   "to UCB url " + url, e
            );
        }
    }

    public JSONObject[] sendPutReturnArray(String id, String postString)
           throws UcbException
    {
        try {
            return jsonArrayConvert(
                        new JSONArray(sendPut(url, id, postString))
            );
        } catch(Exception e) {
            throw new UcbException("Failed to send put " + postString +
                                   " (id:" + id + ") " +
                                   "to UCB url " + url, e
            );
        }
    }

    public JSONObject sendGetReturnObject(String id)
           throws UcbException
    {
        try {
            return new JSONObject(sendGet(url, id));
        } catch(Exception e) {
            throw new UcbException("Failed to send get " + id +
                                   "to UCB url " + url, e
            );
        }
    }

    public JSONObject[] sendGetReturnArray(String id)
           throws UcbException
    {
        try {
            return jsonArrayConvert(
                        new JSONArray(sendGet(url, id))
            );
        } catch(Exception e) {
            throw new UcbException("Failed to send get " + id +
                                   "to UCB url " + url, e
            );
        }
    }


    /**
     * Convert a JSONArray to an array of JSONObject
     *
     * @param       arr array to convert
     * @returns array of JSONObject
     */
    protected JSONObject[] jsonArrayConvert(JSONArray arr)
            throws UcbException
    {
        try {
            JSONObject[] ret = new JSONObject[arr.length()];

            for(int i = 0; i < arr.length(); i++) {
                ret[i] = arr.getJSONObject(i);
            }

            return ret;
        } catch(JSONException e) {
            throw new UcbException("Failed to convert JSON array", e);
        }
    }

    /**
     * Perform a GET request, passing the provided parameters.
     *
     * @param url           The URL fragment to grab.
     * @param params        GET parameters to use, or null if none.
     * @return the resulting page content
     * @throws MigrateException on any failure, likely transport related.
     */
    public String sendRawGet(final String url, Map<String, String> params)
           throws MigrateException
    {
        try {
            // Re-used logic from sendRawPost, but tweaked for GET structure
            Login login = Login.getCurrentLogin();
            UriBuilder builder =  UriBuilder.fromPath(login.getBaseUrl())
                                            .path(url);

            if(params != null) {
                for(Map.Entry<String, String> ent : params.entrySet()) {
                    builder.queryParam(ent.getKey(), ent.getValue());
                }
            }

            String getUrl = builder.build(new Object[0]).toString();

            HttpGet getMethod = new HttpGet(getUrl);

            try {
                getMethod.setHeader("Referer", getUrl); // CSRF
                DefaultHttpClient client = getDefaultHttpClient();
                HttpResponse response = client.execute(getMethod);

                int responseCode = response.getStatusLine().getStatusCode();

                if((responseCode < 200) || (responseCode >= 300)) {
                    // failed
                    throw new MigrateException(
                        "GET to " + getUrl + " failed with response " +
                        responseCode
                    );
                }

                // Get the Content
                InputStream content = response.getEntity().getContent();
                return IO.readText(content, "UTF-8");
            } finally {
                getMethod.releaseConnection();
            }
        } catch(IOException e) {
            throw new MigrateException("Error while communicating with UCB", e);
        }
    }

    /**
     * So UCB (lopsidedly -- i.e. not on every page) implements a "cid" which
     * is some kind of CSRF protection or session.  You'll see it in the URL
     * on some pages.
     *
     * It seems most of the time CID can be ignored, but sometimes it can't.
     * If you need a CID, you can use this method to scour a page for one.
     *
     * It will throw an exception if a CID cannot be found.  GET parameters
     * are pretty much always required for CID capture.
     *
     * @param url           URL fragment to grab
     * @parm params         GET parameters to use
     * @return a CID string
     * @throws MigrateException on any failure, including cannot find CID.
     *
     * NOTE: I'm not sure if CIDs can be use multi-threaded.  So, the
     *       cidLock object is public for synch purposes.
     */
    private static final Pattern cidPattern = Pattern.compile("cid=(\\d+)");
    public static final Object cidLock = new Object();
    public String getCIDFromGet(final String url, Map<String, String> params)
           throws MigrateException
    {
        // Find the CID
        Matcher cidMatch = cidPattern.matcher(sendRawGet(url, params));

        if(!cidMatch.find()) {
            throw new MigrateException(
                "Could not find CID in server response to: " + url
            );
        }

        return cidMatch.group(1);
    }

    /**
     * There are some things we can't get out of UCB's API at all.  These
     * things look like the underlying code was copy/pasted from Anthill
     * and they retain their "Web 1.0" trappings.
     *
     * In particular, I'm looking at Notification Schemes and their
     * buddies, event selector, etc.  None of them have any sort of API
     * access.
     *
     * This will harvest a map of names to ID, based on a given
     * pattern string.  Group 1 will be assumed to be the ID, and
     * Group 2 will be assumed to be the name.
     *
     * It will fetch from the URL selected by the constructor.
     *
     * @param pattern       Our pattern, should have 2 groups in it
     * @param params        Parameters to pass to the GET, may be null.
     * @return map of String to String
     * @throws MigrateException on any failure.
     */
    public Map<String, String> harvestPattern(final String pattern,
                                              Map<String, String> params)
           throws MigrateException
    {
        Pattern toHarvest = Pattern.compile(pattern);
        Map<String, String> ret = new HashMap<>();

        Matcher matches = toHarvest.matcher(sendRawGet(this.url, params));

        while(matches.find()) {
            ret.put(matches.group(2), matches.group(1));
        }

        return ret;
    }
}
