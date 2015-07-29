package de.rwth.i9.palm.feature.publication;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.codehaus.jackson.map.ObjectMapper;

public interface PublicationManage
{
	public Map<String, Object> extractPublicationFromPdf( String publicationId ) throws IOException, InterruptedException, ExecutionException;

	public Map<String, Object> editPublication( ObjectMapper publicationJson );
}
