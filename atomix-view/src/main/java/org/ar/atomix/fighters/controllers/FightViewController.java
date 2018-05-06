package org.ar.atomix.fighters.controllers;

import org.ar.atomix.fighters.data.Status;
import org.ar.atomix.fighters.service.FightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class FightViewController {

  @Autowired
  private FightService fightService;

  @GetMapping
  public String getMainPage() {
    return "index.html";
  }

  @GetMapping("status")
  @ResponseBody
  public Status getStatus() {

    return fightService.getStatus();
  }

  @PostMapping("fight")
  @ResponseBody
  public String startFight() {

    fightService.startFight();

    return "started";
  }

  @PostMapping("fight/restart")
  @ResponseBody
  public String resetFight() {

    fightService.restartFight();

    return "restarted";
  }
}
