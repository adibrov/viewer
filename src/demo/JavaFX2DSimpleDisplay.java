package demo;


import image.DirectAccessImageByteGray;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.nio.ByteBuffer;

/**
 * Created by dibrov on 12/04/17.
 */
public class JavaFX2DSimpleDisplay extends Application {
    private ImageView imageView_Source;
    private DirectAccessImageByteGray imgIn;


    public JavaFX2DSimpleDisplay(DirectAccessImageByteGray img) {
        this.imgIn = img;
    }

    @Override
    public void start(Stage primaryStage) {


        imageView_Source = new ImageView();
        imageView_Source.setImage(imgIn);

        HBox hBoxImage = new HBox();
        hBoxImage.getChildren().addAll(imageView_Source);


        StackPane root = new StackPane();
        root.getChildren().add(hBoxImage);
        Scene scene = new Scene(root, 1400, 1000);
        primaryStage.setTitle("java-buddy.blogspot.com");
        primaryStage.setScene(scene);
        primaryStage.show();


    }

    public void setImgIn(DirectAccessImageByteGray imgIn) {

        this.imgIn = imgIn;
        imageView_Source.setImage(this.imgIn);
    }

    public void run(String[] args) {
        launch(args);
    }

    public static void main(String[] args) {
        DirectAccessImageByteGray black = new DirectAccessImageByteGray(500, 500);
        DirectAccessImageByteGray white = new DirectAccessImageByteGray(500, 500);
        white.setBufferUniform((byte) 127);

        ByteBuffer blackBuf = ByteBuffer.allocate(500 * 500);

        ByteBuffer whiteBuf = ByteBuffer.allocate(500 * 500);
        blackBuf.rewind();
        whiteBuf.rewind();
        for (int i = 0; i < whiteBuf.array().length; i++) {
            whiteBuf.array()[i] = (byte) 255;
            blackBuf.put((byte) 0);
        }

        blackBuf.rewind();
        whiteBuf.rewind();


        JavaFX2DSimpleDisplay display = new JavaFX2DSimpleDisplay(black);

        new JFXPanel();
        Platform.runLater(() -> {
            Stage s = new Stage();
            display.start(s);
        });

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean currImgBlack = true;
        while (true) {
//            System.out.println("in the loop");
            try {
                Thread.sleep(15);
                black.setBuffer(currImgBlack ? whiteBuf : blackBuf);
//                display.setImgIn(currImgBlack?white:black);

                currImgBlack = !currImgBlack;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
