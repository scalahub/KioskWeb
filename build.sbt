name := "KioskWeb"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "io.github.ergoplatform" %% "jde" % "0.1.0-SNAPSHOT",
  "io.github.scalahub" %% "auto_web" % "0.1.0-SNAPSHOT", // EasyWeb
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.3"
)

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "SonaType Snapshots s01" at "https://s01.oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

lazy val root = (project in file("."))
  .settings(
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    mainClass in (Compile, run) := Some("kiosk.CodeGen"),
    assemblyMergeStrategy in assembly := {
      case PathList("reference.conf")    => MergeStrategy.concat
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x                             => MergeStrategy.first
    }
  )

enablePlugins(JettyPlugin)
