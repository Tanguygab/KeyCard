package io.github.tanguygab.keycard;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class MapRender extends MapRenderer {

    private Image image;

    @Override
    public void initialize(MapView map) {
        map.getRenderers().clear();
        map.addRenderer(this);
        image = new ImageIcon(getClass().getResource("/scanner.png")).getImage();

    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        canvas.drawImage(0,0,image);
    }


}
