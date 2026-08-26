import os

path = "C:/Users/Rahmi/.gemini/antigravity-ide/brain/cc854bc2-39d0-4f8b-9e27-42b654db14ad/task.md"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

code = code.replace("- [ ] Ensure `PodcastListScreen` properly displays the dummy podcasts", "- [x] Ensure `PodcastListScreen` properly displays the dummy podcasts")

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
