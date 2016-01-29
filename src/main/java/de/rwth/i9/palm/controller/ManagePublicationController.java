package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.xml.sax.SAXException;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.pdfextraction.service.PdfExtractionService;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@SessionAttributes( "publication" )
@RequestMapping( value = "/publication" )
public class ManagePublicationController
{
	private static final String LINK_NAME = "administration";

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PdfExtractionService pdfExtractionService;

	@Autowired
	private SecurityService securityService;

	/**
	 * Load the add publication form together with publication object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.GET )
	public ModelAndView addNewPublication( 
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		if ( securityService.getUser() == null )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.PUBLICATION, "add" );

		// create blank Publication
		Publication publication = new Publication();

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "publication", publication );

		return model;
	}

	/**
	 * Save changes from Add publication detail, via Spring binding
	 * 
	 * @param extractionServiceListWrapper
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> saveNewPublication( 
			@ModelAttribute( "publication" ) Publication publication, 
			final HttpServletResponse response) throws InterruptedException
	{

		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		return responseMap;
	}

	/**
	 * Upload article via jquery ajax file upload saved to database
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws TikaException
	 * @throws SAXException
	 */
	@RequestMapping( value = "/upload", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> multiUpload( 
			MultipartHttpServletRequest request, HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException
	{
		return uploadAndExtractPdf( request );
	}

	private Map<String, Object> uploadAndExtractPdf( 
			MultipartHttpServletRequest request ) throws IOException, InterruptedException, ExecutionException
	{
		// build an iterator
		Iterator<String> itr = request.getFileNames();

		Map<String, Object> extractedPdfMap = null;

		// get each file
		while ( itr.hasNext() )
		{
			MultipartFile mpf = request.getFile( itr.next() );

			// extract pdf file
			extractedPdfMap = pdfExtractionService.extractPdfFromInputStream( mpf.getInputStream() );

			break;
		}
		return extractedPdfMap;
	}
	
	/**
	 * Load the add publication form together with publication object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/edit", method = RequestMethod.GET )
	public ModelAndView editPublication( 
			@RequestParam( value = "sessionid", required = false ) final String sessionId,
			@RequestParam( value = "id") final String publicationId,
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		if ( securityService.getUser() == null )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.PUBLICATION, "edit" );

		// create blank Publication
		Publication publication = persistenceStrategy.getPublicationDAO().getById( publicationId );
		publication.setAuthors();

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "publication", publication );

		return model;
	}
}