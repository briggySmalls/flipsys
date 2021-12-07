package config

import com.typesafe.config.Config
import play.api.{ConfigLoader, Configuration}

case class ApplicationSettings(signs: Seq[SignConfig])

object ApplicationSettings {
  implicit val configLoader: ConfigLoader[ApplicationSettings] = (rootConfig: Config, path: String) => {
    val config = Configuration(rootConfig).get[Configuration](path)
    ApplicationSettings(
      signs = config.get[Seq[SignConfig]]("signs")
    )
  }
}