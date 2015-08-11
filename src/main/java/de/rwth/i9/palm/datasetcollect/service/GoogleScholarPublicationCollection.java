package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleScholarPublicationCollection extends PublicationCollection
{
	private final static Logger log = LoggerFactory.getLogger( GoogleScholarPublicationCollection.class );

	public GoogleScholarPublicationCollection()
	{
		super();
	}

	public static List<Map<String, String>> getListOfAuthors( String authorName ) throws IOException
	{
		List<Map<String, String>> authorList = new ArrayList<Map<String, String>>();

		String url = "https://scholar.google.com/citations?view_op=search_authors&mauthors=" + authorName.replace( " ", "-" );

		// Using jsoup java html parser library
		Document document = PublicationCollectionHelper.getDocumentWithJsoup( url, 5000, getGoogleScholarCookie() );

		if ( document == null )
			return Collections.emptyList();

		Elements authorListNodes = document.select( HtmlSelectorConstant.GS_AUTHOR_LIST_CONTAINER );

		if ( authorListNodes.size() == 0 )
		{
			log.info( "No author with name '{}' with selector '{}' on google scholar '{}'", authorName, HtmlSelectorConstant.GS_AUTHOR_LIST_CONTAINER, url );
			return Collections.emptyList();
		}

		// if the authors is present
		for ( Element authorListNode : authorListNodes )
		{
			Map<String, String> eachAuthorMap = new LinkedHashMap<String, String>();
			String name = authorListNode.select( HtmlSelectorConstant.GS_AUTHOR_LIST_NAME ).text();
			// get author name
			eachAuthorMap.put( "name", name );
			// set source
			eachAuthorMap.put( "source", "googlescholar" );
			// get author url
			eachAuthorMap.put( "url", authorListNode.select( "a" ).first().absUrl( "href" ) );
			// get author photo
			String photoUrl = authorListNode.select( "img" ).first().absUrl( "src" );
			if ( !photoUrl.contains( "avatar_scholar" ) )
				eachAuthorMap.put( "photo", photoUrl );
			// get author affiliation
			eachAuthorMap.put( "affiliation", authorListNode.select( HtmlSelectorConstant.GS_AUTHOR_LIST_AFFILIATION ).html().replace( "&#x2026;", "" ).trim() );
			
			String citedBy = authorListNode.select( HtmlSelectorConstant.GS_AUTHOR_LIST_NOCITATION ).html();
			if( citedBy != null && citedBy.length() > 10)
				eachAuthorMap.put( "citedby", citedBy.substring( "Cited by".length() ).trim() );

			authorList.add( eachAuthorMap );
		}

		return authorList;
	}

	public static List<Map<String, String>> getPublicationListByAuthorUrl( String url ) throws IOException
	{
		List<Map<String, String>> publicationMapLists = new ArrayList<Map<String, String>>();

		// Using jsoup java html parser library
		Document document = PublicationCollectionHelper.getDocumentWithJsoup( url + "&cstart=0&pagesize=1000", 5000, getGoogleScholarCookie() );

		if ( document == null )
			return Collections.emptyList();

		Elements publicationRowList = document.select( HtmlSelectorConstant.GS_PUBLICATION_ROW_LIST );

		if ( publicationRowList.size() == 0 )
		{
			log.info( "Np publication found " );
			return Collections.emptyList();
		}

		for ( Element eachPublicationRow : publicationRowList )
		{
			Map<String, String> publicationDetails = new LinkedHashMap<String, String>();
			// set source
			publicationDetails.put( "source", "googlescholar" );
			publicationDetails.put( "url", eachPublicationRow.select( "a" ).first().absUrl( "href" ) );
			publicationDetails.put( "title", eachPublicationRow.select( "a" ).first().text() );
			publicationDetails.put( "coauthor", eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_COAUTHOR_AND_VENUE ).first().text() );
			String venue = eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_COAUTHOR_AND_VENUE ).get( 1 ).text().trim();
			if( !venue.equals( "" ))
				publicationDetails.put( "venue", venue );
			String noCitation = eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_NOCITATION ).text().replaceAll( "[^\\d]", "" );
			if( !noCitation.equals( "" ))
				publicationDetails.put( "nocitation", noCitation );
			String year = eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_YEAR ).text().trim();
			if( !year.equals( "" ))
				publicationDetails.put( "year", year );

			publicationMapLists.add( publicationDetails );
		}

		return publicationMapLists;
	}

	public static Map<String, String> getPublicationDetailByPublicationUrl( String url ) throws IOException
	{
		Map<String, String> publicationDetailMaps = new LinkedHashMap<String, String>();
		// Using jsoup java html parser library
		Document document = PublicationCollectionHelper.getDocumentWithJsoup( url, 5000, getGoogleScholarCookie() );

		if ( document == null )
			return Collections.emptyMap();

		Elements publicationDetailContainer = document.select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_CONTAINER );

		if ( publicationDetailContainer.size() == 0 )
		{
			log.info( "Np publication detail found " );
			return Collections.emptyMap();
		}

		publicationDetailMaps.put( "title", publicationDetailContainer.get( 0 ).select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_TITLE ).text() );

		String docName = publicationDetailContainer.get( 0 ).select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PDF ).text();
		if ( docName != null )
			publicationDetailMaps.put( "doc", publicationDetailContainer.get( 0 ).select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PDF ).text() );

		try
		{
			Elements publicationPdfUrl = publicationDetailContainer.get( 0 ).select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PDF );
			if ( publicationPdfUrl != null )
				publicationDetailMaps.put( "doc_url", publicationPdfUrl.select( "a" ).first().absUrl( "href" ) );
		}
		catch ( Exception e )
		{
			// TODO: handle exception
		}

		Elements publicationDetailsRows = publicationDetailContainer.get( 0 ).select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PROP );

		for ( Element publicationDetail : publicationDetailsRows )
			publicationDetailMaps.put( publicationDetail.select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PROP_LABEL ).text(), publicationDetail.select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PROP_VALUE ).text() );

		return publicationDetailMaps;
	}
	
	private static Map<String, String> getGoogleScholarCookie()
	{
		Map<String, String> cookies = new HashMap<String, String>();
		cookies.put( "GOOGLE_ABUSE_EXEMPTION", "ID=df93c59979ded4c7:TM=1438589257:C=c:IP=95.223.161.25-:S=APGng0uX_nGeZZbVdG0c4vxsxbRwXg08fA" );
		cookies.put( "GSP", "LM=1438509418:S=OsqfoXZicqz09iBT" );
		cookies.put( "NID", "70=hcw9rL3hPrp4dWMkd4C4DeF_Q8BO7BpB-bo9z0Ix_WPeM7IwAmbYCR2jolHcJQW_Oy7cJQuEWuRg_CKaPku4MPHyu2ReS86KcCExepqy3GRJJPhVuYg42Z1ZrCbE26AC" );
		cookies.put( "PREF", "ID=1111111111111111:FF=0:TM=1438520627:LM=1438520627:V=1:S=8w-e8EQt08Or09Lx" );
		return cookies;
	}
}
