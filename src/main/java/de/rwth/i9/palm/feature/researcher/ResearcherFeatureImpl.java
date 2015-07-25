package de.rwth.i9.palm.feature.researcher;

import org.springframework.beans.factory.annotation.Autowired;

public class ResearcherFeatureImpl implements ResearcherFeature
{
	@Autowired( required = false )
	private ResearcherInterest researcherInterest;
	
	@Autowired( required = false )
	private ResearcherInterestEvolution researcherInterestEvolution;
	
	@Override
	public ResearcherInterest getResearcherInterest()
	{
		if( this.researcherInterest == null )
			this.researcherInterest = new ResearcherInterestImpl();

		return this.researcherInterest;
	}

	@Override
	public ResearcherInterestEvolution getResearcherInterestEvolution()
	{
		if( this.researcherInterestEvolution == null )
			this.researcherInterestEvolution = new ResearcherInterestEvolutionImpl();

		return this.researcherInterestEvolution;
	}

}