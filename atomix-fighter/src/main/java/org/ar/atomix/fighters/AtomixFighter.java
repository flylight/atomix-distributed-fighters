package org.ar.atomix.fighters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;
import io.atomix.protocols.backup.partition.PrimaryBackupPartitionGroup;

@Component
public class AtomixFighter {

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

    Member judgeMember = Member.builder(name)
        .withType(Member.Type.EPHEMERAL)
        .withAddress(host, judgePort)
        .build();

    Atomix atomix = Atomix.builder()
        .withClusterName("Distributed-Fighters")
        .withLocalMember(localMember)
        .withMembers(judgeMember)
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

  @PostConstruct
  private void registerFighter() {

    node.getSet("fightersSet").add(fighterName);

  }
}
