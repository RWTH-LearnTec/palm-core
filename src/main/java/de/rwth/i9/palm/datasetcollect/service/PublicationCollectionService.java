package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorAlias;
import de.rwth.i9.palm.model.AuthorSource;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.FileType;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationFile;
import de.rwth.i9.palm.model.PublicationSource;
import de.rwth.i9.palm.model.PublicationType;
import de.rwth.i9.palm.model.Source;
import de.rwth.i9.palm.model.SourceMethod;
import de.rwth.i9.palm.model.SourceType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationService;

@Service
public class PublicationCollectionService
{
	private final static Logger log = LoggerFactory.getLogger( PublicationCollectionService.class );

	@Autowired
	private AsynchronousPublicationDetailCollectionService asynchronousPublicationDetailCollectionService;

	@Autowired
	private AsynchronousCollectionService asynchronousCollectionService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;
	
	@Autowired
	private PalmAnalytics palmAnalitics;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private MendeleyOauth2Helper mendeleyOauth2Helper;

	/**
	 * Fetch author' publication list from academic networks
	 * 
	 * @param responseMap
	 * @param author
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ParseException
	 * @throws TimeoutException
	 * @throws org.apache.http.ParseException
	 * @throws OAuthProblemException
	 * @throws OAuthSystemException
	 */
	public void collectPublicationListFromNetwork( Map<String, Object> responseMap, Author author ) throws IOException, InterruptedException, ExecutionException, ParseException, TimeoutException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		// get author sources
		Set<AuthorSource> authorSources = author.getAuthorSources();
		if ( authorSources == null )
		{
			// TODO update author sources
			responseMap.put( "result", "error" );
			responseMap.put( "reason", "no author sources found" );
		}

		// getSourceMap
		Map<String, Source> sourceMap = applicationService.getAcademicNetworkSources();

		// future list for publication list
		// extract dataset from academic network concurrently
		// Stopwatch stopwatch = Stopwatch.createStarted();

		List<Future<List<Map<String, String>>>> publicationFutureLists = new ArrayList<Future<List<Map<String, String>>>>();

		for ( AuthorSource authorSource : authorSources )
		{
			if ( authorSource.getSourceType() == SourceType.GOOGLESCHOLAR && sourceMap.get( SourceType.GOOGLESCHOLAR.toString() ).isActive() )
				publicationFutureLists.add( asynchronousCollectionService.getListOfPublicationsGoogleScholar( authorSource.getSourceUrl(), sourceMap.get( SourceType.GOOGLESCHOLAR.toString() ) ) );
			else if ( authorSource.getSourceType() == SourceType.CITESEERX && sourceMap.get( SourceType.CITESEERX.toString() ).isActive() )
				publicationFutureLists.add( asynchronousCollectionService.getListOfPublicationCiteseerX( authorSource.getSourceUrl(), sourceMap.get( SourceType.CITESEERX.toString() ) ) );
			else if ( authorSource.getSourceType() == SourceType.DBLP && sourceMap.get( SourceType.DBLP.toString() ).isActive() )
				publicationFutureLists.add( asynchronousCollectionService.getListOfPublicationDBLP( authorSource.getSourceUrl(), sourceMap.get( SourceType.DBLP.toString() ) ) );
			else if ( authorSource.getSourceType() == SourceType.DBLP && sourceMap.get( SourceType.MENDELEY.toString() ).isActive() )
			{
				// check for token validity
				mendeleyOauth2Helper.checkAndUpdateMendeleyToken( sourceMap.get( SourceType.MENDELEY.toString() ) );

			}
		}

		// Wait until they are all done
//		boolean processIsDone = true;
//		do
//		{
//			processIsDone = true;
//			for ( Future<List<Map<String, String>>> futureList : publicationFutureLists )
//			{
//				if ( !futureList.isDone() )
//				{
//					processIsDone = false;
//					break;
//				}
//			}
//			// 10-millisecond pause between each check
//			Thread.sleep( 10 );
//		} while ( !processIsDone );

