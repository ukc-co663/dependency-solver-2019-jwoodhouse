package depsolver;

import java.util.ArrayList;
import java.util.List;

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
	  
	  public String packageAsExpression()
	  {
		  return null;
	  }
	  
	}
