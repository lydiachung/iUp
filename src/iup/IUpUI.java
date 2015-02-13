package iup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class IUpUI extends Application {

	@Override
    public void start(final Stage stage) {
		
		String lastVersion = null;
		
		try {
			lastVersion = loadLastVersion();
		} catch (Exception e) {
			e.printStackTrace();
			closeApp(stage);
		}
		
		final BorderPane borderPane = new BorderPane();
		final TextField textField = new TextField(lastVersion);
		final SimpleStringProperty messageProperty = new SimpleStringProperty();
		final SimpleDoubleProperty progressProperty = new SimpleDoubleProperty();
		
		borderPane.setPadding(new Insets(20));
		borderPane.setCenter(createLabelAndTextField(textField));
		borderPane.setBottom(createButtons(stage, borderPane, messageProperty, progressProperty, textField));

		// ---------------------------------------------------
        // stage & root
		Scene scene = new Scene(borderPane);
		scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent keyEvent) {
				if (keyEvent.getCode() == KeyCode.ESCAPE) {
					closeApp(stage);
				} else if (keyEvent.getCode() == KeyCode.ENTER) {
					if (textField.isVisible()) {
						try {
							retrieveLatestArtifact(stage, borderPane, messageProperty, progressProperty, textField);
						} catch (IOException e) {
							e.printStackTrace();
							closeApp(stage);
						}	
					}
				}
			}
		});
		
        stage.setTitle("iUp!");
        stage.setScene(scene); 
        stage.setOnCloseRequest(new EventHandler<WindowEvent>(){
			@Override
			public void handle(WindowEvent arg0) {
				System.exit(0);
			}
        });
        
        stage.show();
		
    }
	
	public HBox createLabelAndTextField(TextField textField) {
		
		Label label = new Label("Version: ");
		
		HBox hBox = new HBox();
		hBox.setSpacing(10);
		hBox.getChildren().addAll(label, textField);
		
		return hBox;
		
	}
	
	public HBox createButtons(final Stage stage, final BorderPane root, final SimpleStringProperty messageProperty, final SimpleDoubleProperty progressProperty, final TextField textField) {
		
		Button okButton, cancelButton;
		
		okButton = new Button("OK");
		okButton.setPrefWidth(70);
		
		cancelButton = new Button("Cancel");
		cancelButton.setPrefWidth(70);
		
		okButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
		        try {
					retrieveLatestArtifact(stage, root, messageProperty, progressProperty, textField);
				} catch (IOException e) {
					e.printStackTrace();
					closeApp(stage);
				}
			}
		});
		
		cancelButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				closeApp(stage);
			}
		});
		
		HBox hBox = new HBox();
		hBox.getChildren().addAll(okButton, cancelButton);
		hBox.setAlignment(Pos.BOTTOM_CENTER);
		hBox.setSpacing(10); 
		hBox.setPadding(new Insets(20, 0, 0, 0));
		
		return hBox;
	}
	
	public TextArea createTextArea(SimpleStringProperty messageProperty) {
		// ---------------------------------------------------
		// text area
        final TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setPrefSize(800, 600);
        
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
        
        return textArea;
	}
	
	public ProgressBar createProgressBar(SimpleDoubleProperty progressProperty, ReadOnlyDoubleProperty widthProperty) {
		// ---------------------------------------------------
        // progress bar
		final ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(0.0);
        progressBar.prefWidthProperty().bind(widthProperty);
        
        progressProperty.addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<?> arg0, Object arg1, Object arg2) {
				progressBar.setProgress((double) arg2);
			}
        });
        return progressBar;
	}
	
	private void closeApp(Stage stage) {
		stage.close();
	}
	
	private void retrieveLatestArtifact(Stage stage, BorderPane borderPane, final SimpleStringProperty messageProperty, final SimpleDoubleProperty progressProperty, final TextField textField) throws IOException {
		
		textField.setVisible(false);
		borderPane.setCenter(createTextArea(messageProperty));
        borderPane.setBottom(createProgressBar(progressProperty, borderPane.widthProperty()));	
        stage.sizeToScene();
        
        Files.write(Paths.get("version.txt"), textField.getText().getBytes());
        
        new Thread(new Runnable() {
			@Override
			public void run() {
				IUp iUp = new IUp(messageProperty, progressProperty, textField.getText());
				iUp.getLatestArtifact();
			}
        }).start();;
	}
	
	private String loadLastVersion() throws IOException {
		Path path = Paths.get("version.txt");
		if (Files.exists(path)) {
			byte[] encoded = Files.readAllBytes(path);
			return new String(encoded);
		} else {
			return "";
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
