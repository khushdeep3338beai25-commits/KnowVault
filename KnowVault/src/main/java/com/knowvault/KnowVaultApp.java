package com.knowvault;

import com.knowvault.controller.MainController;
import com.knowvault.database.DatabaseManager;
import com.knowvault.repository.CategoryRepository;
import com.knowvault.repository.NoteRepository;
import com.knowvault.service.ExportService;
import com.knowvault.service.NoteService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class KnowVaultApp extends Application {
    private DatabaseManager database; private NoteService notes;
    @Override public void start(Stage stage){
        database=new DatabaseManager();NoteRepository noteRepository=new NoteRepository(database);notes=new NoteService(noteRepository);
        MainController controller=new MainController(stage,notes,new CategoryRepository(database),new ExportService());
        Scene scene=new Scene(controller.root(),1280,820);scene.getStylesheets().add(getClass().getResource("/com/knowvault/css/theme.css").toExternalForm());
        stage.setTitle("KnowVault – Personal Knowledge Base & Smart Notes");stage.setMinWidth(980);stage.setMinHeight(680);stage.setScene(scene);stage.show();controller.start();
    }
    @Override public void stop(){if(notes!=null)notes.close();if(database!=null)database.close();}
    public static void main(String[] args){launch(args);}
}
