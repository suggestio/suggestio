package models.mext

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 13:41
  * Description: Модель данных по посту (сообщению, твиту) на внешнем сервисе.
  */

/** Абстрактные метаданные по посту на внешнем сервисе. */
trait IExtPostInfo {

  def id: String

  def url: String

  override def toString: String = {
    getClass.getSimpleName + "(" + id + "," + url + ")"
  }

}


/** Инфа по одному твиту. Потом наверное будет вынесена в отдельный файл модели. */
case class MExtPostInfo(override val id: String) extends IExtPostInfo {
  override def url: String = {
    "https://twitter.com/"
  } // TODO Надо что-то типа https://twitter.com/Flickr/status/423511451970445312
}
