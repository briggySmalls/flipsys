package config.hardware

import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
class SimulatedHardwareSettings @Inject() (config: Configuration) {
  config.get[Configuration]("flipsys.hardware")
}
