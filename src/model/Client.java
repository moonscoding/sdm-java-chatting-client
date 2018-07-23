package model;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import util.Define;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.List;

public class Client {


    public SocketChannel socketChannel = null;
    TextArea txtDisplay;
    TextField txtInput;
    TextField txtId;
    Button btnCreate;
    Button btnSend;
    ListView listView;
    Button btnLeave;
    Room room;

    public Client(
            TextArea txtDisplay,
            TextField txtInput,
            TextField txtId,
            Button btnCreate,
            Button btnSend,
            ListView listView,
            Button btnLeave
            ) {
        this.txtDisplay = txtDisplay;
        this.txtInput = txtInput;
        this.txtId = txtId;
        this.btnCreate = btnCreate;
        this.btnSend = btnSend;
        this.listView = listView;
        this.btnLeave = btnLeave;
        startClient();
    }

    /**
     * [ Method :: startClient ]
     *
     * @DES ::
     * @IP1 ::
     * @O.P ::
     * @S.E ::
     * */
    public void startClient() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    socketChannel = SocketChannel.open();

                    // ### 블로킹방식 (명시적) ###
                    socketChannel.configureBlocking(true);

                    // ### connect(new InetSocketAddress) ###
                    socketChannel.connect(new InetSocketAddress(Define.HOST , Define.PORT));

