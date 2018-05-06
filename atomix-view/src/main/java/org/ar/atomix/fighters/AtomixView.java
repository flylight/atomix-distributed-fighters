package org.ar.atomix.fighters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;

@Component
public class AtomixView {

  private final Atomix node;

  public AtomixView(@Value("${atomix.fighter.host}") String host,
                    @Value("${atomix.fighter.port}") int port,
                    @Value("${atomix.judge.port}") int judgePort) {

    Member localMember = Member.builder("View")
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
}
