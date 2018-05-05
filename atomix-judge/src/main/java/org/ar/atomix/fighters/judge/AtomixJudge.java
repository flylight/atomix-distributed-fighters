package org.ar.atomix.fighters.judge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;
import io.atomix.core.map.AsyncConsistentMap;
import io.atomix.core.map.ConsistentMap;
import io.atomix.core.map.MapEvent;
import io.atomix.core.set.DistributedSet;
import io.atomix.core.set.SetEvent;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;

@Component
public class AtomixJudge {

  public static final String FIGHT_STATE_VAR_NAME = "fightState";
  public static final String HEALTH_MAP_VAR_NAME = "healthMap";
  public static final String REGISTRATION_SET_VAR_NAME = "fightersSet";
  public static final String ATTACK_MULTI_MAP_VAR_NAME = "attackMap";

  private static final Integer INITIAL_FIGHTER_HEALTH_CAPACITY = 100;

  private static final Logger LG = Logger.getLogger(AtomixJudge.class.getName());

  private final Atomix node;

  public AtomixJudge(@Value("${atomix.judge.host}") String host, @Value("${atomix.judge.port}") int port) {

    Member judgeMember = Member.builder("Judge")
        .withType(Member.Type.PERSISTENT)
        .withAddress(host, port)
        .build();

    Atomix atomix = Atomix.builder()
        .withClusterName("Distributed-Fighters")
        .withLocalMember(judgeMember)
        .withManagementGroup(RaftPartitionGroup.builder("system")
            .withMembers(judgeMember)
            .withNumPartitions(1)
            .build())
        .withPartitionGroups(
            RaftPartitionGroup.builder("data")
                .withNumPartitions(1)
                .withMembers(judgeMember)
                .build())
        .build();

    node = atomix.start().join();

    LG.info("Judge node started on port : " + port);
  }

  public Atomix getNode() {
    return node;
  }

  @PostConstruct
  public void buildResources() {
    initRegistrationSet();

    initHealthMap();

    initAttackMap();

    initFightState();

    listenRegistrationSet();

    listenHealthMap();

    startJudging();
  }

  /*
   Example of resource listeners
  */

  private void startJudging() {

    node.<String, List<Integer>>getConsistentMap(ATTACK_MULTI_MAP_VAR_NAME)
        .async()
        .addListener(this::judgingListener);
  }

  private void listenHealthMap() {

    node.<String, Integer>getConsistentMap(HEALTH_MAP_VAR_NAME)
        .async()
        .addListener(this::stopFightIfNoMoreHealthListener);
  }

  private void listenRegistrationSet() {

    node.<String>getSet(REGISTRATION_SET_VAR_NAME)
        .async()
        .addListener(this::registerNewFighterListener);
  }

  /*
   Example of distributed resource creation
  */

  private CompletableFuture<DistributedSet<String>> initRegistrationSet() {

    return node.<String>setBuilder(REGISTRATION_SET_VAR_NAME).buildAsync();
  }

  private CompletableFuture<ConsistentMap<String, Integer>> initHealthMap() {

    return node.<String, Integer>consistentMapBuilder(HEALTH_MAP_VAR_NAME).buildAsync();
  }

  private CompletableFuture<ConsistentMap<String, List<Integer>>> initAttackMap() {

    return node.<String, List<Integer>>consistentMapBuilder(ATTACK_MULTI_MAP_VAR_NAME).buildAsync();
  }

  private void initFightState() {
    node.getAtomicValue(FIGHT_STATE_VAR_NAME).async().set(false);
  }

  /*
   Listeners
  */

  private void registerNewFighterListener(SetEvent<String> event) {

    LG.info("Registering new fighter : " + event.entry());

    if (SetEvent.Type.ADD.equals(event.type())) {

      node.<String, Integer>getConsistentMap(HEALTH_MAP_VAR_NAME)
          .async().put(event.entry(), INITIAL_FIGHTER_HEALTH_CAPACITY);

    }
  }

  private void stopFightIfNoMoreHealthListener(MapEvent<String, Integer> event) {

    LG.info("Health map activity : " + event.type());

    if (MapEvent.Type.UPDATE.equals(event.type()) && event.newValue().value() <= 0) {

      node.<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME)
          .async()
          .set(false);

    }
  }

  private void judgingListener(MapEvent<String, List<Integer>> event) {

    LG.info("Attack map activity : " + event.type());

    if (MapEvent.Type.INSERT.equals(event.type())) {

      String fighterName = event.key();

      List<Integer> attackList = event.newValue().value();

      Integer latestAttack = attackList.get(attackList.size() - 1);

      AsyncConsistentMap<String, Integer> healthMap = node.<String, Integer>getConsistentMap(HEALTH_MAP_VAR_NAME)
          .async();

      healthMap.keySet()
          .whenComplete((keys, throwable) -> {

            keys.stream().filter(someFighterName -> !someFighterName.equals(fighterName))
                .forEach(someFighterName -> {

                  healthMap.computeIfPresent(someFighterName, (s, health) -> health - latestAttack);

                });

          });

    }
  }
}
