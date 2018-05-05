package org.ar.atomix.fighters.judge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;
import io.atomix.core.map.AsyncConsistentMap;
import io.atomix.core.map.ConsistentMap;
import io.atomix.core.map.MapEvent;
import io.atomix.core.set.DistributedSet;
import io.atomix.protocols.backup.partition.PrimaryBackupPartitionGroup;

@Component
public class AtomixJudge {

  public static final String FIGHT_STATE_VAR_NAME = "fightState";
  public static final String HEALTH_MAP_VAR_NAME = "healthMap";
  public static final String FIGHTER_SET_VAR_NAME = "fightersSet";
  public static final String ATTACK_MAP_VAR_NAME = "attackMap";

  private final Atomix node;

  public AtomixJudge(@Value("${atomix.judge.host}") String host, @Value("${atomix.judge.port}") int port) {
    Member localMember = Member.builder("Judge")
        .withType(Member.Type.EPHEMERAL)
        .withAddress(host, port)
        .build();

    Atomix atomix = Atomix.builder()
        .withClusterName("Distributed-Fighters")
        .withLocalMember(localMember)
        .withMembers(localMember)
        .withManagementGroup(PrimaryBackupPartitionGroup
            .builder("FighterGroup")
            .withNumPartitions(1)
            .build())
        .withPartitionGroups(
            PrimaryBackupPartitionGroup.builder("data")
                .withNumPartitions(4)
                .build())
        .build();

    node = atomix.start().join();
  }

  public Atomix getNode() {
    return node;
  }

  @PostConstruct
  private void buildResources() {
    initRegistrationSet();

    initHealthMap();

    initAttackMap();

    listenHealthMap();

    startJudging();
  }

  /*
   Example of resource listeners
  */

  private void startJudging() {

    node.<String, Integer>getConsistentMap(ATTACK_MAP_VAR_NAME)
        .async()
        .addListener(this::judging);

  }

  private void listenHealthMap() {

    node.<String, Integer>getConsistentMap(HEALTH_MAP_VAR_NAME)
        .async()
        .addListener(this::stopFightIfNoMoreHealth);
  }

  /*
   Example of distributed resource creation
  */

  private CompletableFuture<DistributedSet<String>> initRegistrationSet() {

    return node.<String>setBuilder(FIGHTER_SET_VAR_NAME).buildAsync();
  }

  private CompletableFuture<ConsistentMap<String, Integer>> initHealthMap() {

    return node.<String, Integer>consistentMapBuilder(HEALTH_MAP_VAR_NAME).buildAsync();
  }

  private CompletableFuture<ConsistentMap<String, Integer>> initAttackMap() {

    return node.<String, Integer>consistentMapBuilder(ATTACK_MAP_VAR_NAME).buildAsync();
  }

  /*
   Behavior
  */

  private void stopFightIfNoMoreHealth(MapEvent<String, Integer> event) {

    if (MapEvent.Type.UPDATE.equals(event.type()) && event.newValue().value() <= 0) {

      node.<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME)
          .async()
          .set(false);

    }
  }

  private void judging(MapEvent<String, Integer> event) {

    if (MapEvent.Type.INSERT.equals(event.type())) {

      String fighterName = event.key();
      Integer attack = event.newValue().value();

      AsyncConsistentMap<String, Integer> helthMap = node.<String, Integer>getConsistentMap(HEALTH_MAP_VAR_NAME)
          .async();

      helthMap.keySet()
          .whenComplete((keys, throwable) -> {

            keys.stream().filter(someFighterName -> !someFighterName.equals(fighterName))
                .forEach(someFighterName -> {

                  helthMap.computeIfPresent(someFighterName, (s, health) -> health - attack);

                });

          });

    }

  }
}
