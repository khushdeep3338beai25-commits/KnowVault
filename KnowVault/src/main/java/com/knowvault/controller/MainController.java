package com.knowvault.controller;

import com.knowvault.exception.KnowVaultException;
import com.knowvault.model.*;
import com.knowvault.repository.CategoryRepository;
import com.knowvault.service.ExportService;
import com.knowvault.service.NoteService;
import com.knowvault.util.TextAnalyzer;
import com.knowvault.view.MainView;
import javafx.animation.*;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class MainController {
    private final Stage stage;
    private final NoteService notes;
    private final CategoryRepository categories;
    private final ExportService exports;
    private final MainView view;
    private final AtomicReference<Task<?>> runningTask = new AtomicReference<>();

    public MainController(Stage stage, NoteService notes, CategoryRepository categories, ExportService exports) {
        this.stage=stage;this.notes=notes;this.categories=categories;this.exports=exports;this.view=new MainView(this::navigate);
    }
    public Parent root(){return view.root();}
    public void start(){navigate("Dashboard");}
    private void navigate(String page){
        view.select(page);
        switch(page){case "Dashboard"->loadDashboard();case "Notes"->loadNotes(false);case "Favorites"->loadNotes(true);case "Analytics"->loadAnalytics();default->loadDashboard();}
    }
    private <T> void background(String message, java.util.concurrent.Callable<T> work, Consumer<T> success){
        ProgressIndicator progress=new ProgressIndicator(); VBox loading=new VBox(12,progress,new Label(message));loading.setAlignment(Pos.CENTER);view.show(loading);
        Task<T> task=new Task<>(){protected T call()throws Exception{return work.call();}}; Task<?> old=runningTask.getAndSet(task);if(old!=null)old.cancel();
        task.setOnSucceeded(e->{if(runningTask.compareAndSet(task,null))success.accept(task.getValue());});
        task.setOnFailed(e->{runningTask.compareAndSet(task,null);showError("Operation failed",task.getException());});
        Thread worker=new Thread(task,"knowvault-ui-task");worker.setDaemon(true);worker.start();
    }

    private void loadDashboard(){background("Calculating your knowledge pulse…",()->{DashboardStats stats=notes.dashboard();Map<String,Long> categoryCounts=notes.all().stream().collect(java.util.stream.Collectors.groupingBy(n->n.getCategoryName()==null?"Uncategorized":n.getCategoryName(),TreeMap::new,java.util.stream.Collectors.counting()));return new DashboardBundle(stats,categoryCounts);},this::showDashboard);}
    private void showDashboard(DashboardBundle bundle){
        DashboardStats stats=bundle.stats;
        VBox page=page("Knowledge Dashboard","Your private learning universe, updated from the local database.");
        HBox counters=new HBox(14,statCard("NOTES",stats.notes(),"#C94F7D"),statCard("FAVORITES",stats.favorites(),"#E47CA1"),statCard("TAGS",stats.tags(),"#A94A70"),statCard("TOTAL VIEWS",stats.totalViews(),"#D76B93"));
        counters.getChildren().forEach(n->HBox.setHgrow(n,Priority.ALWAYS));
        HBox middle=new HBox(18); VBox activity=new VBox(12);activity.getStyleClass().add("glass-card");activity.setPadding(new Insets(18));
        Label title=new Label("Recently edited");title.getStyleClass().add("card-title");activity.getChildren().add(title);
        if(stats.recent().isEmpty())activity.getChildren().add(new Label("Create your first note to begin."));
        stats.recent().forEach(n->{Button b=new Button(n.getTitle()+"  ·  "+prettyDate(n));b.getStyleClass().add("note-link");b.setOnAction(e->openEditor(n));activity.getChildren().add(b);});
        VBox.setVgrow(activity,Priority.ALWAYS); HBox.setHgrow(activity,Priority.ALWAYS); middle.getChildren().addAll(activity,knowledgeSphere());
        HBox charts=new HBox(18,tagChart(stats.topTags()),categoryPie(bundle.categoryCounts)); charts.getChildren().forEach(n->HBox.setHgrow(n,Priority.ALWAYS));
        page.getChildren().addAll(counters,middle,charts);view.show(new ScrollPane(page));
    }
    private VBox statCard(String label,int target,String color){
        Label number=new Label("0");number.getStyleClass().add("stat-number");number.setStyle("-fx-text-fill:"+color+";");Label caption=new Label(label);caption.getStyleClass().add("stat-label");
        VBox card=new VBox(5,number,caption);card.getStyleClass().add("glass-card");card.setPadding(new Insets(18));card.setMaxWidth(Double.MAX_VALUE);
        IntegerPropertyAdapter value=new IntegerPropertyAdapter(v->number.setText(Integer.toString(v)));Timeline timeline=new Timeline(new KeyFrame(Duration.millis(700),new KeyValue(value.property,target,Interpolator.EASE_OUT)));timeline.play();return card;
    }
    private Node knowledgeSphere(){
        VBox shell=new VBox(8);shell.getStyleClass().add("glass-card");shell.setPadding(new Insets(12));shell.setPrefSize(340,250);
        Label label=new Label("Knowledge Sphere");label.getStyleClass().add("card-title");shell.getChildren().add(label);
        if(!Platform.isSupported(ConditionalFeature.SCENE3D)){Label fallback=new Label("◉\n3D acceleration unavailable\nYour notes are still fully accessible.");fallback.setAlignment(Pos.CENTER);fallback.getStyleClass().add("sphere-fallback");VBox.setVgrow(fallback,Priority.ALWAYS);shell.getChildren().add(fallback);return shell;}
        Sphere sphere=new Sphere(75,48);PhongMaterial mat=new PhongMaterial(Color.web("#D86491"));mat.setSpecularColor(Color.web("#FFD6E4"));sphere.setMaterial(mat);sphere.setEffect(new Glow(.34));
        Group group=new Group(sphere);PerspectiveCamera camera=new PerspectiveCamera(true);camera.setTranslateZ(-420);SubScene sub=new SubScene(group,315,200,true,SceneAntialiasing.BALANCED);sub.setFill(Color.TRANSPARENT);sub.setCamera(camera);
        RotateTransition rotate=new RotateTransition(Duration.seconds(9),sphere);rotate.setAxis(Rotate.Y_AXIS);rotate.setByAngle(360);rotate.setCycleCount(Animation.INDEFINITE);rotate.setInterpolator(Interpolator.LINEAR);rotate.play();
        VBox.setVgrow(sub,Priority.ALWAYS);shell.getChildren().add(sub);return shell;
    }
    private Node tagChart(List<TagStat> data){
        CategoryAxis x=new CategoryAxis();NumberAxis y=new NumberAxis();BarChart<String,Number> chart=new BarChart<>(x,y);chart.setTitle("Frequently used tags");chart.setLegendVisible(false);chart.setAnimated(true);XYChart.Series<String,Number> series=new XYChart.Series<>();data.forEach(t->series.getData().add(new XYChart.Data<>(t.tag(),t.count())));chart.getData().add(series);chart.getStyleClass().add("glass-card");chart.setMinHeight(280);return chart;
    }
    private Node categoryPie(Map<String,Long> grouped){
        PieChart chart=new PieChart();chart.setTitle("Notes by category");grouped.forEach((k,v)->chart.getData().add(new PieChart.Data(k,v)));chart.getStyleClass().add("glass-card");chart.setMinHeight(280);return chart;
    }

    private void loadNotes(boolean favoritesOnly){background("Loading notes…",()->{List<Note> all=notes.all();return favoritesOnly?all.stream().filter(Note::isFavorite).toList():all;},list->showNotes(list,favoritesOnly));}
    private void showNotes(List<Note> initial,boolean favoritesOnly){
        VBox page=page(favoritesOnly?"Favorite Notes":"All Notes",favoritesOnly?"Your hand-picked references.":"Search, sort, edit, connect, and export your knowledge.");
        TextField search=new TextField();search.setPromptText("Search title, content, or tags…");ComboBox<String> sort=new ComboBox<>(FXCollections.observableArrayList("Recently edited","Title A–Z","Most viewed"));sort.setValue("Recently edited");
        Button add=actionButton("＋ New note");Button exportText=secondaryButton("Collection TXT");Button exportCsv=secondaryButton("Collection CSV");Region push=new Region();HBox.setHgrow(push,Priority.ALWAYS);HBox toolbar=new HBox(10,search,sort,push,exportText,exportCsv,add);toolbar.setAlignment(Pos.CENTER_LEFT);search.setPrefWidth(360);
        ListView<Note> list=new ListView<>();list.getStyleClass().add("notes-list");list.setCellFactory(v->new NoteCell(this::openEditor));list.getItems().setAll(initial);VBox.setVgrow(list,Priority.ALWAYS);
        Runnable applySort=()->{Comparator<Note> c=switch(sort.getValue()){case "Title A–Z"->Comparator.comparing(Note::getTitle,String.CASE_INSENSITIVE_ORDER);case "Most viewed"->Comparator.comparingInt(Note::getViewCount).reversed();default->Comparator.comparing(Note::getUpdatedAt,Comparator.nullsLast(Comparator.reverseOrder()));};FXCollections.sort(list.getItems(),c);};sort.setOnAction(e->applySort.run());
        PauseTransition debounce=new PauseTransition(Duration.millis(260));search.textProperty().addListener((o,a,b)->{debounce.stop();debounce.setOnFinished(e->runSearch(b,favoritesOnly,list,applySort));debounce.playFromStart();});
        add.setOnAction(e->openEditor(null));exportText.setOnAction(e->exportCollectionText(new ArrayList<>(list.getItems())));exportCsv.setOnAction(e->exportCollection(new ArrayList<>(list.getItems())));list.setOnMouseClicked(e->{if(e.getClickCount()==2&&list.getSelectionModel().getSelectedItem()!=null)openEditor(list.getSelectionModel().getSelectedItem());});
        page.getChildren().addAll(toolbar,list);view.show(page);
    }
    private void runSearch(String query,boolean favorites,ListView<Note> target,Runnable after){
        Task<List<Note>> task=new Task<>(){protected List<Note> call(){return notes.search(query);}};task.setOnSucceeded(e->{List<Note> result=task.getValue();if(favorites)result=result.stream().filter(Note::isFavorite).toList();target.getItems().setAll(result);after.run();});task.setOnFailed(e->showError("Search failed",task.getException()));Thread t=new Thread(task,"knowvault-search");t.setDaemon(true);t.start();
    }

    private void openEditor(Note selected){
        Note original=selected==null?null:notes.open(selected.getId()); Dialog<ButtonType> dialog=new Dialog<>();dialog.initOwner(stage);dialog.setTitle(original==null?"Create note":"Edit note");dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Save",ButtonBar.ButtonData.OK_DONE),ButtonType.CANCEL);
        TextField title=new TextField(original==null?"":original.getTitle());title.setPromptText("Note title");TextArea content=new TextArea(original==null?"":original.getContent());content.setPromptText("Write your knowledge here…");content.setWrapText(true);content.setPrefRowCount(15);
        ComboBox<Category> category=new ComboBox<>(FXCollections.observableArrayList(categories.findAll()));category.setPromptText("Uncategorized");if(original!=null&&original.getCategoryId()!=null)category.getItems().stream().filter(c->c.id()==original.getCategoryId()).findFirst().ifPresent(category::setValue);
        TextField tagInput=new TextField(original==null?"":original.tagsText());tagInput.setPromptText("java, collections, study");CheckBox favorite=new CheckBox("Favorite");favorite.setSelected(original!=null&&original.isFavorite());
        FlowPane bubbles=new FlowPane(7,7);tagInput.textProperty().addListener((o,a,b)->renderTagBubbles(b,bubbles,tagInput));renderTagBubbles(tagInput.getText(),bubbles,tagInput);
        Button newCategory=secondaryButton("＋ Category");newCategory.setOnAction(e->{TextInputDialog input=new TextInputDialog();input.initOwner(dialog.getOwner());input.setTitle("New category");input.setHeaderText("Create a category");input.setContentText("Name:");input.showAndWait().map(String::strip).filter(s->!s.isEmpty()).ifPresent(name->{try{Category created=categories.save(new Category(0,name,"#D86491"));category.getItems().add(created);category.setValue(created);}catch(Exception failure){showError("Could not create category",failure);}});});
        HBox tools=new HBox(10,category,newCategory,favorite);tools.setAlignment(Pos.CENTER_LEFT);VBox form=new VBox(11,new Label("TITLE"),title,new Label("CONTENT"),content,new Label("CATEGORY & FAVORITE"),tools,new Label("TAGS"),tagInput,bubbles);form.setPadding(new Insets(10));form.setPrefWidth(720);dialog.getDialogPane().setContent(form);
        if(original!=null){ButtonType history=new ButtonType("History",ButtonBar.ButtonData.LEFT);ButtonType related=new ButtonType("Related",ButtonBar.ButtonData.LEFT);ButtonType exportTxt=new ButtonType("TXT",ButtonBar.ButtonData.LEFT);ButtonType exportCsv=new ButtonType("CSV",ButtonBar.ButtonData.LEFT);ButtonType delete=new ButtonType("Delete",ButtonBar.ButtonData.LEFT);dialog.getDialogPane().getButtonTypes().addAll(history,related,exportTxt,exportCsv,delete);dialog.setResultConverter(button->{if(button==history){showHistory(original);return null;}if(button==related){showRelated(original);return null;}if(button==exportTxt){exportNote(original);return null;}if(button==exportCsv){exportNoteCsv(original);return null;}if(button==delete){deleteNote(original);return null;}return button;});}
        Node saveButton=dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().getFirst());saveButton.disableProperty().bind(title.textProperty().isEmpty());
        Optional<ButtonType> result=dialog.showAndWait();if(result.isPresent()&&result.get().getButtonData()==ButtonBar.ButtonData.OK_DONE){
            Note note=original==null?new Note():original;note.setTitle(title.getText());note.setContent(content.getText());note.setCategoryId(category.getValue()==null?null:category.getValue().id());note.setFavorite(favorite.isSelected());note.getTags().clear();parseTags(tagInput.getText()).forEach(t->note.getTags().add(new Tag(0,t)));
            Task<Note> save=new Task<>(){protected Note call(){return notes.save(note);}};save.setOnSucceeded(e->navigate("Notes"));save.setOnFailed(e->showError("Could not save note",save.getException()));Thread thread=new Thread(save,"knowvault-save");thread.setDaemon(true);thread.start();
        }
    }
    private void renderTagBubbles(String value,FlowPane pane,TextField input){pane.getChildren().clear();parseTags(value).forEach(tag->{Button b=new Button("#"+tag+"  ×");b.getStyleClass().add("tag-bubble");b.setOnAction(e->{List<String> tags=new ArrayList<>(parseTags(input.getText()));tags.remove(tag);input.setText(String.join(", ",tags));});pane.getChildren().add(b);});}
    private LinkedHashSet<String> parseTags(String value){LinkedHashSet<String> result=new LinkedHashSet<>();if(value!=null)Arrays.stream(value.split(",")).map(String::strip).filter(s->!s.isEmpty()).map(String::toLowerCase).limit(30).forEach(result::add);return result;}
    private void showHistory(Note note){List<NoteHistory> history=notes.history(note.getId());Dialog<NoteHistory> dialog=new Dialog<>();dialog.initOwner(stage);dialog.setTitle("Edit history — "+note.getTitle());ListView<NoteHistory> list=new ListView<>(FXCollections.observableArrayList(history));list.setCellFactory(v->new ListCell<>(){protected void updateItem(NoteHistory h,boolean empty){super.updateItem(h,empty);setText(empty||h==null?null:h.savedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))+"  —  "+h.title());}});dialog.getDialogPane().setContent(list);ButtonType restore=new ButtonType("Restore selected",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(restore,ButtonType.CLOSE);dialog.setResultConverter(b->b==restore?list.getSelectionModel().getSelectedItem():null);dialog.showAndWait().ifPresent(h->{notes.restore(h.id());navigate("Notes");});}
    private void showRelated(Note note){List<RelatedNote> related=notes.related(note.getId());Alert alert=new Alert(Alert.AlertType.INFORMATION);alert.initOwner(stage);alert.setTitle("Related notes");alert.setHeaderText("Connections for "+note.getTitle());StringBuilder body=new StringBuilder();related.forEach(r->body.append(String.format(Locale.ROOT,"%.0f%%  •  %s  (%d shared tag%s)%n",r.similarityPercent(),r.note().getTitle(),r.sharedTags(),r.sharedTags()==1?"":"s")));alert.setContentText(body.isEmpty()?"No shared-tag connections yet.":body.toString());alert.showAndWait();}
    private void deleteNote(Note note){ButtonType delete=new ButtonType("Delete",ButtonBar.ButtonData.OK_DONE);Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Delete “"+note.getTitle()+"”? This cannot be undone.",delete,ButtonType.CANCEL);confirm.initOwner(stage);confirm.showAndWait().filter(delete::equals).ifPresent(b->{notes.delete(note.getId());navigate("Notes");});}
    private void exportNote(Note note){FileChooser chooser=fileChooser("Export note","Text files","*.txt",safeName(note.getTitle())+".txt");File file=chooser.showSaveDialog(stage);if(file!=null){exports.exportText(note,file.toPath());toast("Exported "+file.getName());}}
    private void exportNoteCsv(Note note){FileChooser chooser=fileChooser("Export note","CSV files","*.csv",safeName(note.getTitle())+".csv");File file=chooser.showSaveDialog(stage);if(file!=null){exports.exportCsv(List.of(note),file.toPath());toast("Exported "+file.getName());}}
    private void exportCollectionText(List<Note> collection){if(collection.isEmpty()){toast("There are no notes to export.");return;}FileChooser chooser=fileChooser("Export collection","Text files","*.txt","knowvault-notes.txt");File file=chooser.showSaveDialog(stage);if(file!=null){exports.exportText(collection,file.toPath());toast("Exported "+collection.size()+" notes");}}
    private void exportCollection(List<Note> collection){if(collection.isEmpty()){toast("There are no notes to export.");return;}FileChooser chooser=fileChooser("Export collection","CSV files","*.csv","knowvault-notes.csv");File file=chooser.showSaveDialog(stage);if(file!=null){exports.exportCsv(collection,file.toPath());toast("Exported "+collection.size()+" notes");}}
    private FileChooser fileChooser(String title,String label,String pattern,String initial){FileChooser c=new FileChooser();c.setTitle(title);c.getExtensionFilters().add(new FileChooser.ExtensionFilter(label,pattern));c.setInitialFileName(initial);return c;}
    private String safeName(String name){return name.replaceAll("[^a-zA-Z0-9._-]+","-").replaceAll("^-|-$","");}

    private void loadAnalytics(){background("Analyzing topics…",()->new AnalyticsBundle(notes.dashboard(),TextAnalyzer.frequentTopics(notes.all(),12)),this::showAnalytics);}
    private void showAnalytics(AnalyticsBundle bundle){VBox page=page("Knowledge Analytics","Live measurements derived from your notes, tags, views, and categories.");FlowPane tags=new FlowPane(10,10);bundle.stats.topTags().forEach(t->{Button b=new Button("#"+t.tag()+"  "+t.count());b.getStyleClass().add("tag-bubble-large");b.setOnAction(e->{navigate("Notes");});tags.getChildren().add(b);});VBox tagCard=card("Tag cloud",tags);TableView<Map.Entry<String,Long>> table=new TableView<>();TableColumn<Map.Entry<String,Long>,String> topic=new TableColumn<>("Topic");topic.setCellValueFactory(d->new ReadOnlyObjectWrapper<>(d.getValue().getKey()));TableColumn<Map.Entry<String,Long>,Long> count=new TableColumn<>("Mentions");count.setCellValueFactory(d->new ReadOnlyObjectWrapper<>(d.getValue().getValue()));table.getColumns().add(topic);table.getColumns().add(count);table.getItems().setAll(bundle.topics.entrySet());table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);VBox views=card("Frequent topics",table);HBox row=new HBox(18,tagCard,views);HBox.setHgrow(tagCard,Priority.ALWAYS);HBox.setHgrow(views,Priority.ALWAYS);page.getChildren().addAll(row,tagChart(bundle.stats.topTags()));view.show(page);}
    private VBox card(String title,Node content){Label heading=new Label(title);heading.getStyleClass().add("card-title");VBox card=new VBox(12,heading,content);card.getStyleClass().add("glass-card");card.setPadding(new Insets(18));card.setMaxWidth(Double.MAX_VALUE);return card;}
    private VBox page(String heading,String sub){Label h=new Label(heading);h.getStyleClass().add("page-title");Label s=new Label(sub);s.getStyleClass().add("page-subtitle");VBox v=new VBox(8,h,s);v.setFillWidth(true);v.setPadding(new Insets(0));return v;}
    private Button actionButton(String text){Button b=new Button(text);b.getStyleClass().add("primary-button");return b;}
    private Button secondaryButton(String text){Button b=new Button(text);b.getStyleClass().add("secondary-button");return b;}
    private String prettyDate(Note n){return n.getUpdatedAt()==null?"":n.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"));}
    private void toast(String message){Alert a=new Alert(Alert.AlertType.INFORMATION,message,ButtonType.OK);a.initOwner(stage);a.setHeaderText(null);a.showAndWait();}
    private void showError(String title,Throwable error){Throwable shown=error;while(shown.getCause()!=null&&!(shown instanceof KnowVaultException))shown=shown.getCause();Alert alert=new Alert(Alert.AlertType.ERROR,shown.getMessage()==null?shown.toString():shown.getMessage(),ButtonType.OK);alert.initOwner(stage);alert.setTitle(title);alert.setHeaderText(title);alert.showAndWait();}
    private record AnalyticsBundle(DashboardStats stats,Map<String,Long> topics){}
    private record DashboardBundle(DashboardStats stats,Map<String,Long> categoryCounts){}
    private static final class IntegerPropertyAdapter {final javafx.beans.property.IntegerProperty property=new javafx.beans.property.SimpleIntegerProperty();IntegerPropertyAdapter(java.util.function.IntConsumer listener){property.addListener((o,a,b)->listener.accept(b.intValue()));}}
    private final class NoteCell extends ListCell<Note>{private final Consumer<Note> open;NoteCell(Consumer<Note> open){this.open=open;}protected void updateItem(Note n,boolean empty){super.updateItem(n,empty);if(empty||n==null){setGraphic(null);setText(null);return;}Label title=new Label((n.isFavorite()?"★  ":"")+n.getTitle());title.getStyleClass().add("note-title");Label meta=new Label((n.getCategoryName()==null?"Uncategorized":n.getCategoryName())+"  •  "+n.tagsText()+"  •  "+n.getViewCount()+" views");meta.getStyleClass().add("note-meta");Label excerpt=new Label(n.getContent().replaceAll("\\s+"," "));excerpt.setMaxWidth(900);excerpt.setTextOverrun(OverrunStyle.ELLIPSIS);Button edit=secondaryButton("Open");edit.setOnAction(e->open.accept(n));Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);HBox top=new HBox(8,title,spacer,edit);top.setAlignment(Pos.CENTER_LEFT);VBox box=new VBox(6,top,meta,excerpt);box.getStyleClass().add("note-row");box.setPadding(new Insets(14));setGraphic(box);}}
}
