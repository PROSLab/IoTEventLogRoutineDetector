package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class CSVUtils {

    private static int cont = 0;

    public void createEvents(String path, List<Event> events) throws FileNotFoundException {
        File inFile = new File(path);
        Scanner inputFile = new Scanner(inFile);
        String str;
        String[] tokens;

        String firstLine = inputFile.nextLine();

        while (inputFile.hasNext()) {
            str = inputFile.nextLine();
            tokens = str.split(",");
            int row = Integer.parseInt(tokens[0]);
            String concept = tokens[1];
            int org = Integer.parseInt(tokens[2]);

            String splitted = tokens[3].split("\\+")[0];
            ZonedDateTime ldate = LocalDateTime.parse(splitted, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneId.of("UTC"));
            long time = ldate.toInstant().toEpochMilli();

            String activity = tokens[4];
            int resource = Integer.parseInt(tokens[5]);
            String lifecycle = tokens[6];
            int id =  Integer.parseInt(tokens[7]);
            String caseConcept = tokens[8];

            // add token[3] for not re-converting timestamp
            Event event = new Event(row, concept, org, time, activity, resource, lifecycle, id, caseConcept, tokens[3]);
            events.add(event);
        }

        inputFile.close();

        /*Collections.sort(events, new Comparator<Event>(){
        	   public int compare(Event one, Event two){
        	      return one.getRow() - two.getRow();
        	   }
        	});*/

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(DataStreamJob.abstractedFileName,true))) {

            firstLine = firstLine + "\n";
            writer.append(firstLine);

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

    }

    public void createLog(String c, int o, String t, String a, int re, String l, String ca, int i) {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(DataStreamJob.abstractedFileName,true))) {

            StringBuilder sb = new StringBuilder();
            sb.append(cont);
            sb.append(',');
            sb.append(c);
            sb.append(',');
            sb.append(o);
            sb.append(',');
            sb.append(t);
            sb.append(',');
            sb.append(a);
            sb.append(',');
            sb.append(re);
            sb.append(',');
            sb.append(l);
            sb.append(',');
            sb.append(i);
            sb.append(',');
            sb.append(ca);
            sb.append('\n');

            cont++;

            writer.append(sb.toString());

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
}
