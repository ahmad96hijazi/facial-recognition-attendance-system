package application;

import static application.AppConst.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;

class Database {

    public Connection con;

    public boolean init() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            try {
                this.con = DriverManager.getConnection("jdbc:mysql://" + URL + ":3306/" + Database_name, Database_user, Database_pass);
            } catch (SQLException e) {
                System.out.println("Error: Database Connection Failed ! Please check the connection Setting");
                return false;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void insertStudent(Student student) {
        String sql = "INSERT INTO `student`(`student_id`, `first_name`, `last_name`) VALUES (?, ?, ?)";
        PreparedStatement statement = null;
        try {
            statement = con.prepareStatement(sql);
            statement.setInt(1, student.getId());
            statement.setString(2, student.getFirstame());
            statement.setString(3, student.getLastname());
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("student insert successfully!");
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void attendStudent(Student student) throws UnsupportedEncodingException, MalformedURLException, ProtocolException, IOException {
        Instant now = Instant.now();
        String date = now.toString().substring(0, 10);
        String sql = "INSERT INTO `attendance_date`(`date`) VALUES ('" + date + "')";
        PreparedStatement statement = null;
        try {
            statement = con.prepareStatement(sql);
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("date inserted " + date);
                updateAttend(student);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
            System.err.println(e.getMessage());
            System.out.println(date);
            updateAttend(student);
        }
    }

    private void updateAttend(Student student) {
        String sql = "INSERT INTO `attendance_record`(`date_id`, `student_id`, `section_id`, `attendance`) VALUES (?, ?, ?, ?)";
        PreparedStatement statement = null;
        try {
            statement = con.prepareStatement(sql);
            statement.setInt(1, 1);
            statement.setInt(2, student.getId());
            statement.setInt(3, 1);
            statement.setInt(4, 1);
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("student insert successfully!");
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public ArrayList<Student> getStudents() throws SQLException {
        ArrayList<Student> students = new ArrayList<>();
        Student st;
        try {
            String sql = "SELECT * FROM `student`";
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                st = new Student();
                st.setId(rs.getInt("id"));
                st.setFirstname(rs.getString("first_name"));
                st.setLastname(rs.getString("last_name"));
                students.add(st);
            }
        } catch (SQLException e) {
            e.getStackTrace();
        }
        return students;
    }

    public void dbClose() throws SQLException {
        try {
            con.close(); // closing connection
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
