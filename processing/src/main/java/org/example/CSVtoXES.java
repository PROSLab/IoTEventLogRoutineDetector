package org.example;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class CSVtoXES {
    public static void main(String[] args) {
        String csvFilePath = "test.csv"; // Update this path
        try {
            // CSV reading setup
            BufferedReader csvReader = new BufferedReader(new FileReader(csvFilePath));
            String row;

            // XML document setup
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            Element log = doc.createElement("log");
            doc.appendChild(log);
            log.setAttribute("xes.version", "1.0");
            log.setAttribute("xmlns", "http://www.xes-standard.org/");

            Map<String, Element> caseMap = new HashMap<>();

            // Read CSV header
            String[] headers = csvReader.readLine().split(",");

            // Process each row in the CSV file
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(",", -1); // Split with -1 limit to include trailing empty strings

                String caseId = data[8].trim();
                String eventName = data[1].trim();
                String timestamp = data[3].trim();
                String activity = data[4].trim();
                String resource = data[5].trim();
                String lifecycle = data[6].trim();
                String eventId = data[7].trim();

                // Create or get the trace for the caseId
                Element trace = caseMap.get(caseId);
                if (trace == null) {
                    trace = doc.createElement("trace");
                    log.appendChild(trace);
                    caseMap.put(caseId, trace);

                    Element caseIdElement = doc.createElement("string");
                    caseIdElement.setAttribute("key", "concept:name");
                    caseIdElement.setAttribute("value", caseId);
                    trace.appendChild(caseIdElement);
                }

                // Create an event
                Element event = doc.createElement("event");
                trace.appendChild(event);

                // Add event details
                Element eventConceptName = doc.createElement("string");
                eventConceptName.setAttribute("key", "concept:name");
                eventConceptName.setAttribute("value", eventName);
                event.appendChild(eventConceptName);

                Element eventResource = doc.createElement("string");
                eventResource.setAttribute("key", "org:resource");
                eventResource.setAttribute("value", resource);
                event.appendChild(eventResource);

                Element eventTimestamp = doc.createElement("date");
                eventTimestamp.setAttribute("key", "time:timestamp");
                eventTimestamp.setAttribute("value", timestamp); // Ensure your timestamp format is XES compliant
                event.appendChild(eventTimestamp);

                Element eventActivity = doc.createElement("string");
                eventActivity.setAttribute("key", "Activity");
                eventActivity.setAttribute("value", activity);
                event.appendChild(eventActivity);

                Element eventLifecycle = doc.createElement("string");
                eventLifecycle.setAttribute("key", "lifecycle:transition");
                eventLifecycle.setAttribute("value", lifecycle);
                event.appendChild(eventLifecycle);
            }
            csvReader.close();

            // Setup to output XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("output.xes"));
            transformer.transform(source, result);

            System.out.println("XES file has been created.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
