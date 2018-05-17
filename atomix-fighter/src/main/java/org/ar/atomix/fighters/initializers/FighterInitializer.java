package org.ar.atomix.fighters.initializers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import io.atomix.core.set.AsyncDistributedSet;

@Component
public class FighterInitializer {

  @Value("${atomix.fighter.name}")
  private String fighterName;

  @Autowired
  private AsyncDistributedSet<String> registrationSet;

  @PostConstruct
  private void initialize() {

    registerOnFight();

  }

  private void registerOnFight() {

    registrationSet.add(fighterName);

  }
}
