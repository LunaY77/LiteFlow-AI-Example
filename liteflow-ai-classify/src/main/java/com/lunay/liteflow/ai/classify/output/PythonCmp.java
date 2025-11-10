package com.lunay.liteflow.ai.classify.output;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("python")
public class PythonCmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        System.out.println("Python component executed.");
    }
}
