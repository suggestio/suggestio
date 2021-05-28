package io.suggest.es.model

import io.suggest.util.logs.MacroLogsDyn
import play.api.{Configuration, Environment, Mode}
import play.api.inject.Injector

import javax.inject.Inject


/** Some configuration constants for ES Models-related stuff. */
final class EsModelConfig @Inject() (
                                      injector: Injector,
                                    )
  extends MacroLogsDyn
{

  private def configuration = injector.instanceOf[Configuration]
  private def environment = injector.instanceOf[Environment]

  /** Default replicas count for newly created indices. */
  def ES_INDEX_REPLICAS_COUNT: Int = {
    val configKey = "es.index.replicas.count"
    configuration
      .getOptional[Int]( configKey )
      .getOrElse {
        val defaultReplicas = environment.mode match {
          case Mode.Prod =>
            1
          case _ =>
            0
        }
        LOGGER.warn(s"$configKey not defined in config. New indexes are created with $defaultReplicas replicas count.")
        defaultReplicas
      }
  }

}
