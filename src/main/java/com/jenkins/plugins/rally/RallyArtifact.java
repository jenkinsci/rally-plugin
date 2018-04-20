package com.jenkins.plugins.rally;

import com.jenkins.plugins.rally.utils.RallyGeneralUtils;

import java.io.Serializable;

public class RallyArtifact implements Serializable{
    private static final long serialVersionUID = 1L;

    private final String ref;
    private final String formattedID;
    private final String name;

    public RallyArtifact(String ref, String formattedID, String name){
        this.ref = ref;
        this.formattedID = formattedID;
        this.name = name;
    }

    public String getRef() {
        return ref;
    }

    @SuppressWarnings("unused")
    public String getFormattedID() {
        return formattedID;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public boolean isStory() {
        return RallyGeneralUtils.isStory(name);
    }

}
