package app.controllers;

import app.LoginController;
import com.jfoenix.controls.JFXButton;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import utilities.Constants;

/**
 * FXML Controller class
 *
 * @author Tony Manjarres
 */
public class HomeController implements Initializable {

    @FXML
    private Label lblName, lblUsername;

    @FXML
    private ImageView imAvatar;

    @FXML
    private Pane avatarPane;

    @FXML
    private VBox vbRepos, vbFollowers;

    @FXML
    private Label lblRepoName, lblPrivate, lblRepoLanguage;

    @FXML
    private Label lblRepoDesc, lblRepoCreation, lblRepoParent, lblBranches;

    @FXML
    private JFXButton btnDeleteRepo;

    //
    private double xPos, yPos;
    public static GHRepository SELECTED = null;

    @FXML
    private void closeWindow(MouseEvent event) {
        System.exit(0);
    }

    @FXML
    private void hideWindow(MouseEvent event) {
        Node node = (Node) event.getSource();
        Stage stage = (Stage) node.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void logOut(MouseEvent event) {
        try {
            Node node = (Node) event.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            stage.close();
            URL path = getClass().getResource(Constants.FXML_LOGIN);
            if (path != null) {
                Parent root = FXMLLoader.load(path);
                root.setOnMousePressed((value) -> {
                    xPos = value.getSceneX();
                    yPos = value.getSceneY();
                });

                root.setOnMouseDragged((value) -> {
                    stage.setX(value.getScreenX() - xPos);
                    stage.setY(value.getScreenY() - yPos);
                });

                LoginController.GH_USER = null;
                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);

                stage.setTitle(Constants.APP_TITLE);
                stage.setScene(scene);
                stage.show();
            } else {
                throw new IOException("Error Loading Login FXML File.");
            }
        } catch (IOException e) {
            System.err.println("Error With Log Out: " + e.getMessage());
        }
    }

    @FXML
    private void deleteRepo(MouseEvent event) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Repository Deletion Confirmation");
        alert.setContentText("Are you sure you want to delete this repository?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            GHRepository selected = SELECTED;
            if (selected != null) try {
                selected.delete();
                clearDetails(null);
                loadRepositories(LoginController.GH_USER);
            } catch (IOException ex) {
                Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @FXML
    private void clearDetails(MouseEvent event) {
        lblRepoName.setText("");
        lblRepoDesc.setText("");
        lblRepoCreation.setText("");
        lblRepoLanguage.setText("");
        lblRepoParent.setText("");
        lblBranches.setText("");
        btnDeleteRepo.setDisable(true);
        SELECTED = null;
    }

    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location The location used to resolve relative paths for the root
     * object, or
     * <tt>null</tt> if the location is not known.
     * @param resources The resources used to localize the root object, or
     * <tt>null</tt> if
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO
        clearDetails(null);
        GHPerson user = LoginController.GH_USER;
        try {
            String name = (user.getName() != null && !user.getName().isEmpty() ? user.getName() : "No Name Available");
            lblName.setText(name);
            lblUsername.setText("@" + user.getLogin());
            Image image = new Image(user.getAvatarUrl());
            imAvatar.setImage(image);
            avatarPane.setMinWidth(image.getRequestedWidth());
            avatarPane.setMaxWidth(image.getRequestedWidth());
            loadRepositories(user);
            loadFollowers((GHUser) user);
        } catch (IOException ex) {
            Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
        }
        initHandler();
    }

    private void initHandler() {
        // TODO: Implement the observer pattern to avoid using this approach
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), (event) -> {
            GHRepository selected = SELECTED;
            if (selected != null) try {
                btnDeleteRepo.setDisable(false);
                lblRepoName.setText(selected.getName());
                lblRepoLanguage.setText(selected.getLanguage());
                lblBranches.setText(String.valueOf(selected.getBranches().size()));
                String desc = selected.getDescription();
                lblRepoDesc.setText("No Description Available.");
                if (desc != null && !desc.isEmpty()) {
                    lblRepoDesc.setText(desc);
                }
                lblPrivate.setText(selected.isPrivate() ? "Private" : "Public");
                lblRepoCreation.setText(selected.getCreatedAt().toString().substring(0, 10));
                lblRepoParent.setText("Does Not Have");
                if (selected.isFork()) {
                    GHRepository parent = selected.getParent();
                    lblRepoParent.setText(parent.getOwnerName());
                }
            } catch (IOException e) {
                System.err.println("Error Rendering Repo Details: " + e.getMessage());
            } finally {
                SELECTED = null;
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void loadRepositories(GHPerson user) throws IOException {
        vbRepos.getChildren().clear();
        for (String key : user.getRepositories().keySet()) {
            GHRepository repo = user.getRepository(key);
            if (repo != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(Constants.FXML_REPO_ITEM));
                RepoItemController controller = new RepoItemController();
                loader.setController(controller);
                vbRepos.getChildren().add(loader.load());
                controller.setRepository(repo);
            }
        }
    }

    private void loadFollowers(GHUser user) throws IOException {
        vbFollowers.getChildren().clear();
        for (GHUser follower : user.getFollowers()) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(Constants.FXML_FOLLOWER_ITEM));
            FollowerItemController controller = new FollowerItemController();
            loader.setController(controller);
            vbFollowers.getChildren().add(loader.load());
            controller.setFollower(user, follower);
        }
    }
}
