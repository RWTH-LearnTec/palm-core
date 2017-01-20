package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public interface SimilarityVisualization
{
	public Map<String, Object> visualizeSimilarResearchers( String type, List<String> idsList, HttpServletRequest request );

	public Map<String, Object> visualizeSimilarConferences( String type, List<String> idsList, HttpServletRequest request );

	public Map<String, Object> visualizeSimilarPublications( String type, List<String> idsList, HttpServletRequest request );

	public Map<String, Object> visualizeSimilarTopics( String type, List<String> idsList, HttpServletRequest request );
}