package modules

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment, Logging}
import services.ApplicationService
import services.hardware.{
  HardwareLayer,
  RealHardwareImpl,
  SimulatedHardwareImpl
}

/** Initialize guice's dynamic DI Access the play configuration following the
  * documented pattern
  * https://www.playframework.com/documentation/2.8.x/ScalaDependencyInjection#Configurable-bindings
  *
  * @param environment
  *   Something something
  * @param configuration
  *   Play configuration
  */
class InitializeAppModule(
    environment: Environment,
    configuration: Configuration
) extends AbstractModule
    with Logging {
  override def configure(): Unit = {
    // Determine appropriate hardware abstraction
    configuration.get[String]("flipsys.hardware.type") match {
      case "real" =>
        logger.info("real hardware")
        bind(classOf[HardwareLayer]).to(classOf[RealHardwareImpl])
      case "simulated" =>
        logger.info("simulated hardware")
        bind(classOf[HardwareLayer]).to(classOf[SimulatedHardwareImpl])
    }
    // Start the application as soon as play starts
    bind(classOf[ApplicationService]).asEagerSingleton()
  }
}
