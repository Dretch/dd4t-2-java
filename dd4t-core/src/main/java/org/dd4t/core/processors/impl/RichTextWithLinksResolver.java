package org.dd4t.core.processors.impl;

import org.dd4t.contentmodel.impl.XhtmlField;
import org.dd4t.core.factories.impl.LinkResolverFactory;
import org.dd4t.core.processors.Processor;
import org.dd4t.core.processors.RunPhase;
import org.dd4t.core.resolvers.LinkResolver;
import org.dd4t.core.util.XSLTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.TransformerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Filter to resolve component links.
 *
 * @author bjornl
 */
public class RichTextWithLinksResolver extends RichTextResolver implements Processor {

	private static final Logger LOG = LoggerFactory.getLogger(RichTextWithLinksResolver.class);
	private final XSLTransformer xslTransformer = XSLTransformer.getInstance();
	private LinkResolver linkResolver;

	public RichTextWithLinksResolver () {
		setRunPhase(RunPhase.AFTER_CACHING);
		LinkResolverFactory factory = LinkResolverFactory.getInstance();
		setLinkResolver(factory.getLinkResolver());
		LOG.debug("Init RichTextWithLinksResolverFilter");
	}

	@Override
	protected void resolveXhtmlField (XhtmlField xhtmlField) throws TransformerException {
		List<Object> xhtmlValues = xhtmlField.getValues();
		List<String> newValues = new ArrayList<String>();

		// get context path from linkResolver
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("contextpath", linkResolver.getContextPath());

		// find all component links and try to resolve them
		Pattern p = Pattern.compile("</?ddtmproot>");
		for (Object xhtmlValue : xhtmlValues) {
			String result = xslTransformer.transformSourceFromFilesource("<ddtmproot>" + xhtmlValue + "</ddtmproot>", "/resolveXhtmlWithLinks.xslt", params);
			newValues.add(p.matcher(result).replaceAll(""));
		}
		xhtmlField.setTextValues(newValues);
	}

	public LinkResolver getLinkResolver () {
		return linkResolver;
	}

	public void setLinkResolver (LinkResolver linkResolver) {
		this.linkResolver = linkResolver;
	}
}