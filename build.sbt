name := "Introduction to Stream Oriented Programming in Scala with FS2"

scalaVersion := "2.12.8"

lazy val root = project.in(file("."))  // your existing library

lazy val docs = project
  .in(file("mdocproject"))
  .dependsOn(root)
  .enablePlugins(MdocPlugin)
  .settings(Seq(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.6.0",
      "org.typelevel" %% "cats-effect" % "1.2.0",
      "co.fs2" %% "fs2-core" % "1.0.4",
      "co.fs2" %% "fs2-io" % "1.0.4",
    ),
  ))