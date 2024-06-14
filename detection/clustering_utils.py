from cdlib import NodeClustering
import networkx as nx
import hashlib
from collections import Counter


def build_cdlib_graph(dfg):
    G = nx.Graph()
    nodes = []
    nameIndex = {}

    for e in dfg:
        # TODO: delete rare edges
        frequency = dfg[e]
        if frequency > 0:
            event_name_source = e[0]
            event_name_target = e[1]

            if event_name_source not in nodes:
                nodes.append(event_name_source)
            if event_name_source not in nameIndex:
                nameIndex[event_name_source] = nodes.index(event_name_source)

            if event_name_target not in nodes:
                nodes.append(event_name_target)
            if event_name_target not in nameIndex:
                nameIndex[event_name_target] = nodes.index(event_name_target)

            # TODO: improve results
            if event_name_source != event_name_target:
                G.add_edge(
                    nodes.index(event_name_source),
                    nodes.index(event_name_target),
                    weight=frequency)

    return G, nodes, nameIndex


def get_events_count(event_log):
    # Extract all activities from the log
    activities = [event['concept:name'] for trace in event_log for event in trace]
    # Count the occurrences of each activity (key_map -> count number)
    activity_count = Counter(activities)

    return activity_count


def build_ground_truth(event_log_labelled, nameIndex, G: nx.Graph):
    # Create structure for Segmented log comparison with resulting communities
    community_set = {}
    g_truth = nx.Graph()
    for t in event_log_labelled:
        variant = t.attributes.get("concept:name")
        if variant not in community_set:
            community_set[variant] = {}

        for element in t:
            # We track this node in the community corresponding to the variant activity
            event_name = element["concept:name"]
            event_index = nameIndex[event_name]
            if event_index not in community_set[variant]:
                community_set[variant][event_index] = 1
            else:
                hits = community_set[variant][event_index]
                hits = hits + 1
                community_set[variant][event_index] = hits
            g_truth.add_node(event_index, _community=community_set[variant])

        # Set communities as keys set to only list nodes' index
        for v in g_truth:
            node_community = set(g_truth.nodes[v]["_community"].keys())
            G.nodes[v]["community"] = node_community
            # .nodes[v]["community"] = node_community

        # Set edges retrieved from DFG graph
        # for v in G:
        #    node_community = set(g_truth.nodes[v]["_community"].keys())
        #    g_truth.nodes[v]["community"] = node_community

    ground_truth = NodeClustering(communities={frozenset(G.nodes[v]['community']) for v in G}, graph=G,
                                  method_name="reference")

    return ground_truth, community_set


def get_ordered_communities(communities, nodes, sort_events=False, community_labels = None):
    # Create structure for Segmented log comparison with resulting communities
    new_dict = []
    idx = 1
    for community in communities:
        print(community)
        vals = []
        for i in community:
            vals.append(nodes[i])
            #print(f"Community [{idx}]\t{nodes[i]}")
        if sort_events:
            vals.sort()
        len_events = len(vals)
        vals_str = "\t".join(vals)
        vals_hash = hashlib.md5(vals_str.encode()).hexdigest()
        #print(f"Community [{idx}]\t{len_events}\t{vals_hash}\t" + ",".join(vals))
        item = {}
        item["len"] = len(vals)
        item["vals"] = vals
        item["evt"] = vals_str
        item["id"] = idx
        item["vals_hash"] = vals_hash
        if community_labels is not None:
            item["community_label"] = community_labels[idx - 1]

        new_dict.append(item)
        idx = idx + 1
    new_dict = sorted(new_dict, key=lambda x: x['len'], reverse=True)
    return new_dict


