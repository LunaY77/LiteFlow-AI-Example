package com.lunay.liteflow.ai.classify.output;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("java")
public class JavaCmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        System.out.println("Java component executed.");
    }
}
