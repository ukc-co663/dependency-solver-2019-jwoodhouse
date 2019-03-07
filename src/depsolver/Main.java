package depsolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.jgrapht.graph.DefaultEdge;
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

@SuppressWarnings("StringConcatenationInLoop")
public class Main {
    public static void main(String[] args) throws IOException
    {
        TypeReference<List<Package>> repoType = new TypeReference<>() {};

        List<Package> repository = JSON.parseObject(readFile(args[0]), repoType);

        TypeReference<List<String>> strListType = new TypeReference<>() {};

        List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
        List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);


        populateRepository(repository);
        List<List<Package>> positiveConstraints = getPackagesFromConstraints(constraints, '+', repository);
        List<List<Package>> negativeConstraints = getPackagesFromConstraints(constraints, '-', repository);
        List<Package> initialState = getPackagesFromInitialState(initial, repository);

        final FormulaFactory f = new FormulaFactory();
        final PropositionalParser p = new PropositionalParser(f);
        Formula formula = null;

        String booleanExp = repositoryToBooleanExp2(repository, positiveConstraints, negativeConstraints);
        System.out.println("BOOLEAN EXP: " + booleanExp);
        try {
            formula = p.parse(booleanExp);
        } catch (ParserException e) {
            e.printStackTrace();
        }

        final SATSolver miniSat = MiniSat.miniSat(f);
        miniSat.add(formula);
        List<Assignment> allValidStates = miniSat.enumerateAllModels();

        List<List<Package>> allValidStatesAsPackages = getPackagesFromModel(allValidStates, repository);

        List<PackageListScored> packageListScored = new ArrayList<>();

        for(List<Package> validState : allValidStatesAsPackages)
        {
            packageListScored.add(new PackageListScored(validState));
        }
        addUninstallationsToScoredPackageLists(packageListScored, initialState);
        addKeepsToScoredPackageLists(packageListScored, initialState);

        for(PackageListScored ppp : packageListScored)
        {
            System.out.println(ppp.getPackageList());
        }

