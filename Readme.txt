Commands:
/score-get [playerName] - Displays the player's score.
/score-add [playerName] <amount> - Add amount points to the player's score.
/score-subtract [playerName] <amount> - Subtracts amount points from the player's score.
/score-reset [playerName] - Resets the player's score to 0.
/score-archive [playerName] - Saves the player's score to a "High Scores" table and resets their current score to 0.

NOTES:
* All commands will use the executing player in place of [playerName] if it is omitted.
* Permissions support is coming AFTER I get archive to work.
* The commands aside from get are intended for admins and whatnot as some other plugin should be using the methods instead of letting player's execute commands.

