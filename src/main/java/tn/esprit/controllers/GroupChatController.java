package tn.esprit.controllers;

import tn.esprit.utils.SessionManager;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.ChatMessage;
import tn.esprit.entities.Evenements;
import tn.esprit.services.ChatService;
import tn.esprit.services.ParticipationService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class GroupChatController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox messagesContainer;
    @FXML private Label chatTitleLabel;
    @FXML private Label chatSubtitleLabel;
    @FXML private Label memberCountLabel;
    @FXML private VBox usernameRow;
    @FXML private TextField usernameField;
    @FXML private Label usernameErrorLabel;
    @FXML private HBox messageRow;
    @FXML private TextField messageField;
    @FXML private Label participantCountLabel;

    private final ChatService          chatService          = new ChatService();
    private final ParticipationService participationService = new ParticipationService();

    private Evenements event;
    private String     currentUser;
    private Thread     pollThread;
    private Timestamp  lastSeen;

    private final Map<String, String> participantColors = new LinkedHashMap<>();

    private static final String[] PALETTE = {
            "#4F6EF7", "#8B5CF6", "#EC4899", "#22D3EE",
            "#F59E0B", "#14B8A6", "#F97316", "#6366F1", "#EF4444"
    };

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");



    public void setEvent(Evenements event) {
        this.event = event;
        if (chatTitleLabel != null)
            chatTitleLabel.setText("ðŸ’¬  " + event.getTitre_evenement());
        if (chatSubtitleLabel != null)
            chatSubtitleLabel.setText("Chat privÃ© Â· participants inscrits uniquement");

        new Thread(() -> {
            try {
                int count = participationService.countByEvent(event.getId_evenemnt());
                Platform.runLater(() -> {
                    if (participantCountLabel != null)
                        participantCountLabel.setText(count + " participant" + (count > 1 ? "s" : "") + " inscrit" + (count > 1 ? "s" : ""));
                });
            } catch (Exception ignored) {}
        }).start();

        // â”€â”€ Connexion automatique depuis la session â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (SessionManager.isLoggedIn()) {
            String nomComplet = SessionManager.getNomComplet().trim();
            if (!nomComplet.isEmpty()) {
                // Bypass l'Ã©cran de saisie du nom
                Platform.runLater(() -> accessGranted(nomComplet));
            }
        }
    }



    @FXML
    private void setUsername() {
        String name = usernameField.getText().trim();
        if (name.length() < 2) {
            showUsernameError("Le nom doit contenir au moins 2 caractÃ¨res.");
            shake(usernameField);
            return;
        }


        new Thread(() -> {
            try {
                boolean isParticipant = participationService.isParticipantByName(name, event.getId_evenemnt());
                Platform.runLater(() -> {
                    if (isParticipant) {
                        accessGranted(name);
                    } else {
                        showUsernameError("âŒ  Vous n'Ãªtes pas inscrit(e) Ã  cet Ã©vÃ©nement.");
                        shake(usernameField);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showUsernameError("Erreur de vÃ©rification : " + e.getMessage()));
            }
        }).start();
    }

    private void accessGranted(String name) {
        currentUser = name;
        hideUsernameError();

        usernameRow.setVisible(false);
        usernameRow.setManaged(false);
        messageRow.setVisible(true);
        messageRow.setManaged(true);

        memberCountLabel.setText(name + "  Â·  ðŸŸ¢ En ligne");
        loadHistory();
        startPolling();
        Platform.runLater(() -> messageField.requestFocus());

        appendSystemMessage("âœ…  Bienvenue " + name + " ! Vous Ãªtes dans le chat privÃ© de l'Ã©vÃ©nement.");
    }



    private void loadHistory() {
        try {
            List<ChatMessage> history = chatService.getLastMessages(event.getId_evenemnt(), 60);
            if (!history.isEmpty()) {
                lastSeen = history.get(history.size() - 1).getSentAt();
                for (ChatMessage msg : history)
                    appendBubble(msg, false);
                scrollToBottom();
            } else {
                lastSeen = new Timestamp(System.currentTimeMillis());
                appendSystemMessage("Soyez le premier Ã  Ã©crire ! ðŸ‘‹");
            }
        } catch (SQLException e) {
            appendSystemMessage("âš  Impossible de charger l'historique.");
        }
    }



    private void startPolling() {
        pollThread = chatService.startPolling(
                event.getId_evenemnt(), lastSeen, this::onNewMessages
        );
    }

    private void onNewMessages(List<ChatMessage> newMsgs) {
        for (ChatMessage msg : newMsgs) appendBubble(msg, true);
        if (!newMsgs.isEmpty()) {
            lastSeen = newMsgs.get(newMsgs.size() - 1).getSentAt();
            scrollToBottom();
        }
    }



    @FXML
    private void sendMessage() {
        if (currentUser == null) return;
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        messageField.clear();
        new Thread(() -> {
            try {
                chatService.sendMessage(event.getId_evenemnt(), currentUser, text);
            } catch (SQLException e) {
                Platform.runLater(() -> appendSystemMessage("âš  Erreur d'envoi : " + e.getMessage()));
            }
        }).start();
    }



    @FXML
    private void closeChat() {
        if (pollThread != null) pollThread.interrupt();
        Stage stage = (Stage) messagesContainer.getScene().getWindow();
        stage.close();
    }



    private void appendBubble(ChatMessage msg, boolean animate) {
        boolean isMine = msg.getSenderName().equals(currentUser);
        String color = isMine ? "#52B788" : getColorForUser(msg.getSenderName());

        HBox row = new HBox(8);
        row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));

        if (!isMine) {
            StackPane avatar = new StackPane();
            avatar.setMinSize(30, 30); avatar.setMaxSize(30, 30);
            avatar.setStyle("-fx-background-color:" + color + "28;" +
                    "-fx-background-radius:50;" +
                    "-fx-border-color:" + color + "66;" +
                    "-fx-border-width:1.5; -fx-border-radius:50;");
            Label initial = new Label(String.valueOf(msg.getSenderName().charAt(0)).toUpperCase());
            initial.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12px;-fx-font-weight:800;");
            avatar.getChildren().add(initial);
            row.getChildren().add(avatar);
        }

        VBox bubble = new VBox(3);
        bubble.setMaxWidth(300);
        bubble.setPadding(new Insets(8, 12, 8, 12));

        if (isMine) {
            bubble.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #1B4332, #2D6A4F);" +
                            "-fx-background-radius: 14 4 14 14;" +
                            "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.30), 6, 0, 0, 2);");
        } else {
            bubble.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.08);" +
                            "-fx-background-radius: 4 14 14 14;" +
                            "-fx-border-color: rgba(255,255,255,0.10);" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 4 14 14 14;");
        }

        if (!isMine) {
            Label senderLbl = new Label(msg.getSenderName());
            senderLbl.setStyle("-fx-font-size:10px;-fx-font-weight:800;-fx-text-fill:" + color + ";");
            bubble.getChildren().add(senderLbl);
        }

        Label textLbl = new Label(msg.getMessage());
        textLbl.setWrapText(true);
        textLbl.setStyle(isMine
                ? "-fx-text-fill:white;-fx-font-size:13px;"
                : "-fx-text-fill:rgba(255,255,255,0.85);-fx-font-size:13px;");
        bubble.getChildren().add(textLbl);

        String timeStr = msg.getSentAt() != null
                ? msg.getSentAt().toLocalDateTime().format(TIME_FMT) : "";
        Label timeLbl = new Label(timeStr);
        timeLbl.setStyle("-fx-font-size:9.5px;" +
                (isMine ? "-fx-text-fill:rgba(255,255,255,0.40);" : "-fx-text-fill:rgba(255,255,255,0.30);"));
        timeLbl.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubble.getChildren().add(timeLbl);

        row.getChildren().add(bubble);
        messagesContainer.getChildren().add(row);

        if (animate) {
            FadeTransition fade = new FadeTransition(Duration.millis(220), row);
            fade.setFromValue(0); fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(220), row);
            slide.setFromY(10); slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fade, slide).play();
        }
    }

    private void appendSystemMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(6, 0, 6, 0));
        Label lbl = new Label(text);
        lbl.setStyle("-fx-background-color:rgba(255,255,255,0.06);" +
                "-fx-background-radius:20;-fx-padding:4 14;" +
                "-fx-text-fill:rgba(255,255,255,0.45);-fx-font-size:11px;-fx-font-style:italic;" +
                "-fx-border-color:rgba(255,255,255,0.10);-fx-border-width:1;-fx-border-radius:20;");
        row.getChildren().add(lbl);
        messagesContainer.getChildren().add(row);
    }



    private void showUsernameError(String msg) {
        if (usernameErrorLabel != null) {
            usernameErrorLabel.setText(msg);
            usernameErrorLabel.setVisible(true);
            usernameErrorLabel.setManaged(true);
        }
    }

    private void hideUsernameError() {
        if (usernameErrorLabel != null) {
            usernameErrorLabel.setVisible(false);
            usernameErrorLabel.setManaged(false);
        }
    }



    private String getColorForUser(String name) {
        return participantColors.computeIfAbsent(name, k ->
                PALETTE[participantColors.size() % PALETTE.length]);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void shake(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(4);
        tt.setAutoReverse(true); tt.play();
    }
}
