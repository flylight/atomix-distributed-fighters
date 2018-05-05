package org.ar.atomix.fighters;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import io.atomix.utils.time.Versioned;

import static org.ar.atomix.fighters.AtomixFighter.ATTACK_MAP_VAR_NAME;
import static org.ar.atomix.fighters.AtomixFighter.FIGHT_STATE_VAR_NAME;
import static org.ar.atomix.fighters.AtomixFighter.REGISTRATION_SET_VAR_NAME;
import static org.junit.Assert.assertTrue;

public class AtomixFighterTest {

  private static AtomixFighter fighter;

  @BeforeClass
  public static void startJudgeEmulation() {

    Atomix cluster = startPersistentNode("localhost", 5000);

    cluster.<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME)
        .async()
        .set(false)
        .join();

    cluster.<String, List<Integer>>consistentMapBuilder(ATTACK_MAP_VAR_NAME).buildAsync().join();

    fighter = new AtomixFighter("localhost", 5007, 5000, "Test");
    fighter.prepareToFight();
  }

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

  private static Atomix startPersistentNode(String host, int port) {
    Member judgeMember = Member.builder("TestCluster")
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

    return atomix.start().join();
  }

  @Test
  public void testRegistration() {

    boolean registered = false;

    int attempts = 0;

    while (!registered && attempts < 10) {

      registered = fighter.getNode().getSet(REGISTRATION_SET_VAR_NAME).async().contains("Test").join();

      attempts++;
    }

    assertTrue(attempts < 10);
  }

  @Test
  public void testFight() throws InterruptedException {

    fighter.getNode().<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME).async().set(true).join();

    Thread.sleep(3000);

    fighter.getNode().<Boolean>getAtomicValue(FIGHT_STATE_VAR_NAME).async().set(false).join();

    Versioned<List<Integer>> attackList = fighter.getNode().<String, List<Integer>>getConsistentMap(ATTACK_MAP_VAR_NAME)
        .async()
        .get("Test")
        .join();

    assertTrue(!attackList.value().isEmpty());

  }
}
