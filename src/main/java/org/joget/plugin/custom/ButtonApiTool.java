package org.joget.plugin.custom;

import java.util.Map;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.plugin.property.model.PropertyEditable;

public class ButtonApiTool extends Element implements FormBuilderPaletteElement, PluginWebSupport, PropertyEditable {

    @Override
    public int getFormBuilderPosition() {
        return 100; // Adjust the position value as needed
    }

    @Override
    public String getFormBuilderCategory() {
        return "Custom"; // Plugin appears in the Custom section
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<div class='form-cell'>Button API Tool</div>";
    }

    @Override
    public String getFormBuilderIcon() {
        return "/plugin/org.joget.apps.form.lib.TextField/images/TextField.png";
    }

    @Override
    public String getName() {
        return "Button API Tool";
    }

    @Override
    public String getLabel() {
        return "Button API Tool (Jenkins)";
    }

    @Override
    public String getDescription() {
        return "Trigger a Jenkins job and monitor its progress via Crumb & JSON polling.";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/ApiProgress.json", null, true, "messages/ApiProgress");
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "apiProgress.ftl"; // Template path inside resources

        // Pass all required plugin properties to FTL
        dataModel.put("buttonLabel", getPropertyString("buttonLabel"));
        dataModel.put("baseUrl", getPropertyString("baseUrl"));
        dataModel.put("postUrl", getPropertyString("postUrl"));
        dataModel.put("progressUrl", getPropertyString("progressUrl"));
        dataModel.put("authorizationHeader", getPropertyString("authorizationHeader"));
        dataModel.put("timeout", getPropertyString("timeout"));
        dataModel.put("resultField", getPropertyString("resultField"));
        dataModel.put("progressField", getPropertyString("progressField"));

        return FormUtil.generateElementHtml(this, formData, template, dataModel);
    }

    @Override
    public void webService(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse res) {
        // Optional: Leave empty or log access if not used.
        try {
            res.getWriter().write("This plugin does not expose webService directly. Use Jenkins API from the browser.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
