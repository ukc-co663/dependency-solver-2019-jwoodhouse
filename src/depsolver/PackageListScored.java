package depsolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class PackageListScored {
	private List<PackageImproved> validPackageState;
	private Integer score;
	private DefaultDirectedGraph<String, DefaultEdge> packageGraph;

	
	public PackageListScored(List<PackageImproved> validPackageState)
	{
		this.validPackageState = validPackageState;
		Integer score = 0;
		
		for(PackageImproved p : validPackageState)
		{
			score = score + p.getSize();
		}
		
		this.score = score;
		packageGraph = generateGraph();
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
	
	private DefaultDirectedGraph<String, DefaultEdge> generateGraph()
	{
		DefaultDirectedGraph<String, DefaultEdge> directedGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
		
		for(PackageImproved p : validPackageState)
		{
			directedGraph.addVertex(p.getName() + "=" + p.getVersion());
		}
		
		for(PackageImproved p : validPackageState)
		{
			for(List<Package> dp : p.getDepends())
			{
				for(Package dpp : dp)
				{
					for(PackageImproved pp : validPackageState)
					{
						if(dpp.toString().equals(pp.toString()))
						{
							directedGraph.addEdge(p.getName() + "=" + p.getVersion(), dpp.getName() + "=" + dpp.getVersion());
						}
					}
				}
			}
		}
		return directedGraph;
	}
	
	public void TopologicalSort() 
	{
		TopologicalOrderIterator<String, DefaultEdge> orderIterator;
		String current;
		orderIterator = new TopologicalOrderIterator<String, DefaultEdge>(packageGraph);
		
		List<String> arrReversed = new ArrayList<>();
		String jsonOut = "[";
		
		
		
		while(orderIterator.hasNext())
		{
			current = orderIterator.next();
			arrReversed.add(current);
		}
		
		for(int i = arrReversed.size() - 1; i >= 0; i--)
		{
			jsonOut = jsonOut + "\"+" + arrReversed.get(i) + "\","; 
		}
		
		jsonOut = jsonOut.substring(0, jsonOut.length() - 1);
		jsonOut = jsonOut + "]";
		System.out.println(jsonOut);
	}
}
