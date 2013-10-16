package models

import util.{StorageUtil, ModelSerialJson, SiobixFs}
import SiobixFs.fs
import io.suggest.model.JsonDfsBackend
import util.DfsModelUtil.getPersonPath
import scala.concurrent.{Future, future}
import play.api.libs.concurrent.Execution.Implicits._
import org.hbase.async.{GetRequest, PutRequest}
import scala.collection.JavaConversions._
import StorageUtil.StorageType._
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 18:19
 * Description: Юзер, зареганный на сайте. Как правило, админ одного или нескольких сайтов.
 * Люди различаются по email'ам. Это является их идентификаторами.
 */

case class MPerson(
  id : String,
  var dkeys : List[String] = Nil // список доменов на панели доменов юзера
) extends MPersonLinks {
  import MPerson.BACKEND

  // Линки в другие модели.
  @JsonIgnore def authz = MPersonDomainAuthz.getForPersonDkeys(id, dkeys)

  /**
   * Сохранить отметку о таком юзере
   * @return Фьючерс с сохраненным экземпляром MPerson.
   */
  @JsonIgnore def save = BACKEND.save(this)


  /**
   * Добавить домен в список доменов, относящихся к юзеру. Затем нужно вызвать save.
   * @param dkey Ключ домена.
   * @return
   */
  def addDkey(dkey: String) : MPerson = {
    dkeys = dkey :: dkeys
    this
  }


  /**
   * Удалить указанный домен из списка доменов.
   * @param dkey ключ домена.
   * @return
   */
  def deleteDkey(dkey:String) : MPerson = {
    dkeys = dkeys.filter { _ != dkey }
    this
  }

}


// Трайт ссылок с юзера на другие модели. Это хорошо
trait MPersonLinks {
  val id : String

  @JsonIgnore def isSuperuser = MPerson isSuperuserId id
  def authzForDomain(dkey:String) = MPersonDomainAuthz.getForPersonDkey(dkey, id)
  @JsonIgnore def allDomainsAuthz = MPersonDomainAuthz.getForPerson(id)
}


// Статическая часть модели.
object MPerson {

  private val BACKEND: Backend = {
    StorageUtil.STORAGE match {
      case DFS    => new DfsBackend
      case HBASE  => new HBaseBackend
    }
    new DfsBackend
  }

  // Список емейлов админов suggest.io. Пока преднамеренно захардкожен, потом -- посмотрим.
  private val suEmails = Set("konstantin.nikiforov@cbca.ru", "ilya@shuma.ru", "sasha@cbca.ru")

  /**
   * Прочитать объект Person из хранилища.
   * @param id адрес эл.почты, который вернула Mozilla Persona и который используется для идентификации юзеров.
   * @return Фьючерс с опциональным MPerson.
   */
  def getById(id: String) = BACKEND.getById(id)


  /**
   * Принадлежит ли указанный мыльник админу suggest.io?
   * @param email емейл
   * @return
   */
  def isSuperuserId(email:String) = suEmails.contains(email)


  /** Интерфейс storage backend'а. для данной модели */
  trait Backend {
    def save(data:MPerson): Future[MPerson]
    def getById(id: String): Future[Option[MPerson]]
  }


  /** DFS-backend для текущей модели. */
  class DfsBackend extends Backend {
    def save(data: MPerson): Future[MPerson] = {
      val path = getPersonPath(data.id)
      future {
        JsonDfsBackend.writeToPath(path, data)
        data
      }
    }

    def getById(id: String): Future[Option[MPerson]] = {
      val filePath = getPersonPath(id)
      future {
        fs.exists(filePath) match {
          // Файл с данными по юзеру пуст - поэтому можно его не читать, а просто сделать необходимый объект.
          case true =>
            val person = JsonDfsBackend.getAs[MPerson](filePath, fs).get
            Some(person)

          case false => None
        }
      }
    }
  }


  /** Hbase backend для текущей модели */
  class HBaseBackend extends Backend with ModelSerialJson {
    import io.suggest.model.MObject.{HTABLE_NAME_BYTES, CF_UPROPS}
    import io.suggest.model.SioHBaseAsyncClient._
    import io.suggest.model.HTapConversionsBasic._

    val KEYPREFIX = "mperson:"
    def QUALIFIER = CF_UPROPS

    def id2key(id: String): Array[Byte] = KEYPREFIX + id
    def deserialize(data: Array[Byte]) = deserializeTo[MPerson](data)

    def save(data: MPerson): Future[MPerson] = {
      val putReq = new PutRequest(HTABLE_NAME_BYTES, id2key(data.id), CF_UPROPS, QUALIFIER, serialize(data))
      ahclient.put(putReq).map(_ => data)
    }

    def getById(id: String): Future[Option[MPerson]] = {
      val getReq = new GetRequest(HTABLE_NAME_BYTES, id2key(id)).family(CF_UPROPS).qualifier(QUALIFIER)
      ahclient.get(getReq) map { al =>
        if (al.isEmpty) None else Some(deserialize(al.head.value()))
      }
    }
  }

}
