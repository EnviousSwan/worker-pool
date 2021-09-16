scalaVersion := "2.13.6"

organization         := "com.pirum"
organizationName     := "Pirum Systems"
organizationHomepage := Some(url("https://www.pirum.com"))

scalacOptions += "-Xsource:3"

libraryDependencies ++= Seq(
  "dev.optics"        %% "monocle-macro"            % "3.1.0",
  "com.typesafe.akka" %% "akka-actor-typed"         % "2.6.16",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.16" % Test,
  "ch.qos.logback"     % "logback-classic"          % "1.2.6",
  "org.scalatest"     %% "scalatest"                % "3.2.9"  % Test
)
