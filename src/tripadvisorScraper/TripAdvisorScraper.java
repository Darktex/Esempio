package tripadvisorScraper;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.princeton.cs.algs4.Date;
import edu.princeton.cs.introcs.StdOut;



public class TripAdvisorScraper {

	MySQLConnection db;
	
	public TripAdvisorScraper() {
		db = new MySQLConnection();
	}
	
	/** Mines all the restaurants of a given city */
	public void mineCity(String city) throws SQLException {
		mineCity(city, 0, -1); // if pageEnd < pageStart it will mine them all
	}
	
	/** Mines all the restaurants of a given city, starting from pageStart */
	public void mineCity(String city, int pageStart) throws SQLException {
		mineCity(city, pageStart, -1);
	}

	/** Mines all the restaurants of a given city between pageStart and pageEnd */
	public void mineCity(String cityName, String cityUrl, int pageStart, int pageEnd) throws SQLException {
		Document doc = getHTMLAndSaveInDB(cityUrl);
		String totalStr = doc.select("div#EATERY_LIST_CONTENTS > div.deckTools > div.pagination.js_pageLinks" +
				" > span.pgCount > b").first().text();
		Integer totalRestaurants = Integer.valueOf(totalStr);
		if (pageEnd < pageStart) pageEnd = totalRestaurants;

		for (int i = pageStart * 30; i < pageEnd * 30; i += 30) { // i grows by 10 at a time as there are 10 restaurants per page
			Elements pageResults = null;
			if (i > 0) {
				String pageUrl = url + "&start=" + i;
				pageResults = getHTMLAndSaveInDB(pageUrl).select("div#EATERY_SEARCH_RESULTS > div.listing");
			}
			else pageResults = doc.select("div#businessresults > div.businessresult.clearfix");
			int j = 1;
			for (Element restaurantDiv : pageResults) {
				String restaurantPostfix = restaurantDiv.select("a.property_title").first().attr("href").toString();
				String restaurantUrl = stripTags("http://www.tripadvisor.com" + restaurantPostfix);
				Document restaurantPage = getHTMLAndSaveInDB(restaurantUrl);
				TripAdvisorRestaurant yr = new TripAdvisorRestaurant(restaurantPage, restaurantUrl);
				Integer q = i + j;
				StdOut.println(q + ") " + yr.name + " (" + yr.totalNumberOfReviews + " reviews)");
				j++;
				if (yr.hasScore() && !isAlreadyInDB(yr)) {
					yr.mineReviewsAndDumpToDB(restaurantPage, this); // We save a lot of time by doing it ONLY at this point
					writeToDB(yr);
				}
				else StdOut.println("\t\tAlready in DB. Skipping...");
				yr = null; // Should be useless
			}
		}
	}
	
	private String stripTags(String _url) {
		Integer index;
		if ((index = _url.indexOf("#query")) == -1)
			return _url;
		else
			return _url.substring(0, index);
	}

	public static Document getHTMLFromPage(String url) {
		System.setProperty("http.proxyHost", "deltoro");
		System.setProperty("http.proxyPort", "3128");

		Document doc = null;
		boolean success = false;

		while (!success) {
			try {
				String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.122 Safari/534.30";
				doc = Jsoup.connect(url).timeout(60 * 1000).userAgent(ua).get();
				success = true;
				break;
			} catch (IOException e) {
				System.out.println("Failed to connect to " + url + "\n\tRetrying...");
			}
		}
		return doc;
	}
	
	public Document getHTMLAndSaveInDB(String url) throws SQLException {
		Document doc = getHTMLFromPage(url);
		String alreadyExistsCheckQuery = "SELECT * FROM `HTMLcache` WHERE `url` = ?";
		PreparedStatement checkStatement = db.con
				.prepareStatement(alreadyExistsCheckQuery);
		checkStatement.setString(1, url); // the ID of this restaurant
		ResultSet alreadyExistsRes = checkStatement.executeQuery();
		if (alreadyExistsRes.first() ) return doc;
		String insertionQuery = "INSERT INTO `restaurant_reviews`.`HTMLcache` (`url`, `date`, `HTML`) " +
				"VALUES (?, CURRENT_TIMESTAMP, ?);";

		PreparedStatement prep = db.con.prepareStatement(insertionQuery);
		prep.setString(1, url);
		prep.setString(2, doc.html()); // Name CANNOT be null!
		
		prep.executeUpdate();
		
		return doc;
	}

	public void writeToDB(TripAdvisorRestaurant r) throws SQLException {
		String insertionQuery = "INSERT INTO  `YelpRestaurant` ("
				+ "`id` , `name` ,`category` ,`addressNum` ,`addressStreet` ,`addressCity` ,"
				+ "`addressRegion` ,`addressZip` ,`website` ,`avgRating` ,`totalNumOfReviews`, `phoneNumber`)"
				+ "VALUES (? ,  ?,  ?,  "
				+ "?,  ?,  ?,  ?,  ?,  "
				+ "?,  ?,  ?, ?);";

		PreparedStatement prep = db.con.prepareStatement(insertionQuery);

		prep.setString(1, r.hashString());
		prep.setString(2, r.name); // Name CANNOT be null!
		safeInsert(prep, 3, r.category);
		safeInsert(prep, 4, r.address.number);
		safeInsert(prep, 5, r.address.street);
		safeInsert(prep, 6, r.address.city);
		safeInsert(prep, 7, r.address.region);
		safeInsert(prep, 8, r.address.zip);
		safeInsert(prep, 9, r.website);
		prep.setFloat(10, r.rating);
		prep.setInt(11, r.totalNumberOfReviews);
		safeInsert(prep, 12, r.phoneNumber);

		if (!isAlreadyInDB(r)) {
			StdOut.println("----\n" + prep + "\n--------");
			prep.execute();
		} else
			StdOut.println("Restaurant " + r.name
					+ " already in the DB. Skipping...");
		for (YelpReview rvw : r.reviews)
			writeToDB(rvw);
	}
	
