package org.dd4t.core.processors.impl;

import org.dd4t.contentmodel.Component;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.GenericPage;
import org.dd4t.contentmodel.Item;
import org.dd4t.core.exceptions.FactoryException;
import org.dd4t.core.factories.impl.ComponentPresentationFactoryImpl;
import org.dd4t.core.processors.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pagefactory processor intended to resolve DCP's on pages at the factory level. It checks the page
 * being produced, finds the dynamic components (if any), and resolves these components through
 * the ComponentFactory.
 *
 * @author Rogier Oudshoorn, Raimond Kempees
 */
public class DcpResolver extends BaseProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(DcpResolver.class);

    @Override
    public void execute(Item item) {
        LOG.debug("[HybridPublishingFilter] acting upon item " + item);

        // filter only acts on pages
        if (item instanceof GenericPage) {
            GenericPage page = (GenericPage) item;

            LOG.debug("[HybridPublishingFilter] Detected " + page.getComponentPresentations().size() + " component presentations.");

            for (ComponentPresentation cp : page.getComponentPresentations()) {
                if (cp.isDynamic()) {
                    LOG.debug("[HybridPublishingFilter] Detected dynamic component presentation " + cp);

                    try {
                        // retrieve the dynamic component based on template
                        Component comp = ComponentPresentationFactoryImpl.getInstance().getComponentPresentation(cp.getComponent().getId(), cp.getComponentTemplate().getId());
                        // set the dynamic component
                        cp.setComponent(comp);
                    } catch (FactoryException e) {
                        // note: the other exceptions (authorization & authentication) are passed on
                        LOG.error("Unable to find component by id " + cp.getComponent().getId(), e);
                    }
                }
            }
        }
        LOG.debug("[HybridPublishingFilter] exits for item " + item);
    }
}