package depsolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
    
    final FormulaFactory f = new FormulaFactory();
	final PropositionalParser p = new PropositionalParser(f);
	Formula formula = null;
	
	String booleanExp = repositoryToBooleanExp2(improvedRepository, positiveConstraints, negativeConstraints);
	
	try {
		formula = p.parse(booleanExp);
	} catch (ParserException e) {
		e.printStackTrace();
	}
		
	final SATSolver miniSat = MiniSat.miniSat(f);
	miniSat.add(formula);
	List<Assignment> allValidStates = miniSat.enumerateAllModels();

	List<List<PackageImproved>> allValidStatesAsPackages = getPackagesFromModel(allValidStates, improvedRepository);
	
	List<PackageListScored> packageListScored = new ArrayList<>();
	
	for(List<PackageImproved> validState : allValidStatesAsPackages)
	{
		packageListScored.add(new PackageListScored(validState));
	}
	addUninstallationsToScoredPackageLists(packageListScored, initialState);
	printInstallationOrder(packageListScored);
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
	  List<String[]> testConstraints = new ArrayList<>();
	  
	  for(List<Package> pc : positiveConstraints)
	  {
		  List<String> newArr = new ArrayList<>();
		  for(Package ppc : pc)
		  {
			  newArr.add("[packageIdent]" + ppc.toString() + "[packageIdent]");
		  }
		  
		  String[] newArr2 = new String[newArr.size()];
		  newArr2 = newArr.toArray(newArr2);
		  testConstraints.add(newArr2);
	  }
	  
	  for(List<Package> nc : negativeConstraints)
	  {
		  List<String> newArr = new ArrayList<>();
		  for(Package nnc : nc)
		  {
			  newArr.add("~" + "[packageIdent]" + nnc.toString() + "[packageIdent]");
		  }
		  String[] newArr2 = new String[newArr.size()];
		  newArr2 = newArr.toArray(newArr2);
		  testConstraints.add(newArr2);
	  }
	  
	  List<ImmutableList<String>> immutableElements = makeListofImmutable(testConstraints);
	  List<List<String>> cartesianProduct = Lists.cartesianProduct(immutableElements);
	  //System.out.println(cartesianProduct);
	  
	  String booleanExp = "(";
	  
	  for(List<String> cp : cartesianProduct)
	  {
		  booleanExp = booleanExp + "(";
		  for(String scp : cp)
		  {
			  booleanExp = booleanExp + scp + " & ";
		  }
		  booleanExp = booleanExp.substring(0, booleanExp.length() - 3) + ") | ";
	  }
	  
	  booleanExp = booleanExp.substring(0, booleanExp.length() - 3) + ")";
	  for(PackageImproved p : improvedRepository)
	  {
		  {
			  booleanExp = booleanExp.replace("[packageIdent]" + p.toString() + "[packageIdent]", packageToBooleanExp(p));
		  }
	  }
	  //System.out.println(booleanExp.replace("[packageIdent]", ""));
	  return booleanExp.replace("[packageIdent]", "");
  }
  
  //LINK: https://stackoverflow.com/a/37490796
  //AUTHOR: https://stackoverflow.com/users/433814/marcello-de-sales
  //DATE: May 27 2016
  private static List<ImmutableList<String>> makeListofImmutable(List<String[]> values) {
	  List<ImmutableList<String>> converted = new LinkedList<>();
	  values.forEach(array -> {
	    converted.add(ImmutableList.copyOf(array));
	  });
	  return converted;
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
  
  static void printInstallationOrder(List<PackageListScored> scoredPackageList)
  {
	  Collections.sort(scoredPackageList, (s1, s2) -> s1.getScore()-s2.getScore());
	  
	  
	  
	  Integer index = 0;
	  Boolean success = false;
	  while(!success)
	  {
		  String output = scoredPackageList.get(index).TopologicalSort();
		  
		  if(!output.equals("Cyclic dependency"))
		  {
			  System.out.println(output);
			  //System.out.println(scoredPackageList.get(index).getScore());
			  success = true;
		  }
		  index++;
	  }
  }
  
  static void addUninstallationsToScoredPackageLists(List<PackageListScored> scoredPackageList, List<Package> initialState)
  {
	  for(Package p : initialState)
	  {
		  for(PackageListScored scoredPackage : scoredPackageList)
		  {
			  for(PackageImproved validPackageState : scoredPackage.getPackageList())
			  {
				  for(Package conflict : validPackageState.getConflicts())
				  {
					  if(p.toString().equals(conflict.toString()))
					  {
						  scoredPackage.addPackageToUninstall(conflict);
					  }
				  }
			  }
		  }
	  }
  }
}
