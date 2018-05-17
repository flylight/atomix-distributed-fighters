package org.ar.atomix.fighters.judge.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.logging.Logger;

import io.atomix.core.map.AsyncConsistentMap;
import io.atomix.core.map.MapEvent;
import io.atomix.core.map.MapEventListener;

@Component
public class JudgingListener implements MapEventListener<String, List<Integer>> {

  private static final Logger LG = Logger.getLogger(JudgingListener.class.getName());

  @Autowired
  private AsyncConsistentMap<String, Integer> healthMap;

  @Override
  public void event(MapEvent<String, List<Integer>> event) {
    LG.info("Attack map activity : " + event.type());

    if (MapEvent.Type.INSERT.equals(event.type()) || MapEvent.Type.UPDATE.equals(event.type())) {

      String fighterName = event.key();

      List<Integer> attackList = event.newValue().value();

      if (!attackList.isEmpty()) {
        Integer latestAttack = attackList.get(attackList.size() - 1);

        healthMap.keySet()
            .whenComplete((keys, throwable) -> {

              keys.stream().filter(someFighterName -> !someFighterName.equals(fighterName))
                  .forEach(someFighterName -> {

                    healthMap.computeIfPresent(someFighterName, (s, health) -> health - latestAttack);

                  });

            });
      }
    }
  }
}
