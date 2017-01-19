package dk.statsbiblioteket.mediestream.larmharvester.anonymiser;

import net.sf.json.JSONObject;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.Span;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * Innovation week Anonymiser Project.
 * Created by baj on 1/16/17.
 */
public class Anonymiser {
        private static String fileName = "/home/baj/Projects/larm-research-data-harvester/anonymiser/resources/entity_data_/fornavne.txt";
        private Stream<String> entities;
        private String[] entitiesArray;
        private HashSet<String> fornavneSet;
        private Dictionary fornavne;

    public Anonymiser() throws IOException {
        //Read entity data
        FileReader fileReader = new FileReader(fileName);
        BufferedReader buffReader = new BufferedReader(fileReader);
        fornavneSet = new HashSet<String>();
        while (buffReader.ready()) {
            fornavneSet.add(buffReader.readLine());
        }
        //System.out.println(Arrays.toString(entitiesArray));


        //FileInputStream fileInputStream = new FileInputStream(fileName);
        //fornavne = Dictionary.parseOneEntryPerLine(fileReader);
                //new Dictionary(new FileInputStream(fileName));
    }

    public JSONObject removeDescription(JSONObject asset) {
        if (asset.has("Body") && asset.get("Body").getClass().equals(JSONObject.class)) {
            JSONObject body = asset.getJSONObject("Body");
        }

        return asset;
    }

    public String anonymise(String text) {
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(text);
        Span[] spans = tokenizer.tokenizePos(text);
        /*
        DictionaryNameFinder namefinder = new DictionaryNameFinder(fornavne);
        Span[] spanArray = namefinder.find(tokens);
        for (Span span: spanArray) {
            System.out.println(span);
        }
        */

        String[] newTextArray = new String[tokens.length];
        int newTextIndex = 0;
        int substringIndexStart = 0;
        int indexOfLastReplacement = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (fornavneSet.contains(tokens[i])) {
                if (newTextIndex>0) {substringIndexStart = spans[indexOfLastReplacement].getEnd();}
                System.out.println(newTextIndex + "    " + substringIndexStart + "    " + i + "      " + spans[i].getStart() + "     " + spans[i].getEnd());
                newTextArray[newTextIndex] = text.substring(substringIndexStart, spans[i].getStart()) + "[FORNAVN]";
                newTextArray[++newTextIndex] = text.substring(spans[i].getEnd(), text.length());
                indexOfLastReplacement = i;
            }
        }
        String newText = "";
        for (String newTextPart:
             newTextArray) {
            if (newTextPart!=null) {newText = newText + newTextPart;}
        }

        return newText;
    }
}
