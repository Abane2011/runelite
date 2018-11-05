package net.runelite.client.plugins.pkhelper;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import java.awt.Color;
import java.time.Instant;
import java.util.List;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import static net.runelite.api.MenuAction.*;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

@PluginDescriptor(
        name = "PK Helper Plugin",
        description = "Highlight players on-screen and/or on the minimap",
        tags = {"highlight", "minimap", "overlay", "players", "pk", "helper"}
)

public class PKHelperPlugin extends Plugin
{
    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PKHelperConfig config;

    @Inject
    private PKHelperOverlay pkHelperOverlay;

    @Inject
    private PKHelperTileOverlay pkHelperTileOverlay;

    @Inject
    private PKHelperMinimapOverlay pkHelperMinimapOverlay;

    @Inject
    private Client client;

    @Getter(AccessLevel.PACKAGE)
    private Actor lastOpponent;

    @Provides
    PKHelperConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PKHelperConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(pkHelperOverlay);
        overlayManager.add(pkHelperTileOverlay);
        overlayManager.add(pkHelperMinimapOverlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(pkHelperOverlay);
        overlayManager.remove(pkHelperTileOverlay);
        overlayManager.remove(pkHelperMinimapOverlay);
    }

    int getWildernessLevelFrom(WorldPoint point)
    {
        int y = point.getY();               //v underground           //v above ground
        int wildernessLevel = y > 6400 ? ((y - 9920) / 8) + 1 : ((y - 3520) / 8) + 1;
        return wildernessLevel > 0 ? wildernessLevel : 15; //if wildy level is below zero we assume it's a pvp world which is -15 to 15 level difference
    }

    public static int clamp(int val, int min, int max)
    {
        return Math.max(min, Math.min(max, val));
    }

    @Subscribe
    public void onMenuEntryAdd(MenuEntryAdded menuEntryAdded)
    {
        int type = menuEntryAdded.getType();

        if (type >= 2000)
            type -= 2000;

        int identifier = menuEntryAdded.getIdentifier();
        if (type == FOLLOW.getId() || type == TRADE.getId()
                || type == ITEM_USE_ON_PLAYER.getId() || type == PLAYER_FIRST_OPTION.getId()
                || type == PLAYER_SECOND_OPTION.getId() || type == PLAYER_THIRD_OPTION.getId()
                || type == PLAYER_FOURTH_OPTION.getId() || type == PLAYER_FIFTH_OPTION.getId()
                || type == PLAYER_SIXTH_OPTION.getId() || type == PLAYER_SEVENTH_OPTION.getId()
                || type == PLAYER_EIGTH_OPTION.getId() || type == SPELL_CAST_ON_PLAYER.getId()
                || type == RUNELITE.getId())
        {
            final Player localPlayer = client.getLocalPlayer();
            Player[] players = client.getCachedPlayers();
            Player player = null;

            if (identifier >= 0 && identifier < players.length)
                player = players[identifier];

            if (player == null)
                return;

            Color color = null;

            if (config.highlightFriends() && player.isFriend())
            {
                color = config.getFriendColor();
            }
            else if (!player.isFriend())
            {
                int lvlDelta =  player.getCombatLevel() - localPlayer.getCombatLevel();
                int wildyLvl = getWildernessLevelFrom(player.getWorldLocation());

                int R = clamp((int)(((float)(lvlDelta + wildyLvl) / (float)(wildyLvl * 2)) * 255.f), 0, 255);
                int G = clamp(255 - R, 0, 255);

                if (Math.abs(lvlDelta) <= wildyLvl)
                    color = Color.getHSBColor(Color.RGBtoHSB(R, G, 0, null)[0], 1.f, 1.f);
            }

            if (color != null)
            {
                MenuEntry[] menuEntries = client.getMenuEntries();
                MenuEntry lastEntry = menuEntries[menuEntries.length - 1];

                // strip out existing <col...
                String target = lastEntry.getTarget();
                int idx = target.indexOf('>');
                if (idx != -1)
                    target = target.substring(idx + 1);

                lastEntry.setTarget(ColorUtil.prependColorTag(target, color));


                client.setMenuEntries(menuEntries);
            }
        }
    }
}
