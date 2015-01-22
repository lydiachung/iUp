package iup;

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class IUpUI extends Application {
	
	protected static TextArea textArea;
	protected static ProgressBar progressBar;

	@Override
    public void start(Stage primaryStage) {
		
		BorderPane root = new BorderPane();
		
		
		// ---------------------------------------------------
		// text area
        textArea = new TextArea();
        textArea.setEditable(false);
        
        SimpleStringProperty messageProperty = new SimpleStringProperty();
        messageProperty.addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, final String arg2) {
				if (arg2 == null) {
					return;
				}
				
				textArea.appendText(arg2);
				textArea.appendText("\n");
			}
        });
        
        
        // ---------------------------------------------------
        // progress bar
        progressBar = new ProgressBar();
        progressBar.setProgress(0.0);
        progressBar.prefWidthProperty().bind(root.widthProperty());
        
        SimpleDoubleProperty progressProperty = new SimpleDoubleProperty();
        progressProperty.addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<?> arg0, Object arg1, Object arg2) {
				progressBar.setProgress((double) arg2);
			}
        });
        
        
        final IUp appUpdater = new IUp(messageProperty, progressProperty);
        
        
        // ---------------------------------------------------
        // stage & root
        root.setCenter(textArea);
        root.setBottom(progressBar);
        
        primaryStage.setTitle("iUp!");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
        
        
        // ---------------------------------------------------
        // main task
        new Thread(new Runnable() {
			@Override
			public void run() {
				appUpdater.getLatestArtifact();
			}
        }).start();;
        
		
    }
	
	public static void main(String[] args) {
		launch(args);
	}

}
