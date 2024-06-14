# from pm4py.vis import view_petri_net
from pm4py.write import write_pnml
from pm4py.discovery import discover_petri_net_inductive
from pm4py.objects.log.importer.xes import importer as xes_importer


def discover_petri_net():
    log = xes_importer.apply('abstraction/abstractedDay.xes')  #
    net, im, fm = discover_petri_net_inductive(log, noise_threshold=0.2)
    write_pnml(net, im, fm, "petri.pnml")
    # view_petri_net(net, im, fm)
