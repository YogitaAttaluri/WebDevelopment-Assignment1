import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.sql.*;

@WebServlet("/customer")
public class CustomerServlet extends HttpServlet {

    static Connection conn;
    static PreparedStatement insertStatement;
    static PreparedStatement selectStatement;
    static PreparedStatement checkUserStatement;
    static PreparedStatement listFlightsStatement;
    static PreparedStatement findFlightStatement;
    static PreparedStatement bookFlightStatement;

    public CustomerServlet() {
        try {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:h2:~/Desktop/myservers/databases/flight",
                    "sa",
                    "");
            // Ensure the CUSTOMER table exists
            conn.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS CUSTOMER " +
                            "(NAME VARCHAR(255), LOGIN VARCHAR(255) PRIMARY KEY, PASSWORD VARCHAR(255))");
            // Prepare SQL statements for later retieving information from database
            insertStatement = conn.prepareStatement(
                    "INSERT INTO CUSTOMER (NAME, LOGIN, PASSWORD) VALUES (?, ?, ?)");
            selectStatement = conn.prepareStatement(
                    "SELECT * FROM CUSTOMER WHERE LOGIN = ? AND PASSWORD = ?");
            checkUserStatement = conn.prepareStatement(
                    "SELECT LOGIN FROM CUSTOMER WHERE LOGIN = ?");
            listFlightsStatement = conn.prepareStatement(
                    "SELECT F.FLIGHT_NUM, F.START_APT, F.END_APT " +
                            "FROM FLIGHT F " +
                            "JOIN PASSENGER P ON F.FLIGHT_ID = P.FLIGHT_ID " +
                            "JOIN CUSTOMER C ON P.NAME = C.NAME " +
                            "WHERE C.LOGIN = ?");
            findFlightStatement = conn.prepareStatement(
                    "SELECT FLIGHT_ID FROM FLIGHT WHERE START_APT = ? AND END_APT = ?");
            bookFlightStatement = conn.prepareStatement(
                    "INSERT INTO PASSENGER (PID, NAME, FLIGHT_ID, MILES) VALUES (?, ?, ?, ?)");
            System.out.println("CustomerServlet: successful connection to H2 database");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String login = request.getParameter("login");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("passwordConfirm");
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("username") != null) {
            if ("listFlights".equals(request.getParameter("action"))) {
                listFlightsForCustomer((String) session.getAttribute("username"), response);
            } else if ("bookFlight".equals(request.getParameter("action"))) {
                String startApt = request.getParameter("destination1");
                String endApt = request.getParameter("destination2");
                if (startApt != null && endApt != null && !startApt.trim().isEmpty() && !endApt.trim().isEmpty()) {
                    bookFlightForCustomer((String) session.getAttribute("username"), startApt, endApt, response);
                } else {
                    redirectToWelcomePage(response, (String) session.getAttribute("username"),
                            "Please specify both start and end airports.");
                }
            } else {
                redirectToWelcomePage(response, (String) session.getAttribute("username"), null);
            }
        }
        try {
            if (name != null && !name.trim().isEmpty() && login != null && password != null
                    && password.equals(confirmPassword)) {
                checkUserStatement.setString(1, login);
                ResultSet rsCheck = checkUserStatement.executeQuery();
                if (rsCheck.next()) {
                    response.sendRedirect("customerlogin.html?registerError=Record+already+exists");
                } else {
                    insertStatement.setString(1, name);
                    insertStatement.setString(2, login);
                    insertStatement.setString(3, password);
                    insertStatement.executeUpdate();
                    request.getSession().setAttribute("username", login);
                    redirectToWelcomePage(response, name, null);
                }
            } else if (name != null && login != null && password != null && !password.equals(confirmPassword)) {
                response.sendRedirect("customerlogin.html?registerError=Passwords+do+not+match");
            } else if (name == null || name.trim().isEmpty()) {
                selectStatement.setString(1, login);
                selectStatement.setString(2, password);
                ResultSet rs = selectStatement.executeQuery();
                if (rs.next()) {
                    request.getSession().setAttribute("username", rs.getString("LOGIN"));
                    redirectToWelcomePage(response, rs.getString("NAME"), null);
                } else {
                    response.sendRedirect("customerlogin.html?loginError=Invalid+Credentials");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("customerlogin.html?registerError=Database+error");
        }
    }

    private void listFlightsForCustomer(String login, HttpServletResponse response) throws IOException {
        try {
            listFlightsStatement.setString(1, login);
            System.out.println(login);
            System.out.println(listFlightsStatement);
            ResultSet rs = listFlightsStatement.executeQuery();
            if (!rs.next()) {
                redirectToWelcomePage(response, login, "There are no current flights you are booked on.");
            } else {
                rs.beforeFirst();
                displayFlightsPage(rs, response);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("customerlogin.html?error=Database+error+while+retrieving+flights");
        }
    }

    private void redirectToWelcomePage(HttpServletResponse response, String customerName, String message)
            throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>JZ Airlines</title>");
        out.println("</head>");
        out.println("<body bgcolor=\"#ddddff\">");
        out.println("<h1>JZ Airlines: Customer</h1>");
        out.println("<div style=\"margin-top: 5%;\">");
        out.println("<h3>Welcome " + customerName + "!</h3>");
        out.println("<form action=\"/customer\" method=\"post\">");
        out.println("<table>");
        out.println("<tr style=\"padding-left: 10px;\">");
        out.println("<td>1. List my current flights</td>");
        out.println("<input type=\"hidden\" name=\"action\" value=\"listFlights\">");
        out.println("<td><input type=\"submit\" value=\"Go\" style=\"margin-left: 50px;\"></td>");
        out.println("</tr>");
        out.println("</table>");
        if (message != null && !message.isEmpty()) {
            out.println("<p style=\"color: red;\">" + message + "</p>");
        }
        out.println("</form>");
        out.println("<form action=\"/customer\" method=\"post\">");
        out.println("<table>");
        out.println("<tr style=\"padding-left: 10px;\">");
        out.println("<td>2. Add flight from</td>");
        out.println("<td><input type=\"text\" name=\"destination1\"></td>");
        out.println("<td>to</td>");
        out.println("<td><input type=\"text\" name=\"destination2\"></td>");
        out.println("<input type=\"hidden\" name=\"action\" value=\"bookFlight\">");
        out.println("<td><input type=\"submit\" value=\"Add\" style=\"margin-left: 50px;\"></td>");
        out.println("</tr>");
        out.println("</table>");
        out.println("</form>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
        out.close();
    }

    private void bookFlightForCustomer(String login, String startApt, String endApt, HttpServletResponse response)
            throws IOException {
        try {
            String customerName = getCustomerNameByLogin(login);
            if (customerName == null) {
                redirectToWelcomePage(response, login, "Unable to retrieve customer details.");
                return;
            }

            findFlightStatement.setString(1, startApt);
            findFlightStatement.setString(2, endApt);
            ResultSet flightResultSet = findFlightStatement.executeQuery();
            if (!flightResultSet.next()) {
                redirectToWelcomePage(response, login, "No flights available from " + startApt + " to " + endApt);
            } else {
                int flightId = flightResultSet.getInt("FLIGHT_ID");
                int nextPid = getNextPassengerId();
                bookFlightStatement.setInt(1, nextPid);
                bookFlightStatement.setString(2, customerName);
                bookFlightStatement.setInt(3, flightId);
                bookFlightStatement.setInt(4, 0);
                bookFlightStatement.executeUpdate();
                redirectToWelcomePage(response, login, "Flight booked successfully!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("customerlogin.html?error=Database+error+while+booking+flight");
        }
    }

    private String getCustomerNameByLogin(String login) throws SQLException {
        String customerName = null;
        PreparedStatement getNameStatement = conn.prepareStatement(
                "SELECT NAME FROM CUSTOMER WHERE LOGIN = ?");
        getNameStatement.setString(1, login);
        ResultSet rs = getNameStatement.executeQuery();
        if (rs.next()) {
            customerName = rs.getString("NAME");
        }
        rs.close();
        getNameStatement.close();
        return customerName;
    }

    private int getNextPassengerId() throws SQLException {
        int nextId = 1;
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT MAX(PID) FROM PASSENGER");
        if (resultSet.next()) {
            nextId = resultSet.getInt(1) + 1;
        }
        resultSet.close();
        statement.close();
        return nextId;
    }

    private void displayFlightsPage(ResultSet rs, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><title>Your Flights</title></head>");
        out.println("<body bgcolor=\"#ddddff\">");
        out.println("<h1>Flight List</h1>");
        try {
            if (!rs.isBeforeFirst()) {
                out.println("<p>There are no current flights you are booked on.</p>");
            } else {
                out.println("<table border='1'>");
                out.println("<tr><th>Flight Number</th><th>From</th><th>To</th></tr>");
                while (rs.next()) {
                    out.println("<tr>");
                    out.println("<td>" + rs.getString("FLIGHT_NUM") + "</td>");
                    out.println("<td>" + rs.getString("START_APT") + "</td>");
                    out.println("<td>" + rs.getString("END_APT") + "</td>");
                    out.println("</tr>");
                }
                out.println("</table>");
            }
        } catch (SQLException e) {
            out.println("<p>Error retrieving flight data.</p>");
            e.printStackTrace();
        }
        out.println("</body>");
        out.println("</html>");
        out.close();
    }

}
