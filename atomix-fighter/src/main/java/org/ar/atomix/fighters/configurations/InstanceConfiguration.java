package org.ar.atomix.fighters.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;

@Configuration
public class InstanceConfiguration {

  @Bean
  public Member localMember(@Value("${atomix.fighter.host}") String host,
                            @Value("${atomix.fighter.port}") int port,
                            @Value("${atomix.fighter.name}") String name) {
    return Member.builder(name)
        .withType(Member.Type.EPHEMERAL)
        .withAddress(host, port)
        .build();
  }

  @Bean
  public Member clusterMember(@Value("${atomix.fighter.host}") String host,
                              @Value("${atomix.judge.port}") int judgePort) {
    return Member.builder("Judge")
        .withType(Member.Type.PERSISTENT)
        .withAddress(host, judgePort)
        .build();
  }

  @Bean
  public Atomix atomix(Member localMember, Member clusterMember) {
    Atomix atomix = Atomix.builder()
        .withClusterName("Distributed-Fighters")
        .withLocalMember(localMember)
        .withMembers(clusterMember)
        .build();

    return atomix.start().join();
  }
}
