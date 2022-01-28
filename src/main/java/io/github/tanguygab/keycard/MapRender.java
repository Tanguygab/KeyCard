package io.github.tanguygab.keycard;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class MapRender extends MapRenderer {

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        canvas.drawImage(0,0,KeyCardPlugin.get().image);
    }

}
