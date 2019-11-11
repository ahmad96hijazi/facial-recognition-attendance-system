package application;

import static application.AppConst.camera;
import java.awt.BasicStroke;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

public class FaceDetector implements Runnable {

    Database database = new Database();

    FaceRecognizer faceRecognizer = new FaceRecognizer();
    OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
    Java2DFrameConverter paintConverter = new Java2DFrameConverter();
    static ArrayList<Student> output = new ArrayList<>();

    @FXML
    public Label ll;
    private Exception exception = null;

    static int count = 0;

    public String classiferName;
    public File classifierFile;

    public boolean saveFace = false;
    public boolean isRecFace = false;
    public boolean isOutput = false;
    public boolean isOcrMode = false;
    public boolean isMotion = false;
    public boolean isEyeDetection = false;
    public boolean isSmile = false;
    public boolean isUpperBody = false;
    public boolean isFullBody = false;
    private boolean stop = false;

    private CvHaarClassifierCascade classifier = null;
    private CvHaarClassifierCascade classifierEye = null;
    private CvHaarClassifierCascade classifierSideFace = null;
    private CvHaarClassifierCascade classifierUpperBody = null;
    private CvHaarClassifierCascade classifierFullBody = null;
    private CvHaarClassifierCascade classifierSmile = null;
    private CvHaarClassifierCascade classifierEyeglass = null;

    public CvMemStorage storage = null;
    private FrameGrabber grabber = null;
    private IplImage grabbedImage = null, temp, temp2, grayImage = null, smallImage = null;
    public ImageView frames2;
    public ImageView frames;

    private CvSeq faces = null;
    private CvSeq eyes = null;
    private CvSeq smile = null;
    private CvSeq upperBody = null;
    private CvSeq sideface = null;
    private CvSeq fullBody = null;

    int recogniseCode;

    Student student;

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public void init() {
        faceRecognizer.init();

        setClassifier("haar/haarcascade_frontalface_alt.xml");
        setClassifierEye("haar/haarcascade_eye.xml");
        setClassifierEyeGlass("haar/haarcascade_eye_tree_eyeglasses.xml");
        setClassifierSideFace("haar/haarcascade_profileface.xml");
        setClassifierFullBody("haar/haarcascade_fullbody.xml");
        setClassifierUpperBody("haar/haarcascade_upperbody.xml");
        setClassifierSmile("haar/haarcascade_smile.xml");

    }

    public void start() {
        try {
            new Thread(this).start();
        } catch (Exception e) {
            if (exception == null) {
                exception = e;
            }
        }
    }

