package com.juandid.medusa.flow.visualizer;

import com.juandid.medusa.flow.visualizer.dto.MedusaConfiguration;
import com.juandid.medusa.flow.visualizer.dto.Plugin;
import com.juandid.medusa.flow.visualizer.dto.PluginList;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.plantuml.SourceStringReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MedusaFlowVisualizer {

    public static void main(String[] args) {

        final String defaultFilePath = "config" + File.separator + "medusa-configuration-sample.xml";
        File file = new File(defaultFilePath);
        if(file.exists()){
            visualizeFlow(file);
        }else{
            log.error("provided configuration file does not exist");
        }

    }

    public static void visualizeFlow(File file) {

        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            String xml = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8.name());
            MedusaConfiguration medusaConfiguration = xmlMapper.readValue(xml, MedusaConfiguration.class);
            List<Plugin> pluginList = medusaConfiguration.getPluginList();

            Map<String, Plugin> authenticationFlowMap = new HashMap<>();
            Map<String, Plugin> remainingPluginMap = new HashMap<>();

            for (Plugin plugin : pluginList) {
                if (plugin.getClazz() != null) {
                    if (plugin.getClazz().equals("com.airlock.iam.authentication.application.configuration.AuthenticationFlowConfig")) {
                        authenticationFlowMap.put(plugin.getUuid(), plugin);
                    } else {
                        remainingPluginMap.put(plugin.getUuid(), plugin);
                    }
                }
            }

            for (String authFlowUuid : authenticationFlowMap.keySet()) {
                drawTreeDiagram(authenticationFlowMap.get(authFlowUuid), remainingPluginMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    private static void drawTreeDiagram(Plugin authFlowPlugin, Map<String, Plugin> remainingPluginMap) {

        String name = authFlowPlugin.getId();
        String authFlowImageFilePath = "doc" + File.separator + "flows" + File.separator + "AuthFlow_" + name.replaceAll("\\s", "_") + ".png";
        String authFlowTextFilePath = "doc" + File.separator + "flows" + File.separator + "AuthFlow_" + name.replaceAll("\\s", "_") + ".txt";

        //Describe the auth flow using the plant uml syntax
        log.info("=== AuthenticationFlow \"{}\" ===", name);

        StringBuilder sb = new StringBuilder("@startmindmap");
        sb.append(System.getProperty("line.separator"));
        sb.append("title Authentication Flow \"" + authFlowPlugin.getId() + "\"");
        sb.append(System.getProperty("line.separator"));

        int level = 1;
        writeItem(sb, level, authFlowPlugin);

        crawlSubtree(level + 1, sb, authFlowPlugin, remainingPluginMap);

        sb.append(System.getProperty("line.separator"));
        sb.append(System.getProperty("line.separator"));
        sb.append("@endmindmap");

        //Use Plant UML (https://plantuml.com/) to render a visualization of the flow
        SourceStringReader reader = new SourceStringReader(sb.toString());
        try {
            OutputStream out = new FileOutputStream(authFlowImageFilePath);
            reader.outputImage(out);
        } catch (Exception e) {
            log.error("failed to render the visualization", e);
        }

        //Dump the plant uml source into a text file
        //this is optional and helps to find syntax error in case rendering fails
        FileUtils.writeStringToFile(new File(authFlowTextFilePath), sb.toString(), StandardCharsets.UTF_8.name());

    }

    public static void crawlSubtree(int level, StringBuilder sb, Plugin authFlowPlugin, Map<String, Plugin> remainingPluginMap) {

        if (authFlowPlugin.getPluginList() != null) {
            for (PluginList pluginList : authFlowPlugin.getPluginList()) {
                if (pluginList.getName() != null && (pluginList.getName().equals("steps") || pluginList.getName().equals("availableOptions"))) {
                    List<Plugin> plugins = pluginList.getPlugins();
                    for (int i = 0; i < plugins.size(); i++) {
                        Plugin plugin = plugins.get(i);
                        if (plugin.getUuidref() != null && !plugin.getUuidref().isEmpty()) {
                            Plugin stepPlugin = remainingPluginMap.get(plugin.getUuidref());
                            if (stepPlugin != null) {
                                String indent = getIndent(level);
                                log.trace("{} step Plugin id={}, class={}", indent, stepPlugin.getId(), stepPlugin.getClazz());
                                writeItem(sb, level, stepPlugin);
                                if (stepPlugin.getClazz().equals("com.airlock.iam.flow.application.configuration.selection.step.SelectionStepConfig")) {
                                    //crawl deeper
                                    crawlSubtree(level + 1, sb, stepPlugin, remainingPluginMap);
                                } else if (stepPlugin.getClazz().equals("com.airlock.iam.flow.application.configuration.selection.step.SelectionOptionConfig")) {
                                    //crawl deeper
                                    crawlSubtree(level + 1, sb, stepPlugin, remainingPluginMap);
                                }
                            } else {
                                log.error("failed to get plugin with uuid={}", plugin.getUuidref());
                            }
                        }
                    }

                }
            }
        }

    }

    private static void writeItem(StringBuilder sb, int level, Plugin plugin) {
        sb.append(System.getProperty("line.separator"));
        String boxTitle = getLabelForClazz(plugin.getClazz());
        boxTitle = wrapAndPad(boxTitle);
        sb.append(getIndent(level)).append(getColourForClazz(plugin.getClazz())).append(" ").append(boxTitle);
        String boxBody = plugin.getId();
        if (boxBody != null) {
            boxBody = wrapAndPad(boxBody);
            sb.append("\\n").append(boxBody);
        }
    }

    private static String wrapAndPad(String text) {

        final int contentLength = 30;
        //wrap
        String wrappedStr = WordUtils.wrap(text, contentLength, "|", false);
        if (wrappedStr != null) {
            String[] wrappedStrArr = wrappedStr.split("\\|");
            //pad
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < wrappedStrArr.length; i++) {
                if (i > 0) sb.append("\\n");
                sb.append(StringUtils.rightPad(wrappedStrArr[i], contentLength));
            }
            return sb.toString();
        } else {
            return text;
        }
    }

    private static String getIndent(int level) {
        String indent = "";
        for (int j = 0; j < level; j++) {
            indent = indent + "*";
        }
        return indent;
    }

    private static String getLabelForClazz(String clazz) {
        String label = extractClassName(clazz);
        label = label.replaceAll("StepConfig", "");
        if (clazz.equals("com.airlock.iam.flow.application.configuration.selection.step.SelectionStepConfig")) {
            label = "SelectionStep";
        } else if (clazz.equals("com.airlock.iam.flow.application.configuration.selection.step.SelectionOptionConfig")) {
            label = "SelectionOption";
        } else if (clazz.equals("com.airlock.iam.authentication.application.configuration.AuthenticationFlowConfig")) {
            label = "Authentication Flow";
        }
        return label;
    }

    private static String extractClassName(String clazz) {
        String[] nameArr = clazz.split("\\.");
        return nameArr[nameArr.length - 1];
    }

    private static String getColourForClazz(String clazz) {
        String label = "[#DeepSkyBlue]";
        if (clazz.equals("com.airlock.iam.flow.application.configuration.selection.step.SelectionStepConfig")) {
            label = "[#YellowGreen]";
        } else if (clazz.equals("com.airlock.iam.flow.application.configuration.selection.step.SelectionOptionConfig")) {
            label = "[#Orange]";
        } else if (clazz.equals("com.airlock.iam.authentication.application.configuration.AuthenticationFlowConfig")) {
            label = "[#Orange]";
        }
        return label;
    }

}
