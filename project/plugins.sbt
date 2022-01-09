resolvers += Resolver.bintrayIvyRepo("rallyhealth", "sbt-plugins")

addSbtPlugin("org.scalameta"             % "sbt-scalafmt"       % "2.4.6")
addSbtPlugin("com.typesafe.sbt"          % "sbt-git"            % "1.0.2")
addSbtPlugin("com.rallyhealth.sbt"       % "sbt-git-versioning" % "1.6.0")
addSbtPlugin("org.xerial.sbt"            % "sbt-sonatype"       % "3.9.10")
addSbtPlugin("com.github.sbt"            % "sbt-pgp"            % "2.1.2")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"       % "0.1.20")
