package models.req

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 14:10
  * Description: Модель данных по remote-адресу клиента: некие расширенные данные на тему ip.
  */
trait IRemoteAddrInfo {

  /** Нормализованное строковое представление удалённого адреса. */
  def remoteAddr: String

  /** Клиент локальный? None если определить это невозможно по какой-то причине. */
  def isLocal: Option[Boolean]

}


/** Дефолтовая реализация модели [[IRemoteAddrInfo]]. */
case class MRemoteAddrInfo(
  override val remoteAddr  : String,
  override val isLocal     : Option[Boolean]
)
  extends IRemoteAddrInfo
