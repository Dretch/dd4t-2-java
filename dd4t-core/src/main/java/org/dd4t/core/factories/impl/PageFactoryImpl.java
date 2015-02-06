package org.dd4t.core.factories.impl;

import org.apache.commons.lang3.StringUtils;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.impl.PageImpl;
import org.dd4t.core.caching.CacheElement;
import org.dd4t.core.exceptions.FactoryException;
import org.dd4t.core.exceptions.ItemNotFoundException;
import org.dd4t.core.exceptions.ProcessorException;
import org.dd4t.core.exceptions.SerializationException;
import org.dd4t.core.factories.PageFactory;
import org.dd4t.core.processors.RunPhase;
import org.dd4t.core.util.TCMURI;
import org.dd4t.databind.DataBindFactory;
import org.dd4t.providers.PageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.text.ParseException;

// TODO: refactor duplicate code
public class PageFactoryImpl extends BaseFactory implements PageFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PageFactoryImpl.class);
    private static final PageFactoryImpl INSTANCE = new PageFactoryImpl();
    @Autowired
    protected PageProvider pageProvider;

    protected PageFactoryImpl() {
        LOG.debug("Create new instance");
    }

    public static PageFactoryImpl getInstance() {
        return INSTANCE;
    }

    /**
     * @param uri of the page
     * @return
     * @throws ItemNotFoundException
     * @throws org.dd4t.core.exceptions.ProcessorException
     * @throws ParseException
     * @throws SerializationException
     */
    @Override
    public Page getPage(String uri) throws FactoryException{
        LOG.debug("Enter getPage with uri: {}", uri);

        CacheElement<Page> cacheElement = cacheProvider.loadFromLocalCache(uri);
        Page page;

        if (cacheElement.isExpired()) {
            synchronized (cacheElement) {
                if (cacheElement.isExpired()) {
                    cacheElement.setExpired(false);
	                String pageSource;
	                try {
		                pageSource = pageProvider.getPageContentById(uri);
	                } catch (ItemNotFoundException | ParseException | SerializationException | IOException e) {
		               throw new FactoryException(e);
	                }

	                if (StringUtils.isEmpty(pageSource)) {
                        cacheElement.setPayload(null);
                        cacheProvider.storeInItemCache(uri, cacheElement);
                        throw new FactoryException("Unable to find page by id " + uri);
                    }

	                try {
		                page = deserialize(pageSource, PageImpl.class);
                        LOG.debug("Running pre caching processors");
                        this.executeProcessors(page, RunPhase.BEFORE_CACHING);
		                cacheElement.setPayload(page);
		                final TCMURI tcmUri = new TCMURI(uri);
		                cacheProvider.storeInItemCache(uri, cacheElement, tcmUri.getPublicationId(), tcmUri.getItemId());
		                LOG.debug("Added page with uri: {} to cache", uri);
	                } catch (FactoryException | ProcessorException | ParseException e) {
		               throw new FactoryException(e);
	                }

                } else {
                    LOG.debug("Return a page with uri: {} from cache", uri);
                    page = cacheElement.getPayload();
                }
            }
        } else {
            LOG.debug("Return page with uri: {} from cache", uri);
            page = cacheElement.getPayload();
        }

        if (page != null) {
            LOG.debug("Running Post caching Processors");
            try {
                this.executeProcessors(page,RunPhase.AFTER_CACHING);
            } catch (ProcessorException e) {
               LOG.error(e.getLocalizedMessage(),e);
            }
        }

        return page;
    }

    /**
     * @param url the url of the page
     * @param publicationId the publication Id
     * @return a GenericPage object
     * @throws org.dd4t.core.exceptions.FactoryException
     */
    @Override
    public Page findPageByUrl(String url, int publicationId) throws FactoryException{
        LOG.debug("Enter findPageByUrl with url: {} and publicationId: {}", url, publicationId);

        String cacheKey = publicationId + "-" + url;
        CacheElement<Page> cacheElement = cacheProvider.loadFromLocalCache(cacheKey);
        Page page;

        if (cacheElement.isExpired()) {
            synchronized (cacheElement) {
                if (cacheElement.isExpired()) {
                    cacheElement.setExpired(false);
	                String pageSource;
	                try {
		                pageSource = pageProvider.getPageContentByURL(url, publicationId);
	                } catch (ItemNotFoundException| SerializationException | IOException e) {
		                throw new FactoryException(e);
	                }
	                if (pageSource == null || pageSource.length() == 0) {
                        cacheElement.setPayload(null);
                        cacheProvider.storeInItemCache(cacheKey, cacheElement);
                        throw new FactoryException("Page with url: " + url + " not found.");
                    }

	                try {
		                page = deserialize(pageSource, PageImpl.class);
<<<<<<< HEAD
=======
                        // TODO: put CPs on the request stack here
		                doFilters(page, RunPhase.BEFORE_CACHING);
>>>>>>> Commits to core necessary to get site running
		                cacheElement.setPayload(page);

		                final TCMURI tcmUri = new TCMURI(page.getId());
                        LOG.debug("Running pre caching processors");
                        this.executeProcessors(page, RunPhase.BEFORE_CACHING);
		                cacheProvider.storeInItemCache(cacheKey, cacheElement, publicationId, tcmUri.getItemId());
		                LOG.debug("Added page with uri: {} and publicationId: {} to cache", url, publicationId);
<<<<<<< HEAD
	                } catch (FactoryException | ProcessorException | ParseException e) {
=======
	                } catch (SerializationException | ParseException | FilterException e) {
>>>>>>> Commits to core necessary to get site running
		                throw new FactoryException(e);
	                }

                } else {
                    LOG.debug("Return a page with url: {} and publicationId: {} from cache", url, publicationId);
                    page = cacheElement.getPayload();
                }
            }
        } else {
            LOG.debug("Return page with url: {} and publicationId: {} from cache", url, publicationId);
            page = cacheElement.getPayload();
        }
<<<<<<< HEAD
        if (page != null) {
            LOG.debug("Running Post caching Processors");
            try {
                this.executeProcessors(page,RunPhase.AFTER_CACHING);
            } catch (ProcessorException e) {
                LOG.error(e.getLocalizedMessage(),e);
            }
        }
=======

        try {
			doFilters(page, RunPhase.AFTER_CACHING);
		} catch (FilterException | SerializationException e) {
            throw new FactoryException(e);
		}
        
>>>>>>> Commits to core necessary to get site running
        return page;
    }


    /**
     * This method explicitly used for querying the Broker Storage
     * for pages and returns raw content as string. Does NOT trigger
     * processors.
     *
     * To handle nulls, check for a returned null in the controller.
     *
     * @param url the url of the page
     * @param publicationId the publication Id
     * @return XML as string
     */
    @Override
    public String findSourcePageByUrl(String url, int publicationId) throws FactoryException {
        LOG.debug("Enter findXMLPageByUrl with url: {} and publicationId: {}", url, publicationId);

        String cacheKey = publicationId + "-" + url;
        CacheElement<String> cacheElement = cacheProvider.loadFromLocalCache(cacheKey);

        String page;

        if (cacheElement.isExpired()) {
            synchronized (cacheElement) {
                if (cacheElement.isExpired()) {
                    cacheElement.setExpired(false);
	                try {
		                page = pageProvider.getPageContentByURL(url, publicationId);
	                } catch (ItemNotFoundException | SerializationException | IOException e) {
		                throw new FactoryException(e);
	                }
	                if (page == null || page.length() == 0) {
                        cacheElement.setPayload(null);
                        cacheProvider.storeInItemCache(cacheKey, cacheElement);
                        throw new FactoryException("XML Page with url: " + url + " not found.");
                    }

                    cacheElement.setPayload(page);
                    cacheProvider.storeInItemCache(cacheKey, cacheElement);

                    LOG.debug("Added XML page with uri: {} and publicationId: {} to cache", url, publicationId);
                } else {
                    LOG.debug("Return a XML page with url: {} and publicationId: {} from cache", url, publicationId);
                    page = cacheElement.getPayload();
                }
            }
        } else {
            LOG.debug("Return XML page with url: {} and publicationId: {} from cache", url, publicationId);
            page = cacheElement.getPayload();
        }

        if (page == null) {
            throw new FactoryException("Page with url: " + url + " not found.");
        }
        return page;
    }

    /**
     * Deserializes a JSON encoded String into an object of the given type, which must
     * derive from the Page interface
     *
     * @param source String representing the JSON encoded object
     * @param clazz  Class representing the implementation type to deserialize into
     * @return the deserialized object
     */

    public <T extends Page> T deserialize (final String source, final Class<? extends T> clazz) throws FactoryException {
        try {
            return DataBindFactory.buildPage(source, clazz);
        } catch (SerializationException e) {
           throw new FactoryException(e);
        }
    }

    /**
     * Method to check whether a page exists in the Tridion Broker.
     *
     * @param url the Url to check
     * @param publicationId the publication Id
     * @return boolean indicating the page is present
     */
    public Boolean isPagePublished(String url, int publicationId) {
        LOG.debug("Enter isPagePublished with url: {} and publicationId: {}", url, publicationId);
        try {
            return pageProvider.checkPageExists(url, publicationId);
        } catch (ItemNotFoundException | SerializationException e) {
           LOG.error(e.getLocalizedMessage(),e);
        }
        return false;
    }

    public void setPageProvider(PageProvider provider) {
        this.pageProvider = provider;
    }
}