from pm4py.objects.log.exporter.xes import exporter as xes_exporter
from pm4py.convert import convert_to_dataframe, convert_to_event_log
from pm4py.filtering import filter_event_attribute_values
from pm4py.objects.log.importer.xes import importer as xes_importer
from pandas.io.parsers.readers import read_csv


# TODO: this code duplicate the logic. To be refactored!
def delete_noise(log, label_log):
    """Delete all the Noise events in both EventLogXES.xes and EventLogXESNoSegment.xes."""
    df = convert_to_dataframe(log)
    label_df = convert_to_dataframe(label_log)

    events_to_drop = []
    for i in df.index:
        if df.at[i, 'concept:name'].startswith("noise"):
            events_to_drop.append(i)

    label_events_to_drop = []
    for i in label_df.index:
        if label_df.at[i, 'concept:name'].startswith("noise"):
            label_events_to_drop.append(i)

    df = df.drop(df.index[events_to_drop])
    # add column 'case:concept:name' in log w\o segmentation
    df['case:concept:name'] = "filtered"
    label_df = label_df.drop(label_df.index[label_events_to_drop])

    return convert_to_event_log(df), convert_to_event_log(label_df)


def discard_events_in_multiple_activities(log, label_log):
    """Discard all the events that appear within multiple EventLogXES.xes traces (activity types) in both
    EventLogXES.xes and EventLogXESNoSegment.xes."""
    df = convert_to_dataframe(log)
    label_df = convert_to_dataframe(label_log)
    # create a set of all the event types
    event_types = set(label_df['concept:name'].tolist())

    events_in_multiple_activities = []
    for name in event_types:
        # create sub-dataframe with events of that type to examine
        events_to_check = label_df.loc[label_df['concept:name'] == name]
        # find if event type appears in multiple activities
        activities_with_checked_event = set(events_to_check['case:concept:name'].tolist())
        if len(activities_with_checked_event) > 1:
            # create sub-dataframe with events to discard
            events_to_discard = events_to_check.loc[events_to_check['case:concept:name'].isin(
                activities_with_checked_event)]
            # save in a list the event ids to discard from log w/o segmentation
            for event_id in events_to_discard['eventId']:
                events_in_multiple_activities.append(event_id)

    df = df.drop(df[df['eventId'].isin(events_in_multiple_activities)].index)
    label_df = label_df.drop(label_df[label_df['eventId'].isin(events_in_multiple_activities)].index)

    return convert_to_event_log(df), convert_to_event_log(label_df)


def create_subprocess_logs(communities, log, segmentation_hour):
    """Create a specific event log segmented on a daily basis at a specific hour for each community."""
    df = convert_to_dataframe(log)
    for i in df.index:
        df.at[i, 'case:concept:name'] = str(df.at[i, 'time:timestamp'])[0:10]
    days = df['case:concept:name'].tolist()
    days = list(dict.fromkeys(days))
    days.insert(0, "")

    file_counter = 10
    for community in communities:
        sub_log = filter_event_attribute_values(log, "concept:name", communities[community], level="event", retain=True)
        df = convert_to_dataframe(sub_log)

        day = ""
        case_id = 0
        for i in df.index:
            event_day = str(df.at[i, 'time:timestamp'])[0:10]
            event_hour = int(str(df.at[i, 'time:timestamp'])[11:13])
            if (day != event_day and event_hour >= segmentation_hour) or (days.index(event_day) - days.index(day) > 1):
                day = event_day
                case_id += 1
            df.at[i, 'case:concept:name'] = str(case_id)

        # for i in df.index:
        #     df.at[i, 'case:concept:name'] = str(df.at[i, 'time:timestamp'])[0:10]

        segmented_sub_log = convert_to_event_log(df)
        xes_exporter.apply(segmented_sub_log, "abstraction/sublog" + str(file_counter) + ".xes")
        file_counter += 1


def create_log_for_hierarchical_model(initial_xes_file, file_radix, file_counter_start, file_counter_end, hour):
    """Create a new event log aggregating all the community sublogs."""
    first_sub_log = xes_importer.apply(initial_xes_file)  # 'abstraction\sublog<number>.xes'
    first_df = convert_to_dataframe(first_sub_log)

    while file_counter_start < file_counter_end:
        next_sub_log = xes_importer.apply(file_radix + str(file_counter_start) + '.xes')  # 'abstraction\sublog'
        next_df = convert_to_dataframe(next_sub_log)
        first_df = first_df.append(next_df, ignore_index=True)
        file_counter_start += 1

    first_df = first_df.sort_values(by=["time:timestamp", "eventId"])
    day = ""
    case_id = 0

    for i in first_df.index:
        day_event = str(first_df.at[i, 'time:timestamp'])[0:10]
        hour_event = int(str(first_df.at[i, 'time:timestamp'])[11:13])

        if day != day_event and hour_event >= hour:
            day = day_event
            case_id += 1

        first_df.at[i, 'case:concept:name'] = str(case_id)
    hierarchical_log = convert_to_event_log(first_df)
    xes_exporter.apply(hierarchical_log, "abstraction/hierarchical.xes")


def discard_specific_events(log, events):
    df = convert_to_dataframe(log)

    # events = ['put_meal_to_fridge', 'go_oven', 'pack_food', 'noise_2', 'go_bed', 'sleep_in_bed', 'go_wardrobe',
    # 'change_clothes']
    for i in df.index:
        if df.at[i, 'concept:name'] in events:
            df = df.drop(i)

    return convert_to_event_log(df)


def find_infrequent_events(percentage, csv_path):
    activity_csv = read_csv(csv_path, sep=';')
    infrequent_activity = []
    for i in activity_csv.index:
        if activity_csv.at[i, 'Count'] < percentage:
            infrequent_activity.append(activity_csv.at[i, 'Activity'])

    return infrequent_activity


def pre_processing_xes(xes, xes_labelled):
    # Pre-processing: removing noises
    new_xes, new_xes_labelled = delete_noise(xes, xes_labelled)
    # Remove repeated activities
    # TODO: we skip this phase otherwise the assumption is trivial i.e.:
    #  if we know the repeated activities then we also know the activities itself (aka communities).
    # log, new_label_log = discard_events_in_multiple_activities(log, new_label_log)
    return new_xes, new_xes_labelled

