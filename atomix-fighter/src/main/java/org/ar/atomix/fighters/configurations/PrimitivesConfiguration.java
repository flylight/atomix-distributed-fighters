package org.ar.atomix.fighters.configurations;

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

  @Bean
  public AsyncDistributedSet<String> registrationSet(
      Atomix atomix, @Value("${registration.set.name}") String registrationSetName) {

    return atomix.<String>getSet(registrationSetName).async();
  }

  @Bean
  public AsyncAtomicValue<Boolean> fightState(
      Atomix atomix, @Value("${fight.state.val.name}") String fightStateName) {

    return atomix.<Boolean>getAtomicValue(fightStateName).async();
  }

  @Bean
  public AsyncConsistentMap<String, List<Integer>> attackMap(
      Atomix atomix, @Value("${attack.map.name}") String attackMapName) {

    return atomix.<String, List<Integer>>getConsistentMap(attackMapName).async();
  }
}
