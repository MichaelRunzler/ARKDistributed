package X34.UI.JFX.Managers;

import X34.Processors.ProcessorMetadataPacket;
import core.CoreUtil.JFXUtil;
import core.UI.ARKManagerBase;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class X34UIProcessorInfoManager extends ARKManagerBase
{
    //
    // CONSTANTS
    //

    public static final String DEFAULT_TITLE = "Processor Info";
    public static final int DEFAULT_WIDTH = (int)(250 * JFXUtil.SCALE);
    public static final int DEFAULT_HEIGHT = (int)(400 * JFXUtil.SCALE);

    //
    // JFX NODES
    //

    private VBox infoContainer;

    private TextFlow siteContainer;

    private Label description;
    private Label author;

    private TextFlow homeSite;

    private Label copyright;

    private Button close;

    public X34UIProcessorInfoManager(double x, double y)
    {
        super(DEFAULT_TITLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, x, y);

        window.setMinWidth(DEFAULT_WIDTH);
        window.setMinHeight(DEFAULT_HEIGHT);

        description = new Label();
        author = new Label();
        copyright = new Label();
        close = new Button("Done");

        description.setWrapText(true);


        infoContainer = new VBox();
        infoContainer.setFillWidth(true);
        infoContainer.setAlignment(Pos.TOP_LEFT);
        infoContainer.setSpacing(10.0 * JFXUtil.SCALE);

        siteContainer = new TextFlow();
        siteContainer.setTextAlignment(TextAlignment.LEFT);
        siteContainer.getChildren().add(new Text("Supported Sites:\n"));

        homeSite = new TextFlow();
        homeSite.setTextAlignment(TextAlignment.LEFT);
        homeSite.getChildren().add(new Label("Home Site: "));

        infoContainer.getChildren().addAll(description, author, siteContainer, homeSite, copyright);
        layout.getChildren().addAll(infoContainer, close);

        close.setOnAction(e -> this.hide());
    }

    private void repositionElements()
    {
        JFXUtil.setElementPositionInGrid(layout, infoContainer, 0, 0, 0, 1);
        JFXUtil.setElementPositionInGrid(layout, close, 0, 0, -1, 0);
    }

    public void setDisplayedInfo(ProcessorMetadataPacket metadata)
    {
        if(homeSite.getChildren().size() > 1) homeSite.getChildren().remove(1);
        siteContainer.getChildren().remove(1, siteContainer.getChildren().size());

        if(metadata == null){
            description.setText("");
            author.setText("Author: ");
            copyright.setText("");
            return;
        }

        description.setText(metadata.getDescription());
        author.setText("Author: " + metadata.getAuthor());
        copyright.setText(metadata.getCopyrightInfo());

        String homeBaseLabel = metadata.getSupportURL().toExternalForm();
        int homeProtocolHeader = homeBaseLabel.contains("://") ? homeBaseLabel.indexOf("://") + 3: 0;
        int homeDirectoryHeader = homeBaseLabel.indexOf('/', homeProtocolHeader + 1);
        Hyperlink home = new Hyperlink(homeBaseLabel.substring(0, homeDirectoryHeader == -1 ? homeBaseLabel.length() : homeDirectoryHeader));

        home.setWrapText(true);
        home.setOnAction(e -> {
            try {Desktop.getDesktop().browse(metadata.getSupportURL().toURI());} catch (IOException | URISyntaxException ignored) {}
        });
        homeSite.getChildren().add(home);


        URL[] supportedSites = metadata.getSupportedSites();
        for(int i = 0; i < supportedSites.length; i++)
        {
            URL site = supportedSites[i];
            Hyperlink h = new Hyperlink(site.toExternalForm());
            h.setWrapText(true);
            h.setOnAction(e ->{
                try {Desktop.getDesktop().browse(site.toURI());} catch (IOException | URISyntaxException ignored) {}
            });

            siteContainer.getChildren().add(h);

            if(i < supportedSites.length - 1) siteContainer.getChildren().add(new Text(", "));
        }
    }

    @Override
    public void display()
    {
        repositionElements();

        if(!window.isShowing()){
            window.showAndWait();
        }
    }
}
