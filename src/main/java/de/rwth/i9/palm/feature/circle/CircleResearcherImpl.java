package de.rwth.i9.palm.feature.circle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.Institution;

public class CircleResearcherImpl implements CircleResearcher
{

	@Override
	public Map<String, Object> getCircleResearcherMap( Circle circle )
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		List<Map<String, Object>> researcherList = new ArrayList<Map<String, Object>>();

		if ( circle.getAuthors() == null || circle.getAuthors().isEmpty() )
		{
			responseMap.put( "count", 0 );
			return responseMap;
		}

		for ( Author researcher : circle.getAuthors() )
		{
			Map<String, Object> researcherMap = new LinkedHashMap<String, Object>();
			researcherMap.put( "id", researcher.getId() );
			researcherMap.put( "name", WordUtils.capitalize( researcher.getName() ) );
			if ( researcher.getPhotoUrl() != null )
				researcherMap.put( "photo", researcher.getPhotoUrl() );
			if ( researcher.getAcademicStatus() != null )
				researcherMap.put( "status", researcher.getAcademicStatus() );
			if ( researcher.getInstitutions() != null )
				for ( Institution institution : researcher.getInstitutions() )
				{
					if ( researcherMap.get( "aff" ) != null )
						researcherMap.put( "aff", researcherMap.get( "aff" ) + ", " + institution.getName() );
					else
						researcherMap.put( "aff", institution.getName() );
				}
			if ( researcher.getCitedBy() > 0 )
				researcherMap.put( "citedBy", Integer.toString( researcher.getCitedBy() ) );

			if ( researcher.getPublicationAuthors() != null )
				researcherMap.put( "publicationsNumber", researcher.getPublicationAuthors().size() );
			else
				researcherMap.put( "publicationsNumber", 0 );
			String otherDetail = "";
			if ( researcher.getOtherDetail() != null )
				otherDetail += researcher.getOtherDetail();
			if ( researcher.getDepartment() != null )
				otherDetail += ", " + researcher.getDepartment();
			if ( !otherDetail.equals( "" ) )
				researcherMap.put( "detail", otherDetail );

			researcherMap.put( "isAdded", researcher.isAdded() );

			researcherList.add( researcherMap );
		}
		responseMap.put( "count", researcherList.size() );
		responseMap.put( "researchers", researcherList );

		return responseMap;
	}

}