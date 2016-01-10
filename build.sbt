name := """animals-play-scala"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  evolutions,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "ws.securesocial" %% "securesocial" % "3.0-M4",
  "com.typesafe.play" %% "play-mailer" % "3.0.0-M1"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)
resolvers += Resolver.sonatypeRepo("releases")

coverageExcludedPackages := "<empty>;router;.*javascript.*;views.html;"

