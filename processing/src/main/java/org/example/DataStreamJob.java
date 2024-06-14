package org.example;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
//import org.apache.flink.streaming.api.windowing.time.Time;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DataStreamJob {
	static String abstractedFileName = "abstracted_activities.csv";

	static public void setAbstractFileName(String newName){
		DataStreamJob.abstractedFileName = "abstracted-" + newName;
	}


	public static void main(String[] args) throws Exception {
		File inputFile = new File("EventLogXESNoSegment.csv");
	
		DataStreamJob.setAbstractFileName("infomap-" + inputFile.getName());

		Files.deleteIfExists(new File(abstractedFileName).toPath());

		List<Event> events = new ArrayList<>();

		CSVUtils log = new CSVUtils();
		log.createEvents(inputFile.getPath(), events);

		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setParallelism(1);

		DataStream<Event> input = env.fromCollection(events);

		DataStream<Event> withTimestampsAndWatermarks = input.assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forMonotonousTimestamps()
				.withTimestampAssigner((event, timestamp) -> event.getTime()));

		Map<String, List<String>> communities = new HashMap<>();
		Set<String> knowEvents = new HashSet<>();

		// create a map that is used for dynamically defining patterns
        for (String line : Files.readAllLines(Paths.get("communities_coms_infomap.txt"))) {
        //for (String line : Files.readAllLines(Paths.get("communities_Louvain_23.txt"))) {
        //for (String line : Files.readAllLines(Paths.get("communities_com_agdl.txt"))) {
        //for (String line : Files.readAllLines(Paths.get("communities_dpclus_79.txt"))) {
        //for (String line : Files.readAllLines(Paths.get("communities_ground_truth.txt"))) {
			if(line.startsWith("#"))
				continue;
            List<String> llevents = new ArrayList<>(Arrays.asList(line.split(",")));
			String communityKey  = llevents.remove(0);
            communities.put(communityKey,llevents);
			knowEvents.addAll(llevents);
        }

		// skipPastLastEvent() does not restart the new sequence from the stop event
		AfterMatchSkipStrategy skipStrategy = AfterMatchSkipStrategy.skipToLast("stop");

		/*This  allows last events with only start or complete but the first event is always required, removing the first
		simple pattern allows sequences starting with every event types. Communities with only one event not working.*/
		for (String key : communities.keySet()) {
			Pattern<Event, ?> pattern = Pattern.<Event>begin("first", skipStrategy)
					.where(new SimpleCondition<Event>() {
						@Override
						public boolean filter(Event element) {
							boolean flag = (communities.get(key).contains(element.getConcept()));
							//return (element.getConcept().equals(communities.get(key).get(0)));
							if(flag)
								System.out.println(">>>>>>>  Start sequence in key " + key + " for value: " + element.getConcept() + " ID: " + element.getId());
							return flag;
						}
					})/*.notFollowedBy("TESTING")
					.where(new SimpleCondition<Event>() {
						@Override
						public boolean filter(Event element) {
							//This condition allows to detect events like "go_bathroom_sink", which is executed
							//during different activities, but it assigned to a single community by Louvain. However, this
							//condition causes the overlooking of the "start" event in community initial events with both
							//"start" and "complete" lifecycle, e.g., "have_bath".
							 //
							return (element.getConcept().equals(communities.get(key).get(0)));
						}
					})*/
					.followedBy("second")
					.where(new SimpleCondition<Event>() {
						@Override
						public boolean filter(Event element) {
							return (communities.get(key).contains(element.getConcept()));
						}
					}).oneOrMore()
					/*.followedBy("end")
					.where(new SimpleCondition<Event>() {
						@Override
						public boolean filter(Event element) {
							int lastIndex = communities.get(key).size()-1;
							return (element.getConcept().equals(communities.get(key).get(lastIndex)));
						}
					})//.times(2)
                    .oneOrMore()*/
                    .followedBy("stop")
					.where(new SimpleCondition<Event>() {
						@Override
						public boolean filter(Event element) {
							//TODO: detection ends when after the last community event appears an other event type
							boolean flag = !(communities.get(key).contains(element.getConcept()))
									&& knowEvents.contains(element.getConcept());
							if(flag){
								System.out.println("+++++++ END sequence for key " + key + ". Got value: " + element.getConcept() + " ID: " + element.getId());
							}else{
								//System.out.println("------- Skip for key " + key + " unknown value: " + element.getConcept() + " ID: " + element.getId());
							}
							return flag;
							//
							//int lastIndex = communities.get(key).size()-1;
							//return !(element.getConcept().equals(communities.get(key).get(lastIndex)));
						}
					});

			PatternStream<Event> patternStream = CEP.pattern(withTimestampsAndWatermarks, pattern).inEventTime();
			// inProcessingTime()
			patternStream.select(new SelectSegment(key)).print();

		}
		env.execute();

	}

	public static class SelectSegment implements PatternSelectFunction<Event, String> {

		String patternName;

		public SelectSegment(String patternName) {
			this.patternName = patternName;
		}

		public String select(Map<String, List<Event>> pattern) {
			// add in a list all the detected events
			pattern.get("first").addAll(pattern.get("second"));
			//pattern.get("first").addAll(pattern.get("end"));

			CSVUtils log = new CSVUtils();

			String activity = patternName;//pattern.get("first").get(pattern.get("first").size()-1).getConcept();
			String trace = "case_id";//pattern.get("first").get(0).getCaseConcept();
			// String trace = pattern.get("first").get(0).getTimestamp().substring(0,10);
			String start = pattern.get("first").get(0).getTimestamp();
            int start_id = pattern.get("first").get(0).getId();
			String end = pattern.get("first").get(pattern.get("first").size()-1).getTimestamp();
            int end_id = pattern.get("first").get(pattern.get("first").size()-1).getId();

			// create the abstracted event log with data of the first and last event
			log.createLog(activity, 0, start, activity, 0, "start", trace, start_id);
			log.createLog(activity, 0, end, activity, 0, "complete", trace, end_id);

            // add into detected events list also the first event of the next communities just for console printing
            pattern.get("first").addAll(pattern.get("stop"));

			return pattern.get("first").toString();
			//return pattern.get("first").toString();
		}
	}

}