package main.dk.statsbiblioteket.mediestream.larmharvester;

import au.edu.apsr.mtk.base.*;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
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
            for (int i = 0; i < 5; i++)
                reader.readNext();
            while ((line = reader.readNext()) != null) {
                if (!line[0].isEmpty() )
                    writeMETS("./test/data/METSfile.xml", line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static METS mets = null;

    public static void writeMETS(String filenameMETS, String[] line) {
        for (int i = 0; i<21; i++) {
            System.out.print(line[i] + " ");
        }
        System.out.println();
        METSWrapper mw = null;
        try {
            mw = new METSWrapper();
            mets = mw.getMETSObject();

            mets.setObjID("hdl:1902/178");
            //mets.setProfile("http://localhost/profiles/scientific-datasets-profile");
            mets.setProfile("http://www.dspace.org/schema/aip/mets_aip_1_0.xsd");
            mets.setType("DSpace ITEM");

            MetsHdr mh = mets.newMetsHdr();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            String currentTime = df.format(cal.getTime());
            mh.setCreateDate(currentTime);
            mh.setLastModDate(currentTime);

            Agent agent = mh.newAgent();
            agent.setRole("CREATOR");
            agent.setType("OTHER");
            agent.setName("SampleMETSBuild");

            mh.addAgent(agent);

            mets.setMetsHdr(mh);

            DmdSec dmd = mets.newDmdSec();
            dmd.setID("J-1");
            MdWrap mdw = dmd.newMdWrap();
            mdw.setMDType("MODS");
            try {
                mdw.setXmlData(createTitleMODS(line[11], line[14]).getDocumentElement());
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            dmd.setMdWrap(mdw);

            mets.addDmdSec(dmd);

            DmdSec dmd2 = mets.newDmdSec();
            dmd2.setID("J-2");
            MdWrap mdw2 = dmd2.newMdWrap();
            mdw2.setMDType("MODS");
            try {
                mdw2.setXmlData(createRoleMODS(line[20]).getDocumentElement());
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            dmd2.setMdWrap(mdw2);

            mets.addDmdSec(dmd2);

            DmdSec dmd3 = mets.newDmdSec();
            dmd3.setID("J-3");
            MdWrap mdw3 = dmd3.newMdWrap();
            mdw3.setMDType("MODS");
            try {
                mdw3.setXmlData(createArtistMODS(line[18]).getDocumentElement());
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            dmd3.setMdWrap(mdw3);

            mets.addDmdSec(dmd3);

            DmdSec dmd4 = mets.newDmdSec();
            dmd4.setID("J-4");
            MdWrap mdw4 = dmd4.newMdWrap();
            mdw4.setMDType("MODS");
            try {
                mdw4.setXmlData(createAuthorMODS(line[16]).getDocumentElement());
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            dmd4.setMdWrap(mdw4);

            mets.addDmdSec(dmd4);

//            DmdSec dmd2 = mets.newDmdSec();
//            dmd2.setID("J-2");
//            MdWrap mdw2 = dmd2.newMdWrap();
//            mdw2.setMDType("MODS");
//            try {
//                mdw2.setXmlData(createTitleMODS("APS Source 125 Deg (includes xfiles, denzo)", "dataset", line[20], line[18], line[16], line[9]).getDocumentElement());
//            } catch (ParserConfigurationException e) {
//                e.printStackTrace();
//            }
//            dmd2.setMdWrap(mdw2);
//
//            mets.addDmdSec(dmd2);
//
//            FileSec fs = mets.newFileSec();
//            FileGrp fg = fs.newFileGrp();
//            fg.setUse("original");
//            au.edu.apsr.mtk.base.File f = fg.newFile();
//            f.setID("F-1");
//            f.setSize(2097152000);
//            f.setMIMEType("application/x-bzip");
//            f.setOwnerID("de.tar.bz.0");
//            f.setChecksum("498d584f08389d40cff70c4adf0659ff");
//            f.setChecksumType("MD5");
//
//            FLocat fl = f.newFLocat();
//            fl.setHref("http://localhost/myapp/get/XTAL_DATASET_de.tar.bz.0");
//            fl.setLocType("URL");
//
//            f.addFLocat(fl);
//            fg.addFile(f);
//
//            au.edu.apsr.mtk.base.File f2 = fg.newFile();
//            f2.setID("F-2");
//            f2.setSize(65193743);
//            f2.setMIMEType("application/x-bzip");
//            f2.setOwnerID("de.tar.bz.1");
//            f2.setChecksum("940faa9ab023cb0d42ef103a34b8c5bd");
//            f2.setChecksumType("MD5");
//
//            FLocat fl2 = f2.newFLocat();
//            fl2.setHref("http://localhost/myapp/get/XTAL_DATASET_de.tar.bz.1");
//            fl2.setLocType("URL");
//
//            f2.addFLocat(fl2);
//            fg.addFile(f2);
//
//            fs.addFileGrp(fg);
//            mets.setFileSec(fs);

            StructMap sm = mets.newStructMap();
            mets.addStructMap(sm);

            Div d = sm.newDiv();
            d.setType("investigation");
            d.setDmdID("J-1");
            sm.addDiv(d);

//            Div d2 = d.newDiv();
//            d2.setType("dataset");
//            d2.setDmdID("J-2");
//            d.addDiv(d2);

//            Fptr fp = d2.newFptr();
//            fp.setFileID("F-1");
//            d2.addFptr(fp);
//
//            Fptr fp2 = d2.newFptr();
//            fp2.setFileID("F-2");
//            d2.addFptr(fp2);
        } catch (METSException e) {
            e.printStackTrace();
        }

        try {
            mw.validate();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println();
        mw.write(System.out);
        System.out.println();

    }

    static private Document createTitleMODS(String title, String genre) throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElementNS("http://www.loc.gov/mods/v3", "mods");
        doc.appendChild(root);

        Element ti = doc.createElement("titleInfo");
        Element t = doc.createElement("title");
        t.setTextContent(title);
        ti.appendChild(t);
        root.appendChild(ti);

        Element g = doc.createElement("genre");
        g.setTextContent(genre);
        root.appendChild(g);

        return doc;
    }

    static private Document createRoleMODS(String role) throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElementNS("http://www.loc.gov/mods/v3", "mods");
        doc.appendChild(root);

        Element rol = doc.createElement("role");
        rol.setTextContent(role);
        root.appendChild(rol);

        return doc;
    }

    static private Document createArtistMODS(String artist) throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElementNS("http://www.loc.gov/mods/v3", "mods");
        doc.appendChild(root);

        Element art = doc.createElement("artist");
        art.setTextContent(artist);
        root.appendChild(art);

        return doc;
    }

    static private Document createAuthorMODS(String author) throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElementNS("http://www.loc.gov/mods/v3", "mods");
        doc.appendChild(root);

        Element aut = doc.createElement("author");
        aut.setTextContent(author);
        root.appendChild(aut);

        return doc;
    }

    static private Document createNoteMODS(String note) throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElementNS("http://www.loc.gov/mods/v3", "mods");
        doc.appendChild(root);

        Element not = doc.createElement("note");
        not.setTextContent(note);
        root.appendChild(not);

        return doc;
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
