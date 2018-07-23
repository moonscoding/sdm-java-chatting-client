import javafx.scene.control.cell.PropertyValueFactory;
import model.Client;
import model.Room;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import model.Token;


public class Main extends Application {

    Client client;
    TextArea txtDisplay;
    TextField txtInput;
    TextField txtId;
    Button btnSend;
    Button btnCreate;
    Button btnLeave;
    TableView<Room> tableView;
    ListView<Room> listView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start( Stage pStage ) throws Exception {

        // Root 생성
        BorderPane root = new BorderPane();
        root.setPrefSize(500, 300);

        // ==================== Center ====================

        BorderPane middle = new BorderPane();

        // Text Display 생성
        txtDisplay = new TextArea();
        txtDisplay.setEditable(false);
        BorderPane.setMargin(txtDisplay, new Insets(0,0,2,0));

        BorderPane middleBottom = new BorderPane();

        // btn leave 생성
        btnLeave = new Button("leave");
        btnLeave.setDisable(true);
        btnLeave.setPrefSize(300, 30);
        btnLeave.setOnAction(e -> {
            client.sendLeave();
        });
        middleBottom.setCenter(btnLeave);
        middle.setCenter(txtDisplay);
        middle.setBottom(middleBottom);
        root.setCenter(middle);

        // ==================== Top ====================

        // Top 영역 생성
        BorderPane top = new BorderPane();
        txtId = new TextField("id");
        txtId.setPrefSize(60,30);

        // 위치삽입
        top.setCenter(txtId);
        root.setTop(top);

        // ==================== Bottom ====================

        // Bottom 영역 생성
        BorderPane bottom = new BorderPane();
        txtInput = new TextField();
        txtInput.setPrefSize(60,30);
        BorderPane.setMargin(txtInput, new Insets(0,1,1,1));

        // 방생성버튼 & 클릭리스너
        btnCreate = new Button("create");
        btnCreate.setPrefSize(60, 30);
        btnCreate.setOnAction(e -> {
            client.sendCreate(new Room("id", txtInput.getText(), 1 ));
            txtInput.setText("");
        });

        // 데이터전송버튼 생성 및 클릭리스너
        btnSend = new Button("send");
        btnSend.setPrefSize(60, 30);
        btnSend.setDisable(true);
        btnSend.setOnAction(e -> {
            client.sendChat(txtInput.getText());
            txtInput.setText("");
        });

        // 위치삽입
        bottom.setCenter(txtInput);
        bottom.setLeft(btnCreate);
        bottom.setRight(btnSend);
        root.setBottom(bottom);

        // ==================== Left ====================

        listView = new ListView();
        listView.setPrefSize(100, 240);
        // listView.setItems(FXCollections.observableArrayList());
        listView.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<Room>() {
                    @Override
                    public void changed(ObservableValue<? extends Room> observable, Room oldValue, Room newValue) {
                        // System.out.println(newValue.id);
                        if(newValue != null) client.sendEntry( newValue );
                    }
                }
        );
        root.setLeft(listView);

        // Scene처리
        Scene scene = new Scene(root);
        pStage.setScene(scene);
        pStage.setTitle("채팅 대기중");
        pStage.setOnCloseRequest(e -> client.stopClient());
        pStage.show();

        client = new Client(txtDisplay, txtInput, txtId, btnCreate, btnSend, listView, btnLeave);
    }
}
