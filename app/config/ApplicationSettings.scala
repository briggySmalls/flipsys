package config

import com.typesafe.config.Config
import play.api.{ConfigLoader, Configuration}

import javax.inject.{Inject, Singleton}

@Singleton
class ApplicationSettings @Inject() (config: Configuration) {
  private val conf = config.get[Configuration]("flipsys")

  val signs = conf.get[Seq[SignConfig]]("signs")
  val port = config.get[String]("port")
  val stubPort = config.getOptional[Boolean]("stub-port").getOrElse(false)
}
