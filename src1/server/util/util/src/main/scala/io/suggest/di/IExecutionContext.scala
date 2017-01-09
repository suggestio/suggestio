package io.suggest.di

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 10:36
 * Description: Интерфейс для DI-инжектируемого поля execution context.
 */
trait IExecutionContext {
  implicit def ec: ExecutionContext
}
