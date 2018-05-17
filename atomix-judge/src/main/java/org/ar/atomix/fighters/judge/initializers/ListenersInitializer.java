package org.ar.atomix.fighters.judge.initializers;


import org.ar.atomix.fighters.judge.listeners.CheckHealthToStopFightListener;
import org.ar.atomix.fighters.judge.listeners.JudgingListener;
import org.ar.atomix.fighters.judge.listeners.RegisterNewFighterListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import javax.annotation.PostConstruct;

import io.atomix.core.map.AsyncConsistentMap;
import io.atomix.core.set.AsyncDistributedSet;

@Component
public class ListenersInitializer {

  @Autowired
  private JudgingListener judgingListener;

  @Autowired
  private RegisterNewFighterListener registerNewFighterListener;

  @Autowired
  private CheckHealthToStopFightListener checkHealthToStopFightListener;

  @Autowired
  private AsyncConsistentMap<String, Integer> healthMap;

  @Autowired
  private AsyncConsistentMap<String, List<Integer>> attackMap;

  @Autowired
  private AsyncDistributedSet<String> registrationSet;

  @PostConstruct
  public void initialize() {

    startJudging();

    listenHealthMap();

    listenRegistrationSet();
  }

  public void startJudging() {

    attackMap.addListener(judgingListener);
  }

  public void listenHealthMap() {

    healthMap.addListener(checkHealthToStopFightListener);
  }

  public void listenRegistrationSet() {

    registrationSet.addListener(registerNewFighterListener);
  }

}
