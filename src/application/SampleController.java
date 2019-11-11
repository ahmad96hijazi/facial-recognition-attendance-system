package application;

import static application.FaceDetector.output;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import javafx.scene.control.ProgressIndicator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SampleController {

    //Mention The file location path where the face will be saved & retrieved
    public String filePath = "./faces";

    @FXML
    private Button startCam;
    @FXML
    private Button stopBtn;
    @FXML
    private Button saveBtn;
    @FXML
    private Button recogniseBtn;
    @FXML
    private Button stopRecBtn;
    @FXML
    private Button attendBtn;
    @FXML
    private ImageView frame;
    @FXML
    private TitledPane dataPane;
    @FXML
    private TextField fname;
    @FXML
    private TextField lname;
    @FXML
    private TextField code;
    @FXML
    public ListView<String> logList;
    @FXML
    public ListView<String> output;
    @FXML
    public ProgressIndicator pb;
    @FXML
    public Label savedLabel;
    @FXML
    public Label warning;
    @FXML
    public Label title;
    @FXML
    public TilePane tile;

    int count = 0;

    FaceDetector faceDetect = new FaceDetector();	//Creating Face detector object				
    Database database = new Database();                 //Creating Database object

    ArrayList<Student> students = new ArrayList<>();
    ArrayList<String> images = new ArrayList<>();

    ImageView imageView1;

    public static ObservableList<String> event = FXCollections.observableArrayList();
    public static ObservableList<String> outEvent = FXCollections.observableArrayList();

    public boolean enabled = false;
    public boolean isDBready = false;

    static Student student;

    public void putOnLog(String data) {
        Instant now = Instant.now();
        String logs = now.toString() + ":\n" + data;
        event.add(logs);
        logList.setItems(event);
    }

    @FXML
    protected void startCamera() throws SQLException {
        //initializing objects from start camera button event
        faceDetect.init();
        faceDetect.setFrame(frame);
        faceDetect.start();
        if (!database.init()) {
            putOnLog("Error: Database Connection Failed ! ");
        } else {
            isDBready = true;
            putOnLog("Success: Database Connection Succesful ! ");
        }
        //Activating other buttons
        startCam.setVisible(false);
        stopBtn.setVisible(true);
        saveBtn.setDisable(false);
        if (isDBready) {
            recogniseBtn.setDisable(false);
        }
        dataPane.setDisable(false);
        if (stopRecBtn.isDisable()) {
            stopRecBtn.setDisable(false);
        }
        tile.setPadding(new Insets(15, 15, 55, 15));
        tile.setHgap(30);

        //Picture Gallary
        File folder = new File(filePath);
        File[] listOfFiles = folder.listFiles();

        //Image reader from the mentioned folder
        for (final File file : listOfFiles) {
            String fileName = file.getName();
            if (fileName.contains(".jpg")) {
                images.add(fileName);
                imageView1 = createImageView(file);
                tile.getChildren().addAll(imageView1);
            }
        }

        //Get student data from database
        students = new ArrayList<>();
        students = database.getStudents();
        FaceDetector.output = students;

        putOnLog(" Real Time WebCam Stream Started !");
    }

    @FXML
    protected void faceRecognise() {
        //Picture Gallary
//        File folder = new File(path);
//        File[] listOfFiles = folder.listFiles();
//        
//       for (final File file : listOfFiles) {
//           String fileName = file.getName();
//           if (fileName.contains(".jpg")) {
//                System.out.println(true);
//           }
//        }

        faceDetect.setIsRecFace(true);
        attendBtn.setDisable(false);

        //Getting detected faces
//        students = faceDetect.getOutput();
        putOnLog("Face Recognition Activated !");
        stopRecBtn.setDisable(false);
    }

    @FXML
    protected void stopRecognise() {
        faceDetect.setIsRecFace(false);
        attendBtn.setDisable(true);
        faceDetect.clearOutput();
        this.students.clear();
        recogniseBtn.setText("Recognise Face");
        stopRecBtn.setDisable(true);
        putOnLog("Face Recognition Deactivated !");
    }

    @FXML
    protected void saveFace() throws SQLException {
        //Input Validation
        if (fname.getText().trim().isEmpty() || code.getText().trim().isEmpty()) {
            new Thread(() -> {
                try {
                    warning.setVisible(true);
                    Thread.sleep(2000);
                    warning.setVisible(false);
                } catch (InterruptedException ex) {
                }
            }).start();

        } else {
            //Progressbar
            pb.setVisible(true);
            savedLabel.setVisible(true);
            new Thread(() -> {
                try {
                    boolean exist = false;
                    Student st = new Student();
                    for (Student s : students) {
                        if (Integer.parseInt(code.getText()) == s.getId()) {
                            st = s;
                            exist = true;
                        }
                    }

                    if (!exist) {
                        st.setFirstname(fname.getText());
                        st.setLastname(lname.getText());
                        st.setId(Integer.parseInt(code.getText()));
                 
                        faceDetect.setStudent(st);
                        database.insertStudent(st);

//                        putOnLog("Student added !");

                    } else {
                        images.clear();
                        File folder = new File(filePath);
                        File[] listOfFiles = folder.listFiles();

                        //Image reader from the mentioned folder
                        for (final File file : listOfFiles) {
                            String fileName = file.getName();
                            if (fileName.contains(".jpg")) {
                                images.add(fileName);
                            }
                        }

                        for (String image : images) {
                            if (image.contains(code.getText())) {
                                count++;
                            }
                        }

                        FaceDetector.count = count;
                        faceDetect.setStudent(student);
                    }

                    pb.setProgress(100);

                    savedLabel.setVisible(true);
                    Thread.sleep(2000);

                    pb.setVisible(false);

                    savedLabel.setVisible(false);

                    //Get student data from database
                    students = new ArrayList<>();
                    students = database.getStudents();
                    FaceDetector.output = students;

                } catch (InterruptedException | SQLException ex) {
                }

            }).start();

            faceDetect.setSaveFace(true);
            count = 0;
        }

    }

    @FXML
    protected void stopCam() throws SQLException {
        faceDetect.stop();
        tile.getChildren().clear();
        frame.setImage(null);
        startCam.setVisible(true);
        stopBtn.setVisible(false);

        putOnLog("Cam Stream Stopped!");
        recogniseBtn.setDisable(true);
        saveBtn.setDisable(true);
        dataPane.setDisable(true);
        stopRecBtn.setDisable(true);
        attendBtn.setDisable(true);

        database.dbClose();
        putOnLog("Database Connection Closed");
        isDBready = false;
    }

    @FXML
    protected void attendStudent() throws SQLException, MalformedURLException, ProtocolException, IOException {
        if (student != null) {
            //Retrieved data will be shown in Fetched Data pane
            String t = "Student ID: " + student.getId();
            outEvent.add(t);
            output.setItems(outEvent);

            database.attendStudent(student);
        }
    }

    private ImageView createImageView(final File imageFile) {
        try {
            final Image img = new Image(new FileInputStream(imageFile), 120, 0, true, true);

            imageView1 = new ImageView(img);
            imageView1.setStyle("-fx-background-color: BLACK");
            imageView1.setFitHeight(120);
            imageView1.setPreserveRatio(true);
            imageView1.setSmooth(true);
            imageView1.setCache(true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return imageView1;
    }

}