	public void writeToDB(YelpReview rev) throws SQLException {

		String alreadyExistsCheckQuery = "SELECT * FROM  `YelpReview` WHERE  `id` =  ?";
		PreparedStatement checkStatement = db.con
				.prepareStatement(alreadyExistsCheckQuery);
		checkStatement.setString(1, rev.id);
		ResultSet alreadyExistsRes = checkStatement.executeQuery(); // if it's already there, don't insert
		String insertionQuery = "INSERT INTO `YelpReview` " +
				"(`id`, `author_id`, `restaurant_id`, `rating`, `usefulCounter`, `funnyCounter`, " +
				"`coolCounter`, `text`, `date`) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";

		PreparedStatement prep = db.con.prepareStatement(insertionQuery);

		prep.setString(1, rev.id); // Always null, handled by DB
		
		writeToDb(rev.author);
		prep.setString(2, rev.author.getId()); // Name CANNOT be null!
		
		prep.setString(3, rev.restaurant.hashString());
		prep.setFloat(4, rev.rating);
		prep.setInt(5,  rev.usefulCounter);
		prep.setInt(6,  rev.funnyCounter);
		prep.setInt(7,  rev.coolCounter);
		safeInsert(prep, 8, rev.text);
		safeInsert(prep, 9, mySQLformat(rev.date));
		
		if (!alreadyExistsRes.first()) {
			prep.execute();
		}
	}
	
	private String mySQLformat(Date d) {
		return d.year() + "-" + d.month() + "-" + d.day();
	}
	
	public void writeToDb(YelpUser u) throws SQLException {

		String alreadyExistsCheckQuery = "SELECT * FROM  `YelpUser` WHERE  `id` =  ?";
		PreparedStatement checkStatement = db.con
				.prepareStatement(alreadyExistsCheckQuery);
		checkStatement.setString(1, u.id);
		ResultSet alreadyExistsRes = checkStatement.executeQuery(); // if it's already there, don't insert
		String insertionQuery = "INSERT INTO `YelpUser` (`id`, `userName`, " +
				"`friendsCount`, `reviewsCount`) " +
				"VALUES (?, ?, ?, ?);";
		PreparedStatement prep = db.con.prepareStatement(insertionQuery);
		prep.setString(1, u.id); 
		prep.setString(2, u.userName); // Name CANNOT be null!
		prep.setInt(3, u.friendsCount);
		prep.setInt(4, u.reviewsCount);
		
		if (!alreadyExistsRes.first()) {
			prep.execute();
		}
	}

	private boolean isAlreadyInDB(TripAdvisorRestaurant r) throws SQLException {
		String alreadyExistsCheckQuery = "SELECT * FROM  `YelpRestaurant` WHERE  `id` =  ?";
		PreparedStatement checkStatement = db.con
				.prepareStatement(alreadyExistsCheckQuery);
		checkStatement.setString(1, r.hashString()); // the ID of this restaurant
		ResultSet alreadyExistsRes = checkStatement.executeQuery();
		if (!alreadyExistsRes.first() ) return false;
		return true;
	}

	private static void safeInsert(PreparedStatement prep, int pos, String field)
			throws SQLException { // JDBC sends an empty string instead of a
		// NULL value.
		if (field.isEmpty())
			prep.setString(pos, null);
		else
			prep.setString(pos, field);
	}

	public static void main(String[] args) throws SQLException {
		StdOut.println("For help, launch this with --help");
		TripAdvisorScraper y = new TripAdvisorScraper();
		
		if (args[0].equals("--mineAllCities") ) {
			for (int i = 1; i < args.length; i++)
				y.mineCity(args[i]);
		}
		else if (args[0].equals("--mineBetween")) {
			if (args.length < 4) throw new Error("Not enough arguments. See --help");
			int start = Integer.valueOf(args[1]);
			int end = Integer.valueOf(args[2]);
			for (int i = 3; i < args.length; i++)
				y.mineCity(args[i], start, end);
		}
		else if (args[0].equals("--startFrom")) {
			if (args.length < 3) throw new Error("Not enough arguments. See --help");
			int start = Integer.valueOf(args[1]);
			for (int i = 2; i < args.length; i++)
				y.mineCity(args[i], start);
		}
		else if (args[0].equals("--mineNeighbourhoods") || args[0].equals("--mineNeighborhoods")) { // UK and US friendly :P
			if (args.length < 3) throw new Error("Not enough arguments. See --help");
			String city = args[1];
			for (int i = 2; i < args.length; i++) {
				String neighbourhoodName = args[i] + "%2C+" + city+ "%2C";
				y.mineCity(neighbourhoodName);
			}
		}
		else {
			StdOut.println("Usage:\nCrawl a list of cities: java -Xmx3g -jar YelpScraper.jar --mineAllCities cityA cityB ... cityN" +
					"\nCrawl a list of cities starting from page n: java -Xmx3g -jar YelpScraper.jar --startFrom n cityA cityB ..." +
					"\nCrawl a list of cities starting from page n, ending in page m: java -Xmx3g -jar YelpScraper.jar --mineBetween n m cityA cityB ..." +
					"\nCrawl a list of neighbourhoods of a city: java -Xmx3g -jar YelpScraper.jar --mineNeighbourhoods city neighA neighB ...");
		}
	}

}
