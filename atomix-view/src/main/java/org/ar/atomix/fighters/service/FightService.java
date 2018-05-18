package org.ar.atomix.fighters.service;

import org.ar.atomix.fighters.data.Fighter;
import org.ar.atomix.fighters.data.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.atomix.core.Atomix;
import io.atomix.utils.time.Versioned;

@Service
public class FightService {

  private static final String FIGHT_STATE_VAR_NAME = "fightState";
  private static final String HEALTH_MAP_VAR_NAME = "healthMap";
  private static final String REGISTRATION_SET_VAR_NAME = "fightersSet";
  private static final String ATTACK_MAP_VAR_NAME = "attackMap";

  @Autowired
  private Atomix atomix;

  public Status getStatus() {

    ArrayList<Fighter> fighters = new ArrayList<>();

    atomix.<String>getSet(REGISTRATION_SET_VAR_NAME).forEach(fighterName -> {

      Versioned<Integer> health = atomix.<String, Integer>getConsistentMap(HEALTH_MAP_VAR_NAME).get(fighterName);
      Versioned<List<Integer>> attacks = atomix.<String, List<Integer>>getConsistentMap(ATTACK_MAP_VAR_NAME).get(fighterName);

      fighters.add(Fighter.builder()
          .name(fighterName)
          .health(Objects.nonNull(health) ? health.value() : 0)
          .attacks(Objects.nonNull(attacks) ? attacks.value() : null)
          .build());
    });

    //fighters.sort(Comparator.comparing(Fighter::getName));

    boolean isFighting = atomix.<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME).get();

    return Status.builder()
        .fighters(fighters)
        .isFighting(isFighting)
        .build();
  }

  public void startFight() {

    atomix.<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME)
        .async()
        .set(true);
  }

  public void restartFight() {
    atomix.<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME)
        .async()
        .set(false);

    atomix.<String, List<Integer>>getConsistentMap(ATTACK_MAP_VAR_NAME)
        .async()
        .keySet().whenComplete((fighters, throwable) -> {

      fighters.forEach(fighter -> {
        atomix.<String, List<Integer>>getConsistentMap(ATTACK_MAP_VAR_NAME)
            .async()
            .put(fighter, new ArrayList<>());
      });
    });

    atomix.<String, Integer>getConsistentMap(HEALTH_MAP_VAR_NAME)
        .async()
        .keySet().whenComplete((fighters, throwable) -> {


      fighters.forEach(fighter -> {
        atomix.<String, Integer>getConsistentMap(HEALTH_MAP_VAR_NAME)
            .async()
            .put(fighter, 100);
      });
    });
  }
}
