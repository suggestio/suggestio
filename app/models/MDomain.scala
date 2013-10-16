package models

import util.{DkeyModelT, SiobixFs}
import SiobixFs.fs
import scala.concurrent.{Future, future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.13 15:50
 * Description: Модель доменов. Frontend-модель для доступа к различным данным доменов через другие модели.
 */

case class MDomain(dkey: String) extends DkeyModelT {
  override def domainOpt = Future.successful(Some(this))
}


// Статическая часть модели живёт здесь.
object MDomain {

  private val BACKEND: Backend = new DfsBackend

  /**
   * Прочитать для dkey. Если нет такого домена, то будет None.
   * @param dkey ключ домена.
   * @return Соответсвующий MDomain, если найден.
   */
  def getForDkey(dkey: String) = BACKEND.getForDkey(dkey)


  // Интерфейс для хранилища модели.
  trait Backend {
    def getForDkey(dkey: String): Future[Option[MDomain]]
  }

  /** Бэкэнд для хранения данных модели в Dfs. */
  class DfsBackend extends Backend {
    // Используем внешний thread pool вместо play'евского чтобы избежать блокировок Dfs-клиентом.
    import scala.concurrent.ExecutionContext.Implicits._

    def getForDkey(dkey: String): Future[Option[MDomain]] = future {
      val dkeyPath = SiobixFs.dkeyPathConf(dkey)
      fs.exists(dkeyPath) match {
        case true  => Some(MDomain(dkey=dkey))
        case false => None
      }
    }
  }

}

