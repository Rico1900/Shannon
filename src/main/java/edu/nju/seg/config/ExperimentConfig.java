package edu.nju.seg.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentConfig {

    private ExperimentalType type;

    private int bound;

    private List<String> targets;

    private String inputFolder;

    private String resultFolder;

}
