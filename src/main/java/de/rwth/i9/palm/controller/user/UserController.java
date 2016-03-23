package de.rwth.i9.palm.controller.user;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.model.UserAuthorBookmark;
import de.rwth.i9.palm.model.UserCircleBookmark;
import de.rwth.i9.palm.model.UserEventGroupBookmark;
import de.rwth.i9.palm.model.UserPublicationBookmark;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@RequestMapping( value = "/user" )
public class UserController
{

	private static final String LINK_NAME = "user";
	
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@RequestMapping( method = RequestMethod.GET )
	public ModelAndView userPage( 
			@RequestParam( value = "page", required = false ) final String page,
			final HttpServletResponse response ) throws InterruptedException
	{
		ModelAndView model = TemplateHelper.createViewWithLink( "user", LINK_NAME );

		if( page != null)
			model.addObject( "activeMenu", page );
		else
			model.addObject( "activeMenu", "profile" );

		return model;
	}

	/**
	 * Add user's bookmark for researcher, publication, conference or circle
	 * 
	 * @param bookmarkType
	 * @param userId
	 * @param bookId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/bookmark/{bookmarkType}", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> bookmarkAdd( 
			@PathVariable String bookmarkType, 
			@RequestParam( value = "userId" ) final String userId, 
			@RequestParam( value = "bookId" ) final String bookId, 
			final HttpServletResponse response)
	{
		// set response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		User user = persistenceStrategy.getUserDAO().getById( userId );

		if ( user == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "error - user not found" );
			return responseMap;
		}

		// saving researcher bookmark
		if ( bookmarkType.equals( "author" ) )
		{
			Author author = persistenceStrategy.getAuthorDAO().getById( bookId );

			if ( author == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - author not found" );
				return responseMap;
			}
			UserAuthorBookmark userAuthorBookmark = new UserAuthorBookmark();
			userAuthorBookmark.setUser( user );
			userAuthorBookmark.setAuthor( author );

			// get current timestamp
			java.util.Date date = new java.util.Date();
			Timestamp currentTimestamp = new Timestamp( date.getTime() );

			userAuthorBookmark.setBookedDate( currentTimestamp );

			user.addUserAuthorBookmark( userAuthorBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// saving publication bookmark
		else if ( bookmarkType.equals( "publication" ) )
		{
			Publication publication = persistenceStrategy.getPublicationDAO().getById( bookId );

			if ( publication == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - publication not found" );
				return responseMap;
			}
			UserPublicationBookmark userPublicationBookmark = new UserPublicationBookmark();
			userPublicationBookmark.setUser( user );
			userPublicationBookmark.setPublication( publication );

			// get current timestamp
			java.util.Date date = new java.util.Date();
			Timestamp currentTimestamp = new Timestamp( date.getTime() );

			userPublicationBookmark.setBookedDate( currentTimestamp );

			user.addUserPublicationBookmark( userPublicationBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// saving circle bookmark
		else if ( bookmarkType.equals( "circle" ) )
		{
			Circle circle = persistenceStrategy.getCircleDAO().getById( bookId );

			if ( circle == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - circle not found" );
				return responseMap;
			}
			UserCircleBookmark userCircleBookmark = new UserCircleBookmark();
			userCircleBookmark.setUser( user );
			userCircleBookmark.setCircle( circle );

			// get current timestamp
			java.util.Date date = new java.util.Date();
			Timestamp currentTimestamp = new Timestamp( date.getTime() );

			userCircleBookmark.setBookedDate( currentTimestamp );

			user.addUserCircleBookmark( userCircleBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// saving eventGroup bookmark
		else if ( bookmarkType.equals( "eventGroup" ) )
		{
			EventGroup eventGroup = persistenceStrategy.getEventGroupDAO().getById( bookId );

			if ( eventGroup == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - eventGroup not found" );
				return responseMap;
			}
			UserEventGroupBookmark userEventGroupBookmark = new UserEventGroupBookmark();
			userEventGroupBookmark.setUser( user );
			userEventGroupBookmark.setEventGroup( eventGroup );

			// get current timestamp
			java.util.Date date = new java.util.Date();
			Timestamp currentTimestamp = new Timestamp( date.getTime() );

			userEventGroupBookmark.setBookedDate( currentTimestamp );

			user.addUserEventGroupBookmark( userEventGroupBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		responseMap.put( "status", "ok" );
		return responseMap;
	}

	/**
	 * Add user's bookmark for researcher, publication, conference or circle
	 * 
	 * @param bookmarkType
	 * @param userId
	 * @param bookId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/bookmark/remove/{bookmarkType}", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> bookmarkRemove( @PathVariable String bookmarkType, @RequestParam( value = "userId" ) final String userId, @RequestParam( value = "bookId" ) final String bookId, final HttpServletResponse response)
	{
		// set response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		User user = persistenceStrategy.getUserDAO().getById( userId );

		if ( user == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "error - user not found" );
			return responseMap;
		}

		// remove bookmark researcher
		if ( bookmarkType.equals( "author" ) )
		{
			Author author = persistenceStrategy.getAuthorDAO().getById( bookId );

			if ( author == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - author not found" );
				return responseMap;
			}

			// get object and remove link
			UserAuthorBookmark userAuthorBookmark = persistenceStrategy.getUserAuthorBookmarkDAO().getByUserAndAuthor( user, author );
			user.removeUserAuthorBookmark( userAuthorBookmark );
			userAuthorBookmark.setUser( null );
			userAuthorBookmark.setAuthor( null );

			// update and store objects
			persistenceStrategy.getUserAuthorBookmarkDAO().delete( userAuthorBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// remove bookmark publication
		else if ( bookmarkType.equals( "publication" ) )
		{
			Publication publication = persistenceStrategy.getPublicationDAO().getById( bookId );

			if ( publication == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - publication not found" );
				return responseMap;
			}

			// get object and remove link
			UserPublicationBookmark userPublicationBookmark = persistenceStrategy.getUserPublicationBookmarkDAO().getByUserAndPublication( user, publication );
			user.removeUserPublicationBookmark( userPublicationBookmark );
			userPublicationBookmark.setUser( null );
			userPublicationBookmark.setPublication( null );

			// update and store objects
			persistenceStrategy.getUserPublicationBookmarkDAO().delete( userPublicationBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// remove bookmark circle
		else if ( bookmarkType.equals( "circle" ) )
		{
			Circle circle = persistenceStrategy.getCircleDAO().getById( bookId );

			if ( circle == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - circle not found" );
				return responseMap;
			}

			// get object and remove link
			UserCircleBookmark userCircleBookmark = persistenceStrategy.getUserCircleBookmarkDAO().getByUserAndCircle( user, circle );
			user.removeUserCircleBookmark( userCircleBookmark );
			userCircleBookmark.setUser( null );
			userCircleBookmark.setCircle( null );

			// update and store objects
			persistenceStrategy.getUserCircleBookmarkDAO().delete( userCircleBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// remove bookmark eventGroup
		if ( bookmarkType.equals( "eventGroup" ) )
		{
			EventGroup eventGroup = persistenceStrategy.getEventGroupDAO().getById( bookId );

			if ( eventGroup == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - eventGroup not found" );
				return responseMap;
			}

			// get object and remove link
			UserEventGroupBookmark userEventGroupBookmark = persistenceStrategy.getUserEventGroupBookmarkDAO().getByUserAndEventGroup( user, eventGroup );
			user.removeUserEventGroupBookmark( userEventGroupBookmark );
			userEventGroupBookmark.setUser( null );
			userEventGroupBookmark.setEventGroup( null );

			// update and store objects
			persistenceStrategy.getUserEventGroupBookmarkDAO().delete( userEventGroupBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		responseMap.put( "status", "ok" );
		return responseMap;
	}

}