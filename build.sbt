name := "KioskWeb"

scalaVersion := "2.12.10"

// resolvers += Resolver.mavenLocal

lazy val root = (project in file("."))
  .settings(
    resolvers ++= Seq(
      "Sonatype Releases" at "https://s01.oss.sonatype.org/content/repositories/releases",
      "Sonatype Releases 2" at "https://oss.sonatype.org/content/repositories/releases/",
      "SonaType" at "https://oss.sonatype.org/content/groups/public",
      "SonaType Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/",
      "SonaType Staging" at "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
    ),
    libraryDependencies ++= Seq(
      "io.github.ergoplatform" %% "jde" % "1.0",
      "io.github.scalahub" %% "easyweb" % "1.1",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.3"
    ),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    mainClass in (Compile, run) := Some("kiosk.CodeGen"),
    assemblyMergeStrategy in assembly := {
      case PathList("reference.conf")    => MergeStrategy.concat
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x                             => MergeStrategy.first
    }
  )

enablePlugins(JettyPlugin)
