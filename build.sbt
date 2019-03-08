name := "Introduction to Stream Oriented Programming in Scala with FS2"

scalaVersion := "2.12.8"

lazy val root = project.in(file("."))  // your existing library

lazy val docs = project
  .in(file("mdocproject"))
  .dependsOn(root)
  .enablePlugins(MdocPlugin)
  .settings(Seq(
  ))