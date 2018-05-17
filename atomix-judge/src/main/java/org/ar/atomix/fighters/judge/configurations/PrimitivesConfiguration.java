package org.ar.atomix.fighters.judge.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import io.atomix.core.Atomix;
import io.atomix.core.map.AsyncConsistentMap;
import io.atomix.core.set.AsyncDistributedSet;
import io.atomix.core.value.AsyncAtomicValue;

@Configuration
public class PrimitivesConfiguration {

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

  @Bean
  public AsyncDistributedSet<String> registrationSet() {

    return atomix.<String>setBuilder(registrationSetName).buildAsync().join().async();
  }

  @Bean
  public AsyncConsistentMap<String, Integer> healthMap() {

    return atomix.<String, Integer>consistentMapBuilder(healthMapName).buildAsync().join().async();
  }

  @Bean
  public AsyncConsistentMap<String, List<Integer>> attackMap() {

    return atomix.<String, List<Integer>>consistentMapBuilder(attackMapName).buildAsync().join().async();
  }

  @Bean
  public AsyncAtomicValue<Boolean> fightState() {

    AsyncAtomicValue<Boolean> fightState =
        atomix.<Boolean>atomicValueBuilder(fightStateValueName).buildAsync().join().async();


    fightState.set(false);

    return fightState;
  }
}
