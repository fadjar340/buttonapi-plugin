package org.joget.plugin;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class ApiProgressPlugin extends Element implements FormBuilderPaletteElement, PluginWebSupport {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public String getName() { 
        return "API Progress Plugin"; 
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
        return AppUtil.readPluginResource(getClassName(), "/properties/ApiProgress.json", null, true, "messages/ApiProgress");
    }

    @Override
    public void webService(HttpServletRequest req, HttpServletResponse res) throws IOException {
    try {
        String operation = req.getParameter("operation");
        String processId = req.getParameter("processId");
        JSONObject result = new JSONObject();

        if ("POST".equalsIgnoreCase(operation)) {
            // Get the URL either from property or form field
            String url;
            if ("true".equalsIgnoreCase(getPropertyString("dynamicPostUrl"))) {
                String fieldId = getPropertyString("postUrlFieldId");
                url = req.getParameter(fieldId); // Get directly from request
            } else {
                url = getPropertyString("postUrl");
            }
            
            String response = executePost(url); // Modified to accept URL directly
            result.put("processId", new JSONObject(response).optString(getPropertyString("processIdField")));
            result.put("success", true);
        } else if ("GET".equalsIgnoreCase(operation)) {
            Map<String, Object> progress = checkProgress(processId);
            result.put("progress", progress.get("progress"));
            result.put("status", progress.get("status"));
        }

        res.setContentType("application/json");
        res.getWriter().write(result.toString());
        } catch (Exception e) {
        LogUtil.error(getClassName(), e, "API Error");
        sendErrorResponse(res, e.getMessage());
        }
    }

    private String executePost(String url) throws Exception {
      if (url == null || url.isEmpty()) {
        throw new Exception("POST URL is not specified");
      }

    validateUrl(url);
    
    JSONArray urlHeaders = new JSONArray(getPropertyString("urlHeaders"));

    
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.noBody());
    
    for (int i = 0; i < urlHeaders.length(); i++) {
          JSONObject header = urlHeaders.getJSONObject(i);
          requestBuilder.header(header.getString("key"), header.getString("value"));
    }
    
    HttpRequest request = requestBuilder.build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    validateResponse(response);
    return response.body();
   }

    private Map<String, Object> checkProgress(String processId) throws Exception {
        String urlTemplate = getPropertyString("progressUrl");
        String url = urlTemplate.replace("{processId}", processId);
        validateUrl(url);
        
        JSONArray urlHeaders = new JSONArray(getPropertyString("urlHeaders"));
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();
        
        for (int i = 0; i < urlHeaders.length(); i++) {
            JSONObject urlHeader = urlHeaders.getJSONObject(i);
            requestBuilder.header(urlHeader.getString("key"), urlHeader.getString("value"));
        }
        
        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        validateResponse(response);
        
        JSONObject json = new JSONObject(response.body());
        Map<String, Object> result = new HashMap<>();
        result.put("progress", calculateProgress(json));
        result.put("status", extractJsonValue(json, getPropertyString("statusField")));
        return result;
    }

    private double calculateProgress(JSONObject json) {
        JSONArray progressFields = new JSONArray(getPropertyString("progressFields"));
        double progress = 0;
        for (int i = 0; i < progressFields.length(); i++) {
            String fieldPath = progressFields.getString(i);
            Object value = extractJsonValue(json, fieldPath);
            if (value instanceof Number) {
                progress += ((Number) value).doubleValue();
            }
        }
        return progress / progressFields.length();
    }

    private Object extractJsonValue(JSONObject json, String path) {
        try {
            String[] keys = path.split("/");
            Object value = json;
            for (String key : keys) {
                if (value instanceof JSONObject) {
                    value = ((JSONObject) value).get(key);
                } else if (value instanceof JSONArray) {
                    int index = Integer.parseInt(key);
                    value = ((JSONArray) value).get(index);
                }
            }
            return value;
        } catch (Exception e) {
            LogUtil.warn(getClassName(), "JSON extraction failed: " + path);
            return null;
        }
    }

    private void validateUrl(String url) throws Exception {
        if (url == null || url.trim().isEmpty()) {
            throw new Exception("URL cannot be empty");
        }
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw new Exception("Invalid URL protocol");
        }
    }

    private void validateResponse(HttpResponse<String> response) throws Exception {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception("HTTP Error: " + response.statusCode());
        }
    }

    private void sendErrorResponse(HttpServletResponse res, String message) throws IOException {
        JSONObject error = new JSONObject();
        error.put("error", true);
        error.put("message", message);
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        res.getWriter().write(error.toString());
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "apiProgress.ftl"; // Path to your FreeMarker template
    
        // Add any necessary data to the dataModel
        // For example, adding element properties
        dataModel.put("elementId", getPropertyString("id"));
        dataModel.put("buttonLabel", getPropertyString("buttonLabel"));
        dataModel.put("postUrl", getPropertyString("postUrl"));
        dataModel.put("progressUrl", getPropertyString("progressUrl"));
        dataModel.put("processIdField", getPropertyString("processIdField"));
        dataModel.put("progressFields", getPropertyString("progressFields"));
        dataModel.put("statusField", getPropertyString("statusField"));
        dataModel.put("urlHeaders", getPropertyString("urlHeaders"));
        dataModel.put("pollInterval", getPropertyString("pollInterval"));
    
        // Generate the HTML using the template and data model
        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }
}