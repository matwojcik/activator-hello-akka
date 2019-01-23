
// Enable the Lightbend Telemetry (Cinnamon) sbt plugin
lazy val helloAkka = project in file(".") enablePlugins (Cinnamon)

// Add the Cinnamon Agent for run and test
cinnamon in run := true
cinnamon in test := true

name := "hello-akka"

version := "1.0"

scalaVersion := "2.12.4"

lazy val AkkaVersion = "2.5.14"
lazy val AkkaHttpVersion    = "10.1.1"
lazy val AlpakkaVersion     = "0.20"

libraryDependencies ++= Seq(
  // Use Coda Hale Metrics and Akka instrumentation
  Cinnamon.library.cinnamonCHMetrics,
  Cinnamon.library.cinnamonAkka,
  Cinnamon.library.cinnamonAkkaStream,
  Cinnamon.library.cinnamonAkkaHttp,
  Cinnamon.library.cinnamonSlf4jMdc,
  Cinnamon.library.cinnamonOpenTracing,
  Cinnamon.library.cinnamonOpenTracingZipkin,
  "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % AkkaVersion,
  "com.typesafe.akka"        %% "akka-http"       % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.lightbend.akka"  %% "akka-stream-alpakka-jms" % AlpakkaVersion,
  "org.apache.activemq" % "activemq-client"          % "5.14.5" exclude ("org.slf4j", "slf4j-api") exclude ("org.apache.geronimo.specs", "geronimo-jms_1.1_spec"),
  "org.scalatest" %% "scalatest" % "3.0.3" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.akka"          %% "akka-slf4j"              % AkkaVersion,
  "com.typesafe.scala-logging" %% "scala-logging"           % "3.5.0",
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

connectInput in run := true
