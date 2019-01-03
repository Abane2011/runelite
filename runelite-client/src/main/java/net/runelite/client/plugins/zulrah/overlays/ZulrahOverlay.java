/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
 * Copyright (c) 2017, Devin French <https://github.com/devinfrench>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.zulrah.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.zulrah.ZulrahInstance;
import net.runelite.client.plugins.zulrah.ZulrahPlugin;
import net.runelite.client.plugins.zulrah.phase.ZulrahPhase;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
public class ZulrahOverlay extends Overlay
{
    private static final Color TILE_BORDER_COLOR = new Color(0, 0, 0, 100);
    private static final Color NEXT_TEXT_COLOR = new Color(255, 255, 255, 100);

    private final Client client;
    private final ZulrahPlugin plugin;

    @Inject
    ZulrahOverlay(Client client, ZulrahPlugin plugin)
    {
        setPosition(OverlayPosition.DYNAMIC);
        this.client = client;
        this.plugin = plugin;
    }

    //LocalPoint delta;

    @Override
    public Dimension render(Graphics2D graphics)
    {
        ZulrahInstance instance = plugin.getInstance();

        if (instance == null)
            return null;

        if (instance.zulrah != null)
        {
            LocalPoint temp1 = LocalPoint.fromWorld(client, instance.zulrah.getWorldLocation());
            LocalPoint temp2 = instance.zulrah.getLocalLocation();
            //delta = new LocalPoint(temp1.getX() - temp2.getX(), temp1.getY() - temp2.getY());

            Point minimapLocation = instance.zulrah.getMinimapLocation();
            if (minimapLocation != null)
            {
                OverlayUtil.renderMinimapLocation(graphics, minimapLocation, Color.RED);
                OverlayUtil.renderTextLocation(graphics, minimapLocation, "Zulrah", Color.RED);
            }
        }

        ZulrahPhase currentPhase = instance.getPhase();
        ZulrahPhase nextPhase = instance.getNextPhase();

        if (currentPhase == null)
            return null;

        WorldPoint startTile = instance.getStartLocation();
        if (nextPhase != null && currentPhase.getStandLocation() == nextPhase.getStandLocation())
        {
            drawStandTiles(graphics, startTile, currentPhase, nextPhase);
            OverlayUtil.renderTextLocation(graphics, new Point(50, 50), "Tile[s]", Color.WHITE);
        }
        else
        {
            drawStandTile(graphics, startTile, currentPhase, false);
            drawStandTile(graphics, startTile, nextPhase, true);
            OverlayUtil.renderTextLocation(graphics, new Point(50, 50), "Tile", Color.YELLOW);
        }
        //drawZulrahTileMinimap(graphics, startTile, currentPhase, false);
        //drawZulrahTileMinimap(graphics, startTile, nextPhase, true);

        return null;
    }

    //localTile = new LocalPoint(localTile.getX() + Perspective.LOCAL_TILE_SIZE / 2, localTile.getY() + Perspective.LOCAL_TILE_SIZE / 2);
    private void drawStandTiles(Graphics2D graphics, WorldPoint startTile, ZulrahPhase currentPhase, ZulrahPhase nextPhase)
    {
        LocalPoint localTile = LocalPoint.fromWorld(client, currentPhase.getStandTile(startTile));
        //localTile = new LocalPoint(localTile.getX() - delta.getX(), localTile.getY() - delta.getY());

        Polygon northPoly = getCanvasTileNorthPoly(client, localTile);
        Polygon southPoly = getCanvasTileSouthPoly(client, localTile);
        Polygon poly = Perspective.getCanvasTilePoly(client, localTile);


        Point textLoc = Perspective.getCanvasTextLocation(client, graphics, localTile, "Next", 0);

        if (northPoly != null && southPoly != null && poly != null && textLoc != null)
        {
            OverlayUtil.renderPolygon(graphics, poly, TILE_BORDER_COLOR);
            OverlayUtil.renderPolygon(graphics, northPoly, currentPhase.getColor());
            OverlayUtil.renderPolygon(graphics, southPoly, nextPhase.getColor());
            OverlayUtil.renderTextLocation(graphics, textLoc, "Next", NEXT_TEXT_COLOR);
        }

        if (nextPhase.isJad())
        {
            BufferedImage jadPrayerImg = ZulrahImageManager.getProtectionPrayerBufferedImage(nextPhase.getPrayer());
            if (jadPrayerImg != null)
            {
                Point imageLoc = Perspective.getCanvasImageLocation(client, localTile, jadPrayerImg, 0);
                if (imageLoc != null)
                {
                    OverlayUtil.renderImageLocation(graphics, imageLoc, jadPrayerImg);
                }
            }
        }
    }

