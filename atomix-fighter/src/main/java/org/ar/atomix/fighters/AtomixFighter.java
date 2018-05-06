package org.ar.atomix.fighters;

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

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;
import io.atomix.core.map.AsyncConsistentMap;

@Component
public class AtomixFighter {

  public static final String FIGHT_STATE_VAR_NAME = "fightState";
  public static final String REGISTRATION_SET_VAR_NAME = "fightersSet";
  public static final String ATTACK_MAP_VAR_NAME = "attackMap";

  private static final int MAX_ATTACK = 20;
  private static final Logger LG = Logger.getLogger(AtomixFighter.class.getName());

  private final Atomix node;

  private final String fighterName;

  public AtomixFighter(@Value("${atomix.fighter.host}") String host,
                       @Value("${atomix.fighter.port}") int port,
                       @Value("${atomix.judge.port}") int judgePort,
                       @Value("${atomix.fighter.name}") String name) {

    this.fighterName = name;

    Member localMember = Member.builder(name)
        .withType(Member.Type.EPHEMERAL)
        .withAddress(host, port)
        .build();

    Member judgeMember = Member.builder("Judge")
        .withType(Member.Type.PERSISTENT)
        .withAddress(host, judgePort)
        .build();

    Atomix atomix = Atomix.builder()
        .withClusterName("Distributed-Fighters")
        .withLocalMember(localMember)
        .withMembers(judgeMember)
        .build();

    node = atomix.start().join();
  }

  public Atomix getNode() {

    return node;
  }

  @PostConstruct
  public void prepareToFight() {

    registerOnFight();

    attackIfFightBegin(Executors.newSingleThreadScheduledExecutor(), new SecureRandom());
  }

  private void registerOnFight() {

    node.<String>getSet(REGISTRATION_SET_VAR_NAME)
        .async()
        .add(fighterName);
  }

  private void attackIfFightBegin(ScheduledExecutorService scheduler, SecureRandom attackRandomizer) {

    node.<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME)
        .async()
        .get()
        .whenComplete((isFightStarted, throwable) -> {

          if (isFightStarted) {

            attack(attackRandomizer.nextInt(MAX_ATTACK));

          }

        });

    scheduler.schedule(() -> attackIfFightBegin(scheduler, attackRandomizer), 1, TimeUnit.SECONDS);
  }

  private void attack(int attack) {

    LG.info(fighterName + " attack with : " + attack);

    AsyncConsistentMap<String, List<Integer>> asyncAttackMap =
        node.<String, List<Integer>>getConsistentMap(ATTACK_MAP_VAR_NAME).async();

    asyncAttackMap.get(fighterName)
        .whenComplete((versionedAttacks, throwable) -> {

          List<Integer> attacks = Objects.nonNull(versionedAttacks) ? versionedAttacks.value() : new ArrayList<>();

          attacks.add(attack);

          asyncAttackMap.put(fighterName, attacks);

        });
  }

}
