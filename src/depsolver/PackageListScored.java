package depsolver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class PackageListScored {
	private List<PackageImproved> validPackageState;
	private List<String> packagesToUninstall;
	private List<String> packagesToKeep;
	private Integer score;
	private DefaultDirectedGraph<String, DefaultEdge> packageGraph;

	
	public PackageListScored(List<PackageImproved> validPackageState)
	{
		packagesToUninstall = new ArrayList<>();
		packagesToKeep = new ArrayList<>();
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
					if(directedGraph.containsVertex(dpp.getName() + "=" + dpp.getVersion()))
					{
						directedGraph.addEdge(p.getName() + "=" + p.getVersion(), dpp.getName() + "=" + dpp.getVersion());
					}
				}
			}
		}
		return directedGraph;
	}
	
	public String TopologicalSort() 
	{
		TopologicalOrderIterator<String, DefaultEdge> orderIterator;
		CycleDetector<String, DefaultEdge> cycleDetector;

		cycleDetector = new CycleDetector<String, DefaultEdge>(packageGraph);

		if (cycleDetector.detectCycles()) {
			return "Cyclic dependency";
		}
		else
		{
			orderIterator = new TopologicalOrderIterator<String, DefaultEdge>(packageGraph);
			String current;

			List<String> arrReversed = new ArrayList<>();
			while(orderIterator.hasNext())
			{
				current = orderIterator.next();
				arrReversed.add(current);
			}

			String jsonOut = "[";
			if(!packagesToUninstall.isEmpty())
			{
			for(String p : packagesToUninstall)
			{
				jsonOut = jsonOut + "\"-" + p + "\",";
			}
			jsonOut = jsonOut.substring(0, jsonOut.length() - 1) + ",";
			}
			
			
			for(int i = arrReversed.size() - 1; i >= 0; i--)
			{
				if(!(packagesToKeep.contains(arrReversed.get(i))))
				{
					jsonOut = jsonOut + "\"+" + arrReversed.get(i) + "\",";
				}
				
			}

			jsonOut = jsonOut.substring(0, jsonOut.length() - 1);
			jsonOut = jsonOut + "]";
			return jsonOut;
		}
	}
		
	public void addPackageToUninstall(Package installedConflict)
	{
		packagesToUninstall.add(installedConflict.getName() + "=" + installedConflict.getVersion());
		score = score + 1000000;
	}
	
	public void addPackageToKeep(Package installedDependency)
	{
		packagesToKeep.add(installedDependency.getName() + "=" + installedDependency.getVersion());
		score = score - installedDependency.getSize();
	}
}
