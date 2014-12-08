package models

import util.SiobixFs.fs
import com.google.common.io.CharStreams
import java.io.{PrintWriter, InputStreamReader}
import util.{StorageUtil, SiobixFs, DkeyModelT, DfsModelStaticT}
import io.suggest.util.StorageType._
import scala.concurrent.Future
import org.apache.hadoop.fs.Path
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.13 16:35
 * Description: Модель хранения пользовательских настроек в виде сырого json, который пишется и читается только на клиенте.
 * JSON имеет произвольный формат, настройки генерируются через редактор и разбираются там же на клиентах.
 *
 * TODO Можно добавить поддержку множества профилей через набор колонок (qualifier).
 */

final case class MDomainUserJson(
  dkey: String,
  data: String
) extends DkeyModelT {
  import MDomainUserJson.BACKEND

  /**
   * Сохранить экземпляр класса в хранилище.
   * @return Текущий экземпляр класса.
   */
  def save = BACKEND.save(this)

  override def domainUserJson = Future.successful(Some(this))
}


object MDomainUserJson extends DfsModelStaticT {

  private val BACKEND: Backend = StorageUtil.STORAGE match {
    case DFS   => new DfsBackend
  }


  /**
   * Прочитать сырые данные для домена.
   * @param dkey ключ домена
   * @return Экземпляр сабжа, если такой найден в хранилищах.
   */
  def getForDkey(dkey:String) = BACKEND.getForDkey(dkey)


  trait Backend {
    def save(data: MDomainUserJson): Future[_]
    def getForDkey(dkey: String): Future[Option[MDomainUserJson]]
  }

  class DfsBackend extends Backend {
    // Имя файла, под именем которого сохраняется всё добро. Имена объектов обычно содержат $ на конце, поэтому это удаляем.
    val filename = MDomainUserJson.getClass.getCanonicalName.replace("$", "")

    /**
     * Сгенерить DFS-путь для указанного сайта и класса модели.
     * @param dkey ключ домена сайта.
     * @return Путь.
     */
    private def getPath(dkey:String) : Path = new Path(SiobixFs.dkeyPathConf(dkey), filename)

    def save(j: MDomainUserJson): Future[_] = {
      Future {
        import j._
        val path = getPath(dkey)
        if (data.isEmpty) {
          fs.delete(path, false)
        } else {
          val os = fs.create(path, true)
          try {
            new PrintWriter(os).print(data)

          } finally {
            os.close()
          }
        }
      }
    }

    def getForDkey(dkey: String): Future[Option[MDomainUserJson]] = {
      Future {
        val path = getPath(dkey)
        fs.exists(path) match {
          case true =>
            val is = fs.open(path)
            try {
              // Прочитать весь файл в строку
              val result = new MDomainUserJson(
                dkey = dkey,
                data = CharStreams.toString(new InputStreamReader(is, "UTF-8"))
              )
              Some(result)

            } finally {
              is.close()
            }

          case false => None
        }
      }
    }
  }

}
