package config

import com.google.inject.AbstractModule
import services.RoutingConfig

class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[AppConfig]).asEagerSingleton()
    bind(classOf[RoutingConfig]).asEagerSingleton()
  }

}
