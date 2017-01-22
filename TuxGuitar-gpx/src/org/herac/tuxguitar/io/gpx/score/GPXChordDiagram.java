package org.herac.tuxguitar.io.gpx.score;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by mattipesonen on 23/05/16.
 */
public class GPXChordDiagram {

    private int id;
    private String name;

    private int firstFret;
    private List<Integer> frets;

    public GPXChordDiagram(int id, String name)
    {
        this.id = id;
        this.name = new String(name);
        // Looking at GP5 behaviour, this is always 1
        this.setFirstFret(1);

        this.frets = new ArrayList<Integer>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getFrets() { return this.frets; }

    public void addFret(Integer fret)
    {
        this.frets.add(fret);
    }

    public int getFirstFret() {
        return firstFret;
    }

    public void setFirstFret(int firstFret) {
        this.firstFret = firstFret;
    }
}
