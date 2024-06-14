from pm4py.objects.conversion.log import converter as log_converter
from pandas.io.parsers.readers import read_csv
from pm4py.utils import format_dataframe
from pm4py.convert import convert_to_event_log
from pm4py.write import write_xes
from pm4py.convert import convert_to_dataframe
from pm4py.objects.log.importer.xes import importer as xes_importer
from pm4py.objects.log.exporter.xes import exporter as xes_exporter


def write_communities(lp_coms, nodes, filename="abstraction/communities.txt", events_count=None):
    """Return a dictionary with the communities and save them in a TXT file. In detail, nodes is a list with all the
    event types and lp_coms is a NodeClustering object with communities of event keys."""

    i = 0
    unlabelled_communities = {}
    f = open(filename, "w")

    for community in lp_coms.communities:
        print("Community n. ", i)

        # Derive community label based on the most frequent event name
        max_event_count = -1
        frequent_event = "community_" + str(i)
        unlabelled_communities[i] = []
        row = ""
        for unnamed_key in community:
            event_name = nodes[unnamed_key]
            unlabelled_communities[i].append(event_name)
            row = row + event_name + ","
            # update community label if any
            if events_count is not None \
                    and event_name in events_count \
                    and events_count[event_name] > max_event_count:
                frequent_event = event_name
                max_event_count = events_count[event_name]
                print("Set community label to " + frequent_event + " with higher counting of " + str(max_event_count))

        row = frequent_event + ":" + str(max_event_count) + "," + row
        i += 1
        f.write(row[:-1] + "\n")

    f.close()
    return unlabelled_communities