                    Platform.runLater(()->{
                        displayText("[채팅클라이언트] 연결성공");
                    });

                } catch (IOException e) {
                    Platform.runLater(()->{ displayText("[채팅클라이언트] 서버통신두절");});
                    if(socketChannel.isOpen()) stopClient();
                }

                // 듣기동작
                receive();
            }
        };
        thread.start();
    }

    /**
     * [ Method :: stopClient ]
     *
     * @DES ::
     * @IP1 ::
     * @O.P ::
     * @S.E ::
     * */
    public void stopClient() {
        try {
            Platform.runLater(()->{
                displayText("[채팅클라이언트] 서버연결종료");
            });
            if(socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
        } catch (IOException e) {}
    }

    /**
     * [ Method :: receive ]
     *
     * @DES ::
     * @IP1 ::
     * @O.P ::
     * @S.E ::
     * */
    public void receive() {
        while(true) {
            try {
                ByteBuffer bb = ByteBuffer.allocate(Define.BUFFER_SIZE);
                Charset cs = Charset.forName("UTF-8");

                // ### read(ByteButter) ###
                int byteCount = socketChannel.read(bb);
                if(byteCount == -1) throw new IOException();
                bb.flip();
                String strJson = cs.decode(bb).toString();
                System.out.println(strJson);

                try {
                    JSONParser jsonParser = new JSONParser();
                    JSONObject token = (JSONObject) jsonParser.parse(strJson);
                    String method = token.get("method").toString();

                    System.out.println("[채팅클라이언트] 요청메소드 " + method);
                    switch (method) {
                        case "/room/status":
                            Platform.runLater(()->{
                                // init listView
                                listView.getItems().clear();

                                // append listView
                                JSONArray rooms = (JSONArray) token.get("rooms");
                                for (int i = 0; i < rooms.size(); i++) {
                                    JSONObject room = (JSONObject) rooms.get(i);
                                    System.out.println(room.toString());
                                    listView.getItems().add(
                                            new Room(
                                                    room.get("id").toString(),
                                                    room.get("title").toString(),
                                                    0/*Integer.parseInt(room.get("size").toString())*/)
                                    );
                                }
                            });
                            break;
                        case "/chat/echo":
                            Platform.runLater(()->{ displayText("[채팅클라이언트] " + token.get("id") + " :: " + token.get("message") ); });
                            break;
                    }
                } catch (ParseException e) {
                    System.out.println("[채팅클라이언트] : 버퍼크기의 문제일 수 있습니다.");
                    e.printStackTrace();
                    printError();
                }
            } catch (IOException e) {
                printError();
                stopClient();
                break;
            }
        }
    }

    /**
     * [ Method :: sendCreate ]
     *
     * @DES ::
     * @IP1 ::
     * @O.P ::
     * @S.E ::
     * */
    public void sendCreate( Room room ) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Client.this.room = room;

                    // ### write(ByteButter) ###
                    String packet = String.format("{\"method\":\"%s\",\"title\":\"%s\"}", "/room/create", room.title);
                    Charset cs = Charset.forName("UTF-8");
                    ByteBuffer bb = cs.encode(packet);
                    socketChannel.write(bb);

                    // ### print() ###
                    printEntry(txtId.getText(), room.title);

                } catch (IOException e) {
                    Platform.runLater(()->{ displayText("[채팅클라이언트] 서버통신두절 in send"); });
                    stopClient();
                }
            }
        };
        thread.start();
    }

    /**
     * [ Method ::  ]
     *
     * @DES ::
     * @IP1 ::
     * @O.P ::
     * @S.E ::
     * */
    public void sendEntry( Room room ) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Client.this.room = room;

                    // ### write(ByteButter) ###
                    String packet = String.format("{\"method\":\"%s\",\"id\":\"%s\"}", "/room/entry", room.id);
                    Charset cs = Charset.forName("UTF-8");
                    ByteBuffer bb = cs.encode(packet);
                    socketChannel.write(bb);

                    // ### print() ###
                    printEntry(txtId.getText(), room.title);

                } catch (IOException e) {
                    printError();
                    stopClient();
                }
            }
        };
        thread.start();
    }

    /**
     * [ Method :: sendLeave ]
     *
     * @DES ::
     * @IP1 ::
     * @O.P ::
     * @S.E ::
     * */
    public void sendLeave() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {

                    // ### write(ByteButter) ###
                    String packet = String.format("{\"method\":\"%s\"}", "/room/leave");
                    Charset cs = Charset.forName("UTF-8");
                    ByteBuffer bb = cs.encode(packet);
                    socketChannel.write(bb);

                    // ### print() ###
                    printLeave();

                } catch (IOException e) {
                    printError();
                    stopClient();
                }
            }
        };
        thread.start();
    }

    /**
     * [ Method :: sendChat ]
     *
     * @DES ::
     * @IP1 ::
     * @O.P ::
     * @S.E ::
     * */
    public void sendChat(String message) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {

                    // ### write(ByteButter) ###
                    String packet = String.format("{\"method\":\"%s\",\"id\":\"%s\",\"message\":\"%s\"}", "/chat/send", txtId.getText(), message);
                    Charset cs = Charset.forName("UTF-8");
                    ByteBuffer bb = cs.encode(packet);
                    socketChannel.write(bb);

                    Platform.runLater(()->{ displayText("[채팅클라이언트] " + txtId.getText() + " :: " + message); });

                } catch (IOException e) {
                    printError();
                    stopClient();
                }
            }
        };
        thread.start();
    }

    void printEntry( String id, String title ) {
        Platform.runLater(()->{
            initText();
            displayText("[채팅클라이언트] \"" + title + "\"에 오신 것을 환영합니다. " + id + "님" );
            displayText("[채팅클라이언트] 채팅방 이름 : " + title );
            txtId.setDisable(true);
            btnCreate.setDisable(true);
            btnSend.setDisable(false);
            btnLeave.setDisable(false);
            listView.setDisable(true);
        });
    }

    void printLeave() {
        Platform.runLater(()->{
            initText();
            displayText("[채팅클라이언트] 채팅을 나갔습니다. 새로운 채팅방을 만들거나 찾아주세요." );
            txtInput.setText("");
            txtId.setDisable(false);
            btnCreate.setDisable(false);
            btnSend.setDisable(true);
            btnLeave.setDisable(true);
            listView.setDisable(false);
        });
    }

    void printError() {
        Platform.runLater(()->{
            initText();
            displayText("[채팅클라이언트] 서버에 문제가 발생했습니다. 다시 시도해주세요.");
        });
    }

    void initText() {
        txtDisplay.setText("");
    }

    void displayText(String txt) {
        txtDisplay.appendText(txt + "\n");
    }
}
