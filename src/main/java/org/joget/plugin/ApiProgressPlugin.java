package org.joget.plugin;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

public class ApiProgressPlugin extends Element implements FormBuilderPaletteElement, PluginWebSupport {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("apiProgress.title", getClassName(), "messages/ApiProgress");
    }

    @Override
    public String getVersion() {
        return "8.1.12";
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("apiProgress.label", getClassName(), "messages/ApiProgress");
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("apiProgress.description", getClassName(), "messages/ApiProgress");
    }

    @Override
    public String getFormBuilderCategory() {
        return FormBuilderPalette.CATEGORY_CUSTOM;
    }

    @Override
    public int getFormBuilderPosition() {
        return 1000;
    }

    @Override
    public String getFormBuilderIcon() {
        return "<i class='fas fa-sync-alt'></i>";
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<div class='form-cell'><span class='api-progress-button'>" + getLabel() + "</span></div>";
    }

    @Override
    public String getPropertyOptions() {
        String options = AppUtil.readPluginResource(getClassName(), "/properties/ApiProgress.json", null, true, "messages/ApiProgress");
        LogUtil.debug(getClassName(), "Loading property options: " + options);
        return options;
    }
    private String getPropertyString(String property, String defaultValue) {
        String value = getPropertyString(property);
        return value != null && !value.trim().isEmpty() ? value : defaultValue;
    }

    private String getFieldValue(String fieldId, HttpServletRequest request) {
        try {
            // Simply get the parameter value directly from request since we have the field ID
            return request.getParameter(fieldId);
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error getting field value");
            return null;
        }
    }

    @Override
    public void webService(HttpServletRequest req, HttpServletResponse res) throws IOException {
            // Add CORS headers
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        res.setHeader("Access-Control-Max-Age", "3600");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        JSONObject result = new JSONObject();
        try {
            String operation = req.getParameter("operation");
            if (operation == null) {
                throw new Exception("Operation parameter is required");
            }
    
            if ("POST".equalsIgnoreCase(operation)) {
                handlePostOperation(req, result);
            } 
            else if ("GET".equalsIgnoreCase(operation)) {
                handleGetOperation(req, result);
            }
            else {
                throw new Exception("Unsupported operation: " + operation);
            }
    
            res.setContentType("application/json");
            res.getWriter().write(result.toString());
        } catch (Exception e) {
            handleErrorResponse(res, e);
        }
    }
    
    private void handlePostOperation(HttpServletRequest req, JSONObject result) throws Exception {
        String postUrlFieldId = getPropertyString("postUrlFieldId");
        // Get the actual value from the form field instead of using it as a parameter name
        String postUrl = getFieldValue(postUrlFieldId, req);
                
        if (postUrl == null || postUrl.trim().isEmpty()) {
            throw new Exception("POST URL field value is empty");
        }
    
        executePost(postUrl);
        result.put("status", "STARTED");
    }
    
    private void handleGetOperation(HttpServletRequest req, JSONObject result) throws Exception {
        String progressUrlFieldId = getPropertyString("progressUrlFieldId");
        // Get the actual value from the form field instead of using it as a parameter name
        String progressUrl = getFieldValue(progressUrlFieldId, req);
        
        if (progressUrl == null || progressUrl.trim().isEmpty()) {
            throw new Exception("Progress URL field value is empty");
        }
    
        String response = executeGet(progressUrl);
        JSONObject jsonResponse = new JSONObject(response);
        
        result.put("inProgress", getBooleanValue(jsonResponse, getPropertyString("inProgressField")));
        result.put("result", getStringValue(jsonResponse, getPropertyString("resultField")));
    }

    private String getCrumb() throws Exception {
        String jenkinsBaseUrl = getPropertyString("jenkinsBaseUrl");
        if (jenkinsBaseUrl == null || jenkinsBaseUrl.trim().isEmpty()) {
            throw new Exception("Jenkins base URL for crumb is not configured");
        }
    
        // Ensure base URL ends with /
        if (!jenkinsBaseUrl.endsWith("/")) {
            jenkinsBaseUrl += "/";
        }
    
        String crumbUrl = jenkinsBaseUrl + "crumbIssuer/api/json";
        
        HttpRequest crumbRequest = HttpRequest.newBuilder()
                .uri(URI.create(crumbUrl))
                .GET()
                .headers(prepareHeaders())
                .build();
        
        HttpResponse<String> crumbResponse = httpClient.send(crumbRequest, HttpResponse.BodyHandlers.ofString());
        
        if (crumbResponse.statusCode() == 200) {
            JSONObject crumbJson = new JSONObject(crumbResponse.body());
            return crumbJson.getString("crumbRequestField") + ":" + crumbJson.getString("crumb");
        }
        throw new Exception("Failed to get Jenkins crumb. Status: " + crumbResponse.statusCode());
    }
    
    private String executePost(String fullUrl) throws Exception {
        validateUrl(fullUrl);
        
        // Get crumb (format: "Jenkins-Crumb:abc123")
        String crumb = getCrumb();
        String[] crumbParts = crumb.split(":");
        
        // Prepare headers including crumb
        String[] headers = prepareHeaders();
        headers = Arrays.copyOf(headers, headers.length + 2);
        headers[headers.length - 2] = crumbParts[0]; // Header name
        headers[headers.length - 1] = crumbParts[1]; // Crumb value
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .POST(HttpRequest.BodyPublishers.noBody())
                .headers(headers)
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        validateResponse(response);
        return response.body();
    }

    private String executeGet(String fullUrl) throws Exception {
        validateUrl(fullUrl);
        
        // Get crumb (same as for POST)
        String crumb = getCrumb();
        String[] crumbParts = crumb.split(":");
        
        // Prepare headers including crumb
        String[] headers = prepareHeaders();
        headers = Arrays.copyOf(headers, headers.length + 2);
        headers[headers.length - 2] = crumbParts[0]; // "Jenkins-Crumb"
        headers[headers.length - 1] = crumbParts[1]; // actual crumb value
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .GET()
                .headers(headers)
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        validateResponse(response);
        return response.body();
    }

    private String[] prepareHeaders() throws Exception {
        JSONArray headersArray = new JSONArray(getPropertyString("urlHeaders"));
        String[] headers = new String[headersArray.length() * 2];
        for (int i = 0; i < headersArray.length(); i++) {
            JSONObject header = headersArray.getJSONObject(i);
            headers[i * 2] = header.getString("key");
            headers[i * 2 + 1] = header.getString("value");
        }
        return headers;
    }

    private boolean getBooleanValue(JSONObject json, String path) {
        try {
            Object value = extractJsonValue(json, path);
            return value instanceof Boolean ? (Boolean)value : false;
        } catch (Exception e) {
            return false;
        }
    }

    private String getStringValue(JSONObject json, String path) {
        try {
            Object value = extractJsonValue(json, path);
            return value != null ? value.toString() : "UNKNOWN";
        } catch (Exception e) {
            return "ERROR";
        }
    }

    private Object extractJsonValue(JSONObject json, String path) {
        String[] keys = path.split("/");
        Object value = json;
        for (String key : keys) {
            if (value instanceof JSONObject) {
                value = ((JSONObject) value).opt(key);
            } else if (value instanceof JSONArray) {
                value = ((JSONArray) value).opt(Integer.parseInt(key));
            }
            if (value == null) break;
        }
        return value;
    }

    private void validateUrl(String url) throws Exception {
        if (url == null || url.trim().isEmpty()) {
            throw new Exception("URL cannot be empty");
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("https") && !scheme.equals("http"))) {
                throw new Exception("Invalid URL protocol - must be http or https");
            }
            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                throw new Exception("Invalid URL - missing host");
            }
        } catch (URISyntaxException e) {
            throw new Exception("Invalid URL format: " + e.getMessage());
        }
    }

    private void validatePollInterval(String interval) throws Exception {
        try {
            int value = Integer.parseInt(interval);
            if (value < 1000 || value > 60000) {
                throw new Exception("Poll interval must be between 1000 and 60000 ms");
            }
        } catch (NumberFormatException e) {
            throw new Exception("Invalid poll interval value - must be a number");
        }
    }

    private void validateResponse(HttpResponse<String> response) throws Exception {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception("HTTP Error: " + response.statusCode());
        }
    }

    private void handleErrorResponse(HttpServletResponse res, Exception e) throws IOException {
        LogUtil.error(getClassName(), e, "API Error");
        JSONObject error = new JSONObject();
        error.put("inProgress", false);
        error.put("result", "ERROR: " + e.getMessage());
        res.setContentType("application/json");
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        res.getWriter().write(error.toString());
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        try {
            String uniqueId = getPropertyString("id") + "_" + System.currentTimeMillis();
            
            // Validate poll interval before rendering
            validatePollInterval(getPropertyString("pollInterval", "5000"));
            
            // Validate headers JSON format
            String headersJson = getPropertyString("urlHeaders", "[]");
            try {
                new JSONArray(headersJson);
            } catch (JSONException e) {
                throw new Exception("Invalid headers JSON format: " + e.getMessage());
            }

            // Ensure all required variables are passed with defaults
            dataModel.put("elementId", uniqueId);
            //dataModel.put("elementId", getPropertyString("id"));
            dataModel.put("postUrlFieldId", getPropertyString("postUrlFieldId", ""));
            dataModel.put("progressUrlFieldId", getPropertyString("progressUrlFieldId", ""));
            dataModel.put("progressMethod", getPropertyString("progressMethod", "GET"));
            dataModel.put("inProgressField", getPropertyString("inProgressField", "inProgress"));
            dataModel.put("resultField", getPropertyString("resultField", "result"));
            dataModel.put("urlHeaders", headersJson);
            dataModel.put("pollInterval", getPropertyString("pollInterval", "5000"));

            LogUtil.debug(getClassName(), "Rendering template for ID: " + uniqueId);
            
            return FormUtil.generateElementHtml(this, formData, "apiProgress.ftl", dataModel);
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error rendering template");
            return "Error: " + e.getMessage();
        }
    }
    

}