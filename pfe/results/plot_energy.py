import pandas as pd
import matplotlib.pyplot as plt
import os

# Liste des stratégies (les suffixes des fichiers CSV)
strategies = ['SSTP', 'SDTP', 'DSTP']

# Création du graphique
plt.figure(figsize=(12, 6))

for strategy in strategies:
    filename = f'host_avg_cpu_{strategy}.csv'

    if os.path.exists(filename):
        df = pd.read_csv(filename)

        # Somme de l'utilisation CPU totale à chaque instant (tous les hôtes)
        df['Total_CPU'] = df.iloc[:, 1:].sum(axis=1)

        # Courbe cumulée
        df['Cumulative_CPU'] = df['Total_CPU'].cumsum()

        plt.plot(df['Time'], df['Cumulative_CPU'], label=strategy)
    else:
        print(f"⚠️ Fichier introuvable : {filename}")

# Mise en forme
plt.title("Courbe Cumulée de la Consommation CPU par Stratégie")
plt.xlabel("Temps (s)")
plt.ylabel("Consommation CPU Cumulée (%)")
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.show()
