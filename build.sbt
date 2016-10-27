name := "hystrix-play"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  "com.netflix.archaius" % "archaius-scala" % "0.7.5",
  "com.netflix.hystrix" % "hystrix-core" % "1.5.6",
  "com.netflix.hystrix" % "hystrix-metrics-event-stream" % "1.5.6",
  "io.reactivex" % "rxscala_2.11" % "0.26.3",
  "io.reactivex" % "rxjava" % "1.2.1",
  "io.reactivex" % "rxjava-reactive-streams" % "1.2.0"
)