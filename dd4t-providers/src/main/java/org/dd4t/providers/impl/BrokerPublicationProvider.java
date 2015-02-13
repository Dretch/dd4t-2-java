package org.dd4t.providers.impl;

import com.tridion.broker.StorageException;
import com.tridion.dynamiccontent.DynamicMetaRetriever;
import com.tridion.meta.PublicationMeta;
import com.tridion.meta.PublicationMetaFactory;
import org.dd4t.core.caching.CacheElement;
import org.dd4t.core.caching.CacheType;
import org.dd4t.core.exceptions.SerializationException;
import org.dd4t.core.factories.impl.CacheProviderFactoryImpl;
import org.dd4t.core.providers.BaseBrokerProvider;
import org.dd4t.core.util.PublicationDescriptor;
import org.dd4t.providers.PayloadCacheProvider;
import org.dd4t.providers.PublicationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * dd4t-2
 *
 * @author R. Kempees
 */
public class BrokerPublicationProvider extends BaseBrokerProvider implements PublicationProvider {
	private static final DynamicMetaRetriever DYNAMIC_META_RETRIEVER = new DynamicMetaRetriever();
	private static final PublicationMetaFactory PUBLICATION_META_FACTORY = new PublicationMetaFactory();
	private static final Logger LOG = LoggerFactory.getLogger(BrokerPublicationProvider.class);
	private final PayloadCacheProvider cacheProvider = CacheProviderFactoryImpl.getInstance().getCacheProvider();
	private Class publicationDescriptor;


	public int discoverPublicationId (final String url) throws SerializationException {
		LOG.debug("Discovering Publication id for url: {}", url);
		final String key = getKey(CacheType.DISCOVER_PUBLICATION_URL, url);
		final CacheElement<Integer> cacheElement = cacheProvider.loadPayloadFromLocalCache(key);
		Integer result = -1;

		if (cacheElement.isExpired()) {
			synchronized (cacheElement) {
				if (cacheElement.isExpired()) {
					cacheElement.setExpired(false);

					final com.tridion.meta.PageMeta pageMeta = DYNAMIC_META_RETRIEVER.getPageMetaByURL(url);
					if (pageMeta != null) {
						result = pageMeta.getPublicationId();
						LOG.debug("Publication Id for URL: {}, is {}", url,result);
					} else {
						LOG.warn("Could not resolve publication Id for URL: {}",url);
					}

					cacheElement.setPayload(result);
					cacheProvider.storeInItemCache(key, cacheElement);
					LOG.debug("Stored Publication Id with key: {} in cache", key);
				} else {
					LOG.debug("Fetched a Publication Id with key: {} from cache", key);
					result = cacheElement.getPayload();
				}
			}
		} else {
			LOG.debug("Fetched Publication Id with key: {} from cache", key);
			result = cacheElement.getPayload();
		}

		return result == null ? -1 : result;
	}

	@Override public String discoverPublicationUrl (int publicationId) {
		final PublicationMeta publicationMeta = getPublicationMeta(publicationId);
		if (publicationMeta == null) {
			return null;
		}
		return publicationMeta.getPublicationUrl();
	}

	@Override public String discoverPublicationPath (int publicationId) {
		final PublicationMeta publicationMeta = getPublicationMeta(publicationId);
		if (publicationMeta == null) {
			return null;
		}
		return publicationMeta.getPublicationPath();
	}

	@Override public String discoverImagesUrl (int publicationId) {
		final PublicationMeta publicationMeta = getPublicationMeta(publicationId);
		if (publicationMeta == null) {
			return null;
		}
		return  publicationMeta.getMultimediaUrl();
	}

	@Override public String discoverImagesPath (int publicationId) {
		final PublicationMeta publicationMeta = getPublicationMeta(publicationId);
		if (publicationMeta == null) {
			return null;
		}
		return publicationMeta.getMultimediaPath();
	}

	@Override public String discoverPublicationTitle (int publicationId) {
		final PublicationMeta publicationMeta = getPublicationMeta(publicationId);
		if (publicationMeta == null) {
			return null;
		}
		return publicationMeta.getTitle();
	}

	@Override public String discoverPublicationKey (int publicationId) {
		final PublicationMeta publicationMeta = getPublicationMeta(publicationId);
		if (publicationMeta == null) {
			return null;
		}
		return publicationMeta.getKey();
	}

	/**
	 * For use in remote scenarios
	 * @param publicationId the publication Id
	 * @return a Publication descriptor
	 */
	@Override public PublicationDescriptor getPublicationDescriptor (final int publicationId) {
		final PublicationMeta publicationMeta = getPublicationMeta(publicationId);
		if (publicationMeta == null) {
			return null;
		}

		try {
			final PublicationDescriptor concretePublicationDescriptor = (PublicationDescriptor)publicationDescriptor.newInstance();
			concretePublicationDescriptor.setId(publicationMeta.getId());
			concretePublicationDescriptor.setKey(publicationMeta.getKey());
			concretePublicationDescriptor.setPublicationUrl(publicationMeta.getPublicationUrl());
			concretePublicationDescriptor.setPublicationPath(publicationMeta.getPublicationPath());
			concretePublicationDescriptor.setMultimediaUrl(publicationMeta.getMultimediaUrl());
			concretePublicationDescriptor.setMultimediaPath(publicationMeta.getMultimediaPath());
			concretePublicationDescriptor.setTitle(publicationMeta.getTitle());
			return concretePublicationDescriptor;
		} catch (InstantiationException | IllegalAccessException e) {
			LOG.error(e.getLocalizedMessage(),e);
		}

		return null;
	}


	private PublicationMeta getPublicationMeta (final int publicationId) {
		final String key = getKey(CacheType.PUBLICATION_META, Integer.toString(publicationId));
		final CacheElement<PublicationMeta> cacheElement = cacheProvider.loadPayloadFromLocalCache(key);

		PublicationMeta publicationMeta = null;

		if (cacheElement.isExpired()) {
			synchronized (cacheElement) {
				if (cacheElement.isExpired()) {
					cacheElement.setExpired(false);
					try {
						publicationMeta = PUBLICATION_META_FACTORY.getMeta(publicationId);
						cacheElement.setPayload(publicationMeta);
						cacheProvider.storeInItemCache(key, cacheElement);
						LOG.debug("Stored Publication Meta with key: {} in cache", key);
					} catch (StorageException e) {
						LOG.error(e.getLocalizedMessage(),e);
					}
				} else {
					LOG.debug("Fetched a Publication Meta with key: {} from cache", key);
					publicationMeta = cacheElement.getPayload();
				}
			}
		} else {
			LOG.debug("Fetched a Publication Meta with key: {} from cache", key);
			publicationMeta = cacheElement.getPayload();
		}

		if (publicationMeta == null) {
			LOG.error("Could not find Publication Meta for publication id: {}",publicationId);
			return null;
		}

		return publicationMeta;
	}

	public void setPublicationDescriptor (final Class publicationDescriptor) {
		this.publicationDescriptor = publicationDescriptor;
	}

	public Class getPublicationDescriptor () {
		return publicationDescriptor;
	}
}
