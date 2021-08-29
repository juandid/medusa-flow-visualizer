package com.juandid.medusa.flow.visualizer.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JacksonXmlRootElement(localName = "medusaConfiguration")
public class MedusaConfiguration {

    @JacksonXmlProperty(localName = "medusa-version")
    private String medusaVersion;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "plugin")
    private List<Plugin> pluginList;

}
