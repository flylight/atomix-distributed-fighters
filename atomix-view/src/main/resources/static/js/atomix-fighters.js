function startStatusCheck() {

    setInterval(checkStatus, 1000);

    $("#start-check-button").hide();
}

function checkStatus() {

    $.get("status", function (data) {

        updateUIAccordingToStatus(data);

    });
}

function startFight() {
    $.post("fight", function () {
    });
}

function resetFight() {
    $.post("fight/restart", function () {
    });
}

function updateUIAccordingToStatus(status) {

    if (status.fighters) {

        if (status.fighters[0]) {

            updateFighterStatus("left", status.fighters[0]);
        } else {

            $("#left-fighter-status").html("Status: Fighter not registered");
        }

        if (status.fighters[1]) {

            updateFighterStatus("right", status.fighters[1]);
        } else {

            $("#right-fighter-status").html("Status: Fighter not registered");
        }

    }

    if (!status.fighting) {

        if (status.fighters && status.fighters[0] && status.fighters[1]) {

            if (status.fighters[0].health === 100 && status.fighters[1].health === 100) {

                $("#start-fight-button").show();
                $("#reset-fight-button").hide();
            } else {

                $("#reset-fight-button").show();
                $("#start-fight-button").hide();
            }

        } else {
            $("#start-fight-button").hide();
            $("#reset-fight-button").hide();
        }

    } else {
        $("#start-fight-button").hide();
        $("#reset-fight-button").hide();
    }
}

function updateFighterStatus(fighterPrefix, fighterData) {

    var statusList = resolveStatusLine(fighterData);

        $("#" + fighterPrefix + "-fighter-status").html(statusList);

    if (fighterData.attacks) {

        var attackList = "";

        for (var i in fighterData.attacks) {

            var attack = fighterData.attacks[fighterData.attacks.length - i - 1];

            attackList += "<p>Attack : " + retrieveAttackColor(attack) + " !</p>";

        }

        $("#" + fighterPrefix + "-fighter-attacks").html(attackList);
    }
}

function resolveStatusLine(fighterData) {

    return "Status: "
           + (fighterData.health <= 0 ? "LOST" : "OK")
           + " (" + fighterData.name + " | "
           + retrieveHealthColor(fighterData.health) + ")";
}

function retrieveAttackColor(attack) {

    if (attack <= 15) {

        return "<span style=\"color:green;font-weight:bold;\">" + attack + "</span>"

    }

    return "<span style=\"color:orangered;font-weight:bold;\">" + attack + "</span>"
}

function retrieveHealthColor(health) {

    if (health < 20) {

        return "<span style=\"color:red;font-weight:bold;\">" + resolveHealthPoints(health) + "</span>"

    } else if (health < 50) {

        return "<span style=\"color:darkorange;font-weight:bold;\">" + resolveHealthPoints(health) + "</span>"

    } else if (health < 75) {

        return "<span style=\"color:saddlebrown;font-weight:bold;\">" + resolveHealthPoints(health) + "</span>"
    }

    return "<span style=\"color:black;font-weight:bold;\">" + resolveHealthPoints(health) + "</span>";
}

function resolveHealthPoints(health) {
    return health < 0 ? 0 : health;
}
