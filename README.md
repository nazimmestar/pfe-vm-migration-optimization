# pfe-vm-migration-optimization
Master's final project (PFE) written in French, focusing on optimizing virtual machine migration in cloud data centers using CloudSim Plus.  Includes threshold-based strategies for host overload detection, VM selection, and energy-aware placement policies.


# VM Migration Optimization in Cloud Computing using CloudSim Plus  
*(Projet de Fin d'Études – rédigé en français)*

## 📌 Overview
This project is my **Master's Final Year Project (PFE)** in *Systèmes d’Information et Aide à la Décision (SIAD)*, fully documented in **French**.  
It focuses on **optimizing Virtual Machine (VM) migration** in cloud data centers to reduce **energy consumption** while maintaining performance.

The simulation is implemented in **Java** using [CloudSim Plus 8.5.5](https://cloudsimplus.org/).

---

## 🎯 Objectives
- Implement **threshold-based migration strategies**.
- Compare **static**, **dynamic**, and **hybrid** migration approaches.
- Optimize **energy-aware VM placement**.
- Measure key metrics:  
  - Energy consumption (kWh)  
  - Number of migrations  
  - Migration time

---

## 🏗️ Architecture
- **Datacenter**: 8 or more hosts.
- **Hosts**: Multiple VMs per host with random workloads.
- **VMs**: Assigned **Cloudlets** simulating tasks.
- **Migration Algorithm**:  
  - Overload detection with dynamic upper and lower thresholds.  
  - VM selection (least utilized).  
  - Best Fit Decreasing (BFD) placement based on energy efficiency.

---

## 📊 Results
Experiments compare:
1. **Static Threshold Policy**
2. **Dynamic Threshold Policy**
3. **Hybrid Policy**

Metrics analyzed:
- Energy consumption
- CPU utilization
- Migration count

---

## 🛠️ Technologies Used
- **Java** (JDK 11+)
- **CloudSim Plus 8.5.5**
- **Maven**
- **Eclipse / IntelliJ IDEA**

---

## 🚀 How to Run
1. **Clone the repository**
```bash
git clone https://github.com/nazimmestar/vm-migration-optimization.git
