package com.erp.copilot.tool;

import com.erp.copilot.dto.ToolExecutionResult;

import java.util.Map;

/**
 * Interface định nghĩa một Agent Tool chuẩn mực trong hệ thống ERP Agentic Copilot.
 * Mỗi Tool tương đương một kỹ năng thực thi nghiệp vụ tự động kết nối Backend.
 */
public interface AgentTool {

    /**
     * Tên định danh duy nhất của Tool (vd: checkProductStock, generateExportReport)
     */
    String getToolName();

    /**
     * Mô tả năng lực và trường hợp nên gọi Tool
     */
    String getDescription();

    /**
     * Schema định nghĩa tham số đầu vào (Arguments Schema)
     */
    Map<String, Object> getParameterSchema();

    /**
     * Thực thi nghiệp vụ thực tế với tham số và quyền hạn người dùng hiện tại
     */
    ToolExecutionResult execute(Map<String, Object> args, String currentUsername);
}
