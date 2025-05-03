package org.joget.plugin.custom;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.plugin.property.model.PropertyEditable;
import org.joget.workflow.util.WorkflowUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

public class ButtonApiTool extends Element implements FormBuilderPaletteElement, PluginWebSupport, PropertyEditable {

    @Override
    public String getFormBuilderTemplate() {
        return "<div class='form-element-label'>@@buttonLabel@@</div>";
    }

    @Override
    public String getName() {
        return "ButtonApiTool";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "A custom button that triggers Jenkins job using Remote API with CSRF crumb support.";
    }

    @Override
    public String getLabel() {
        return "Button API Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/ApiProgress.json", null, true, "messages/ApiProgress");
    }

    @Override
    public String getFormBuilderCategory() {
        return "Custom Elements";
    }

    @Override
    public int getFormBuilderPosition() {
        return 900;
    }

    @Override
    public String getFormBuilderIcon() {
        return "/plugin/org.joget.plugin.custom.ButtonApiTool/images/icon.png";
    }

    public boolean isInput() {
        return false;
    }

    public boolean isValidator() {
        return false;
    }

    public boolean isReadonly() {
        return false;
    }

    @Override
    public String getDefaultPropertyValues() {
        return "{}";
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        String contextPath = request != null ? request.getContextPath() : "";
        String pluginUrl = contextPath + "/web/json/plugin/" + getClassName() + "/service";
    
        dataModel.put("pluginUrl", pluginUrl);
        dataModel.put("buttonLabel", getPropertyString("buttonLabel"));
        dataModel.put("baseUrl", getPropertyString("baseUrl"));
        dataModel.put("authorizationHeader", getPropertyString("authorizationHeader"));
        dataModel.put("timeout", getPropertyString("timeout"));
        dataModel.put("resultField", getPropertyString("resultField"));
        dataModel.put("progressField", getPropertyString("progressField"));
    
        // 🔥 FIX: Resolve postUrl field value
        String postFieldId = getPropertyString("postUrl");
        String resolvedPostUrl = postFieldId;
        if (formData != null && formData.getRequestParameterValues(postFieldId) != null) {
            String[] values = formData.getRequestParameterValues(postFieldId);
            if (values.length > 0) {
                resolvedPostUrl = values[0];
            }
        }
        dataModel.put("postUrl", resolvedPostUrl);
    
        // 🔥 FIX: Resolve progressUrl field value
        String progressFieldId = getPropertyString("progressUrl");
        String resolvedProgressUrl = progressFieldId;
        if (formData != null && formData.getRequestParameterValues(progressFieldId) != null) {
            String[] values = formData.getRequestParameterValues(progressFieldId);
            if (values.length > 0) {
                resolvedProgressUrl = values[0];
            }
        }
        dataModel.put("progressUrl", resolvedProgressUrl);
    
        return FormUtil.generateElementHtml(this, formData, "apiProgress.ftl", dataModel);
    }


    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) {
        String action = request.getParameter("action");
        String baseUrl = request.getParameter("baseUrl");
        String postUrl = request.getParameter("postUrl");
        String progressUrl = request.getParameter("progressUrl");
        String crumbField = request.getParameter("crumbField");
        String crumbValue = request.getParameter("crumbValue");
        String auth = request.getHeader("Authorization");

        response.setContentType("application/json");

        try (PrintWriter out = response.getWriter()) {
            if ("crumb".equals(action)) {
                URL url = new URL(baseUrl + "/crumbIssuer/api/json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", auth);
                conn.setDoInput(true);

                int status = conn.getResponseCode();
                InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
                String json = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
                response.setStatus(status);
                out.print(json);
            } else if ("trigger".equals(action)) {
                URL url = new URL(postUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", auth);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                if (crumbField != null && crumbValue != null) {
                    conn.setRequestProperty(crumbField, crumbValue);
                }
                conn.getOutputStream().write(new byte[0]);

                int status = conn.getResponseCode();
                InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
                String responseText = new Scanner(is, "UTF-8").useDelimiter("\\A").hasNext() ? new Scanner(is, "UTF-8").next() : "";
                response.setStatus(status);
                out.print("{\"status\":\"" + status + "\", \"message\":\"" + responseText.replace("\"", "\\\"") + "\"}");
            } else if ("progress".equals(action)) {
                URL url = new URL(progressUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", auth);
                conn.setDoInput(true);

                int status = conn.getResponseCode();
                InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
                String json = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
                response.setStatus(status);
                out.print(json);
            } else {
                response.setStatus(400);
                out.print("{\"error\":\"Invalid action\"}");
            }
        } catch (Exception e) {
            try {
                response.setStatus(500);
                response.getWriter().print("{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
            } catch (Exception ignored) {}
        }
    }
} 
