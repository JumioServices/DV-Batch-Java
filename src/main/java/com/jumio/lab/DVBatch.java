package com.jumio.lab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Properties;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class DVBatch {
	
    private static final String LINE_FEED = "\r\n";

	private static final String PROPERTIES_FILE = "config.properties";
	
	private static final String API_SECRET_ = "secret=";
	private static final String API_TOKEN_ = "token=";

	private static final String USER_AGENT_TXT = "Jumio NV Test Tool/v1.0";
    private static final String API_SECRET = "secret";
    private static final String API_TOKEN = "token";
	private static final String PATH_TO_FOLDER = "pathToFolder";
	private static final String SERVER_URL = "serverUrl";
	private static final String MERCHANT_SCAN_REFERENCE = "merchantScanReference";
	private static final String CUSTOMER_ID = "customerId";
	private static final String TYPE = "type";
	private static final String COUNTRY = "country";
	private static final String ENABLE_EXTRACTION = "enableExtraction";
	private static final String SCAN_REFERENCE = "scanReference";
	private static final String TIME_STAMP = "timestamp";
	private static final String NUMBER_TO_SUBMIT = "numberToSubmit";
	private static final String FRONTSIDE_IMAGE = "frontsideImage";
	
    private static String serverUrl;
    private static String merchantScanReference;
    private static String auth;
	
	public static void main(String[] args) {
        FileInputStream inputStream = null;
		try {
            inputStream = new FileInputStream(PROPERTIES_FILE);			
			inputStream = new FileInputStream(PROPERTIES_FILE);			
        } catch(IOException ioexc){
			System.out.println(ioexc.getMessage());
            return;
		}

        Properties prop = new Properties();
		try {
            prop.load(inputStream);
        } catch(IOException ioexc){
			System.out.println(ioexc.getMessage());
            return;
		}
        
        String pathToFolder = prop.getProperty(PATH_TO_FOLDER);
        if (pathToFolder == null) {
            System.out.println("pathToFolder is missing in config.properties.");
            return;
        }

        serverUrl = prop.getProperty(SERVER_URL);
        if (serverUrl == null) {
            System.out.println("serverUrl is missing in config.properties.");
            return;
        }

        merchantScanReference = prop.getProperty(MERCHANT_SCAN_REFERENCE);
        if (merchantScanReference == null) {
            System.out.println("merchantScanReference is missing in config.properties.");
            return;
        }

        String token = prop.getProperty(API_TOKEN);
        String secret = prop.getProperty(API_SECRET);

        int numberToSubmit = Integer.parseInt(prop.getProperty(NUMBER_TO_SUBMIT));

        //arguments from command line
        for(int i = 0; i < args.length; i++) {
            if(args[i].contains(API_SECRET_)) {
                secret = args[i].replace(API_SECRET_, "");
            }
            else if(args[i].contains(API_TOKEN_)) {
                token = args[i].replace(API_TOKEN_, "");
            }
        }

        if(secret == null || secret.equals("")) {
            System.out.println("API secret is missing.");
            return;
        }

        if(token == null || token.equals("")) {
            System.out.println("API token is missing.");
            return;
        }

        File folder = new File(pathToFolder);

        // Create the completed folder if missing so we can move submitted files
        StringBuffer completedPath = new StringBuffer(pathToFolder).append(File.separator).append("completed");
        File completedFolder = new File(completedPath.toString());
        if (!completedFolder.exists()) {
            completedFolder.mkdir();
        }

        ArrayList<String> filesArray = getAllFilesFromDirectory(folder);
        if (filesArray == null && filesArray.size() == 0) {
            System.out.println("No files to submit");
            return;
        }

        // Convert btoa
        auth = token + ":" + secret;
        auth = Base64.getEncoder().encodeToString(auth.getBytes());

        int counter = 0;
        for (String str : filesArray) {
            if (counter >= numberToSubmit) {
                System.out.println("Final Total Submitted: " + counter);
                break;
            }

            // Set path
            Path file = Paths.get(str);
            String filename = file.getFileName().toString();

            String scan_ref = initiate(filename, "BS", "USA");
            if (scan_ref != null) {
                System.out.println(filename + ", " + scan_ref);
                if (upload(scan_ref,file) != null) {
                    System.out.println("Uploading");
                    if (finalize(scan_ref) != null) {
                        System.out.println("Success!");
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }


            } else {
                continue;
            }

            // if successfully submitted, move the files to completed.
            // if (path != null) {
            //    path.toFile().renameTo(new File(completedPath + path.getFileSystem().getSeparator() + filename));
            //}

            counter++;

        }

        System.out.println("Total Submitted: " + counter);
	}

    private static String initiate(String customerId, String type, String country) {
        // Open Connection
        HttpURLConnection conn;
        URL url = null;
        try {
            url = new URL(serverUrl);
        } catch(MalformedURLException muexc){
            System.out.println(muexc);
            return null;
        }

        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch(IOException ioexc){
            System.out.println(ioexc.getMessage());
            return null;
        }

        // Set headers
        conn.setDoOutput(true);
        conn.setDoInput(true);
        try {
            conn.setRequestMethod("POST");
        } catch(ProtocolException pexc){
            System.out.println(pexc.getMessage());
            return null;
        }
        conn.setRequestProperty("Authorization", "Basic " + auth);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT_TXT);

        // Combine JSON
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(MERCHANT_SCAN_REFERENCE, merchantScanReference);
        jsonObject.addProperty(CUSTOMER_ID, customerId);
        jsonObject.addProperty(TYPE, type);
        jsonObject.addProperty(COUNTRY, country);
        jsonObject.addProperty(ENABLE_EXTRACTION, "true");

        // Finished building jsonObject; Send to server
        OutputStreamWriter wr = null;
        try {
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(jsonObject.toString());
            wr.flush();
            wr.close();
        } catch (IOException ioexc) {
            System.out.println(customerId + ": " + ioexc.getMessage());
            return null;
        }

        // Receive response
        String streamToString = null;
        try {
            streamToString = convertStreamToString(conn.getInputStream());
        } catch (IOException ioexc) {
            System.out.println(customerId + ": " + ioexc.getMessage());
            return null;
        }

        // Parse response
        JsonParser parser = new JsonParser();
        JsonObject jsonObj = null;
        try {
            jsonObj = (JsonObject) parser.parse(streamToString);
        } catch (JsonSyntaxException jsexc) {
            System.out.println(customerId + ": " + streamToString);
            return null;
        }

        // Disconnect
        conn.disconnect();

        // Get response
        JsonElement jsonElm = jsonObj.get(SCAN_REFERENCE);
        if (jsonElm != null) {
            return jsonElm.getAsString();
        }

        return null;
    }

    private static String upload(String scan_ref, Path file) {
        // Open Connection
        HttpURLConnection conn;
        URL url = null;
        try {
            url = new URL(serverUrl + "/" + scan_ref + "/document");
        } catch(MalformedURLException muexc){
            System.out.println(muexc);
            return null;
        }

        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch(IOException ioexc){
            System.out.println(ioexc.getMessage());
            return null;
        }

        // Boundary
        String boundary = "--" + System.currentTimeMillis(); 
        System.out.println(boundary);

        // Set headers
        conn.setDoOutput(true);
        conn.setDoInput(true);
        try {
            conn.setRequestMethod("POST");
        } catch(ProtocolException pexc){
            System.out.println(pexc.getMessage());
            return null;
        }
        conn.setRequestProperty("Authorization", "Basic " + auth);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        conn.setRequestProperty("User-Agent", USER_AGENT_TXT);
        
        // Finished building jsonObject; Send to server
        OutputStream outputStream = null;
        PrintWriter wr = null;
        try {
            outputStream = conn.getOutputStream();
            wr = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
            wr.append("--" + boundary).append(LINE_FEED).flush();
            //wr.append("Content-Disposition: form-data; name=\"image\"; filename=\"" + file.getFileName().toString() + "\"").append(LINE_FEED);
            //wr.append("Content-Type: image/pdf").append(LINE_FEED);

            FileInputStream inputStream = new FileInputStream(file.toString());
            byte[] buf = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();

            wr.append(LINE_FEED).flush();
            wr.append("--" + boundary + "--").flush();
            wr.close();
        } catch (IOException ioexc) {
            System.out.println(scan_ref + ": " + ioexc.getMessage());
            return null;
        }

        // Receive response
        String streamToString = null;
        try {
            streamToString = convertStreamToString(conn.getInputStream());
        } catch (IOException ioexc) {
            System.out.println(scan_ref + ": " + ioexc.getMessage());
            return null;
        }

        // Parse response
        JsonParser parser = new JsonParser();
        JsonObject jsonObj = null;
        try {
            jsonObj = (JsonObject) parser.parse(streamToString);
        } catch (JsonSyntaxException jsexc) {
            System.out.println(scan_ref + ": " + streamToString);
            return null;
        }

        // Disconnect
        conn.disconnect();

        // Get response
        JsonElement jsonElm = jsonObj.get(TIME_STAMP);
        if (jsonElm != null) {
            return jsonElm.getAsString();
        }

        return null;
    }

    private static String finalize(String scan_ref) {
        // Open Connection
        HttpURLConnection conn;
        URL url = null;
        try {
            url = new URL(serverUrl + "/" + scan_ref);
        } catch(MalformedURLException muexc){
            System.out.println(muexc);
            return null;
        }

        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch(IOException ioexc){
            System.out.println(ioexc.getMessage());
            return null;
        }

        // Set headers
        conn.setDoOutput(true);
        conn.setDoInput(true);
        try {
            conn.setRequestMethod("PUT");
        } catch(ProtocolException pexc){
            System.out.println(pexc.getMessage());
            return null;
        }
        conn.setRequestProperty("Authorization", "Basic " + auth);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT_TXT);
        
        // Finished building jsonObject; Send to server
        OutputStreamWriter wr = null;
        try {
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.flush();
            wr.close();
        } catch (IOException ioexc) {
            System.out.println(scan_ref + ": " + ioexc.getMessage());
            return null;
        }

        // Receive response
        String streamToString = null;
        try {
            streamToString = convertStreamToString(conn.getInputStream());
        } catch (IOException ioexc) {
            System.out.println(scan_ref + ": " + ioexc.getMessage());
            return null;
        }

        // Parse response
        JsonParser parser = new JsonParser();
        JsonObject jsonObj = null;
        try {
            jsonObj = (JsonObject) parser.parse(streamToString);
        } catch (JsonSyntaxException jsexc) {
            System.out.println(scan_ref + ": " + streamToString);
            return null;
        }

        // Disconnect
        conn.disconnect();

        // Get response
        JsonElement jsonElm = jsonObj.get(TIME_STAMP);
        if (jsonElm != null) {
            return jsonElm.getAsString();
        }

        return null;
    }

	/**
	 * getAllFilesFromDirectory creates a list of doc.  
	 * 
	 * @param directory - Directory of all files to be verified
	 * @return
	 */
	private static ArrayList<String> getAllFilesFromDirectory(File directory) {
        ArrayList<String> resultList = new ArrayList<String>(1);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (name.endsWith("pdf")) {
					return true;
				} else {
					return false;
				}
			}
		};
		File[] f = directory.listFiles(filter);
		if (f != null) {
			for (File file : f) {
				try {
					resultList.add(file.getCanonicalPath());
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			}
		} else {
			System.out.println(directory.getName() + " does not exist.");
			return null;
		}

		if (resultList.size() > 0) {
			if (resultList.size() > 100) {
				System.out.println("There are more than 100 files in the folder. Only the first 100 will be processed.");
        	}
            return resultList;
        }
        else {
        	System.out.println(directory.getName() + " is empty.");
            return null;
        }
    }

	private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        }
        catch (Exception e) {
        	System.out.println(e.getMessage());
        }
        finally {
            try {
                is.close();
            }
            catch (Exception e) {
            	System.out.println(e.getMessage());
            }
        }
        return sb.toString();
    }
}
