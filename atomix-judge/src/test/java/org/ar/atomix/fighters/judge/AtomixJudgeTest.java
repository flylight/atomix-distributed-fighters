package org.ar.atomix.fighters.judge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import io.atomix.core.map.AsyncConsistentMap;
import io.atomix.core.set.DistributedSet;
import io.atomix.utils.time.Versioned;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.ar.atomix.fighters.judge.AtomixJudge.ATTACK_MAP_VAR_NAME;
import static org.ar.atomix.fighters.judge.AtomixJudge.FIGHTER_SET_VAR_NAME;
import static org.ar.atomix.fighters.judge.AtomixJudge.FIGHT_STATE_VAR_NAME;
import static org.ar.atomix.fighters.judge.AtomixJudge.HEALTH_MAP_VAR_NAME;
import static org.junit.Assert.assertFalse;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AtomixJudgeTest {

  @Autowired
  private AtomixJudge judge;

  @Test
  public void testHealthMaps() {
    AsyncConsistentMap<String, Integer> attackMap = judge.getNode().<String, Integer>getConsistentMap(HEALTH_MAP_VAR_NAME).async();

    Versioned<Integer> value = attackMap.put("test", 3344)
        .thenCompose(objectVersioned -> judge.getNode()
            .<String, Integer>getConsistentMap(HEALTH_MAP_VAR_NAME).async().get("test")).join();

    assertEquals(Integer.valueOf(3344), value.value());
  }

  @Test
  public void testAttackMaps() {
    AsyncConsistentMap<String, Integer> attackMap = judge.getNode().<String, Integer>getConsistentMap(ATTACK_MAP_VAR_NAME)
        .async();

    Versioned<Integer> value = attackMap.put("test", 10)
        .thenCompose(objectVersioned -> judge.getNode().<String, Integer>getConsistentMap(ATTACK_MAP_VAR_NAME)
            .async()
            .get("test")).join();

    assertEquals(Integer.valueOf(10), value.value());
  }

  @Test
  public void testRegistrationSet() {
    DistributedSet<String> fightersSet = judge.getNode().getSet(FIGHTER_SET_VAR_NAME);

    Boolean added = fightersSet.async().add("Test").join();

    assertTrue(added);

    fightersSet = judge.getNode().getSet(FIGHTER_SET_VAR_NAME);

    assertFalse(fightersSet.isEmpty());
    assertEquals(1, fightersSet.size());
    assertTrue(fightersSet.contains("Test"));
  }

  @Test
  public void fightTest() throws InterruptedException {

    judge.getNode().<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME).set(true);

    Boolean fightState = judge.getNode().<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME).get();

    assertTrue(fightState);

    judge.getNode().getConsistentMap(HEALTH_MAP_VAR_NAME).async().put("Fighter1", 100).join();

    Boolean isFighterRegistered = judge.getNode().getConsistentMap(HEALTH_MAP_VAR_NAME).async().containsKey("Fighter1").join();

    assertTrue(isFighterRegistered);

    judge.getNode().getConsistentMap(ATTACK_MAP_VAR_NAME).async().put("Fighter2", 100).join();

    int iteration = 0;

    while (fightState || iteration < 10) {

      fightState = judge.getNode().<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME).async().get().join();

      iteration++;
    }

    assertFalse(fightState);
  }

}
