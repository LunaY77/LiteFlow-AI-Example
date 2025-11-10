package com.lunay.liteflow.ai.toolcall.service;

import com.yomahub.liteflow.ai.engine.tool.annotation.Tool;
import com.yomahub.liteflow.ai.engine.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Mysql 服务示例
 *
 * @author 苍镜月
 * @since 2.16.0
 */

@Service
public class MysqlService {

    @Tool(name = "mysql_select_tool", value = "数据库选择工具")
    public String selectData(@ToolParam("数据库输入") SqlInput sqlInput) {
        if ("users".equalsIgnoreCase(sqlInput.getTableName()) && "123".equals(sqlInput.getId())) {
            return "User found: {id: 123, name: 'John Doe', email: '1234567@email.com'}";
        } else {
            return "No data found for id: " + sqlInput.getId() + " in table: " + sqlInput.getTableName();
        }
    }

    @Tool(name = "mysql_delete_tool", value = "数据库删除工具")
    public String deleteData(@ToolParam("数据库输入") SqlInput sqlInput) {
        if ("users".equalsIgnoreCase(sqlInput.getTableName()) && "123".equals(sqlInput.getId())) {
            return "User with id: 123 has been deleted.";
        } else {
            return "No data found for id: " + sqlInput.getId() + " in table: " + sqlInput.getTableName() + ". Deletion failed.";
        }
    }
}
