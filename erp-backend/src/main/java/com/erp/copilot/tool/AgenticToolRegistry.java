package com.erp.copilot.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry quản lý và điều phối toàn bộ kho vũ khí Agentic Tools của hệ thống.
 * Tự động phát hiện và đăng ký mọi Spring Bean implement AgentTool.
 */
@Slf4j
@Component
public class AgenticToolRegistry {

    private final Map<String, AgentTool> toolMap = new ConcurrentHashMap<>();

    public AgenticToolRegistry(List<AgentTool> tools) {
        if (tools != null) {
            for (AgentTool tool : tools) {
                toolMap.put(tool.getToolName().toLowerCase(), tool);
                log.info("Registered Agentic Copilot Tool: [{}] - {}", tool.getToolName(), tool.getDescription());
            }
        }
    }

    public Optional<AgentTool> getTool(String toolName) {
        if (toolName == null) return Optional.empty();
        return Optional.ofNullable(toolMap.get(toolName.trim().toLowerCase()));
    }

    public Collection<AgentTool> getAllTools() {
        return Collections.unmodifiableCollection(toolMap.values());
    }

    public List<Map<String, Object>> getToolsSchemaList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AgentTool tool : toolMap.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", tool.getToolName());
            item.put("description", tool.getDescription());
            item.put("parameters", tool.getParameterSchema());
            list.add(item);
        }
        return list;
    }
}
