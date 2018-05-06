package org.ar.atomix.fighters.data;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Status {

  private boolean isFighting;

  private List<Fighter> fighters;

}
