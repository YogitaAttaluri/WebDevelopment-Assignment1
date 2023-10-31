import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;

public class MovieSearchServlet extends HttpServlet {

    private Connection conn;
    private Statement statement;

    public MovieSearchServlet() {
        try {
            openConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            handleRequest(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        String query = request.getParameter("query");

        List<String> titleResults = searchTitles(query);
        List<String> descriptionResults = searchDescriptions(query);

        out.println("<html><head><title>Movie Search</title></head><body bgcolor=\"#ddddff\">");
        out.println("<h1>Movie Search</h1>");

        out.println("<form action=\"/moviequery\" method=\"get\">");
        out.println("Enter search word: <input type=\"text\" name=\"query\" value=\"" + query + "\" required>");
        out.println("<input type=\"submit\" value=\"Go\">");
        out.println("</form>");

        out.println("<h2>Query Results</h2>");

        out.println("OCCURRENCE IN TITLES:<br>");
        for (String title : titleResults) {
            out.println(" - " + title + "<br>");
        }

        out.println("<br>OCCURRENCE IN DESCRIPTION:<br>");
        for (String title : descriptionResults) {
            out.println(" - " + title + "<br>");
        }

        out.println("</body></html>");
    }

    private List<String> searchTitles(String word) throws Exception {
        List<String> results = new ArrayList<>();

        String sql = "SELECT TITLE FROM MOVIESUMMARY WHERE TITLE ILIKE ?";
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        preparedStatement.setString(1, "%" + word + "%");
        ResultSet rs = preparedStatement.executeQuery();

        while (rs.next()) {
            String title = rs.getString(1);
            results.add(title);
        }
        return results;
    }

    private List<String> searchDescriptions(String word) throws Exception {
        List<String> results = new ArrayList<>();

        String sql = "SELECT TITLE FROM MOVIESUMMARY WHERE DESCRIPTION ILIKE ?";
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        preparedStatement.setString(1, "%" + word + "%");
        ResultSet rs = preparedStatement.executeQuery();

        while (rs.next()) {
            String title = rs.getString(1);
            results.add(title);
        }
        return results;
    }

    private void openConnection() throws Exception {
        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection("jdbc:h2:~/Desktop/myservers/databases/moviesearch", "sa", "");
        statement = conn.createStatement();
    }
}
