name := "hystrix-play"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.netflix.archaius" % "archaius-scala" % "0.4.1",
  "com.netflix.hystrix" % "hystrix-core" % "1.4.0-RC4",
  "com.netflix.hystrix" % "hystrix-metrics-event-stream" % "1.4.0-RC4",
  "com.netflix.rxjava"  % "rxjava-scala" % "0.18.2"
)

play.Project.playScalaSettings
