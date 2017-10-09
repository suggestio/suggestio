package io.suggest.ctx

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 15:00
  * Description: Интерфейс для опционального сериализованного ctxId.
  */
trait ICtxIdStrOpt {

  def ctxIdOpt: Option[String]

}
