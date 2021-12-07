package config

import com.typesafe.config.Config
import play.api.{ConfigLoader, Configuration}

case class SignConfig(name: String, address: Int, size: (Int, Int))

object SignConfig {
  implicit val configLoader: ConfigLoader[Seq[SignConfig]] = (rootConfig: Config, path: String) => {
    val config = Configuration(rootConfig).get[Seq[Configuration]](path)
    val signs = config.map(c => {
      val sizeSeq = c.get[Seq[Int]]("size")
      require(sizeSeq.length == 2)
      SignConfig(
        name = c.get[String]("name"),
        address = c.get[Int]("address"),
        size = (sizeSeq(0), sizeSeq(1))
      )
    })
    val signNames = signs.map(_.name)
    require(signNames.size == signNames.distinct.size)
    signs
  }
}
