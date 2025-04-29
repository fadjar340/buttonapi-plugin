package org.joget.plugin.custom;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration<?>> registrationList;

    @Override
    public void start(BundleContext context) throws Exception {
        registrationList = new ArrayList<>();
        registrationList.add(context.registerService(ButtonApiTool.class.getName(), new ButtonApiTool(), null));
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (registrationList != null) {
            for (ServiceRegistration<?> registration : registrationList) {
                registration.unregister();
            }
        }
    }
}
