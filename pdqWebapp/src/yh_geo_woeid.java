import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class yh_geo_woeid extends HttpServlet {

	/**
	 * @author Mark Ridler
	 * 
	 *         This webapp implements a simple return of a table from a Postgres
	 *         database. It returns in XML format to be compatible with the REST
	 *         services on the client side.
	 * 
	 *         This is best deployed as a Dynamic Web Project in Eclipse. This is
	 *         not installed by default so you will have to upgrade. Alongside this
	 *         goes the Apache Tomcat server which again can be integrated with
	 *         Eclipse on the Servers tab. Then right mouse click on the tomcat and
	 *         select "Add and Remove" to add the dynamic web project. After this
	 *         has been done you will be able to automatically publish to the tomcat
	 *         whenever the source file has changed.
	 * 
	 *         The one thing that we haven't been able to get working is the
	 *         integration with git. At the moment a separate git project has the
	 *         source files which must be copied into the dynamic web project and
	 *         vice versa
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static Map<String, String> getQueryMap(String query) {
		String[] params = query.split("&");
		Map<String, String> map = new HashMap<String, String>();
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			map.put(name, value);
		}
		return map;
	}

	private static void printGetString(PrintWriter out, Object rs, String string) throws SQLException {
		out.println("<" + string + ">" + rs + "</" + string + ">");
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.setContentType("application/xml");
		PrintWriter out = response.getWriter();
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
		try {
			String s = request.getParameter("woeid");
			if (s == null || s.equals("3")) {
				out.println("<yh_geo_woeid>");
				out.println("<YahooPlaces>");
				printGetString(out, 3, "woeid");
				printGetString(out, 2, "name");
				printGetString(out, 3, "type");
				printGetString(out, "Point of Interest", "placeTypeName");
				printGetString(out, 5, "country");
				printGetString(out, 6, "admin1");
				printGetString(out, 7, "admin2");
				printGetString(out, 8, "admin3");
				printGetString(out, 9, "locality1");
				printGetString(out, 10, "locality2");
				printGetString(out, 11, "postal");
				printGetString(out, 12, "centroid_lat");
				printGetString(out, 13, "centroid_lng");
				printGetString(out, 14, "bboxNorth");
				printGetString(out, 15, "bboxSouth");
				printGetString(out, 16, "bboxEast");
				printGetString(out, 17, "bboxWest");
				printGetString(out, 18, "timezone");
				out.println("</YahooPlaces>");
				out.println("<YahooPlaces>");
				printGetString(out, 3, "woeid");
				printGetString(out, "China", "name");
				printGetString(out, 3, "type");
				printGetString(out, "PointofInterest", "placeTypeName");
				printGetString(out, "China", "country");
				printGetString(out, 6, "admin1");
				printGetString(out, 7, "admin2");
				printGetString(out, 8, "admin3");
				printGetString(out, 9, "locality1");
				printGetString(out, 10, "locality2");
				printGetString(out, 11, "postal");
				printGetString(out, 12, "centroid_lat");
				printGetString(out, 13, "centroid_lng");
				printGetString(out, 14, "bboxNorth");
				printGetString(out, 15, "bboxSouth");
				printGetString(out, 16, "bboxEast");
				printGetString(out, 17, "bboxWest");
				printGetString(out, 18, "timezone");
				out.println("</YahooPlaces>");
				
				out.println("</yh_geo_woeid>");
			}
		} catch (Exception e) {
			out.println("<pre>" + e.toString() + "</pre>");
		}
	}

	/**
	 * We are going to perform the same operations for POST requests as for GET
	 * methods, so this method just sends the request to the doGet method.
	 */

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		doGet(request, response);
	}
}
