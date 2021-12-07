package modules

import com.google.inject.AbstractModule
import services.ApplicationService

class InitializeAppModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ApplicationService]).asEagerSingleton()
  }
}
