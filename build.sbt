
name := "tproll"

version := "0.1-SNAPSHOT"

organization := "com.darkyen"

autoScalaLibrary := false

crossPaths := false

val slf4jVersion = "1.7.22"

libraryDependencies += "org.slf4j" % "slf4j-api" % slf4jVersion % "provided"


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
