package com.juandid.medusa.flow.visualizer.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Plugin {

    @JacksonXmlProperty(localName = "id", isAttribute = true)
    private String id;

    @JacksonXmlProperty(localName = "class", isAttribute = true)
    private String clazz;

    @JacksonXmlProperty(localName = "uuid", isAttribute = true)
    private String uuid;

    @JacksonXmlProperty(localName = "uuidref", isAttribute = true)
    private String uuidref;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "pluginList")
    private List<PluginList> pluginList;

}
