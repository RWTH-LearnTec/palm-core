package de.rwth.i9.palm.controller;


import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
public class LoginController
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@RequestMapping( value = "/login", method = RequestMethod.GET )
	@Transactional
	public ModelAndView home( @RequestParam( value = "sessionid", required = false ) final String sessionId, final HttpServletResponse response )
	{
		ModelAndView mav = new ModelAndView( "login", "link", "login" );

		if ( sessionId != null && sessionId.equals( "0" ) )
			response.setHeader( "SESSION_INVALID", "yes" );

		return mav;
	}
}