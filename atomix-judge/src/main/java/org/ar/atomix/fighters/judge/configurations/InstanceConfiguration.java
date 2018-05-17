package org.ar.atomix.fighters.judge.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;

@Configuration
public class InstanceConfiguration {

  @Bean
  public Member localMember(@Value("${atomix.judge.host}") String host,
                            @Value("${atomix.judge.port}") int port) {

    return Member.builder("Judge")
        .withType(Member.Type.PERSISTENT)
        .withAddress(host, port)
        .build();
  }

  @Bean
  public Atomix atomix(Member localMember) {

    Atomix atomix = Atomix.builder()
        .withClusterName("Distributed-Fighters")
        .withLocalMember(localMember)
        .withManagementGroup(RaftPartitionGroup.builder("system")
            .withMembers(localMember)
            .withNumPartitions(1)
            .build())
        .withPartitionGroups(
            RaftPartitionGroup.builder("data")
                .withNumPartitions(1)
                .withMembers(localMember)
                .build())
        .build();

    return atomix.start().join();
  }

}
