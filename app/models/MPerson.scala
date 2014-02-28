package models

import util.{StorageUtil, ModelSerialJson, SiobixFs}
import SiobixFs.fs
import io.suggest.model.JsonDfsBackend
import io.suggest.util.StorageType._
import util.DfsModelUtil.getPersonPath
import scala.concurrent.{Future, future}
import play.api.libs.concurrent.Execution.Implicits._
import org.hbase.async.{DeleteRequest, GetRequest, PutRequest}
import scala.collection.JavaConversions._
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioModelUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 18:19
 * Description: Юзер, зареганный на сайте. Как правило, админ одного или нескольких сайтов.
 * Люди различаются по email'ам. Это является их идентификаторами.
 * @param id Идентификатор юзера, т.е. его email.
 * @param lang Язык интерфейса для указанного пользователя.
 *             Формат четко неопределён, и соответствует коду выхлопа Controller.lang().
 */

case class MPerson(
  id : String,
  var lang: String
) extends MPersonLinks {
  import MPerson.BACKEND

  // Линки в другие модели.
  @JsonIgnore def authz = MPersonDomainAuthz.getForPerson(id)

  /**
   * Сохранить отметку о таком юзере
   * @return Фьючерс с сохраненным экземпляром MPerson.
   */
  @JsonIgnore def save = BACKEND.save(this)

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
      case HBASE  => new HBaseBackendBulk
    }
  }

  // Список емейлов админов suggest.io. Пока преднамеренно захардкожен, потом -- посмотрим.
  private val suEmails = Set("konstantin.nikiforov@cbca.ru", "ilya@shuma.ru", "sasha@cbca.ru", "maksim.sharipov@cbca.ru")

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
    def save(data:MPerson): Future[_]
    def getById(id: String): Future[Option[MPerson]]
  }


  /** DFS-backend для текущей модели. */
  class DfsBackend extends Backend {
    def save(data: MPerson): Future[_] = {
      val path = getPersonPath(data.id)
      future {
        JsonDfsBackend.writeToPath(path, data)
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


  import io.suggest.model.MObject.{HTABLE_NAME_BYTES, CF_UPROPS}
  import io.suggest.model.SioHBaseAsyncClient._
  import io.suggest.model.HTapConversionsBasic._

  /** Hbase backend для текущей модели, сохраняющий все данные в одну колонку через JSON. */
  class HBaseBackendBulk extends Backend with ModelSerialJson {
    private val CF_UPROPS_B = CF_UPROPS.getBytes
    private def QUALIFIER = CF_UPROPS_B

    protected def id2rowkey(id: String): Array[Byte] = SioModelUtil.serializeStrForHCellCoord(id)
    protected def deserialize(data: Array[Byte]) = deserializeTo[MPerson](data)

    def save(data: MPerson): Future[_] = {
      val putReq = new PutRequest(HTABLE_NAME_BYTES, id2rowkey(data.id), CF_UPROPS_B, QUALIFIER, serialize(data))
      ahclient.put(putReq)
    }

    def getById(id: String): Future[Option[MPerson]] = {
      val getReq = new GetRequest(HTABLE_NAME_BYTES, id2rowkey(id))
        .family(CF_UPROPS_B)
        .qualifier(QUALIFIER)
      ahclient.get(getReq) map { al =>
        if (al.isEmpty) None else Some(deserialize(al.head.value()))
      }
    }
  }


  /* TODO Экспериментальный-сырой-неиспользуемый HBase backend, сохраняющий отдельные поля в отдельные колонки.
   * Теоретически, может помочь в организации ускорения при росте объёмов данных.
   * Но сначала нужно обдумать асинхронное i/o для удобного доступа к кускам записей.
   * Возможно, тут поможет apache gora, её маппинги, и PersistenceManager для in-memory записей. */
  class HBaseBackendQualified extends Backend {

    val Q_DKEYS   = "a"
    val Q_CREATED = "b"
    val Q_LANG    = "c"

    def id2key(id: String): Array[Byte] = id

    val DKEY_SER_SEP = ","
    def serializeDkeys(dkeys: List[String]): Array[Byte] = dkeys.mkString(DKEY_SER_SEP).getBytes
    def deserializeDkeys(dkeysSer: Array[Byte]): List[String] = {
      if (dkeysSer.length == 0) {
        Nil
      } else {
        new String(dkeysSer).split(DKEY_SER_SEP).toList
      }
    }

    def serializeLang(langIso2: String): Array[Byte]  = langIso2
    def deserializeLang(langSer: Array[Byte]): String = new String(langSer)

    def save(data: MPerson): Future[_] = {
      val rowKey = id2key(data.id)
      val table = HTABLE_NAME_BYTES

      // Язык, всегда задан.
      val langFut: Future[_] = {
        val value = serializeLang(data.lang)
        val putReq = new PutRequest(table, rowKey, CF_UPROPS.getBytes, Q_LANG.getBytes, value)
        ahclient.put(putReq)
      }

      // Асинхронно дождаться выполнения всех реквестов.
      for {
        langIso2 <- langFut
      } yield ()
    }

    def getById(id: String): Future[Option[MPerson]] = {
      val getReq = new GetRequest(HTABLE_NAME_BYTES, id2key(id)).family(CF_UPROPS)
      ahclient.get(getReq) map { row =>
        if (row.isEmpty) {
          None
        } else {
          val result = new MPerson(id, lang=null)
          // Такатываем все qualifier'ы на исходную запись.
          // TODO lang всегда задан, поэтому тут должна быть его десериализация без вариантов.
          row.foreach { kv =>
            new String(kv.qualifier()) match {
              case Q_LANG  => result.lang = deserializeLang(kv.value)
            }
          }
          Some(result)
        }
      }
    }

  }

}
