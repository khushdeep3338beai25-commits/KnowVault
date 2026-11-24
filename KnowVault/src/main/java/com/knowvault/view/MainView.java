package com.knowvault.view;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class MainView {
    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final Map<String, Button> navigation = new LinkedHashMap<>();

    public MainView(Consumer<String> onNavigate) {
        VBox side = new VBox(12);
        side.getStyleClass().add("sidebar"); side.setPadding(new Insets(28,18,24,18)); side.setPrefWidth(220);
        Label brand = new Label("KNOWVAULT"); brand.getStyleClass().add("brand");
        Label subtitle = new Label("SMART NOTES"); subtitle.getStyleClass().add("brand-subtitle");
        side.getChildren().addAll(brand, subtitle, spacer(18));
        addNavigation(side,"Dashboard","◈",onNavigate); addNavigation(side,"Notes","✎",onNavigate);
        addNavigation(side,"Favorites","★",onNavigate); addNavigation(side,"Analytics","⌁",onNavigate);
        Region grow = new Region(); VBox.setVgrow(grow,Priority.ALWAYS);
        Label local = new Label("●  OFFLINE • PRIVATE"); local.getStyleClass().add("offline-badge");
        side.getChildren().addAll(grow,local);
        content.getStyleClass().add("content-area"); content.setPadding(new Insets(26));
        root.setLeft(side); root.setCenter(content); root.getStyleClass().add("app-root");
    }
    private void addNavigation(VBox side,String name,String icon,Consumer<String> action){
        Button button=new Button(icon+"   "+name); button.getStyleClass().add("nav-button"); button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e->action.accept(name)); navigation.put(name,button); side.getChildren().add(button);
    }
    private Region spacer(double height){ Region r=new Region();r.setMinHeight(height);return r; }
    public BorderPane root(){return root;}
    public StackPane content(){return content;}
    public void select(String page){ navigation.forEach((name,button)->button.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"),name.equals(page))); }
    public void show(Node node){ content.getChildren().setAll(node); node.setOpacity(0); node.setTranslateY(12); javafx.animation.FadeTransition f=new javafx.animation.FadeTransition(javafx.util.Duration.millis(260),node);f.setToValue(1); javafx.animation.TranslateTransition t=new javafx.animation.TranslateTransition(javafx.util.Duration.millis(260),node);t.setToY(0);new javafx.animation.ParallelTransition(f,t).play(); }
}
