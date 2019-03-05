rm -Recurse -Force lib/*
New-Item lib -ItemType Directory
Invoke-WebRequest http://search.maven.org/remotecontent?filepath=com/alibaba/fastjson/1.2.45/fastjson-1.2.45.jar -OutFile lib/fastjson-1.2.45.jar
Invoke-WebRequest http://central.maven.org/maven2/org/logicng/logicng/1.4.1/logicng-1.4.1.jar -OutFile lib/logicng-1.4.1.jar
Invoke-WebRequest http://central.maven.org/maven2/org/antlr/antlr4-runtime/4.7.1/antlr4-runtime-4.7.1.jar -OutFile lib/antlr4-runtime-4.7.1.jar
Invoke-WebRequest http://central.maven.org/maven2/org/jgrapht/jgrapht-core/1.3.0/jgrapht-core-1.3.0.jar -OutFile lib/jgrapht-core-1.3.0.jar
Invoke-WebRequest http://central.maven.org/maven2/org/jheaps/jheaps/0.9/jheaps-0.9.jar -OutFile lib/jheaps-0.9.jar
Invoke-WebRequest http://central.maven.org/maven2/com/google/guava/guava/27.0.1-jre/guava-27.0.1-jre.jar -OutFile lib/guava-27.0.1-jre.jar