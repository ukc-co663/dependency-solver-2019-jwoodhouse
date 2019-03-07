package depsolver;


import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;


import java.util.ArrayList;
import java.util.List;


public class Package {
    private String name;
    private String version;
    private Integer size;
    private List<List<String>> depends = new ArrayList<>();
    private List<String> conflicts = new ArrayList<>();
    private List<List<Package>> dependsAsPackages = new ArrayList<>();
    private List<Package> conflictsAsPackages = new ArrayList<>();


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

    public List<List<Package>> getDependsAsPackages()
    {
        return dependsAsPackages;
    }

    public List<Package> getConflictsAsPackages()
    {
        return conflictsAsPackages;
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

    public void setDependsAsPackages(List<List<Package>> dependsAsPackages)
    {
        this.dependsAsPackages = dependsAsPackages;
    }

    public void setConflictsAsPackages(List<Package> conflictsAsPackages)
    {
        this.conflictsAsPackages = conflictsAsPackages;
    }

    public String toString()
    {
        return (this.getName() + this.getVersion().replace(".", ""));
    }

}
