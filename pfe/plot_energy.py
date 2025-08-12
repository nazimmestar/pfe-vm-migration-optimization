import pandas as pd
import matplotlib.pyplot as plt

# Charger le fichier CSV
df = pd.read_csv("energy_base.csv")  # ou energy_SSTP.csv, etc.

# Tracer la courbe d'énergie cumulée
plt.figure(figsize=(10, 5))
plt.plot(df['Time(s)'], df['Energy_Wh'], color='green', linewidth=2)

plt.title("Énergie Cumulée (Wh) - Stratégie de base")
plt.xlabel("Temps (s)")
plt.ylabel("Énergie cumulée (Wh)")
plt.grid(True)
plt.tight_layout()
plt.show()
