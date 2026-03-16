package org.example.gatewaysvc.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, List<String>> customHeaders = new LinkedHashMap<>();

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public void putHeader(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        customHeaders.put(name, new ArrayList<>(List.of(value)));
    }

    @Override
    public String getHeader(String name) {
        List<String> values = customHeaders.get(name);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = customHeaders.get(name);
        if (values != null) {
            return Collections.enumeration(values);
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = new ArrayList<>(customHeaders.keySet());
        Enumeration<String> base = super.getHeaderNames();
        while (base.hasMoreElements()) {
            String header = base.nextElement();
            if (!names.contains(header)) {
                names.add(header);
            }
        }
        return Collections.enumeration(names);
    }
}
