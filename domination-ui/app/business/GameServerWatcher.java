package business;

import com.linkedin.domination.api.Event;
import com.linkedin.domination.api.Planet;
import com.linkedin.domination.api.Player;
import com.linkedin.domination.api.Universe;
import com.linkedin.domination.server.LandingEvent;
import com.linkedin.domination.server.LaunchEvent;
import com.linkedin.domination.server.Watcher;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: wfender
 * Date: 2/12/13
 * Time: 6:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class GameServerWatcher implements Watcher
{
    private PrintStream _resultStream;
    private Universe _universe;
    private Map<Integer, List<Event>> _turnEventMap = new HashMap<Integer, List<Event>>();
    private Map<Integer, Player> _players = new HashMap<Integer, Player>();
    private String _initialPlanetState;
    private int _lastTurn = 0;

    public GameServerWatcher(File result)
    {
        try
        {
            result.createNewFile();
            _resultStream = new PrintStream(new FileOutputStream(result));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void setUniverse(Universe universe)
    {
        _universe = universe;
        _initialPlanetState = initialPlanetState();
    }

    public void setEvent(Event event, int turn)
    {
        List<Event> events = _turnEventMap.get(turn);
        if (events == null)
        {
            events = new ArrayList<Event>();
            _turnEventMap.put(turn, events);
        }
        events.add(event);
        if (turn > _lastTurn)
        {
            _lastTurn = turn;
        }
    }

    public void setPlayer(int playerNbr, Player player)
    {
        _players.put(playerNbr, player);
    }

    public void gameOver()
    {
        _resultStream.println("{");
        printPlayers();
        printPlanets();
        printEvents();
        _resultStream.println("}");
    }

    private void printPlayers()
    {
        _resultStream.println(quote("players") + ": [");
        for (Integer playerId : _players.keySet())
        {
            _resultStream.println("  {\"id\": " + playerId + ", \"name\": " + quote(_players.get(playerId).getPlayerName()) + "},");
        }
        _resultStream.println("],");
    }

    private String quote(String item)
    {
        return "\"" + item + "\"";
    }

    private String braces(String item)
    {
        return "{" + item +  "}";
    }

    private String initialPlanetState()
    {
        StringBuffer result = new StringBuffer(quote("planets"));
        result.append(": [\n");
        for(Planet planet : _universe.getPlanets())
        {
            result.append("{");
            result.append(quote("id")).append(": ").append(planet.getId()).append(", ");
            result.append(quote("owner")).append(": ").append(planet.getOwner()).append(", ");
            result.append(quote("ships")).append(": ").append(planet.getPopulation()).append(", ");
            result.append(quote("x")).append(": ").append(planet.getX()).append(", ");
            result.append(quote("y")).append(": ").append(planet.getY());
            result.append("},\n");
        }
        result.append("],");
        return result.toString();
    }

    private void printPlanets()
    {
        _resultStream.println(_initialPlanetState);
    }

    private void printEvents()
    {
        _resultStream.println(quote("events") + ": [");
        // Ugly hack to get the turns to print in order
        for (int cntr = 0; cntr <= _lastTurn; cntr++)
        {
            List<Event> events = _turnEventMap.get(cntr);
            if (events == null)
            {
                continue;
            }
            for (Event event : events)
            {
                if (event instanceof LaunchEvent)
                {
                    printLaunchEvent((LaunchEvent)event, cntr);
                }
                else if (event instanceof LandingEvent)
                {
                    printLandingEvent((LandingEvent)event, cntr);
                }
            }
        }
        _resultStream.println("]");
    }

    private void printLaunchEvent(LaunchEvent launchEvent, int turn)
    {
        _resultStream.println(braces(
                quote("destination") + ": " + launchEvent.getToPlanet() + ", " +
                        quote("duration") + ": " + launchEvent.getFlightDuration() + ", " +
                        quote("origin") + ": " + launchEvent.getFromPlanet() + ", " +
                        quote("player") + ": " + launchEvent.getFleetOwner() + ", " +
                        quote("ships") + ": " + launchEvent.getSentShipCount() + ", " +
                        quote("turn") + ": " + turn) + ",");
    }

    private void printLandingEvent(LandingEvent landingEvent, int turn)
    {
        if (landingEvent.getAfterBattleShipCount() <= 0)
        {
            return;
        }
        _resultStream.println(braces(
                quote("planet") + ": " + landingEvent.getToPlanet() + ", " +
                        quote("player") + ": " + landingEvent.getFleetOwner() + ", " +
                        quote("ships") + ": " + landingEvent.getAfterBattleShipCount() + ", " +
                        quote("turn") + ": " + turn) + ",");
    }
}
