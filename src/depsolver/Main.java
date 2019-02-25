package depsolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.ImmutableList;
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

	public String toString()
	{
		return (this.getName() + this.getVersion().replace(".", ""));
	}
}

class PackageImproved {
	  private String name;
	  private String version;
	  private Integer size;
	  private List<List<Package>> depends = new ArrayList<>();
	  private List<Package> conflicts = new ArrayList<>();
	  
	  public PackageImproved(String name, String version, Integer size, List<List<Package>> depends, List<Package> conflicts)
	  {
		  this.name = name;
		  this.version = version;
		  this.size = size;
		  this.depends = depends;
		  this.conflicts = conflicts;
	  }

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

	  public List<List<Package>> getDepends() 
	  {
		  return depends;
	  }

	  public List<Package> getConflicts() 
	  {
		  return conflicts;
	  }
	  
	  public String toString()
	  {
		  return (this.getName() + this.getVersion().replace(".", ""));
	  }
	}

public class Main {
  public static void main(String[] args) throws IOException 
  {
    TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
    
    List<Package> repository = JSON.parseObject(readFile(args[0]), repoType);
    
    TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
    
    List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
    List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);
    
    
    List<PackageImproved> improvedRepository = buildImprovedRepository(repository);
    List<List<Package>> positiveConstraints = getPackagesFromConstraints(constraints, '+', repository);
    List<List<Package>> negativeConstraints = getPackagesFromConstraints(constraints, '-', repository);
    List<Package> initialState = getPackagesFromInitialState(initial, repository);
    
    for(PackageImproved p : improvedRepository)
    {
    	System.out.printf("package %s version %s\n", p.getName(), p.getVersion());
    	
    	for(List<Package> deps : p.getDepends())
    	{
    		System.out.print("DEP: ");
    		for(Package dp : deps)
    		{
    			System.out.printf("[package %s version %s] ", dp.getName(), dp.getVersion());
    		}
    		System.out.print("\n");
    	}
    	
    	for(Package conf : p.getConflicts())
    	{
    		System.out.printf("CONF: [package %s version %s]\n", conf.getName(), conf.getVersion());
    	}
    	System.out.print("\n");
    }
    
    for(List<Package> pc : positiveConstraints)
    {
    	System.out.print("+CONSTRAINT: ");
    	for(Package ppc : pc)
    	{
    		System.out.printf("[package %s version %s]", ppc.getName(), ppc.getVersion());
    	}
    	System.out.print("\n");
    	
    }
    
    for(List<Package> nc : negativeConstraints)
    {
    	System.out.print("-CONSTRAINT: ");
    	for(Package nnc : nc)
    	{
    		System.out.printf("[package %s version %s]", nnc.getName(), nnc.getVersion());
    	}
    	System.out.print("\n");
    }
    
    for(Package ic : initialState)
    {
    	System.out.printf("INSTALLED: package %s version %s\n", ic.getName(), ic.getVersion());
    }
    
    final FormulaFactory f = new FormulaFactory();
	final PropositionalParser p = new PropositionalParser(f);
	Formula formula = null;
	
	List<String> listOfExpressions = new ArrayList<>();
	
	
	
	
	
	try {
		formula = p.parse("(X11 & ((X21) | (X31) | (X41 & (X11 | X31 | (X51)))) & ((X21) | (X40 & ~X41) | (X50 & ~X51)) & ((X30 & ~X31) | (X40 & ~X41) | (X50 & ~X51)) & ((X20 & ~X21) | (X40 & ~X41) | (X50 & ~X51))) & X11 & X20 & X30 & X41 & X50");
	} catch (ParserException e) {
		e.printStackTrace();
	}
		
	final SATSolver miniSat = MiniSat.miniSat(f);
	miniSat.add(formula);
	//final Tristate result = miniSat.sat();
	List<Assignment> pog = miniSat.enumerateAllModels();
	
	List<List<PackageImproved>> pogu = getPackagesFromModel(pog, improvedRepository);

	//for(List<PackageImproved> lp : pogu)
	//{
	//	System.out.print("SET: ");
	//	for(PackageImproved pp : lp)
	//	{
	//		System.out.printf("[package %s version %s] ", pp.getName(), pp.getVersion());
	//	}
	//	System.out.print("\n");
	//}
	
