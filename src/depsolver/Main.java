package depsolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Sets;


class Package {
  private String name;
  private String version;
  private Integer size;
  private List<List<String>> depends = new ArrayList<>();
  private List<String> conflicts = new ArrayList<>();

	public String getName() 
	{
		return name;
	}

	public String getVersion() 
	{
		return version;
	}

	public Integer getSize() 
	{
		return size;
	}

	public List<List<String>> getDepends() 
	{
		return depends;
	}

	public List<String> getConflicts() 
	{
		return conflicts;
	}

	public void setName(String name) 
	{
		this.name = name;
	}

	public void setVersion(String version) 
	{
		this.version = version;
	}

	public void setSize(Integer size) 
	{
		this.size = size;
	}

	public void setDepends(List<List<String>> depends) 
	{
		this.depends = depends;
	}

	public void setConflicts(List<String> conflicts) 
	{
		this.conflicts = conflicts;
	}
}

public class Main {
  public static void main(String[] args) throws IOException 
  {
    TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
    
    List<Package> repository = JSON.parseObject(readFile(args[0]), repoType);
    
    TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
    
    List<String> initialState = JSON.parseObject(readFile(args[1]), strListType);
    List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);

    // CHANGE CODE BELOW:
    // using repo, initial and constraints, compute a solution and print the answer
    for (Package repoPackage : repository) 
    {
      System.out.printf("package %s version %s\n", repoPackage.getName(), repoPackage.getVersion());
      
      for (List<String> depClause : repoPackage.getDepends()) 
      {
        System.out.printf("  dependency:");
        
        for (String dependency : depClause) 
        {
          System.out.printf(" %s", dependency);
        }
        
        System.out.printf("\n");
      }
      
      for (String confClause : repoPackage.getConflicts())
      {
    	  System.out.printf("  conflict: %s\n", confClause);
      }
    }
    
