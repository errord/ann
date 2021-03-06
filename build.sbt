lazy val root = (project in file(".")).
  settings(
    name := "anna",
    version := "1.0",
    scalaVersion := "2.11.8"
  )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.3",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.typesafe" % "config" % "1.3.0",
  "junit" % "junit" % "4.11" % "test",
  "com.novocode" % "junit-interface" % "0.10" % "test",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "org.json4s" %% "json4s-native" % "3.3.0"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

initialCommands in console :=
  """
    |import anna.Commands._
    |import anna.logger.LOG
    |import anna.Context
    |import anna.epengine.Probability
    |import anna.epengine.Probability._
    |import anna.utils.DoubleRange
    |import anna.utils.DoubleRange._
    |import anna.utils.RandomNumber
    |import anna.async.NeuronCounter
    |import anna.epengine.MutationsProfile
    |import anna.epengine.MutationsProfile._
    |LOG.addLogToStdout()
    |setDotLineConfig()
  """.stripMargin