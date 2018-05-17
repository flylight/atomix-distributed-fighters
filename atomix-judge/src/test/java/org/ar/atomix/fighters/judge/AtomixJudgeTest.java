package org.ar.atomix.fighters.judge;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

import io.atomix.core.Atomix;
import io.atomix.core.map.AsyncConsistentMap;
import io.atomix.core.set.DistributedSet;
import io.atomix.utils.time.Versioned;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AtomixJudgeTest {

  @Autowired
  private Atomix atomix;

  @Value("${registration.set.name}")
  private String registrationSetName;

  @Value("${health.map.name}")
  private String healthMapName;

  @Value("${attack.map.name}")
  private String attackMapName;

  @Value("${fight.state.val.name}")
  private String fightStateValueName;

  @AfterClass
  public static void deleteData() throws Exception {
    Path directory = new File(".data").toPath();
    if (Files.exists(directory)) {
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  @Test
  public void testHealthMaps() {
    AsyncConsistentMap<String, Integer> attackMap = atomix.<String, Integer>getConsistentMap(healthMapName)
        .async();

    Versioned<Integer> value = attackMap.put("test", 3344)
        .thenCompose(objectVersioned -> atomix
            .<String, Integer>getConsistentMap(healthMapName).async().get("test")).join();

    assertEquals(Integer.valueOf(3344), value.value());
  }

  @Test
  public void testAttackMaps() {
    AsyncConsistentMap<String, List<Integer>> attackMap = atomix.<String, List<Integer>>getConsistentMap(attackMapName)
        .async();

    Versioned<List<Integer>> result = attackMap.put("test", Collections.singletonList(100))
        .thenCompose(objectVersioned -> atomix.<String, List<Integer>>getConsistentMap(attackMapName)
            .async()
            .get("test"))
        .join();

    assertEquals(1, result.value().size());

    assertEquals(Integer.valueOf(100), result.value().get(0));
  }

  @Test
  public void testRegistrationSet() {
    DistributedSet<String> fightersSet = atomix.getSet(registrationSetName);

    Boolean added = fightersSet
        .async()
        .add("Test")
        .join();

    assertTrue(added);

    fightersSet = atomix.getSet(registrationSetName);

    assertFalse(fightersSet.isEmpty());
    assertEquals(1, fightersSet.size());
    assertTrue(fightersSet.contains("Test"));
  }

  @Test
  public void testNewFighterAppearing() throws InterruptedException {

    Boolean isTestFighterPresent = atomix.<String, Integer>getConsistentMap(healthMapName)
        .async()
        .containsKey("TestFighter")
        .join();

    assertFalse(isTestFighterPresent);

    Boolean addedNewFighter = atomix.<String>getSet(registrationSetName)
        .async()
        .add("TestFighter")
        .join();

    assertTrue(addedNewFighter);

    int attempts = 0;

    while (!isTestFighterPresent && attempts < 10) {

      isTestFighterPresent = atomix.<String, Integer>getConsistentMap(healthMapName)
          .async()
          .containsKey("TestFighter")
          .join();

      attempts++;
      Thread.sleep(1000);
    }

    Versioned<Integer> fighterHealth = atomix.<String, Integer>getConsistentMap(healthMapName)
        .async()
        .get("TestFighter")
        .join();

    assertEquals(Integer.valueOf(100), fighterHealth.value());
  }

  @Test
  public void testFight() throws InterruptedException {

    atomix.<Boolean>getAtomicValue(fightStateValueName).set(true);

    Boolean fightState = atomix.<Boolean>getAtomicValue(fightStateValueName).get();

    assertTrue(fightState);

    atomix.getConsistentMap(healthMapName)
        .async()
        .put("Fighter1", 100)
        .join();

    Boolean isFighterRegistered = atomix.getConsistentMap(healthMapName)
        .async()
        .containsKey("Fighter1")
        .join();

    assertTrue(isFighterRegistered);

    atomix.getConsistentMap(attackMapName)
        .async()
        .put("Fighter2", Collections.singletonList(100))
        .join();

    atomix.<Boolean>getAtomicValue(fightStateValueName).async().set(true).join();

    int iteration = 0;

    while (fightState && iteration < 10) {

      fightState = atomix.<Boolean>getAtomicValue(fightStateValueName).async().get().join();

      iteration++;

      Thread.sleep(1000);
    }

    assertTrue(iteration < 10);

    assertFalse(fightState);
  }
}
