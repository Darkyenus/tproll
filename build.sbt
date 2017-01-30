
name := "tproll"

version := "1.2.1"

organization := "com.darkyen"

autoScalaLibrary := false

crossPaths := false

val slf4jVersion = "1.7.22"

libraryDependencies += "org.slf4j" % "slf4j-api" % slf4jVersion % "provided"

libraryDependencies += "org.slf4j" % "slf4j-api" % slf4jVersion % "test"

libraryDependencies += "junit" % "junit" % "4.12" % "test"

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

//Integrations
libraryDependencies += "com.esotericsoftware.minlog" % "minlog" % "1.2" % "provided"


javacOptions in (Compile, compile) ++= Seq(
  "-Xlint",
  "-encoding", "UTF-8",
  "-source", "1.8",
  "-target", "1.8",
  "-g",
  "-Xdiags:verbose"
)

javacOptions in Compile ++= Seq( //For javadoc
  "-encoding", "UTF-8",
  "-source", "1.8",
  "-Xdoclint:-missing"
)

fork in Test := true //Needed because tests use static TPLogger fields with different settings
