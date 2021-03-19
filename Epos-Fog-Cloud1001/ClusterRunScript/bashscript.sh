  
#!/bin/bash -l
#SBATCH --nodes 1
#SBATCH --ntasks-per-node 1
#SBATCH --cpus-per-task 1
#SBATCH --mem 125GB
#SBATCH --time 06:00:00
#SBATCH --array=407-418
cd Epos-Fog-Cloud${SLURM_ARRAY_TASK_ID}
srun java -jar  EposFogCloud${SLURM_ARRAY_TASK_ID}.jar /home/nezami/Epos-Fog-Cloud${SLURM_ARRAY_TASK_ID}