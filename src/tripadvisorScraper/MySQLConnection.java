package tripadvisorScraper;

import java.sql.*;

public class MySQLConnection {
	public Connection con;
	
	
	public MySQLConnection() {
		con = null;
		String url = "jdbc:mysql://myleott.com/";
		String db = "restaurant_reviews";
        String options = "?useCompression=true";
		String driver = "com.mysql.jdbc.Driver";
		try{
			Class.forName(driver);
			con = DriverManager.getConnection(url+db+options,"davide","5upray8F");
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
}
