name := "hystrix-play"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  "com.netflix.archaius" % "archaius-scala" % "0.7.4",
  "com.netflix.hystrix" % "hystrix-core" % "1.5.3",
  "com.netflix.hystrix" % "hystrix-metrics-event-stream" % "1.5.3",
  "com.netflix.rxjava"  % "rxjava-scala" % "0.18.2"
)
