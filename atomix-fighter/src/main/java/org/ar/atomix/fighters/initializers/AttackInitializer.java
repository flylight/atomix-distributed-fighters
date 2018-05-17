package org.ar.atomix.fighters.initializers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import io.atomix.core.map.AsyncConsistentMap;
import io.atomix.core.value.AsyncAtomicValue;

@Component
public class AttackInitializer {
  private static final int MAX_ATTACK = 20;

  private static final Logger LG = Logger.getLogger(AttackInitializer.class.getName());

  @Value("${atomix.fighter.name}")
  private String fighterName;

  @Autowired
  private AsyncAtomicValue<Boolean> fightState;

  @Autowired
  private AsyncConsistentMap<String, List<Integer>> attackMap;

  @PostConstruct
  private void initialize() {
    attackIfFightBegin(Executors.newSingleThreadScheduledExecutor(), new SecureRandom());
  }

  private void attackIfFightBegin(ScheduledExecutorService scheduler, SecureRandom attackRandomizer) {

    fightState.get()
        .whenComplete((isFightStarted, throwable) -> {

          if (isFightStarted) {

            attack(attackRandomizer.nextInt(MAX_ATTACK));

          }

        });

    scheduler.schedule(() -> attackIfFightBegin(scheduler, attackRandomizer), 1, TimeUnit.SECONDS);
  }

  private void attack(int attack) {

    LG.info(fighterName + " attack with : " + attack);

    attackMap.get(fighterName)
        .whenComplete((versionedAttacks, throwable) -> {

          List<Integer> attacks = Objects.nonNull(versionedAttacks) ? versionedAttacks.value() : new ArrayList<>();

          attacks.add(attack);

          attackMap.put(fighterName, attacks);

        });
  }
}
