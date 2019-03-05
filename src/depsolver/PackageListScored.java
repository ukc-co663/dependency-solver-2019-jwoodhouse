package depsolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import com.alibaba.fastjson.JSON;

public class PackageListScored {
	private List<Package> validPackageState;
	private List<String> packagesToUninstall;
	private List<String> packagesToKeep;
	private Integer score;
	private DefaultDirectedGraph<String, DefaultEdge> packageGraph;

	
	public PackageListScored(List<Package> validPackageState)
	{
		packagesToUninstall = new ArrayList<>();
		packagesToKeep = new ArrayList<>();
		this.validPackageState = validPackageState;
		score = 0;
		
		for(Package p : validPackageState)
		{
			score = score + p.getSize();
		}
		
		packageGraph = generateGraph();
	}
	
	public List<Package> getPackageList()
	{
		return validPackageState;
	}
	
	public Integer getScore()
	{
		return score;
	}
	
	private DefaultDirectedGraph<String, DefaultEdge> generateGraph()
	{
		DefaultDirectedGraph<String, DefaultEdge> directedGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
		
		for(Package p : validPackageState)
		{
			directedGraph.addVertex(p.getName() + "=" + p.getVersion());
		}
		
		for(Package p : validPackageState)
		{
			for(List<Package> dp : p.getDependsAsPackages())
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
	
	public String topologicalSortAsJSON() 
	{
		TopologicalOrderIterator<String, DefaultEdge> orderIterator;
		CycleDetector<String, DefaultEdge> cycleDetector;

		cycleDetector = new CycleDetector<>(packageGraph);

		if (cycleDetector.detectCycles()) {
			return "Cyclic dependency";
		}
		else
		{
			orderIterator = new TopologicalOrderIterator<>(packageGraph);
			String current;

			List<String> arrReversed = new ArrayList<>();

			while(orderIterator.hasNext())
			{
				current = orderIterator.next();
				arrReversed.add("+" + current);
			}
			Collections.reverse(arrReversed);

			ArrayList<String> jsonOut = new ArrayList<>(packagesToUninstall);
			
			for(String p : arrReversed)
			{
				if(!(packagesToKeep.contains(p)))
				{
					jsonOut.add(p);
				}
			}

			return JSON.toJSONString(jsonOut);
		}
	}
		
	public void addPackageToUninstall(Package installedConflict)
	{
		packagesToUninstall.add("-" + installedConflict.getName() + "=" + installedConflict.getVersion());
		score = score + 1000000;
	}
	
	public void addPackageToKeep(Package installedDependency)
	{
		packagesToKeep.add("+" + installedDependency.getName() + "=" + installedDependency.getVersion());
		score = score - installedDependency.getSize();
	}
}
