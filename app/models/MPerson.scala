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

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 18:19
 * Description: Юзер, зареганный на сайте. Как правило, админ одного или нескольких сайтов.
 * Люди различаются по email'ам. Это является их идентификаторами.
 * @param id Идентификатор юзера, т.е. его email.
 * @param dkeys Список доменов на панели доменов юзера.
 * @param langIso2 Язык интерфейса для указанного пользователя.
 */

case class MPerson(
  id : String,
  var dkeys : List[String] = Nil,
  var langIso2: Option[String] = None
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
      case HBASE  => new HBaseBackendBulk
    }
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


  import io.suggest.model.MObject.{HTABLE_NAME_BYTES, CF_UPROPS}
  import io.suggest.model.SioHBaseAsyncClient._
  import io.suggest.model.HTapConversionsBasic._

  /** Hbase backend для текущей модели, сохраняющий все данные в одну колонку через JSON. */
  class HBaseBackendBulk extends Backend with ModelSerialJson {
    private def QUALIFIER = CF_UPROPS.getBytes   // !!! при изменении надо менять val col в save().
    val KEYPREFIX = "u:"

    def id2key(id: String): Array[Byte] = KEYPREFIX + id
    def deserialize(data: Array[Byte]) = deserializeTo[MPerson](data)

    def save(data: MPerson): Future[MPerson] = {
      val putReq = new PutRequest(HTABLE_NAME_BYTES, id2key(data.id), CF_UPROPS.getBytes, QUALIFIER, serialize(data))
      ahclient.put(putReq).map(_ => data)
    }

    def getById(id: String): Future[Option[MPerson]] = {
      val getReq = new GetRequest(HTABLE_NAME_BYTES, id2key(id))
        .family(CF_UPROPS)
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

    def save(data: MPerson): Future[MPerson] = {
      val rowKey = id2key(data.id)
      val table = HTABLE_NAME_BYTES

      // Отправляем в базу колонку с ключами доменов
      val dkeysFut: Future[_] = if (!data.dkeys.isEmpty) {
        val value = serializeDkeys(data.dkeys)
        val putReq = new PutRequest(table, rowKey, CF_UPROPS.getBytes, Q_DKEYS.getBytes, value)
        ahclient.put(putReq)
      } else {
        // Доменов нет, возможно юзер их удалил. Удалить надо бы из базы.
        val delReq = new DeleteRequest(table, rowKey, CF_UPROPS.getBytes, Q_DKEYS.getBytes)
        ahclient.delete(delReq)
      }

      // Язык, если задан.
      val langFut: Future[_] = if (data.langIso2.isDefined) {
        val value = serializeLang(data.langIso2.get)
        val putReq = new PutRequest(table, rowKey, CF_UPROPS.getBytes, Q_LANG.getBytes, value)
        ahclient.put(putReq)
      } else {
        // Язык внезапно не выставлен.
        val delReq = new DeleteRequest(table, rowKey, CF_UPROPS.getBytes, Q_LANG.getBytes)
        ahclient.delete(delReq)
      }

      // Асинхронно дождаться выполнения всех реквестов.
      for {
        dkeys <- dkeysFut
        langIso2 <- langFut
      } yield data
    }

    def getById(id: String): Future[Option[MPerson]] = {
      val getReq = new GetRequest(HTABLE_NAME_BYTES, id2key(id)).family(CF_UPROPS)
      ahclient.get(getReq) map { row =>
        if (row.isEmpty) {
          None
        } else {
          val result = new MPerson(id)
          // Такатываем все qualifier'ы на исходную запись.
          row.foreach { kv =>
            new String(kv.qualifier()) match {
              case Q_DKEYS => result.dkeys = deserializeDkeys(kv.value)
              case Q_LANG  => result.langIso2 = Some(deserializeLang(kv.value))
            }
          }
          Some(result)
        }
      }
    }

  }

}
