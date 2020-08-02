package de.hda.fbi.db2.stud.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

//import javax.persistence.Entity;
//import javax.persistence.Id;
import java.util.Objects;
import javax.persistence.*;

/**
 * Player Class.
 */

@Entity
@Table(name = "Player", schema = "myschema")
@NamedQueries({
    @NamedQuery(name = "Player.findAllNames",
        query = "select p.name from Player p"),
    @NamedQuery(name = "Player.numberOfPlayers",
        query = "select count(p.name) from Player p"),
    @NamedQuery(name = "Player.findByName",
        query = "select p from Player p where p.name=:name")})
public class Player {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @Column(unique = true, nullable = false)
  private String name;

  @OneToMany(mappedBy = "playingGamer", cascade = { CascadeType.PERSIST })
  private List<Match> matchList = new ArrayList();

  public List<Match> getMatchList(){
    return matchList;
  }

  public void setMatchList(List<Match> matchList) {
    this.matchList = matchList;
  }

  public Player(){// Default constructor
    this.name = "empty";
  }

  public Player(String name){
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setNewMatch(Match newMatch) {
    matchList.add(newMatch);
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Player player = (Player) o;
    return Objects.equals(id, player.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
