package org.ar.atomix.fighters.judge.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

import io.atomix.core.map.MapEvent;
import io.atomix.core.map.MapEventListener;
import io.atomix.core.value.AsyncAtomicValue;

@Component
public class CheckHealthToStopFightListener implements MapEventListener<String, Integer> {

  private static final Logger LG = Logger.getLogger(CheckHealthToStopFightListener.class.getName());

  @Autowired
  private AsyncAtomicValue<Boolean> fightState;

  @Override
  public void event(MapEvent<String, Integer> event) {
    LG.info("Health map activity : " + event.type());

    if (MapEvent.Type.UPDATE.equals(event.type()) && event.newValue().value() <= 0) {

      fightState.set(false);

    }
  }
}