	System.out.println(repositoryToBooleanExp(improvedRepository, improvedRepository.get(0)));
  }

  static String readFile(String filename) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    StringBuilder sb = new StringBuilder();
    br.lines().forEach(line -> sb.append(line));
    return sb.toString();
  }
  
  static String repositoryToBooleanExp(List<PackageImproved> improvedRepository, PackageImproved packageToInstall)
  {
	  String booleanExp = packageToBooleanExp(packageToInstall);
	  
	  for(PackageImproved p : improvedRepository)
	  {
		  if(!p.toString().equals(packageToInstall.toString()))
		  {
			  booleanExp = booleanExp.replace("[packageIdent]" + p.toString() + "[packageIdent]", packageToBooleanExp(p));
		  }
	  }
	  return booleanExp.replace("[packageIdent]", "");
  }
  
  static String repositoryToBooleanExp2(List<PackageImproved> improvedRepository, List<List<Package>> positiveConstraints, List<List<Package>> negativeConstraints)
  {
	  String booleanExp = "";
	  
	  
	  
	  return null;
  }
  
  static String packageToBooleanExp(PackageImproved p)
  {
	  String booleanExp = "(" + "[packageIdent]" + p.toString() + "[packageIdent]";

	  for(List<Package> deps : p.getDepends())
	  {
		  booleanExp = booleanExp + " & (";
		  for(Package dp : deps)
		  {
			  booleanExp = booleanExp + "[packageIdent]" + dp.toString() + "[packageIdent]" + " | ";
		  }
		  booleanExp = booleanExp.substring(0, booleanExp.length() -3) + ")";
	  }

	  for(Package conf : p.getConflicts())
	  {
		  booleanExp = booleanExp + " & ~" + "[packageIdent]" + conf.toString() + "[packageIdent]";
	  }
	  	  
	  return booleanExp + ")";
  }
  
  static List<PackageImproved> buildImprovedRepository(List<Package> repository)
  {
	  List<PackageImproved> improvedRepository = new ArrayList<>();
	  for (Package p : repository)
	  {
		  String name = p.getName();
		  String version = p.getVersion();
		  Integer size = p.getSize();
		  List<List<Package>> depends = new ArrayList<>();
		  List<Package> conflicts = new ArrayList<>();
		  
		  for(List<String> deps : p.getDepends())
		  {
			  List<Package> orDepends = new ArrayList<>();
			  
			  for(String singleDep : deps)
			  {
				  List<Package> strToPackages = getPackagesFromString(singleDep, repository);
				  for(Package dp : strToPackages)
				  {
					  orDepends.add(dp);
				  }
				  
			  }
			  depends.add(orDepends);
		  }
		  
		  for(String conf : p.getConflicts())
		  {
			  List<Package> strToPackages = getPackagesFromString(conf, repository);
			  for(Package cp : strToPackages)
			  {
				  conflicts.add(cp);
			  }
		  }
		  
		  PackageImproved pi = new PackageImproved(name, version, size, depends, conflicts);
		  improvedRepository.add(pi);
	  }
	  return improvedRepository;
  }
  
  static List<List<Package>> getPackagesFromConstraints(List<String> constraints, Character constraintType, List<Package> repository)
  {
	  List<List<Package>> actionConstraints = new ArrayList<>();
	  
	  for(String constraint : constraints)
	  {
		  if(constraint.charAt(0) == constraintType)
		  {
			  List<Package> subConstraint = getPackagesFromString(constraint.substring(1), repository);
			 actionConstraints.add(subConstraint);
		  }
	  }
	  return actionConstraints;
  }
  
  static List<Package> getPackagesFromInitialState(List<String> initial, List<Package> repository)
  {
	  List<Package> initialState = new ArrayList<>();
	  for(String init : initial)
	  {
		  initialState.add(getPackagesFromString(init, repository).get(0));
	  }
	  return initialState;
  }

  static int compareVersions(String versionA, String versionB)
  {
	  //this definitely works for lexicographical versions.
	  //Positive return = A > B
	  //Negative return = B < A
	  //0 return = A == B
	  return versionA.compareTo(versionB);
  }
  
  static List<List<PackageImproved>> getPackagesFromModel(List<Assignment> satModel, List<PackageImproved> improvedRepository)
  {
	  List<List<PackageImproved>> listOfStates = new ArrayList<>();
	  
	  for(Assignment a : satModel)
	  {
		  List<PackageImproved> state = new ArrayList<>();
		  for (Variable v : a.positiveLiterals())
		  {
			for(PackageImproved p : improvedRepository)
			{
				if(p.toString().equals(v.toString()))
				{
					state.add(p);
				}
			}
		  }
		  listOfStates.add(state);
	  }
	  return listOfStates;
  }
  
  //converts a string name of package to a list of packages matched against it.
  static List<Package> getPackagesFromString(String packageString, List<Package> repository)
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
}
