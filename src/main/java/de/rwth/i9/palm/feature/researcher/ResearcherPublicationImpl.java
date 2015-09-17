package de.rwth.i9.palm.feature.researcher;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.helper.comparator.PublicationByDateComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Institution;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class ResearcherPublicationImpl implements ResearcherPublication
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getPublicationListByAuthorId( String authorId )
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// get author
		Author targetAuthor = persistenceStrategy.getAuthorDAO().getById( authorId );

		if ( targetAuthor == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "message", "Error - author not found" );
			return responseMap;
		}

		if ( targetAuthor.getPublications() == null || targetAuthor.getPublications().isEmpty() )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "message", "Error - author not found" );
			return responseMap;
		}

		responseMap.put( "status", "ok" );

		List<Map<String, Object>> publicationList = new ArrayList<Map<String, Object>>();

		// get publication list
		List<Publication> publications = new ArrayList<Publication>( targetAuthor.getPublications() );

		// sort based on period
		Collections.sort( publications, new PublicationByDateComparator() );

		for ( Publication publication : publications )
		{

			// put publication detail
			Map<String, Object> publicationMap = new LinkedHashMap<String, Object>();
			publicationMap.put( "id", publication.getId() );
			publicationMap.put( "title", publication.getTitle() );
			if ( publication.getAbstractText() != null )
				publicationMap.put( "abstract", publication.getAbstractText() );
			// coauthor
			List<Map<String, Object>> coathorList = new ArrayList<Map<String, Object>>();
			for ( Author author : publication.getCoAuthors() )
			{
				Map<String, Object> authorMap = new LinkedHashMap<String, Object>();
				authorMap.put( "id", author.getId() );
				authorMap.put( "name", author.getName() );
				if ( author.getInstitutions() != null )
					for ( Institution institution : author.getInstitutions() )
					{
						if ( authorMap.get( "aff" ) != null )
							authorMap.put( "aff", authorMap.get( "aff" ) + ", " + institution.getName() );
						else
							authorMap.put( "aff", institution.getName() );
					}
				if ( author.getPhotoUrl() != null )
					authorMap.put( "photo", author.getPhotoUrl() );

				coathorList.add( authorMap );
			}
			publicationMap.put( "coauthor", coathorList );

			if ( publication.getKeywordText() != null )
				publicationMap.put( "keyword", publication.getKeywordText() );

			if ( publication.getPublicationDate() != null )
			{
				SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd" );
				publicationMap.put( "date", sdf.format( publication.getPublicationDate() ) );
			}

			if ( publication.getLanguage() != null )
				publicationMap.put( "language", publication.getLanguage() );

			if ( publication.getCitedBy() != 0 )
				publicationMap.put( "cited", publication.getCitedBy() );

			if ( publication.getPublicationType() != null )
			{
				String publicationType = publication.getPublicationType().toString();
				publicationType = publicationType.substring( 0, 1 ).toUpperCase() + publicationType.substring( 1 );
				publicationMap.put( "type", publicationType );
			}

			if ( publication.getEvent() != null )
			{
				Map<String, Object> eventMap = new LinkedHashMap<String, Object>();
				eventMap.put( "id", publication.getEvent().getId() );
				eventMap.put( "name", publication.getEvent().getEventGroup().getName() );
				publicationMap.put( "event", eventMap );
			}

			if ( publication.getVolume() != null )
				publicationMap.put( "volume", publication.getVolume() );

			if ( publication.getIssue() != null )
				publicationMap.put( "issue", publication.getIssue() );

			if ( publication.getPages() != null )
				publicationMap.put( "pages", publication.getPages() );

			if ( publication.getPublisher() != null )
				publicationMap.put( "publisher", publication.getPublisher() );

			publicationList.add( publicationMap );
		}
		responseMap.put( "publications", publicationList );

		return responseMap;
	}

}
