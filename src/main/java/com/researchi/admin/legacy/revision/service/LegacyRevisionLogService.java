package com.researchi.admin.legacy.revision.service;

import com.researchi.admin.legacy.revision.domain.LegacyRevisionLog;
import com.researchi.admin.legacy.revision.mapper.LegacyRevisionLogMapper;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;

@Service
public class LegacyRevisionLogService {

    private final LegacyRevisionLogMapper legacyRevisionLogMapper;

    public LegacyRevisionLogService(LegacyRevisionLogMapper legacyRevisionLogMapper) {
        this.legacyRevisionLogMapper = legacyRevisionLogMapper;
    }

    public void backupBeforeUpdate(String tableName, String legacyKey, Object beforeValue, Long changedBy) {
        LegacyRevisionLog revisionLog = new LegacyRevisionLog();
        revisionLog.setLegacyTableName(tableName);
        revisionLog.setLegacyKey(legacyKey);
        revisionLog.setBeforeJson(toJson(beforeValue));
        revisionLog.setActionType("UPDATE");
        revisionLog.setChangedBy(changedBy);
        legacyRevisionLogMapper.insert(revisionLog);
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        List<Method> getters = List.of(value.getClass().getMethods()).stream()
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> method.getName().startsWith("get"))
                .filter(method -> !"getClass".equals(method.getName()))
                .sorted(Comparator.comparing(Method::getName))
                .toList();

        StringBuilder builder = new StringBuilder("{");
        for (int i = 0; i < getters.size(); i++) {
            Method getter = getters.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(escapeJson(propertyName(getter.getName()))).append('"').append(':');
            try {
                appendJsonValue(builder, getter.invoke(value));
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to serialize legacy revision backup.", ex);
            }
        }
        builder.append('}');
        return builder.toString();
    }

    private String propertyName(String getterName) {
        String name = getterName.substring(3);
        return name.isEmpty() ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private void appendJsonValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }
        builder.append('"').append(escapeJson(String.valueOf(value))).append('"');
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(ch);
            }
        }
        return builder.toString();
    }
}
