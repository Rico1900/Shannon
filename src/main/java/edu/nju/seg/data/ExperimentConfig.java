package edu.nju.seg.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentConfig {

    private String inputFolder;

    private String resultFolder;

}
