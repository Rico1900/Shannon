package edu.nju.seg.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentConfig {

    private int bound;

    private String inputFolder;

    private String resultFolder;

}