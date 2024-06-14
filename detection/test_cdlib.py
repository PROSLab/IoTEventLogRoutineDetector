import networkx as nx
from cdlib import evaluation, algorithms

G = nx.karate_club_graph()
louvain_communities = algorithms.louvain(G)
sz = evaluation.size(G,louvain_communities)
print(sz)

leiden_communities = algorithms.leiden(G)
f1 = evaluation.f1(louvain_communities,leiden_communities)
print(f1)

#myf1 = evaluation.f1(louvain_communities,leiden_communities)
