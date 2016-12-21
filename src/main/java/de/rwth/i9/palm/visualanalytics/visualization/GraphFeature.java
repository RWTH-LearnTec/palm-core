package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

public interface GraphFeature
{
	public Map<String, Object> getGephiGraph( String type, List<Author> authorList, Set<Publication> authorPublications, List<String> idsList, List<Author> selectedAuthors, HttpServletRequest request );
}
