package com.linkedin.domination.sample;

import com.linkedin.domination.api.*;

import java.util.*;

/**
 * This is a dumb AI.
 */
public class SamplePlayer implements Player
{
    private int me;

    public SamplePlayer() {
        me = 0;
    }

    @Override
    public String getPlayerName() {
        return "Scarecrow";
    }

    @Override
    public void initialize(Integer playerNbr) {
        me = playerNbr;
    }

    @Override
    public List<Move> makeMove(Universe universe, List<Event> events) {
        List<Planet> myPlanets = getMyPlanets(universe);
        List<Planet> targets = getTargetPlanetsForPerson(universe, largestEnemy(universe));

        List<Move> myMoves = new ArrayList<Move>(myPlanets.size());
      Set<Planet> underAttack = new HashSet<Planet>();
      for (Event e : events) {
        if (e.getEventType() == Event.EventType.LAUNCH && e.getFleetOwner() != me) {
          Planet destination = universe.getPlanetMap().get(e.getToPlanet());
          if (destination.getOwner() == me) {
            // Someone is attacking me, the jerk!
            Move attack = new Move(destination, universe.getPlanetMap().get(e.getFromPlanet()), Move.FleetType.RAIDING);
            myMoves.add(attack);
            underAttack.add(destination);

          }
        }
      }
        for(Planet planet : myPlanets) {
            if(planet.getSize().equals(Size.LARGE) && !underAttack.contains(planet))
            {
                Move attack = new Move(planet, getClosestPlanet(planet, targets), Move.FleetType.RAIDING);
                myMoves.add(attack);
            }
        }

        return myMoves;
    }

    private Planet getClosestPlanet(Planet ideal, Collection<Planet> planets)
    {
        Planet closest = null;
        int nearestDistance = Integer.MAX_VALUE;

        for(Planet planet : planets)
        {
            int distanceFromIdeal = Universe.getTimeToTravel(planet, ideal);
            if (distanceFromIdeal < nearestDistance)
            {
                nearestDistance = distanceFromIdeal;
                closest = planet;
            }
        }

        return closest;
    }

    private List<Planet> getTargetPlanetsForPerson(Universe universe, int target)
    {
        List<Planet> targetPlanets = new ArrayList<Planet>();
        for(Planet planet : universe.getPlanets())
        {
            if (planet.getOwner() == target)
            {
                targetPlanets.add(planet);
            }
        }
        return targetPlanets;
    }

    private int largestEnemy(Universe universe)
    {
        Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
        for(Planet planet : universe.getPlanets())
        {
            int owner = planet.getOwner();
            if (owner != me)
            {
                int currentCount = counts.containsKey(owner) ? counts.get(owner) : 0;
                counts.put(owner, currentCount + 1);
            }
        }
        int max = Integer.MIN_VALUE;
        int biggestOwner = 0;
        for(int key : counts.keySet())
        {
            if (counts.get(key) > max)
            {
                max = counts.get(key);
                biggestOwner = key;
            }
        }
        return biggestOwner;
    }

    private List<Planet> getMyPlanets(Universe universe) {
        List<Planet> myPlanets = new ArrayList<Planet>();
        for(Planet planet : universe.getPlanets())
        {
            if (planet.getOwner() == me)
            {
                myPlanets.add(planet);
            }
        }
        return myPlanets;
    }
}