    private void drawStandTile(Graphics2D graphics, WorldPoint startTile, ZulrahPhase phase, boolean next)
    {
        if (phase == null)
            return;

        LocalPoint localTile = LocalPoint.fromWorld(client, phase.getStandTile(startTile));
        //localTile = new LocalPoint(localTile.getX() - delta.getX(), localTile.getY() - delta.getY());

        Polygon poly = Perspective.getCanvasTilePoly(client, localTile);

        if (poly != null)
            OverlayUtil.renderPolygon(graphics, poly, phase.getColor());

        if (!next)
            return;

        Point textLoc = Perspective.getCanvasTextLocation(client, graphics, localTile, "Next", 0);

        if (textLoc != null)
            OverlayUtil.renderTextLocation(graphics, textLoc, "Next", NEXT_TEXT_COLOR);

        if (phase.isJad())
        {
            BufferedImage jadPrayerImg = ZulrahImageManager.getProtectionPrayerBufferedImage(phase.getPrayer());

            if (jadPrayerImg != null)
            {
                Point imageLoc = Perspective.getCanvasImageLocation(client, localTile, jadPrayerImg, 0);
                if (imageLoc != null)
                {
                    OverlayUtil.renderImageLocation(graphics, imageLoc, jadPrayerImg);
                }
            }
        }
    }

    private void drawZulrahTileMinimap(Graphics2D graphics, WorldPoint startTile, ZulrahPhase phase, boolean next)
    {
        if (phase == null)
            return;

        LocalPoint zulrahLocalTile = LocalPoint.fromWorld(client, phase.getZulrahTile(startTile));

        if (zulrahLocalTile == null)
            return;

        Point zulrahMinimapPoint = Perspective.localToMinimap(client, zulrahLocalTile);

        if (zulrahMinimapPoint == null)
            return;

        OverlayUtil.renderMinimapLocation(graphics, zulrahMinimapPoint, phase.getColor());

        if (!next)
            return;

        OverlayUtil.renderTextLocation(graphics, new Point(zulrahMinimapPoint.getX() - graphics.getFontMetrics().stringWidth("Next") / 2, zulrahMinimapPoint.getY() - 2), "Next", NEXT_TEXT_COLOR);

    }

    private Polygon getCanvasTileNorthPoly(Client client, LocalPoint localLocation)
    {
        int plane = client.getPlane();
        int halfTile = Perspective.LOCAL_TILE_SIZE / 2;

        Point p1 = Perspective.localToCanvas(client, new LocalPoint(localLocation.getX() - halfTile, localLocation.getY() - halfTile), plane);
        Point p2 = Perspective.localToCanvas(client, new LocalPoint(localLocation.getX() - halfTile, localLocation.getY() + halfTile), plane);
        Point p3 = Perspective.localToCanvas(client, new LocalPoint(localLocation.getX() + halfTile, localLocation.getY() + halfTile), plane);

        if (p1 == null || p2 == null || p3 == null)
        {
            return null;
        }

        Polygon poly = new Polygon();
        poly.addPoint(p1.getX(), p1.getY());
        poly.addPoint(p2.getX(), p2.getY());
        poly.addPoint(p3.getX(), p3.getY());

        return poly;
    }

    private Polygon getCanvasTileSouthPoly(Client client, LocalPoint localLocation)
    {
        int plane = client.getPlane();
        int halfTile = Perspective.LOCAL_TILE_SIZE / 2;

        Point p1 = Perspective.localToCanvas(client, new LocalPoint(localLocation.getX() - halfTile, localLocation.getY() - halfTile), plane);
        Point p2 = Perspective.localToCanvas(client, new LocalPoint(localLocation.getX() + halfTile, localLocation.getY() + halfTile), plane);
        Point p3 = Perspective.localToCanvas(client, new LocalPoint(localLocation.getX() + halfTile, localLocation.getY() - halfTile), plane);

        if (p1 == null || p2 == null || p3 == null)
        {
            return null;
        }

        Polygon poly = new Polygon();
        poly.addPoint(p1.getX(), p1.getY());
        poly.addPoint(p2.getX(), p2.getY());
        poly.addPoint(p3.getX(), p3.getY());

        return poly;
    }
}