    Set<Set<Package>> sortedSet = processSets_Dependencies(makePowerSet(repository), repository);
    sortedSet = processSets_Conflicts(sortedSet, repository);
    for(Set<Package> r : sortedSet)
	  {
		  System.out.printf("[");
		  for(Package p : r)
		  {
			  System.out.printf("%s=%s, ", p.getName(), p.getVersion());
		  }
		  
		  System.out.printf("]\n");
	  }
  }

  static String readFile(String filename) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    StringBuilder sb = new StringBuilder();
    br.lines().forEach(line -> sb.append(line));
    return sb.toString();
  }
   
  static Set<Set<Package>> makePowerSet(List<Package> repository)
  {
	  Set<Package> listToSet = Sets.newHashSet(repository);
	  Set<Set<Package>> powerSet = Sets.powerSet(listToSet);
	  return powerSet;
  }
   
  static Set<Set<Package>> processSets_Dependencies(Set<Set<Package>> powerSet, List<Package> repository)
  {
	  Set<Set<Package>> sortedSet = new HashSet<>();
	  for(Set<Package> subSet : powerSet)
	  {
		  List<Boolean> satisfied = new ArrayList<>();
		  
		  for(Package p : subSet)
		  {
			if(isSetValid_Dependencies(subSet, p, repository))
			{
				satisfied.add(true);
			}
			else
			{
				satisfied.add(false);
			}
		  }
		  if(!(satisfied.contains(false)))
		  {
			  sortedSet.add(subSet);
		  }
	  }
	  
	  return sortedSet;
  }
  
  static Set<Set<Package>> processSets_Conflicts(Set<Set<Package>> powerSet, List<Package> repository)
  {
	  Set<Set<Package>> sortedSet = new HashSet<>();
	  for(Set<Package> subSet : powerSet)
	  {
		  List<Boolean> satisfied = new ArrayList<>();
		  
		  for(Package p : subSet)
		  {
			  if(isSetValid_Conflicts(subSet, p, repository))
			  {
				  satisfied.add(true);
			  }
			  else
			  {
				  satisfied.add(false);
			  }
		  }
		  if(!(satisfied.contains(false)))
		  {
			  sortedSet.add(subSet);
		  }
	  }
	  return sortedSet;
  }
  
  static boolean isSetValid_Dependencies(Set<Package> subSet, Package p, List<Package> repository)
  {
	  List<Boolean> satisfied = new ArrayList<>();
	  
	  for(List<String> dependency : p.getDepends())
	  {
		  List<Package> depAsPack = dependencyToPackage(dependency, repository);
		  boolean subsat = false;
		  
		  for(Package dp :  depAsPack)
		  {
			  if (subSet.contains(dp))
			  {
				  subsat = true;
			  }
		  }
		  if(subsat == true)
		  {
			  satisfied.add(true);
		  }
		  else
		  {
			  satisfied.add(false);
		  }
	  }
	  
	  if(satisfied.contains(false))
	  {
		  return false;
	  }
	  return true;
  }
  
  static boolean isSetValid_Conflicts(Set<Package> subSet, Package p, List<Package> repository)
  {
	  List<Boolean> satisfied = new ArrayList<>();
	  
	  for(String conflict : p.getConflicts())
	  {
		  List<Package> confAsPack = conflictToPackage(conflict, repository);
		  boolean subSat = true;
		  
		  for(Package cp : confAsPack)
		  {
			  if(subSet.contains(cp))
			  {
				  subSat = false;
			  }
		  }
		  if(subSat == true)
		  {
			  satisfied.add(true);
		  }
		  else
		  {
			  satisfied.add(false);
		  }
	  }
	  
	  if(satisfied.contains(false))
	  {
		  return false;
	  }
	  
	  return true;
  }
  
  static List<Package> dependencyToPackage(List<String> dependency, List<Package> repository)
  {

	  List<Package> depsAsPackages = new ArrayList<>();

	  
	  for(String dep : dependency)
	  {
		if(dep.contains("<="))
		{
			String[] nameVersion = dep.split("<=");
			String name = nameVersion[0];
			int version = Integer.parseInt(nameVersion[1].replace(".", ""));
			
			for(Package p : repository)
			{
				int pVersion = Integer.parseInt(p.getVersion().replace(".", ""));
				if(p.getName().equals(name) && pVersion <= version)
				{
					depsAsPackages.add(p);
				}
			}
		}
		else if(dep.contains(">="))
		{
			String[] nameVersion = dep.split(">=");
			String name = nameVersion[0];
			int version = Integer.parseInt(nameVersion[1].replace(".", ""));
			
			for(Package p : repository)
			{
				int pVersion = Integer.parseInt(p.getVersion().replace(".", ""));
				if(p.getName().equals(name) && pVersion >= version)
				{
					depsAsPackages.add(p);
				}
			}
		}
		else if(dep.contains("<"))
		{
			String[] nameVersion = dep.split("<");
			String name = nameVersion[0];
			int version = Integer.parseInt(nameVersion[1].replace(".", ""));
			
			for(Package p : repository)
			{
				int pVersion = Integer.parseInt(p.getVersion().replace(".", ""));
				if(p.getName().equals(name) && pVersion < version)
				{
					depsAsPackages.add(p);
				}
			}
		}
		else if(dep.contains(">"))
		{
			String[] nameVersion = dep.split(">");
			String name = nameVersion[0];
			int version = Integer.parseInt(nameVersion[1].replace(".", ""));
			
			for(Package p : repository)
			{
				int pVersion = Integer.parseInt(p.getVersion().replace(".", ""));
				if(p.getName().equals(name) && pVersion > version)
				{
					depsAsPackages.add(p);
				}
			}
		}
		else if(dep.contains("="))
		{
			String[] nameVersion = dep.split("=");
			String name = nameVersion[0];
			int version = Integer.parseInt(nameVersion[1].replace(".", ""));
			
			for(Package p : repository)
			{
				int pVersion = Integer.parseInt(p.getVersion().replace(".", ""));
				if(p.getName().equals(name) && pVersion == version)
				{
					depsAsPackages.add(p);
				}
			}
		}
		else
		{
			String name = dep;
			
			for(Package p : repository)
			{
				if(p.getName().equals(name))
				{
					depsAsPackages.add(p);
				}
			}
		}
	  }
	  return depsAsPackages;
  }
  
  static List<Package> conflictToPackage(String conflict, List<Package> repository)
  {
	  List<Package> confsAsPackages = new ArrayList<>();
	  
	  if(conflict.contains("<="))
	  {
		  String[] nameVersion = conflict.split("<=");
		  String name = nameVersion[0];
		  int version = Integer.parseInt(nameVersion[1].replace(".", ""));
		  
		  for(Package p : repository)
		  {
			  int pVersion = Integer.parseInt(p.getVersion().replace(".", ""));
			  
			  if(p.getName().equals(name) && pVersion <= version)
			  {
				  confsAsPackages.add(p);
			  }
		  }
	  }
	  else if(conflict.contains(">="))
	  {
		  String[] nameVersion = conflict.split(">=");
		  String name = nameVersion[0];
		  int version = Integer.parseInt(nameVersion[1].replace(".", ""));
		  
		  for(Package p : repository)
		  {
			  int pVersion = Integer.parseInt(p.getVersion().replace(".", ""));
			  
			  if(p.getName().equals(name) && pVersion >= version)
			  {
				  confsAsPackages.add(p);
			  }
		  }
	  }
	  else if(conflict.contains("<"))
	  {
		  String[] nameVersion = conflict.split("<");
		  String name = nameVersion[0];
		  int version = Integer.parseInt(nameVersion[1].replace(".", ""));
		  
		  for(Package p : repository)
		  {
			  int pVersion = Integer.parseInt(p.getVersion().replace(".", ""));
			  
			  if(p.getName().equals(name) && pVersion < version)
			  {
				  confsAsPackages.add(p);
			  }
		  }
	  }
	  else if(conflict.contains(">"))
	  {
		  String[] nameVersion = conflict.split(">");
		  String name = nameVersion[0];
		  int version = Integer.parseInt(nameVersion[1].replace(".", ""));
		  
		  for(Package p : repository)
		  {
			  int pVersion = Integer.parseInt(p.getVersion().replace(".", ""));
			  
			  if(p.getName().equals(name) && pVersion > version)
			  {
				  confsAsPackages.add(p);
			  }
		  }
	  }
	  else if(conflict.contains("="))
	  {
		  String[] nameVersion = conflict.split("=");
		  String name = nameVersion[0];
		  int version = Integer.parseInt(nameVersion[1].replace(".", ""));
		  
		  for(Package p : repository)
		  {
			  int pVersion = Integer.parseInt(p.getVersion().replace(".", ""));
			  
			  if(p.getName().equals(name) && pVersion == version)
			  {
				  confsAsPackages.add(p);
			  }
		  }
	  }
	  else
	  {
		  String name = conflict;
			
			for(Package p : repository)
			{
				if(p.getName().equals(name))
				{
					confsAsPackages.add(p);
				}
			}
	  }
	  
	  return confsAsPackages;
  }
}
