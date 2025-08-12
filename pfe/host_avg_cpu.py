import pandas as pd
import matplotlib.pyplot as plt

# Charger le fichier CSV
df = pd.read_csv("host_avg_cpu.csv")

# Supprimer la colonne 'Time' pour ne garder que les colonnes des hôtes
df_hosts = df.drop(columns=['Time'])

# Calcul de la somme de l'utilisation CPU de tous les hôtes à chaque instant
df['Total_CPU_Usage'] = df_hosts.sum(axis=1)

# Calcul de la somme cumulée dans le temps
df['Cumulative_CPU_Usage'] = df['Total_CPU_Usage'].cumsum()

# Tracer la courbe cumulée
plt.figure(figsize=(10, 6))
plt.plot(df['Time'], df['Cumulative_CPU_Usage'], color='green', linewidth=2)

plt.title("Courbe Cumulée de la Consommation CPU Totale (Sans Migration)")
plt.xlabel("Temps (s)")
plt.ylabel("Consommation CPU Cumulée (%)")
plt.grid(True)
plt.tight_layout()

plt.show()
