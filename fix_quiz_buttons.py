import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/QuizScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

# Replace Blitz Arena onClick
blitz_start = code.find("// Blitz Arena")
if blitz_start != -1:
    blitz_onclick_pos = code.find("onClick = { showComingSoon = true }", blitz_start)
    if blitz_onclick_pos != -1:
        code = code[:blitz_onclick_pos] + "onClick = { screenModel.findMultiplayerMatch() }" + code[blitz_onclick_pos + len("onClick = { showComingSoon = true }"):]

# Replace Liderlik Tablosu onClick
leaderboard_start = code.find("// Liderlik Tablosu")
if leaderboard_start != -1:
    leaderboard_onclick_pos = code.find("onClick = { showComingSoon = true }", leaderboard_start)
    if leaderboard_onclick_pos != -1:
        code = code[:leaderboard_onclick_pos] + "onClick = { screenModel.loadLeaderboard() }" + code[leaderboard_onclick_pos + len("onClick = { showComingSoon = true }"):]

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
