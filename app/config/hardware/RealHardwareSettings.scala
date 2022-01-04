package config.hardware

import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
class RealHardwareSettings @Inject() (config: Configuration) {
  private val conf = config.get[Configuration]("flipsys.hardware")

  val port: String = conf.get[String]("port")
}
