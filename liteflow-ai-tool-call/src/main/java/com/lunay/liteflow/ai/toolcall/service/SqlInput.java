package com.lunay.liteflow.ai.toolcall.service;

import com.yomahub.liteflow.ai.engine.tool.annotation.ToolParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * sql工具输入
 *
 * @author 苍镜月
 * @since 2.16.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SqlInput {

    @ToolParam("目标ID")
    private String id;

    @ToolParam("目标表名")
    private String tableName;
}
