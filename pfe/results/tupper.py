import pandas as pd
import matplotlib.pyplot as plt
import os

# Nom de la stratégie utilisée
strategy = "SDTP"  # Remplace par SSTP, DSTP, base... si besoin
filename = f"tupper_tlower_{strategy}.csv"  # Plus de "results/"

if os.path.exists(filename):
    df = pd.read_csv(filename)

    # Colonnes Tupper uniquement
    tupper_cols = [col for col in df.columns if col.startswith("Tupper")]

    # Tracé des courbes Tupper
    plt.figure(figsize=(12, 6))
    for col in tupper_cols:
        plt.plot(df['Time'], df[col], label=col)

    plt.title(f"Tupper par Hôte - Stratégie {strategy}")
    plt.xlabel("Temps")
    plt.ylabel("Tupper (%)")
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    plt.show()
else:
    print(f"⚠️ Fichier introuvable : {filename}")
