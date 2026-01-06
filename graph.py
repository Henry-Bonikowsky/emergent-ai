import matplotlib.pyplot as plt
import csv
import sys

def graph_results(filename='src/survival_log.csv'):
    generations = []
    avg_survival = []

    try:
        with open(filename, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                generations.append(int(row['generation']))
                avg_survival.append(float(row['avg_survival']))
    except FileNotFoundError:
        print(f"File not found: {filename}")
        print("Run the simulation first to generate data.")
        return

    if not generations:
        print("No data in file yet. Let the simulation run longer.")
        return

    plt.figure(figsize=(12, 6))
    plt.plot(generations, avg_survival, 'b-', linewidth=1, alpha=0.7)

    # Add trend line if enough data
    if len(generations) > 10:
        import numpy as np
        z = np.polyfit(generations, avg_survival, 1)
        p = np.poly1d(z)
        plt.plot(generations, p(generations), 'r--', linewidth=2,
                 label=f'Trend: {z[0]*100:+.4f} per 100 generations')
        plt.legend()

    plt.xlabel('Generation')
    plt.ylabel('Average Survival (steps)')
    plt.title('Emergent AI: Evolutionary Selection')
    plt.grid(True, alpha=0.3)

    plt.tight_layout()
    plt.savefig('survival_graph.png', dpi=150)
    print(f"Graph saved to survival_graph.png")
    print(f"Data points: {len(generations)}")
    print(f"Generations: {generations[0]:,} to {generations[-1]:,}")
    print(f"Avg survival range: {min(avg_survival):.1f} to {max(avg_survival):.1f}")

    # Show trend
    if len(generations) > 1:
        start_avg = sum(avg_survival[:5]) / min(5, len(avg_survival))
        end_avg = sum(avg_survival[-5:]) / min(5, len(avg_survival))
        print(f"Start avg: {start_avg:.1f}, End avg: {end_avg:.1f}, Change: {end_avg - start_avg:+.1f}")

    plt.show()

if __name__ == '__main__':
    filename = sys.argv[1] if len(sys.argv) > 1 else 'src/survival_log.csv'
    graph_results(filename)
