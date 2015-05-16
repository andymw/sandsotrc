package sand.server;

import java.io.*;
import java.net.*;

public class LocationUtilities {

	/**
	 * Returns location of user's login based on ip address
	 */
	public static String loginLocationFinder(String ipAddress) {
		try {
			URL url = new URL("http://freegeoip.net/csv/" + ipAddress);
			HttpURLConnection ipConnection = (HttpURLConnection)url.openConnection();
			ipConnection.setConnectTimeout(4000);
			ipConnection.setReadTimeout(4000);
			ipConnection.connect();

			if (ipConnection.getResponseCode() != 200) {
				System.out.println("Error contacting server\n");
				return "";
			}
			BufferedReader URLReader = new BufferedReader(new InputStreamReader(
				ipConnection.getInputStream()));
			String APIResultOutput[] = new String[10];

			for (String line; (line = URLReader.readLine()) != null; ) {
				System.out.println(line);
				APIResultOutput = line.split(",");
			}
			return APIResultOutput[2];
		} catch (IOException e) {
			return ""; // access to the service timed out
		}
	}

	/**
	 * Verifies user login location based on usual location
	 */
	public static boolean abnormalLoginChecker(String ipAddress, String usualLocation) {
		String currentLocation = loginLocationFinder(ipAddress);
		boolean notNullLocation = !(currentLocation == null);
		return !(notNullLocation && currentLocation.equalsIgnoreCase(usualLocation));
	}

}