		// merge the result
		this.mergePublicationInformation( publicationFutureLists, author );
	}
	
	/**
	 * Collect publication information and combine it into publication object
	 * 
	 * @param publicationFutureLists
	 * @param author
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 * @throws ParseException
	 * @throws TimeoutException 
	 */
	public void mergePublicationInformation( List<Future<List<Map<String, String>>>> publicationFutureLists, Author author ) throws InterruptedException, ExecutionException, IOException, ParseException, TimeoutException
	{
		if ( publicationFutureLists.size() > 0 )
		{
			// list/set of selected publication, either from database or completely new 
			List<Publication> selectedPublications = new ArrayList<Publication>();
			
			// first construct the publication
			// get it from database or create new if still doesn't exist
			this.constructPublicationWithSources( selectedPublications, publicationFutureLists , author );
			
			// extract and combine information from multiple sources
			this.getPublicationInformationFromSources( selectedPublications, author );

			// enrich the publication information by extract information
			// from html or pdf source
			this.enrichPublicationByExtractOriginalSources( selectedPublications, author, false );

			// at the end save everything
			for ( Publication publication : selectedPublications )
			{
				publication.setContentUpdated( true );
				persistenceStrategy.getPublicationDAO().persist( publication );
			}
		}
		
	}
	
	/**
	 * Construct the publication and publicationSources, with the data gathered
	 * from academic networks
	 * 
	 * @param selectedPublications
	 * @param publicationFutureLists
	 * @param author
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void constructPublicationWithSources( List<Publication> selectedPublications,  List<Future<List<Map<String, String>>>> publicationFutureLists , Author author ) throws InterruptedException, ExecutionException{
		for ( Future<List<Map<String, String>>> publicationFutureList : publicationFutureLists )
		{
			if ( publicationFutureList.isDone() )
			{
				// here, if process has not been completed yet. It will wait,
				// until process complete
				List<Map<String, String>> publicationMapLists = publicationFutureList.get();
				for ( Map<String, String> publicationMap : publicationMapLists )
				{
					Publication publication = null;
					String publicationTitle = publicationMap.get( "title" );
					
					if( publicationTitle == null )
						continue;
					
					// check publication with the current selected list.
					if( !selectedPublications.isEmpty()){
						for( Publication pub : selectedPublications){
							if ( palmAnalitics.getTextCompare().getDistanceByLuceneLevenshteinDistance( pub.getTitle().toLowerCase(), publicationTitle.toLowerCase() ) > .9f )
							{
								publication = pub;
								break;
							}
						}
					}
					
					// check with publication from database
					if ( publication == null )
					{
						// get the publication object
						List<Publication> fromDbPublications = persistenceStrategy.getPublicationDAO().getPublicationViaPhraseSlopQuery( publicationTitle.toLowerCase(), 2 );
						// check publication from database
						if ( !fromDbPublications.isEmpty() )
						{
							if ( fromDbPublications.size() > 1 )
							{
								// check with year
								for ( Publication pub : fromDbPublications )
								{
									if ( pub.getPublicationDate() == null )
										continue;

									Calendar cal = Calendar.getInstance();
									cal.setTime( pub.getPublicationDate() );
									if ( Integer.toString( cal.get( Calendar.YEAR ) ).equals( publicationMap.get( "year" ) ) )
									{
										publication = pub;
										break;
									}
								}
								// if publication still null, due to publication
								// date is null
								if ( publication == null )
									publication = fromDbPublications.get( 0 );
							}
							else
								publication = fromDbPublications.get( 0 );
							// added to selected list
							selectedPublications.add( publication );
						}
						// remove old publicationSource
						if ( publication != null )
							publication.removeNonUserInputPublicationSource();
					}

					// if not exist any where create new publication
					if( publication == null ){
						publication = new Publication();
						publication.setTitle( publicationTitle );
						selectedPublications.add( publication );
					}
					// add coauthor
					publication.addCoAuthor( author );
					
					// create publication sources and assign it to publication
					PublicationSource publicationSource = new PublicationSource();
					publicationSource.setTitle( publicationTitle );
					publicationSource.setSourceUrl( publicationMap.get( "url" ) );
					publicationSource.setSourceMethod( SourceMethod.PARSEPAGE );
					publicationSource.setSourceType( SourceType.valueOf(publicationMap.get( "source" ).toUpperCase() ) );
					publicationSource.setPublication( publication );

					if ( publicationMap.get( "nocitation" ) != null )
						publicationSource.setCitedBy( Integer.parseInt( publicationMap.get( "nocitation" ) ) );

					if ( publicationMap.get( "coauthor" ) != null )
						publicationSource.setCoAuthors( publicationMap.get( "coauthor" ) );

					if ( publicationMap.get( "coauthorUrl" ) != null )
						publicationSource.setCoAuthorsUrl( publicationMap.get( "coauthorUrl" ) );

					if ( publicationMap.get( "year" ) != null )
						publicationSource.setDate( publicationMap.get( "year" ) );

					if ( publicationMap.get( "doc" ) != null )
						publicationSource.setMainSource( publicationMap.get( "doc" ) );

					if ( publicationMap.get( "doc_url" ) != null )
						publicationSource.setMainSourceUrl( publicationMap.get( "doc_url" ) );

					if ( publicationMap.get( "type" ) != null )
						publicationSource.setPublicationType( publicationMap.get( "type" ) );

					publication.addPublicationSource( publicationSource );
								
					// combine information from multiple sources;
					
					// check  print 
//					for ( Entry<String, String> eachPublicationDetail : publicationMap.entrySet() )
//						System.out.println( eachPublicationDetail.getKey() + " : " + eachPublicationDetail.getValue() );
//					System.out.println();
				}
			}
		}
	}
	
	/**
	 * combine publication information from multiple publication sources
	 * 
	 * @param selectedPublications
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ParseException
	 */
	public void getPublicationInformationFromSources( List<Publication> selectedPublications, Author pivotAuthor ) throws IOException, InterruptedException, ExecutionException, ParseException
	{
		//multi thread future publication detail
		List<Future<Publication>> selectedPublicationFutureList = new ArrayList<Future<Publication>>();
		for( Publication publication : selectedPublications){
			selectedPublicationFutureList.add( asynchronousPublicationDetailCollectionService.asyncWalkOverSelectedPublication( publication ) );
		}
		
		// Wait until they are all done
		// Thread.sleep( 1000 );
		boolean walkingPublicationIsDone = true;
		// list coauthor of pivotauthor
		/*List<Author> coAuthors = new ArrayList<Author>();*/
		do
		{
			walkingPublicationIsDone = true;
			for ( Future<Publication> selectedPublicationFuture : selectedPublicationFutureList )
			{
				if ( !selectedPublicationFuture.isDone() )
					walkingPublicationIsDone = false;
				else
				{
					// combine from sources to publication
					Publication publication = mergingPublicationInformation( selectedPublicationFuture.get(), pivotAuthor/* , coAuthors */ );
//
//					// set is updated true
//					publication.setContentUpdated( true );
//
//					// persist
//					persistenceStrategy.getPublicationDAO().persist( publication );
				}

			}
			// 10-millisecond pause between each check
			Thread.sleep( 10 );
		} while ( !walkingPublicationIsDone );
		
	}

	/**
	 * Extract publication information from original source either as html or
	 * pdf with asynchronous multi threads
	 * 
	 * @param publication
	 * @param pivotAuthor
	 * @param persistResult
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public void enrichPublicationByExtractOriginalSources( List<Publication> selectedPublications, Author pivotAuthor, boolean persistResult ) throws IOException, InterruptedException, ExecutionException, TimeoutException
	{
		log.info( "Start publications enrichment for Auhtor " + pivotAuthor.getName() );
		List<Future<Publication>> selectedPublicationFutureList = new ArrayList<Future<Publication>>();

		for ( Publication publication : selectedPublications )
		{
			selectedPublicationFutureList.add( asynchronousPublicationDetailCollectionService.asyncEnrichPublicationInformationFromOriginalSource( publication ) );
		}

		// check process completion
		for ( Future<Publication> selectedPublicationFuture : selectedPublicationFutureList )
		{
			Publication publication = selectedPublicationFuture.get();

			if ( persistResult )
			{
				publication.setContentUpdated( true );
				persistenceStrategy.getPublicationDAO().persist( publication );
			}
		}
		log.info( "Done publications enrichment for Auhtor " + pivotAuthor.getName() );
	}

	/**
	 * 
	 * @param selectedPublications
	 * @throws ParseException
	 */
	public Publication mergingPublicationInformation( Publication publication,
			Author pivotAuthor/* , List<Author> coAuthors */ ) throws ParseException
	{
		DateFormat dateFormat = new SimpleDateFormat( "yyyy/M/d", Locale.ENGLISH );
		Calendar calendar = Calendar.getInstance();
		Set<String> existingMainSourceUrl = new HashSet<String>();

		for ( PublicationSource pubSource : publication.getPublicationSources() )
		{
			Date publicationDate = null;
			PublicationType publicationType = null;
			// Get unique characteristic on each of the source
			if ( pubSource.getSourceType() == SourceType.GOOGLESCHOLAR )
			{
				// publication date
				if ( pubSource.getDate() != null )
				{
					String pubSourceDate = pubSource.getDate();
					String publicationDateFormat = "yyyy/M/d";
					if ( pubSourceDate.length() == 4 )
					{
						pubSourceDate += "/1/1";
						publicationDateFormat = "yyyy";
					}
					else if ( pubSourceDate.length() < 8 )
					{
						pubSourceDate += "/1";
						publicationDateFormat = "yyyy/M";
					}
					publicationDate = dateFormat.parse( pubSourceDate );
					publication.setPublicationDate( publicationDate );
					publication.setPublicationDateFormat( publicationDateFormat );
				}

				if ( pubSource.getPages() != null )
					publication.setPages( pubSource.getPages() );

				if ( pubSource.getPublisher() != null )
					publication.setPublisher( pubSource.getPublisher() );

				if ( pubSource.getIssue() != null )
					publication.setIssue( pubSource.getIssue() );

				if ( pubSource.getVolume() != null )
					publication.setVolume( pubSource.getVolume() );

			}
			else if ( pubSource.getSourceType() == SourceType.CITESEERX )
			{
				// nothing to do
			}
			// for general information
			// author
			if ( pubSource.getCoAuthors() != null )
			{
				String[] authorsArray = pubSource.getCoAuthors().split( "," );
				// for DBLP where the coauthor have a source link
				String[] authorsUrlArray = null;
				if ( pubSource.getCoAuthorsUrl() != null )
					authorsUrlArray = pubSource.getCoAuthorsUrl().split( " " );

				if ( authorsArray.length > publication.getCoAuthors().size() )
				{
					for ( int i = 0; i < authorsArray.length; i++ )
					{
						String authorString = authorsArray[i].toLowerCase().replace( ".", "" ).trim();

						if ( authorString.equals( "" ) )
							continue;

						String[] splitName = authorString.split( " " );
						String lastName = splitName[splitName.length - 1];
						String firstName = authorString.substring( 0, authorString.length() - lastName.length() ).trim();
						
						Author author = null;
						if ( pivotAuthor.getName().toLowerCase().equals( authorString.toLowerCase() ) )
							author = pivotAuthor;
						else
						{
							// first check from database by full name
							List<Author> coAuthorsDb = persistenceStrategy.getAuthorDAO().getByName( authorString );
							if( !coAuthorsDb.isEmpty() ){
								// TODO: check other properties
								// for now just get the first element
								// later check whether there is already a connection with pivotAuthor
								// if not check institution
								author = coAuthorsDb.get( 0 );
							}
							
							// if there is no exact name, check for ambiguity,
							// start from lastname
							// and then check whether there is abbreviation name on first name
							if( author == null ){
								coAuthorsDb = persistenceStrategy.getAuthorDAO().getByLastName( lastName );
								if( !coAuthorsDb.isEmpty() ){
									String[] firstNameSplit = firstName.split( " " );
									for ( Author coAuthorDb : coAuthorsDb )
									{
										if ( coAuthorDb.isAliasNameFromFirstName( firstNameSplit ) )
										{
											// select longest name as the
											// fullname
											if ( coAuthorDb.getFirstName().length() > firstName.length() )
											{
												AuthorAlias authorAlias = new AuthorAlias();
												authorAlias.setCompleteName( authorString );
												authorAlias.setAuthor( coAuthorDb );
												coAuthorDb.addAlias( authorAlias );
												persistenceStrategy.getAuthorDAO().persist( coAuthorDb );
											}
											else
											{
												// change name with longer name
												String tempName = coAuthorDb.getName();
												coAuthorDb.setName( authorString );
												coAuthorDb.setFirstName( firstName );

												AuthorAlias authorAlias = new AuthorAlias();
												authorAlias.setCompleteName( tempName );
												authorAlias.setAuthor( coAuthorDb );
												coAuthorDb.addAlias( authorAlias );
												persistenceStrategy.getAuthorDAO().persist( coAuthorDb );
											}

											author = coAuthorDb;
											break;
										}
									}
								}
							}

							// TODO : this probably not correct, since author
							// name are ambigous, the best way is to check their
							// relation and their affiliation
							if( author == null ){
								// create new author
								author = new Author();
								// set for all possible name
								author.setPossibleNames( authorString );
	
								// save new author
								persistenceStrategy.getAuthorDAO().persist( author );

								author.addPublication( publication );
								publication.addCoAuthor( author );

							}

							// assign with authorSource, if exist
							if ( authorsUrlArray != null && !author.equals( pivotAuthor ) )
							{
								AuthorSource authorSource = new AuthorSource();
								authorSource.setName( author.getName() );
								authorSource.setSourceUrl( authorsUrlArray[i] );
								authorSource.setSourceType( pubSource.getSourceType() );
								authorSource.setAuthor( author );

								author.addAuthorSource( authorSource );
								// persist new source
								persistenceStrategy.getAuthorDAO().persist( author );
							}
						}
					}
				}
			}

			// abstract ( searching the longest)
			if ( pubSource.getAbstractText() != null )
				if ( publication.getAbstractText() == null || publication.getAbstractText().length() < pubSource.getAbstractText().length() )
					publication.setAbstractText( pubSource.getAbstractText() );

			if ( publication.getPublicationDate() == null && publicationDate == null && pubSource.getDate() != null )
			{
				publicationDate = dateFormat.parse( pubSource.getDate() + "/1/1" );
				publication.setPublicationDate( publicationDate );
				publication.setPublicationDateFormat( "yyyy" );
			}

			if ( pubSource.getCitedBy() > 0 && pubSource.getCitedBy() > publication.getCitedBy() )
				publication.setCitedBy( pubSource.getCitedBy() );

			// venuetype
			if ( pubSource.getPublicationType() != null )
			{
				publicationType = PublicationType.valueOf( pubSource.getPublicationType() );
				publication.setPublicationType( publicationType );


				if ( ( publicationType.equals( "CONFERENCE" ) || publicationType.equals( "JOURNAL" ) ) && pubSource.getVenue() != null && publication.getEvent() == null )
				{
					String eventName = pubSource.getVenue();
					EventGroup eventGroup = null;
					Event event = null;
					List<EventGroup> eventGroups = persistenceStrategy.getEventDAO().getEventViaFuzzyQuery( eventName, .8f, 1 );
					if ( eventGroups.isEmpty() )
					{
						if ( publicationType != null )
						{
							// create event group
							eventGroup = new EventGroup();
							eventGroup.setName( eventName );
							String notationName = null;
							String[] eventNameSplit = eventName.split( " " );
							for ( String eachEventName : eventNameSplit )
								if ( !eachEventName.equals( "" ) && Character.isUpperCase( eachEventName.charAt( 0 ) ) )
									notationName += eachEventName.substring( 0, 1 );
							eventGroup.setNotation( notationName );
							eventGroup.setPublicationType( publicationType );
							// create event
							if ( publicationDate != null )
							{
								// save event group
								persistenceStrategy.getEventGroupDAO().persist( eventGroup );

								calendar.setTime( publicationDate );
								event = new Event();
								event.setDate( publicationDate );
								event.setYear( Integer.toString( calendar.get( Calendar.YEAR ) ) );
								event.setEventGroup( eventGroup );
								publication.setEvent( event );
							}
						}
					}
				}
			}

			// original source
			if ( pubSource.getMainSourceUrl() != null )
			{

				String[] mainSourceUrls = pubSource.getMainSourceUrl().split( " " );
				String[] mainSources = pubSource.getMainSource().split( "," );
				for ( int i = 0; i < mainSourceUrls.length; i++ )
				{
					if ( existingMainSourceUrl.contains( mainSourceUrls[i] ) )
						continue;

					existingMainSourceUrl.add( mainSourceUrls[i] );

					// not exist create new
					PublicationFile pubFile = new PublicationFile();
					pubFile.setSourceType( pubSource.getSourceType() );
					pubFile.setUrl( mainSourceUrls[i] );
					if ( mainSources[i].equals( "null" ) )
						pubFile.setSource( pubSource.getSourceType().toString().toLowerCase() );
					else
						pubFile.setSource( mainSources[i] );

					if ( mainSourceUrls[i].toLowerCase().endsWith( ".pdf" ) || mainSourceUrls[i].toLowerCase().endsWith( "pdf.php" ) || mainSources[i].toLowerCase().contains( "pdf" ) )
						pubFile.setFileType( FileType.PDF );
					else if ( mainSourceUrls[i].toLowerCase().endsWith( ".xml" ) )
					{
						// nothing to do
					}
					else
						pubFile.setFileType( FileType.HTML );
					pubFile.setPublication( publication );

					if ( pubFile.getFileType() != null )
						publication.addPublicationFile( pubFile );

				}
			}

		}

		return publication;
	}

}
