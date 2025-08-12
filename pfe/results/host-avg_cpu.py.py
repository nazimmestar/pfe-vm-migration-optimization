import pandas as pd
import matplotlib.pyplot as plt
import os

# Liste des stratégies
strategies = ['SSTP', 'DSTP', 'WNMP']
avg_cpu_per_strategy = {}

colors = {'SSTP': '#1f77b4', 'DSTP': '#ff7f0e', 'WNMP': '#2ca02c'}

for strategy in strategies:
    filename = f'host_avg_cpu_{strategy}.csv'

    if os.path.exists(filename):
        df = pd.read_csv(filename)
        df_clean = df.drop(columns=['Time'], errors='ignore')
        df_numeric = df_clean.select_dtypes(include='number')
        avg_cpu = df_numeric.mean().mean()
        avg_cpu_per_strategy[strategy] = avg_cpu
    else:
        print(f"⚠️ Fichier introuvable : {filename}")

# Création du graphique en barres
plt.figure(figsize=(8, 5))
strategy_names = list(avg_cpu_per_strategy.keys())
strategy_values = list(avg_cpu_per_strategy.values())
strategy_colors = [colors[strat] for strat in strategy_names]

bars = plt.bar(strategy_names, strategy_values, color=strategy_colors, edgecolor='black')

# Ajouter les valeurs au-dessus des barres
for bar in bars:
    yval = bar.get_height()
    plt.text(bar.get_x() + bar.get_width()/2, yval + 0.5, f"{yval:.2f}", ha='center', va='bottom')

# Labels et titre avec espacements
plt.title("Average CPU Usage by Strategy", pad=20)
plt.xlabel("Migration Strategy", labelpad=15)
plt.ylabel("Average CPU Utilization (%)", labelpad=15)
plt.grid(axis='y')
plt.tight_layout()
plt.show()
