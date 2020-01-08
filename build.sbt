name := "Introduction to Stream Oriented Programming in Scala with FS2"

scalaVersion in ThisBuild := "2.13.1"

lazy val root = project.in(file("."))  // your existing library

lazy val docs = project
  .in(file("mdocproject"))
  .dependsOn(root)
  .enablePlugins(MdocPlugin)
  .settings(Seq(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.1.0",
      "org.typelevel" %% "cats-effect" % "2.0.0",
      "co.fs2" %% "fs2-core" % "2.1.0",
      "co.fs2" %% "fs2-io" % "2.1.0",
    ),
  ))