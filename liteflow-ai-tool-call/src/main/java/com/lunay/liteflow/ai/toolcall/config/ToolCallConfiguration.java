package com.lunay.liteflow.ai.toolcall.config;

import com.lunay.liteflow.ai.toolcall.service.MysqlService;
import com.lunay.liteflow.ai.toolcall.service.SqlInput;
import com.yomahub.liteflow.ai.engine.tool.function.FunctionToolCallback;
import com.yomahub.liteflow.ai.engine.tool.registry.ScanningToolRegistry;
import com.yomahub.liteflow.ai.engine.tool.registry.StaticToolRegistry;
import com.yomahub.liteflow.ai.engine.tool.registry.ToolRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Tool Call 配置类
 *
 * @author 苍镜月
 * @since 2.16.0
 */

@Configuration
public class ToolCallConfiguration {

    /**
     * 方式 1：通过包扫描初始化工具注册中心
     */
    @Bean("scanningToolRegistry")
    public ToolRegistry scanningToolRegistry() {
        ScanningToolRegistry toolRegistry = new ScanningToolRegistry("com.lunay.liteflow.ai.toolcall.service");
        assert toolRegistry.getAllTools().size() == 2;
        return toolRegistry;
    }

    /**
     * 方式 2：通过手动注册初始化工具注册中心
     */
    @Bean("functionToolRegistry")
    @Primary
    public ToolRegistry functionToolRegistry(MysqlService mysqlService) {
        StaticToolRegistry toolRegistry = new StaticToolRegistry();
        toolRegistry.register(
                FunctionToolCallback
                        .builder(mysqlService::selectData)
                        .name("mysql_select_tool")
                        .inputType(SqlInput.class)
                        .description("数据库选择工具")
                        .build()
        );
        toolRegistry.register(
                FunctionToolCallback
                        .builder(mysqlService::deleteData)
                        .name("mysql_delete_tool")
                        .inputType(SqlInput.class)
                        .description("数据库删除工具")
                        .build()
        );
        assert toolRegistry.getAllTools().size() == 2;
        return toolRegistry;
    }
}
