package models.msc

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 14:07
  * Description: Модель-контейнер для данных об удалённой диагностированной проблеме, не обязательно ошибке.
  */
case class MScRemoteDiag(
  message : String,
  url     : Option[String],
)
