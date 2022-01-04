package config.hardware

import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
class SimulatedHardwareSettings @Inject() (config: Configuration) {
  private val conf = config.get[Configuration]("flipsys.hardware")
}
