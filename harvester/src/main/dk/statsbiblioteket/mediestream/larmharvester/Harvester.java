package main.dk.statsbiblioteket.mediestream.larmharvester;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by baj on 7/28/16.
 */
public class Harvester {
    private static Logger log = LoggerFactory.getLogger(Harvester.class);

    private String urlStrStart;
    private String searchStr = "Search/Get?q=*";
    private String sessionCreateStr = "Session/Create?";
    private String pageIndexStr = "&pageIndexStr=";
    private String formatStr = "formatStr=json2";
    private String sessionGUIDStr = "&sessionGUIDStr=";
    private String sessionGUID;

    private String userHTTPStatusCodes = "&userHTTPStatusCodes=False";
    private String pageSize = "&pageSize=100";

    public static final String ampersand = "&";
    private static final String urlStart = "http://api.prod.larm.fm/v6/EZAsset/Get?id=";
    private static final String urlEnd = "&format=json2&userHTTPStatusCodes=False";

    public Harvester() throws IOException {
        this("https://dev.api.dighumlab.org/v6/");
    }

    public Harvester(String urlStrStart) throws IOException {
        this.urlStrStart = urlStrStart;
        sessionCreate();
        log.debug("sessionGUID = " + sessionGUID);
        System.out.println(sessionGUID);
        sessionGUID = "7ad87c25\u00AD1174\u00AD4622\u00ADb1e4\u00ADf5020cbe10e1";
    }

    public String harvest() throws IOException {
        String jsonStr = "";
        for (int i=0; i<10; i++) {
            jsonStr = harvest(i);

        }
        return jsonStr;
    }

    public void sessionCreate() throws IOException {
        String jsonStr = httpGet(urlStrStart + sessionCreateStr + formatStr + userHTTPStatusCodes);
        log.debug(urlStrStart + sessionCreateStr + formatStr + userHTTPStatusCodes);
        log.debug("sessionCreate jsonStr = ", jsonStr);
        //sessionGUID = new dk.statsbiblioteket.mediestream.larmharvester.Parser().parseSessionCreateToSessionGuid(jsonStr);
    }

    public static void writeJSONFile(String inFile, String outFile) throws FileNotFoundException {
        List<String> lines = new ArrayList<String>();
        List<String> uniqueLines = new ArrayList<String>();

        try {
            Stream<String> idStream = getIDStream(inFile);
            //Find first column element
            splitIDStream(lines, idStream);
            //Insert in uniqueLines JSON for each unique ID
            lines.stream().distinct().forEach(id->{
                try {
                    uniqueLines.add(Harvester.httpGet(urlStart+id+urlEnd));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Files.write(Paths.get(outFile), uniqueLines);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * gets a stream of lines, where the first line is skipped
     *
     * @param inFile name of the file
     */
    public static Stream<String> getIDStream(String inFile) throws IOException {
        return Files.lines(Paths.get(inFile),
                Charset.forName("ISO-8859-2"))
                .skip(1);
    }

    /**
     * split the idStream up in columns.
     *
     * @param idStream stream of lines where the columns are separated by ;
     * @param lines list, which gets filled with the streams first column
     */
    public static void splitIDStream(List<String> lines, Stream<String> idStream) {
        for (String line : idStream.collect(Collectors.toList())) {
            lines.add(line.split(";")[0]);
        }
    }

    /**
     * harvest all assets page number i.
     *
     * https://dev.api.dighumlab.org/v6/Search/Get?q=*&pageIndexStr=0&pageSize=100&sessionGUIDStr=c19480c5-ed5c-4a99-b758-a3c05fdbb2c5&formatStr=json2&userHTTPStatusCodes=False
     *
     * @i page number
     */
    public String harvest(int i) throws IOException {
        String urlStr = urlStrStart + searchStr + pageIndexStr + i + pageSize + sessionGUIDStr + sessionGUID
                + ampersand + formatStr + userHTTPStatusCodes;
        log.debug("urlStr = " + urlStr);
        return httpGet(urlStr);
    }

    public static String httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();

        if (conn.getResponseCode() != 200) {
            throw new IOException(conn.getResponseMessage());
        }

        // Buffer the result into a string
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();

        conn.disconnect();
        return sb.toString();
    }

    public static String httpPost(String urlStr, String[] paramName,
                                  String[] paramVal) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setAllowUserInteraction(false);
        conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

        // Create the form content
        OutputStream out = conn.getOutputStream();
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        for (int i = 0; i < paramName.length; i++) {
            writer.write(paramName[i]);
            writer.write("=");
            writer.write(URLEncoder.encode(paramVal[i], "UTF-8"));
            writer.write("&");
        }
        writer.close();
        out.close();

        if (conn.getResponseCode() != 200) {
            throw new IOException(conn.getResponseMessage());
        }

        // Buffer the result into a string
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();

        conn.disconnect();
        return sb.toString();
    }

    /**
     * writes a file containing unique id, title and description
     *
     * @param inFile file that contains the unique ids
     * @param csvFile the file that shall be written
     */
    public static void writeCSVFile(String inFile, String csvFile) {
        List<String> lines = new ArrayList<String>();
        List<String> csvLines = new ArrayList<String>();

        try {
            Stream<String> idStream = getIDStream(inFile);
            //Find first column element
            splitIDStream(lines, idStream);
            //Find occurences of "Title" and "Description" in the csvLine
            lines.stream().distinct().forEach(id->{
                try {
                    csvLines.add(getTitleAndDescription(id, Harvester.httpGet(urlStart+id+urlEnd)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Files.write(Paths.get(csvFile), csvLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * read a csv-file
     *
     * @param csvFile the file that shall be written
     */
    public static void readCSVFile(String csvFile) {


        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(csvFile), ',');
            String[] line;
            while ((line = reader.readNext()) != null) {
                for (int i = 0; i<15; i++) {
                    System.out.print(line[i] + " ");
                }
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * gives a descriptionline which content is both the title and description in the json lines
     *
     * @param id identifies the current item
     * @param uniqueLine the json line, which contains title and description
     */
    public static String getTitleAndDescription(String id, String uniqueLine) {
        String descriptionLine = "";
        String tmpTitle = "";
        String[] title = uniqueLine.split("Title");
        String[] description = uniqueLine.split("\"Description");
        for (int i = 1; i < description.length - 1; i++) {
            try {
                if (i < title.length)
                    tmpTitle = title[i].substring(3, title[i].length() - 1);
                String tmpDescription = description[i].substring(3, description[i].length() - 1);;
                if (tmpDescription.contains("IsPositionAdjusted")) {
                    tmpDescription = description[i].substring(3, description[i].indexOf("IsPositionAdjusted"));
                }
                if (!(tmpDescription.startsWith("Source") || tmpDescription.startsWith("Project"))) {
                    descriptionLine += id + "; ";
                    if (i < title.length)
                        descriptionLine += "Title: " + tmpTitle.substring(0, tmpTitle.indexOf("\",\"")) + "; ";
                    descriptionLine +=
                            "Description: " + tmpDescription.substring(0, tmpDescription.indexOf("\",\"")) + "; \n";
                }
            }
            catch (Exception ex) {
                System.out.println(descriptionLine + "\n" + description[i]);
            }
        }
        return descriptionLine;
    }

}
