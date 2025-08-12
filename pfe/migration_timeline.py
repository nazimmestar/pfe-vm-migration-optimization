import pandas as pd
import matplotlib.pyplot as plt

df = pd.read_csv("migration_timeline.csv")

plt.figure(figsize=(8, 5))
plt.step(df["Time(s)"], df["CumulativeMigrations"], where='post', color='purple')
plt.xlabel("Temps (s)")
plt.ylabel("Nombre cumulé de migrations")
plt.title("Évolution des migrations au cours du temps")
plt.grid(True)
plt.tight_layout()
plt.show()
