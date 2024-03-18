ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.0"

lazy val root = (project in file("."))
  .settings(
    name := "game",
    libraryDependencies ++= Seq(
       "org.typelevel" %% "kittens" % "3.3.0",
       "org.typelevel" %% "cats-core" % "2.10.0",
       "org.typelevel" %% "cats-effect" % "3.5.3"
    )
  )