    @Override
    public void run() {
        try {
            try {
                grabber = OpenCVFrameGrabber.createDefault(camera); //parameter 0 default camera , 1 for secondary
                grabber.setImageWidth(700);
                grabber.setImageHeight(700);
                grabber.start();
                grabbedImage = grabberConverter.convert(grabber.grab());
                storage = CvMemStorage.create();
            } catch (FrameGrabber.Exception e) {
                if (grabber != null) {
                    grabber.release();
                }
                grabber = new OpenCVFrameGrabber(0);
                grabber.setImageWidth(700);
                grabber.setImageHeight(700);
                grabber.start();
                grabbedImage = grabberConverter.convert(grabber.grab());

            }
            grayImage = cvCreateImage(cvGetSize(grabbedImage), 8, 1); //converting image to grayscale

            //reducing the size of the image to speed up the processing
            smallImage = cvCreateImage(cvSize(grabbedImage.width() / 4, grabbedImage.height() / 4), 8, 1);

            stop = false;

            while (!stop && (grabbedImage = grabberConverter.convert(grabber.grab())) != null) {

                Frame frame = grabberConverter.convert(grabbedImage);
                BufferedImage image = paintConverter.getBufferedImage(frame, 2.2 / grabber.getGamma());
                Graphics2D g2 = image.createGraphics();

                if (faces == null) {
                    cvClearMemStorage(storage);

                    //creating a temporary image
                    temp = cvCreateImage(cvGetSize(grabbedImage), grabbedImage.depth(), grabbedImage.nChannels());

                    cvCopy(grabbedImage, temp);

                    cvCvtColor(grabbedImage, grayImage, CV_BGR2GRAY);
                    cvResize(grayImage, smallImage, CV_INTER_AREA);

                    //cvHaarDetectObjects(image, cascade, storage, scale_factor, min_neighbors, flags, min_size, max_size)
                    faces = cvHaarDetectObjects(smallImage, classifier, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
                    //face detection

                    CvPoint org = null;
                    if (grabbedImage != null) {

                        if (faces.total() == 0) {
                            faces = cvHaarDetectObjects(smallImage, classifierSideFace, storage, 1.1, 3,
                                    CV_HAAR_DO_CANNY_PRUNING);
                        }

                        if (faces != null) {
                            g2.setColor(Color.green);
                            g2.setStroke(new BasicStroke(2));
                            int total = faces.total();

                            for (int i = 0; i < total; i++) {

                                //printing rectange box where face detected frame by frame
                                CvRect r = new CvRect(cvGetSeqElem(faces, i));
                                g2.drawRect((r.x() * 4), (r.y() * 4), (r.width() * 4), (r.height() * 4));

                                CvRect re = new CvRect((r.x() * 4), r.y() * 4, (r.width() * 4), r.height() * 4);

                                cvSetImageROI(temp, re);

                                // File f = new File("captures.png");
                                org = new CvPoint(r.x(), r.y());

                                if (isRecFace) {
                                    this.recogniseCode = faceRecognizer.recognize(temp);
                                    Student st = new Student();
                                    for (Student s : output) {
                                        if (this.recogniseCode == s.getId()) {
                                            st = s;
                                            SampleController.student = s;
                                            break;
                                        }
                                    }

                                    //printing recognised person name into the frame
                                    g2.setColor(Color.GREEN);
                                    g2.setFont(new Font("Arial Black", Font.BOLD, 20));
                                    String names = st.getFirstame() + " " + st.getLastname();
                                    g2.drawString(names, (int) (r.x() * 6), r.y() * 4);
                                }

                                if (saveFace) { //saving captured face to the disk
                                    //keep it in mind that face code should be unique to each person
                                    String imageName = "faces/" + student.getId() + "-" + student.getFirstame() + "_"
                                            + student.getLastname() + "-" + count + ".jpg";
                                    cvSaveImage(imageName, temp);
                                }

                            }
                            this.saveFace = false;
                            faces = null;
                        }

                        WritableImage showFrame = SwingFXUtils.toFXImage(image, null);

                        frames.setImage(showFrame);
                    }
                    cvReleaseImage(temp);
                }

            }

        } catch (FrameGrabber.Exception e) {
            if (exception == null) {
                exception = e;
            }
        }
    }

    public void stop() {
        stop = true;

        grabbedImage = grayImage = smallImage = null;
        try {
            grabber.stop();
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {

            e.printStackTrace();
        }
        try {
            grabber.release();
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {

            e.printStackTrace();
        }
        grabber = null;
    }

    public void setClassifier(String name) {

        try {

            setClassiferName(name);
            classifierFile = Loader.extractResource(classiferName, null, "classifier", ".xml");

            if (classifierFile == null || classifierFile.length() <= 0) {
                throw new IOException("Could not extract \"" + classiferName + "\" from Java resources.");
            }

            // Preload the opencv_objdetect module to work around a known bug.
            Loader.load(opencv_objdetect.class);
            classifier = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
            classifierFile.delete();
            if (classifier.isNull()) {
                throw new IOException("Could not load the classifier file.");
            }

        } catch (IOException e) {
            if (exception == null) {
                exception = e;
            }
        }

    }

    public void setClassifierEye(String name) {

        try {

            classiferName = name;
            classifierFile = Loader.extractResource(classiferName, null, "classifier", ".xml");

            if (classifierFile == null || classifierFile.length() <= 0) {
                throw new IOException("Could not extract \"" + classiferName + "\" from Java resources.");
            }

            // Preload the opencv_objdetect module to work around a known bug.
            Loader.load(opencv_objdetect.class);
            classifierEye = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
            classifierFile.delete();
            if (classifier.isNull()) {
                throw new IOException("Could not load the classifier file.");
            }

        } catch (IOException e) {
            if (exception == null) {
                exception = e;

            }
        }

    }

    public void setClassifierSmile(String name) {

        try {

            setClassiferName(name);
            classifierFile = Loader.extractResource(classiferName, null, "classifier", ".xml");

            if (classifierFile == null || classifierFile.length() <= 0) {
                throw new IOException("Could not extract \"" + classiferName + "\" from Java resources.");
            }

            // Preload the opencv_objdetect module to work around a known bug.
            Loader.load(opencv_objdetect.class);
            classifierSmile = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
            classifierFile.delete();
            if (classifier.isNull()) {
                throw new IOException("Could not load the classifier file.");
            }

        } catch (IOException e) {
            if (exception == null) {
                exception = e;

            }
        }

    }

    public void printResult(CvSeq data, int total, Graphics2D g2) {
        for (int j = 0; j < total; j++) {
            CvRect eye = new CvRect(cvGetSeqElem(eyes, j));

            g2.drawOval((eye.x() * 4), (eye.y() * 4), (eye.width() * 4), (eye.height() * 4));

        }
    }

    public void setClassifierSideFace(String name) {

        try {

            classiferName = name;
            classifierFile = Loader.extractResource(classiferName, null, "classifier", ".xml");

            if (classifierFile == null || classifierFile.length() <= 0) {
                throw new IOException("Could not extract \"" + classiferName + "\" from Java resources.");
            }

            // Preload the opencv_objdetect module to work around a known bug.
            Loader.load(opencv_objdetect.class);
            classifierSideFace = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
            classifierFile.delete();
            if (classifier.isNull()) {
                throw new IOException("Could not load the classifier file.");
            }

        } catch (IOException e) {
            if (exception == null) {
                exception = e;

            }
        }

    }

    public void setClassifierFullBody(String name) {

        try {

            setClassiferName(name);
            classifierFile = Loader.extractResource(classiferName, null, "classifier", ".xml");

            if (classifierFile == null || classifierFile.length() <= 0) {
                throw new IOException("Could not extract \"" + classiferName + "\" from Java resources.");
            }

            // Preload the opencv_objdetect module to work around a known bug.
            Loader.load(opencv_objdetect.class);
            classifierFullBody = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
            classifierFile.delete();
            if (classifier.isNull()) {
                throw new IOException("Could not load the classifier file.");
            }

        } catch (IOException e) {
            if (exception == null) {
                exception = e;

            }
        }

    }

    public void setClassifierEyeGlass(String name) {
        try {
            setClassiferName(name);
            classifierFile = Loader.extractResource(classiferName, null, "classifier", ".xml");

            if (classifierFile == null || classifierFile.length() <= 0) {
                throw new IOException("Could not extract \"" + classiferName + "\" from Java resources.");
            }

            // Preload the opencv_objdetect module to work around a known bug.
            Loader.load(opencv_objdetect.class);
            classifierEyeglass = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
            classifierFile.delete();
            if (classifier.isNull()) {
                throw new IOException("Could not load the classifier file.");
            }

        } catch (IOException e) {
            if (exception == null) {
                exception = e;
            }
        }

    }

    public void setClassifierUpperBody(String name) {

        try {

            classiferName = name;
            classifierFile = Loader.extractResource(classiferName, null, "classifier", ".xml");

            if (classifierFile == null || classifierFile.length() <= 0) {
                throw new IOException("Could not extract \"" + classiferName + "\" from Java resources.");
            }

            // Preload the opencv_objdetect module to work around a known bug.
            Loader.load(opencv_objdetect.class);
            classifierUpperBody = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
            classifierFile.delete();
            if (classifier.isNull()) {
                throw new IOException("Could not load the classifier file.");
            }

        } catch (IOException e) {
            if (exception == null) {
                exception = e;

            }
        }

    }

    public String getClassiferName() {
        return classiferName;
    }

    public void setClassiferName(String classiferName) {
        this.classiferName = classiferName;
    }

    public void setFrames2(ImageView frames2) {
        this.frames2 = frames2;
    }

    public void setSmile(boolean isSmile) {
        this.isSmile = isSmile;
    }

    public void setUpperBody(boolean isUpperBody) {
        this.isUpperBody = isUpperBody;
    }

    public void setFullBody(boolean isFullBody) {
        this.isFullBody = isFullBody;
    }

    public boolean isEyeDetection() {

        return isEyeDetection;
    }

    public void setEyeDetection(boolean isEyeDetection) {
        this.isEyeDetection = isEyeDetection;
    }

    public boolean getOcrMode() {
        return isOcrMode;
    }

    public void setOcrMode(boolean isOcrMode) {
        this.isOcrMode = isOcrMode;
    }

    public void destroy() {
    }

    public boolean isMotion() {
        return isMotion;
    }

    public void setMotion(boolean isMotion) {
        this.isMotion = isMotion;
    }

    public ArrayList<Student> getOutput() {
        return output;
    }

    public void clearOutput() {
        this.output.clear();
    }

    public void setOutput(ArrayList<Student> output) {
        this.output = output;
    }

    public int getRecogniseCode() {
        return recogniseCode;
    }

    public void setRecogniseCode(int recogniseCode) {
        this.recogniseCode = recogniseCode;
    }

    public void setFrame(ImageView frame) {
        this.frames = frame;
    }

    public void setSaveFace(Boolean f) {
        this.saveFace = f;
    }

    public Boolean getIsRecFace() {
        return isRecFace;
    }

    public void setIsRecFace(Boolean isRecFace) {
        this.isRecFace = isRecFace;
    }

}
