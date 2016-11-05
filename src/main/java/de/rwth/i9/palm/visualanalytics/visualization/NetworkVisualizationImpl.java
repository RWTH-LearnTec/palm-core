package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.analytics.util.InterestParser;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class NetworkVisualizationImpl implements NetworkVisualization
{
	@Autowired
	private GraphFeature graphFeature;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	public Map<String, Object> visualizeNetwork( String type, List<Author> authorList, Set<Publication> publications, List<String> idsList, String startYear, String endYear, String authoridForCoAuthors )
	{
		System.out.println( "pubs: net: " + publications.size() );
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		List<Author> selectedAuthors = new ArrayList<Author>();

		Author authorForCoAuthors = new Author();
		if ( authoridForCoAuthors != null )
		{
			authorForCoAuthors = persistenceStrategy.getAuthorDAO().getById( authoridForCoAuthors );
		}

		// List of authors with the selected conferences
		if ( type.equals( "conference" ) )
		{
			List<Integer> count = new ArrayList<Integer>();
			for ( int i = 0; i < idsList.size(); i++ )
			{
				EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
				List<Author> eventAuthors = new ArrayList<Author>();

				List<Event> events = eg.getEvents();
				for ( Event e : events )
				{
					List<Publication> eventPublications = e.getPublications();
					for ( Publication p : eventPublications )
					{
						if ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) )
						{
							List<Author> authors = p.getAuthors();
							for ( Author a : authors )
							{
								if ( !eventAuthors.contains( a ) )
								{
									eventAuthors.add( a );
								}
							}
						}
					}
				}
				for ( Author a : eventAuthors )
				{
					if ( !selectedAuthors.contains( a ) )
					{
						selectedAuthors.add( a );
						count.add( 0 );
					}
					else
					{
						count.set( selectedAuthors.indexOf( a ), count.get( selectedAuthors.indexOf( a ) ) + 1 );
					}
				}
			}

			for ( int i = 0; i < selectedAuthors.size(); i++ )
			{
				if ( count.get( i ) != idsList.size() - 1 )
				{
					count.remove( i );
					selectedAuthors.remove( i );
					i--;
				}
			}
		}

		// List of authors with the selected interests
		if ( type.equals( "topic" ) )
		{
			List<String> interestList = new ArrayList<String>();
			for ( int i = 0; i < idsList.size(); i++ )
			{
				Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );
				interestList.add( interest.getTerm() );
			}
			System.out.println( interestList.toString() );
			List<DataMiningAuthor> DMAuthors = persistenceStrategy.getAuthorDAO().getDataMiningObjects();
			for ( DataMiningAuthor dma : DMAuthors )
			{
				Map<String, Double> interests = new HashMap<String, Double>();
				interests = InterestParser.parseInterestString( dma.getAuthor_interest_flat().getInterests() );
				if ( interests.keySet().containsAll( interestList ) )
				{
					selectedAuthors.add( persistenceStrategy.getAuthorDAO().getById( dma.getId() ) );
				}
			}

		}

		// List of authors with the selected circles
		if ( type.equals( "circle" ) )
		{
			List<Integer> count = new ArrayList<Integer>();
			for ( int i = 0; i < idsList.size(); i++ )
			{
				Circle c = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
				for ( Author a : c.getAuthors() )
				{
					if ( !selectedAuthors.contains( a ) )
					{
						selectedAuthors.add( a );
						count.add( 0 );
					}
					else
					{
						count.set( selectedAuthors.indexOf( a ), count.get( selectedAuthors.indexOf( a ) ) + 1 );
					}
				}
			}

			for ( int i = 0; i < selectedAuthors.size(); i++ )
			{
				if ( count.get( i ) != idsList.size() - 1 )
				{
					count.remove( i );
					selectedAuthors.remove( i );
					i--;
				}
			}
		}

		visMap.put( "graphFile", graphFeature.getGephiGraph( type, authorList, publications, idsList, authorForCoAuthors, selectedAuthors ).get( "graphFile" ) );
		return visMap;
	}
}