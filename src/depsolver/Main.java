package depsolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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

    for (Package repoPackage : repository) {
    	System.out.printf("package %s version %s\n", repoPackage.getName(), repoPackage.getVersion());

    	for (List<String> depClause : repoPackage.getDepends()) {
    		System.out.printf("  dependency:");

    		for (String dependency : depClause) {
    			System.out.printf(" %s", dependency);
    		}

    		System.out.printf("\n");
    	}

    	for (String confClause : repoPackage.getConflicts()) {
    		System.out.printf("  conflict: %s\n", confClause);
    	}
    } 
    
    List<Package> packagesToInst = getPackageFromConstraints(constraints, repository);
    Set<Package> relevantPackages = Sets.newHashSet(getValidRepository(packagesToInst, repository));
    
    for(Package p : relevantPackages)
    {
    	System.out.printf("package %s version %s\n", p.getName(), p.getVersion());
    }
  }

  static String readFile(String filename) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    StringBuilder sb = new StringBuilder();
    br.lines().forEach(line -> sb.append(line));
    return sb.toString();
  }

  static List<Package> getPackageFromConstraints(List<String> constraints, List<Package> repository)
  {
	  //probably be lazy here and just import every version of the package if there is no version
	  //cause of uninstall weight.
	  
	  //also need to handle uninstalls at some stage. not sure how :(
	  /**
	   * FOR EACH CONSTRAINT
	   * 	GET NAME
	   * 	FOR EACH PACKAGE IN REPO
	   * 		ADD TO PARSEDCONSTRAINTS AS PACKAGE
	   * 	END
	   * END
	   */
	  List<Package> parsedConstraints = new ArrayList<>();
	  
	  for(String constraint : constraints)
	  {
		  if(constraint.contains("="))
		  {
			  String[] nameVersion = constraint.split("=");
			  String constraintName = nameVersion[0].substring(1);
			  String version = nameVersion[1];

			  for(Package p : repository)
			  {
				  if(p.getName().equals(constraintName) && p.getVersion().equals(version))
				  {
					  parsedConstraints.add(p);
				  }
			  }
		  }
		  else
		  {
			  //dont need this?
			  String latestVersion = "0";
			  String constraintName = constraint.substring(1);
			  if(constraint.charAt(0) == '+')
			  {
				  for(Package p : repository)
				  {
					  if(p.getName().equals(constraintName) && compareVersions(p.getVersion(), latestVersion) > 0)
					  {
						  latestVersion = p.getVersion();
					  }
				  }
				  for(Package p : repository)
				  {
					  if(p.getName().equals(constraintName) && p.getVersion().equals(latestVersion))
					  {
						  parsedConstraints.add(p);
					  }
				  }
			  }
		  }
	  }
	  return parsedConstraints;
  }

  static int compareVersions(String versionA, String versionB)
  {
	  //this definitely works for lexicographical versions.
	  //Positive return = A > B
	  //Negative return = B < A
	  //0 return = A == B
	  return versionA.compareTo(versionB);
  }

  static List<Package> getValidRepository(List<Package> parsedConstraints, List<Package> repository)
  {
	  List<Package> relevantPackages = new ArrayList<>();
	  List<Package> dependencyPackages = new ArrayList<>();
	  List<Package> conflictPackages = new ArrayList<>();
	  
	  //Using a queue + iteration to avoid using recursion.
	  Queue<Package> evaluationQueue = new LinkedList<>();
	  
	  for(Package p : parsedConstraints)
	  {
		  evaluationQueue.add(p);
	  }
	  
	  //can traverse all dependencies from top to bottom by queueing and dequeueing
	  //until queue is empty
	  while(!evaluationQueue.isEmpty())
	  {
		  Package p = evaluationQueue.remove();
		  dependencyPackages.add(p);
		  //System.out.printf("name: %s version %s\n", p.getName(), p.getVersion());
		  
		  for(List<String> dependencies : p.getDepends())
		  {
			  for(String dependency : dependencies)
			  {
				  for(Package pp : getPackageTest(dependency, repository))
				  {
					  if(!dependencyPackages.contains(pp))
					  {
						  evaluationQueue.add(pp);
					  }
				  }
			  }
		  }
	  }
	  
	  //opposite of dependencies, we only dequeue as we know all the packages we care about.
	  for(Package p : dependencyPackages)
	  {
		  evaluationQueue.add(p);
	  }
	  
	  //we only get conflicts for packages in the generated dependency list.
	  while(!evaluationQueue.isEmpty())
	  {
		  Package p = evaluationQueue.remove();
		  
		  for(String conflict : p.getConflicts())
		  {
			  for(Package pp : getPackageTest(conflict, repository))
			  {
				  conflictPackages.add(pp);
			  }
		  }
		  
	  }
	  
	  //easy way to clear duplicates from a List.
	  Set<Package> printTest = Sets.newHashSet(conflictPackages);
	  
	  for(Package p : printTest)
	  {
		  System.out.printf("CONF: package %s version %s\n", p.getName(), p.getVersion());
	  }
	  
	  //return needs to be changed to relevantPackages.
	  return dependencyPackages;
  }
  
  //converts a string name of package to a list of packages matched against it.
  static List<Package> getPackageTest(String packageString, List<Package> repository)
  {
	  List<Package> packages = new ArrayList<>();

	  if(packageString.contains("<="))
	  {
		  String[] nameVersion = packageString.split("<=");
		  String name = nameVersion[0];
		  String version = nameVersion[1];

		  for(Package p : repository)
		  {
			  if(p.getName().equals(name) && compareVersions(p.getVersion(), version) <= 0)
			  {
				  packages.add(p);
			  }
		  }
	  }
	  else if(packageString.contains(">="))
	  {
		  String[] nameVersion = packageString.split(">=");
		  String name = nameVersion[0];
		  String version = nameVersion[1];

		  for(Package p : repository)
		  {
			  if(p.getName().equals(name) && compareVersions(p.getVersion(), version) >= 0)
			  {
				  packages.add(p);
			  }
		  }
	  }
	  else if(packageString.contains("<"))
	  {
		  String[] nameVersion = packageString.split("<");
		  String name = nameVersion[0];
		  String version = nameVersion[1];

		  for(Package p : repository)
		  {
			  if(p.getName().equals(name) && compareVersions(p.getVersion(), version) < 0)
			  {
				  packages.add(p);
			  }
		  }
	  }
	  else if(packageString.contains(">"))
	  {
		  String[] nameVersion = packageString.split(">");
		  String name = nameVersion[0];
		  String version = nameVersion[1];

		  for(Package p : repository)
		  {
			  if(p.getName().equals(name) && compareVersions(p.getVersion(), version) > 0)
			  {
				  packages.add(p);
			  }
		  }
	  }
	  else if(packageString.contains("="))
	  {
		  String[] nameVersion = packageString.split("=");
		  String name = nameVersion[0];
		  String version = nameVersion[1];

		  for(Package p : repository)
		  {
			  if(p.getName().equals(name) && compareVersions(p.getVersion(), version) == 0)
			  {
				  packages.add(p);
			  }
		  }
	  }
	  else
	  {				
		  for(Package p : repository)
		  {
			  if(p.getName().equals(packageString))
			  {
				  packages.add(p);
			  }
		  }
	  }

	  return packages;
  }
  
  //need a pog method to determine safety of a removal.
}