        printInstallationOrder(packageListScored);

    }

    static String readFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        br.lines().forEach(sb::append);
        return sb.toString();
    }

    static String repositoryToBooleanExp(List<Package> repository, Package packageToInstall)
    {
        String booleanExp = packageToBooleanExp(packageToInstall);

        for(Package p : repository)
        {
            if(!p.toString().equals(packageToInstall.toString()))
            {
                booleanExp = booleanExp.replace("[packageIdent]" + p.toString() + "[packageIdent]", packageToBooleanExp(p));
            }
        }
        return booleanExp.replace("[packageIdent]", "");
    }

    static String repositoryToBooleanExp3(List<Package> repository, List<List<Package>> positiveConstraints, List<List<Package>> negativeConstraints)
    {
        return null;
    }

    static String repositoryToBooleanExp2(List<Package> repository, List<List<Package>> positiveConstraints, List<List<Package>> negativeConstraints)
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

        String[] arrTest = booleanExp.split("\\[packageIdent]");

        Queue<Package> queueTest = new LinkedList<>();
        List<Package> visited = new ArrayList<>();
        for(String sp : arrTest)
        {
            for(Package pp :repository)
            {
                if(pp.toString().equals(sp))
                {
                    queueTest.add(pp);
                }
            }
        }

        while(!queueTest.isEmpty())
        {
            Package currentPackage = queueTest.remove();
            visited.add(currentPackage);
            booleanExp = booleanExp.replace("[packageIdent]" + currentPackage.toString() + "[packageIdent]", packageToBooleanExp(currentPackage));
            String[] arrTest2 = booleanExp.split("\\[packageIdent]");
            for(String sp : arrTest2)
            {
                for(Package pp :repository)
                {
                    if(pp.toString().equals(sp) && !visited.contains(pp) && !queueTest.contains(pp))
                    {
                        queueTest.add(pp);
                    }
                }
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
        values.forEach(array -> converted.add(ImmutableList.copyOf(array)));
        return converted;
    }

    static String packageToBooleanExp(Package p)
    {
        String booleanExp = "(" + "[packageIdent]" + p.toString() + "[packageIdent]";

        for(List<Package> deps : p.getDependsAsPackages())
        {
            booleanExp = booleanExp + " & (";
            for(Package dp : deps)
            {
                booleanExp = booleanExp + "[packageIdent]" + dp.toString() + "[packageIdent]" + " | ";
            }
            booleanExp = booleanExp.substring(0, booleanExp.length() -3) + ")";
        }

        for(Package conf : p.getConflictsAsPackages())
        {
            //noinspection StringConcatenationInLoop
            booleanExp = booleanExp + " & ~" + "[packageIdent]" + conf.toString() + "[packageIdent]";
        }

        return booleanExp + ")";
    }

    static void populateRepository(List<Package> repository)
    {
        for(Package p : repository)
        {
            List<List<Package>> dependsAsPackages = new ArrayList<>();
            List<Package> conflictsAsPackages = new ArrayList<>();

            for(List<String> dependencySetsString : p.getDepends())
            {
                List<Package> dependencySetAsPackageList = new ArrayList<>();
                for(String singleDependencyString : dependencySetsString)
                {
                    List<Package> dependencyStringAsPackages = getPackagesFromString(singleDependencyString, repository);
                    dependencySetAsPackageList.addAll(dependencyStringAsPackages);
                }
                dependsAsPackages.add(dependencySetAsPackageList);
            }

            for(String conflictAsString : p.getConflicts())
            {
                List<Package> conflictStringAsPackages = getPackagesFromString(conflictAsString, repository);
                conflictsAsPackages.addAll(conflictStringAsPackages);
            }

            p.setDependsAsPackages(dependsAsPackages);
            p.setConflictsAsPackages(conflictsAsPackages);
        }
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

        String[] arrVersionA = versionA.split("\\.");
        String[] arrVersionB = versionB.split("\\.");

        Integer[] versionAAsInt = new Integer[arrVersionA.length];
        Integer[] versionBAsInt = new Integer[arrVersionB.length];

        for(int i = 0; i < arrVersionA.length; i++)
        {
            versionAAsInt[i] = Integer.parseInt(arrVersionA[i]);
        }

        for(int i = 0; i < arrVersionB.length; i++)
        {
            versionBAsInt[i] = Integer.parseInt(arrVersionB[i]);
        }

        versionA = "";
        for (Integer integer : versionAAsInt)
        {
            versionA = versionA + integer + ".";
        }
        versionA = versionA.substring(0, versionA.length() - 1);

        versionB = "";
        for (Integer integer : versionBAsInt)
        {
            versionB = versionB + integer + ".";
        }
        versionB = versionB.substring(0, versionB.length() - 1);

        return versionA.compareTo(versionB);
    }

    static List<List<Package>> getPackagesFromModel(List<Assignment> satModel, List<Package> repository)
    {
        List<List<Package>> listOfStates = new ArrayList<>();

        for(Assignment a : satModel)
        {
            List<Package> state = new ArrayList<>();
            for (Variable v : a.positiveLiterals())
            {
                for(Package p : repository)
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
        scoredPackageList.sort(Comparator.comparingInt(PackageListScored::getScore));

        Integer index = 0;
        Boolean success = false;

        while(!success)
        {
            String output = scoredPackageList.get(index).topologicalSortAsJSON();

            if(!output.equals("Cyclic dependency"))
            {
                System.out.println(output);
                System.out.println(scoredPackageList.get(index).getScore());
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
                for(Package validPackageState : scoredPackage.getPackageList())
                {
                    for(Package conflict : validPackageState.getConflictsAsPackages())
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

    static void addKeepsToScoredPackageLists(List<PackageListScored> scoredPackageList, List<Package> initialState)
    {
        for(Package p : initialState)
        {
            for(PackageListScored scoredPackage : scoredPackageList)
            {
                for(Package validPackageState : scoredPackage.getPackageList())
                {
                    for(List<Package> dependencies : validPackageState.getDependsAsPackages())
                    {
                        for(Package dependency : dependencies)
                        {
                            if(p.toString().equals(dependency.toString()))
                            {
                                scoredPackage.addPackageToKeep(dependency);
                            }
                        }
                    }
                }
            }
        }
    }
}
