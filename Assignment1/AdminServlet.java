import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.sql.*;

@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    static Connection conn;
    static Statement statement;

    public AdminServlet() {
        try {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:h2:~/Desktop/myservers/databases/flight",
                    "sa",
                    "");
            statement = conn.createStatement();
            System.out.println("AdminServlet: successful connection to H2 dbase");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("username");
        String providedUsername = request.getParameter("login");
        String providedPassword = request.getParameter("password");
        String tableName = request.getParameter("tableName");

        if (providedUsername != null && providedPassword != null) {
            if (providedUsername.equals("admin") && providedPassword.equals("123")) {
                session.setAttribute("username", providedUsername);
            } else {
                session.invalidate();
                response.sendRedirect("/adminlogin.html?error=Login failed");
                return;
            }
        } else if (username == null) {
            response.sendRedirect("/adminlogin.html?error=Login required");
            return;
        }

        if (tableName == null) {
            out.println("<html>");
            out.println("<head><title>JZ Airlines</title></head>");
            out.println("<body bgcolor=\"#ddddff\">");
            out.println("<font color=\"#020202\">");
            out.println("<h1>JZ Airlines: Admin</h1>");
            out.println("<div class=\"container\" style=\"margin-top: 10%;\">");
            out.println("<form action=\"/admin\" method=\"post\" style=\"text-align: center;\">");
            out.println("<table>");
            out.println("<tr>");
            out.println("<td>Enter Table Name:</td>");
            out.println("<td><input type=\"text\" name=\"tableName\" required></td>");
            out.println("<td><input type=\"submit\" value=\"List Rows\" style=\"margin-left: 10px;\"></td>");
            out.println("</tr>");
            out.println("</table>");
            out.println("</form>");
            out.println("</div>");
            out.println("</body>");
            out.println("</html>");
        } else {
            String tableDataHTML = generateTableDataHTML(tableName);
            out.println("<html>");
            out.println("<head><title>Query Results</title></head>");
            out.println("<body bgcolor=\"#ddddff\">");
            out.println("<h2>Query Results</h2>");
            out.println(tableDataHTML);
            out.println("</body>");
            out.println("</html>");
        }
        out.close();
    }

    private String generateTableDataHTML(String tableName) {
        try {
            String sql = "SELECT * FROM " + tableName;
            ResultSet rs = statement.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();
            int numColumns = rsmd.getColumnCount();

            StringBuilder htmlString = new StringBuilder();
            htmlString.append("<table border='1'>\n");

            htmlString.append("<tr>");
            for (int i = 1; i <= numColumns; i++) {
                htmlString.append("<th>").append(rsmd.getColumnName(i)).append("</th>");
            }
            htmlString.append("</tr>\n");

            while (rs.next()) {
                htmlString.append("<tr>");
                for (int i = 1; i <= numColumns; i++) {
                    htmlString.append("<td>").append(rs.getString(i)).append("</td>");
                }
                htmlString.append("</tr>\n");
            }

            htmlString.append("</table>\n");
            return htmlString.toString();

        } catch (SQLException e) {
            return "<h3>Error: Please enter a valid table name.</h3>";
        }
    }

}
