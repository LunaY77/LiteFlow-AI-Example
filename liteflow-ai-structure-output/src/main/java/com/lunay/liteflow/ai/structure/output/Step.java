package com.lunay.liteflow.ai.structure.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Step {

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("output")
    private String output;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    Step {\n");
        sb.append("      Explanation: \"").append(explanation).append("\"\n");
        sb.append("      Output: \"").append(output).append("\"\n");
        sb.append("    }");
        return sb.toString();
    }
}
