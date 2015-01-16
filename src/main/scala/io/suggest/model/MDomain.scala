package io.suggest.model

import scala.concurrent.Future
import org.joda.time.DateTime

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.13 11:52
 * Description: hbase-модель для domain-данных. Таблица включает в себя инфу по доменам, выраженную props CF и в др.моделях.
 */

/**
 * Класс записи этой модели.
 * @param dkey ключ домена.
 * @param addedBy Описание того, кто добавил домен в индексацию.
 * @param addedAt Дата добавления в базу.
 */
case class MDomain(
  dkey: String,
  addedBy: String,
  addedAt: DateTime = DateTime.now
) {
  import MDomain._

  /**
   * Сохранить текущую запись в хранилище. Если такая уже существует, то она будет перезаписана.
   * @return Фьючерс с сохраненным экземпляром записи.
   */
  def save = BACKEND.save(this)

  def delete = MDomain.delete(dkey)
}


object MDomain {

  // 2015.jan.16 d555039065a2: удалён единственный оставшийся hbase backend
  val BACKEND: Backend = null

  /**
   * Прочитать экземпляр модели из хранилища по ключу.
   * @param dkey Ключ домена.
   * @return Фьючерс с опциональным экземпляром класса этой модели.
   */
  def getForDkey(dkey: String) = BACKEND.getForDkey(dkey)

  /**
   * Удалить из хранилища указанный домен.
   * @param dkey Ключ домена.
   * @return Фьючерс с неопределенными данными. Чисто для синхронизации.
   */
  def delete(dkey: String) = BACKEND.delete(dkey)


  /**
   * Выдать все домены.
   * @return Фьючерс со списком записей в неопределенном порядке.
   */
  def getAll = BACKEND.getAll


  /**
   * Выдать несколько доменов из общей кучи.
   * HBase-функция оптимизирована под продакшен и максимально эффективное потребление ресурсов.
   * @param dkeys Список ключей. Он будет унимально отсортирован.
   * @return Фьючерс со списком записей в неопределенном порядке.
   */
  def getSeveral(dkeys: Seq[String]) = BACKEND.getSeveral(dkeys)


  /** Интерфейс хранилищ модели. */
  trait Backend {
    def getForDkey(dkey: String): Future[Option[MDomain]]
    def getAll: Future[List[MDomain]]
    def save(d: MDomain): Future[MDomain]
    def delete(dkey: String): Future[Any]
    def getSeveral(dkeys: Seq[String]): Future[List[MDomain]]
  }


}
