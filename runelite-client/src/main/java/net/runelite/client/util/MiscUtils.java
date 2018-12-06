package net.runelite.client.util;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;

public class MiscUtils
{
    private static int[] abovePointsX = { 2944, 3392, 3392, 2944 };
    private static int[] abovePointsY = { 3523, 3523, 3971, 3971 };
    private static int[] belowPointsX = { 2944, 2944, 3264, 3264 };
    private static int[] belowPointsY = { 9918, 10360, 10360, 9918 };
    
    private static Polygon abovePoly = new Polygon(abovePointsX, abovePointsY, abovePointsX.length);
    private static Polygon belowPoly = new Polygon(belowPointsX, belowPointsY, belowPointsX.length);
    
    //test replacement so private for now
    private static boolean inWildy(WorldPoint point)
    {
        if (point == null)
            return false;
        
        /*
        Polygon abovePoly = new Polygon();
        Polygon underPoly = new Polygon();
        
        // Above ground
        abovePoly.addPoint(2944, 3523);
        abovePoly.addPoint(3392, 3523);
        abovePoly.addPoint(3392, 3971);
        abovePoly.addPoint(2944, 3971);
    
        // Underground
        underPoly.addPoint(2944, 9918);
        underPoly.addPoint(2944, 10360);
        underPoly.addPoint(3264, 10360);
        underPoly.addPoint(3264, 9918);
        */
    
        return abovePoly.contains(point.getX(), point.getY()) || belowPoly.contains(point.getX(), point.getY());
    }
    
    public static int getWildernessLevelFrom(Client client, WorldPoint point)
    {
        if (client == null)
            return 0;

        if (point == null)
            return 0;

        int y = point.getY();
                                                //v underground        //v above ground
        int wildernessLevel = clamp(y > 6400 ? ((y - 9920) / 8) + 1 : ((y - 3520) / 8) + 1, 0, 56);
        
        if (client.getWorldType().stream().anyMatch(x -> x == WorldType.PVP || x == WorldType.PVP_HIGH_RISK))
        {
            wildernessLevel += 15;
        }

        return Math.max(0, wildernessLevel);
    }

    public static int clamp(int val, int min, int max)
    {
        return Math.max(min, Math.min(max, val));
    }

    public static boolean inWilderness(Client client)
    {
        Player localPlayer = client.getLocalPlayer();

        if (localPlayer == null)
            return false;
        
        return inWildy(localPlayer.getWorldLocation());

        //return getWildernessLevelFrom(client, localPlayer.getWorldLocation()) > 0;
    }
}
