package depsolver;

import java.util.List;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

public class PackageListScored {
	private List<PackageImproved> validPackageState;
	private Integer score;
	
	public PackageListScored(List<PackageImproved> validPackageState)
	{
		this.validPackageState = validPackageState;
		Integer score = 0;
		
		for(PackageImproved p : validPackageState)
		{
			score = score + p.getSize();
		}
		
		this.score = score;
	}
	
	public List<PackageImproved> getPackageList()
	{
		return validPackageState;
	}
	
	public Integer getScore()
	{
		return score;
	}
	
	//graph return
	
	public Graph<PackageImproved> getGraph()
	{
		MutableGraph<PackageImproved> test = GraphBuilder.directed().build();
		
		for(PackageImproved p : validPackageState)
		{
			for(List<Package> dp : p.getDepends())
			{
				for(Package dpp: dp)
				{
					
				}
			}
		}
		
		return null;
	}
}
