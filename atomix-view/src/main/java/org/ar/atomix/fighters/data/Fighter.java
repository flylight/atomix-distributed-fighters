package org.ar.atomix.fighters.data;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Fighter {

  private String name;

  private Integer health;

  private List<Integer> attacks;
}
