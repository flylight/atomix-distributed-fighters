package org.ar.atomix.fighters.judge.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

import io.atomix.core.map.AsyncConsistentMap;
import io.atomix.core.set.SetEvent;
import io.atomix.core.set.SetEventListener;

@Component
public class RegisterNewFighterListener implements SetEventListener<String> {
  private static final Logger LG = Logger.getLogger(RegisterNewFighterListener.class.getName());
  private static final Integer INITIAL_FIGHTER_HEALTH_CAPACITY = 100;

  @Autowired
  private AsyncConsistentMap<String, Integer> healthMap;

  @Override
  public void event(SetEvent<String> event) {
    LG.info("Registering new fighter : " + event.entry());

    if (SetEvent.Type.ADD.equals(event.type())) {

      healthMap.put(event.entry(), INITIAL_FIGHTER_HEALTH_CAPACITY);
    }

  }

}
