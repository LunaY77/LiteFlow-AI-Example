package com.lunay.liteflow.ai.structure.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MathReasoning {

    @JsonProperty("steps")
    private List<Step> steps;

    @JsonProperty("final_answer")
    private String finalAnswer;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MathReasoning {\n");
        sb.append("  Final Answer: \"").append(finalAnswer).append("\"\n");

        sb.append("  Steps: [\n");
        if (steps != null && !steps.isEmpty()) {
            // 迭代所有 step，并调用它们的 toString()
            for (int i = 0; i < steps.size(); i++) {
                sb.append(steps.get(i).toString());
                if (i < steps.size() - 1) {
                    sb.append(",\n"); // 在步骤之间添加逗号和换行
                } else {
                    sb.append("\n"); // 最后一个步骤后只有换行
                }
            }
        } else {
            sb.append("    (No steps)\n");
        }
        sb.append("  ]\n"); // Steps 列表的闭合方括号
        sb.append("}"); // MathReasoning 对象的闭合花括号

        return sb.toString();
    }
}
