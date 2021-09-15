sys.props.get("sbt.gitlab.version") match {
  case Some(ver) => addSbtPlugin("nl.zolotko.sbt" % "sbt-gitlab" % ver)
  case _ =>
    sys.error(
      """|The system property 'sbt.gitlab.version' is not defined.
                                 |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    )
}
