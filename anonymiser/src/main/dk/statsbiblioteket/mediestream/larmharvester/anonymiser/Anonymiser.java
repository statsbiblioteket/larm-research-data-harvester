package dk.statsbiblioteket.mediestream.larmharvester.anonymiser;

import net.sf.json.JSONObject;

/**
 * Innovation week Anonymiser Project.
 * Created by baj on 1/16/17.
 */
public class Anonymiser {
    public JSONObject removeDescription(JSONObject asset) {
        if (asset.has("Body") && asset.get("Body").getClass().equals(JSONObject.class)) {
            JSONObject body = asset.getJSONObject("Body");
        }

        return asset;
    }
}
