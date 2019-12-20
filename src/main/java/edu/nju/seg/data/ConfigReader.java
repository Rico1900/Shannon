package edu.nju.seg.data;

import edu.nju.seg.model.ExperimentConfig;
import edu.nju.seg.util.$;
import edu.nju.seg.util.JsonUtil;

import java.io.File;
import java.util.Optional;

public class ConfigReader {

    private String filePath;

    public ConfigReader(String filePath) {
        this.filePath = filePath;
    }

    /**
     * parser experiment config from the file
     * @return maybe the config class
     */
    public Optional<ExperimentConfig> getConfig() {
        File f = new File(filePath);
        return $.readContent(f)
                .map(str -> JsonUtil.fromJson(str, ExperimentConfig.class));
    }

}
