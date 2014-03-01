package models

import org.joda.time.{ReadableDuration, DateTime, Duration}
import io.suggest.util.{SioModelUtil, StringUtil}
import util._
import SiobixFs.fs
import org.apache.hadoop.fs.{FileStatus, Path}
import io.suggest.model.{AsyncHbaseScannerFold, JsonDfsBackend}
import io.suggest.util.StorageType._
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Future, future}
import play.api.libs.concurrent.Execution.Implicits._
import scala.collection.JavaConversions._
import org.hbase.async.{KeyValue, GetRequest, DeleteRequest, PutRequest}
import scala.Some
import play.api.Logger
import org.elasticsearch.client.Client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.13 16:15
 * Description: Тут порт модели person_domain из эрланговского sioweb. Поля и логика совпадают.
 * Модель нужна для хранения данных авторизации юзеров, владеющими различными сайтами.
 */

case class MPersonDomainAuthz(
  id                    : String,
  // dkey и personId хранятся в пути к файлу json, а тут дублируются для упрощения работы (сериализации/десериализации).
  dkey                  : String,
  personId              : String,
  typ                   : String, // "Тип" - это или qi, или va (validation). См. TYPE_* у объекта-компаньона.
  bodyCode              : String, // пустая строка для qi или случайная строка с кодом для validation
  dateCreated           : DateTime = DateTime.now,
  var dtLastChecked     : DateTime = MPersonDomainAuthz.dtDefault, // Не используем Option для облегчения сериализации + тут почти всегда будет Some().
  var isVerified        : Boolean = false,
  var lastErrors        : List[String] = Nil
) extends MDomainAuthzT {

  import MPersonDomainAuthz._
  import LOGGER._

  // Связи с другими моделями и компонентами системы.
  @JsonIgnore def person(implicit client: Client) = MPerson.getById(personId)
  def maybeRevalidate(sendEvents:Boolean = true) = DomainValidator.maybeRevalidate(this, sendEvents)
  def revalidate(sendEvents:Boolean = true)      = DomainValidator.revalidate(this, sendEvents)


  /**
   * Сохранить текущий экземпляр класса в базу.
   */
  def save = {
    trace(s"save(): Saving authz id=$id dkey=$dkey for person=$personId.")
    BACKEND.save(this)
  }

  def isQiType = typ == TYPE_QI
  def isValidationType = typ == TYPE_VALIDATION

  /**
   * Убедиться, что это добро подходит для qi. Validation-тип не конвертим в qi, ибо это пока не требуется.
   * @return Инсанс, подходящий для qi. Обычно функция возвращает this.
   */
  @JsonIgnore def toQi: MPersonDomainAuthz = if (isQiType) this else ???

  /**
   * Сделать объект validation. Если текущий класс уже есть validation, то вернуть this. Иначе, сгенерить новый, заполнив body_code.
   * @return Экземпляр класса, имеющего тип равный qi.
   */
  @JsonIgnore def toValidation: MPersonDomainAuthz = {
    if (isValidationType)
      this
    else if (isQiType) {
      val body_code1 = if (bodyCode == "")
        genBodyCodeValidation
      else
        bodyCode
      new MPersonDomainAuthz(id=id, dkey=dkey, personId=personId, typ=TYPE_VALIDATION, bodyCode=body_code1, dateCreated=dateCreated)

    } else ???
  }


  /**
   * Обернуть body_code в option.
   * @return для qi-ключей обычно None. Для Validation-ключей всегда Some()
   */
  def bodyCodeOpt = if (bodyCode == "") None  else  Some(bodyCode)


  /**
   * Верифицировано ли? И если да, то является ли верификация актуальной?
   * @return true, если всё ок.
   */
  def isValid = isVerified && !breaksHardLimit

  /**
   * Пора ли проводить повторную переверификацию? Да, если is_verified=false или время true истекло.
   * @return true, если пора пройти валидацию.
   */
  def isNeedRevalidation = !isVerified || breaksSoftLimit

  @JsonIgnore def breaksHardLimit = breaksLimit(VERIFY_DURATION_HARD)
  @JsonIgnore def breaksSoftLimit = breaksLimit(VERIFY_DURATION_SOFT)
  def breaksLimit(limit:ReadableDuration): Boolean = {
    dtLastChecked.minus(limit) isAfter DateTime.now
  }


  def delete = MPersonDomainAuthz.delete(personId=personId, dkey=dkey)
  def personIdOpt: Option[String] = Some(personId)

  /**
   * Сгенерить проверочную ссылку на удаленном сервере.
   * @param isUnicode true влияет на IDN-домены, выдавая хостнейм в национальном алфавите.
   * @return Строка ссылки, которая будет использована как основа для проверки.
   */
  def fileUrl(isUnicode: Boolean = false): String = {
    val hostname = if (isUnicode) dkeyUnicode else dkey
    "http://" + hostname + "/" + remoteFilename
  }

  def remoteFilename: String = id + ".txt"

  override def authzForPerson(_person_id: String): Future[Option[MPersonDomainAuthz]] = {
    if (personId == _person_id) {
      Future.successful(Some(this))
    } else {
      super.authzForPerson(personId)
    }
  }
}


// Статическая часть модели
object MPersonDomainAuthz {

  private val LOGGER = Logger(getClass)

  private val BACKEND: Backend = StorageUtil.STORAGE match {
    case DFS    => new DfsBackend
    case HBASE  => new HBaseBackend
  }

  // Длина кода валидации
  private val BODY_CODE_LEN = 16

  // Допустимые значения поля typ класса.
  val TYPE_QI         = "qi"
  val TYPE_VALIDATION = "va"

  // Нужно периодически обновлять данные по валидности доступа. Софт и хард лимиты описывают периодичность проверок.
  // Софт-лимит не ломает верификацию, однако намекает что надо бы сделать повторную проверку.
  private val VERIFY_DURATION_SOFT = new Duration(25.minutes.toMillis)
  // Превышения хард-лимита означает, что верификация уже истекла и её нужно проверять заново.
  private val VERIFY_DURATION_HARD = new Duration(40.minutes.toMillis)

  // Дата, точность которой не важна и которая используется как "давно/никогда".
  private val dtDefault = new DateTime(1970, 1, 1, 0, 0)


  /**
   * Прочитать из хранилища json-файл по данным юзера.
   * @param dkey ключ домена
   * @param personId id юзера
   * @return
   */
  def getForPersonDkey(dkey:String, personId:String) = BACKEND.getForPersonDkey(dkey=dkey, personId=personId)


  /**
   * Выдать домены, которые юзер админит или хотел бы админить.
   * @param personId id юзера
   * @return Список сохраненных авторизаций, в т.ч. пустой. В алфавитном порядке.
   */
  def getForPerson(personId: String) = BACKEND.getForPerson(personId) map { _.sortBy(_.dkey) }


  /**
   * Собрать все идентификационные данные в доменах для указанного юзера.
   * @param personId мыльник
   * @return
   */
  def getForPersonDkeys(personId:String, dkeys:Seq[String]) = BACKEND.getForPersonDkeys(personId, dkeys)



  /**
   * Сгенерить код для "тела" файла валидации.
   * @return случайную строку длины BODY_CODE_LEN
   */
  def genBodyCodeValidation = StringUtil.randomId(BODY_CODE_LEN)

  /**
   * Сгенерить экземпляр сабжа для нужд qi.
   * @param id qi_id
   * @param dkey ключ домена
   * @param personId id юзера
   * @param isVerified Проверены ли эти данные на сайте клиента?
   * @param lastErrors Список ошибок в ходе проверки. По умолчанию - пустой список.
   * @return Экземпляр MPersonDomainAuthz типа TYPE_QI.
   */
  def newQi(id:String, dkey:String, personId:String, isVerified:Boolean, lastErrors:List[String] = Nil): MPersonDomainAuthz = {
    new MPersonDomainAuthz(id=id, dkey=dkey, personId=personId, typ=TYPE_QI, bodyCode="",
      isVerified = isVerified,
      dtLastChecked = DateTime.now(),
      lastErrors = lastErrors
    )
  }


  /**
   * Сгенерить экземпляр сабжа для нужд validation.
   * @param id ключ. По дефолту - рандомный UUID.
   * @param dkey id домена.
   * @param personId id юзера.
   * @param bodyCode код в теле файла валидации.
   * @param isVerified проверена ли инфа? Обычно, нет.
   * @param lastErrors Список ошибок. По умолчанию - пустой.
   * @return Экземпляр MPersonDomainAuthz типа TYPE_VALIDATION.
   */
  def newValidation(id:String = UUID.randomUUID().toString, dkey:String, personId:String,
                    bodyCode:String = genBodyCodeValidation, isVerified:Boolean = false, lastErrors:List[String] = Nil,
                    dtLastChecked:Option[DateTime] = None): MPersonDomainAuthz = {
    new MPersonDomainAuthz(
      id=id, dkey=dkey, personId=personId, typ=TYPE_VALIDATION, bodyCode=bodyCode,
      isVerified = isVerified,
      lastErrors = lastErrors,
      dtLastChecked = dtLastChecked getOrElse dtDefault
    )
  }


  /**
   * Удалить файл с сериализованными данными сабжа из хранилища.
   * @param personId id юзера.
   * @param dkey id ключа.
   * @return true, если файл удален. Иначе - false
   */
  def delete(personId:String, dkey:String) = BACKEND.delete(personId=personId, dkey=dkey)

  /**
   * Найти все авторизации для указанного домена. Не такая быстрая функция, и только для целевого использования:
   * в /sys/ или для других нечастых задач.
   * @param dkey Ключ домена
   * @return Фьючерс с найденными Authz. Возможно, в порядке (прямом или обратном) поля personId.
   */
  def getForDkey(dkey: String) = BACKEND.getForDkey(dkey)


  /** Интерфейс для storage backend'ов этой модели. */
  trait Backend {
    def save(data: MPersonDomainAuthz): Future[_]
    def delete(personId:String, dkey:String): Future[Any]
    def getForPersonDkey(personId:String, dkey:String) : Future[Option[MPersonDomainAuthz]]
    def getForPersonDkeys(personId:String, dkeys:Seq[String]) : Future[List[MPersonDomainAuthz]] = {
      // По дефолту фильтрануть getForPerson на предмет желаемых dkey.
      getForPerson(personId) map { authz =>
        authz.filter { dkeys contains _.dkey }
      }
    }
    def getForPerson(personId: String): Future[List[MPersonDomainAuthz]]
    def getForDkey(dkey:String) : Future[List[MPersonDomainAuthz]]
  }


  /** Backend для хранения данных модели в DFS. */
  class DfsBackend extends Backend {
    /** Имя файла, в котором хранятся все данные модели. Инфа важная, поэтому менять или
     * ставить в зависимость от имени класса нельзя. */
    private val fileName = new Path("authz")

    private val personSubdir = new Path("domains")

    /**
     * Путь к поддиректории юзера с доменами. Вероятно, следует вынести в отдельную модель.
     * @param personId id юзера
     * @return Путь к поддиректории domains в директории указанного юзера.
     */
    private def personPath(personId:String) = new Path(DfsModelUtil.getPersonPath(personId), personSubdir)

    /**
     * Путь к файлу данных по указанному юзеру в рамках домена. Имеет вид m_person/putin@kremlin.ru/sugggest.io/authz
     * @param dkey ключ домена
     * @param personId id юзера, т.е. email
     * @return Путь, указывающий на файл авторизации в папке доменов юзера. Часть этой функции имеет смысл вынести в MPersonDomain.
     */
    private def dkeyPersonPath(personId:String, dkey:String) = {
      val personDir = personPath(personId)
      val personDkeyDir = new Path(personDir, dkey)
      authzFilePath(personDkeyDir)
    }

    private def dkeyAllPath(dkey: String) = dkeyPersonPath(personId="*", dkey=dkey)

    /**
     * Выдать путь к файлу с данными авторизации.
     * @param personDkeyDir папка домена внутри папки пользователя.
     */
    private def authzFilePath(personDkeyDir: Path) = new Path(personDkeyDir, fileName)


    /**
     * Аккуратненько прочитать файл. Если файла нет или чтение не удалось, то в логах будет экзепшен и None в результате.
     * @param path путь, который читать.
     * @return Option[MDomainPerson]
     */
    private def readOne(path:Path) : Option[MPersonDomainAuthz] = {
      DfsModelUtil.readOne[MPersonDomainAuthz](path)
    }


    /**
     * Враппер над readOne для удобства вызова из foldLeft()().
     * @param acc аккамулятор типа List[MDomainPerson]
     * @param path путь, из которого стоит читать данные
     * @return аккамулятор
     */
    private def readOneAcc(acc:List[MPersonDomainAuthz], path:Path) : List[MPersonDomainAuthz] = {
      DfsModelUtil.readOneAcc[MPersonDomainAuthz](acc, path)
    }


    /**
     * Собрать все идентификации для домена в один список (в неопределенном порядке)
     * @param dkey
     * @return
     */
    def getForDkey(dkey:String) : Future[List[MPersonDomainAuthz]] = future {
      val path = dkeyAllPath(dkey)
      fs.listStatus(path)
        .toList
        .foldLeft[List[MPersonDomainAuthz]] (Nil) { (acc, fstatus:FileStatus) =>
          readOneAcc(acc, fstatus.getPath)
        }
    }


    def save(data: MPersonDomainAuthz): Future[_] = {
      val filepath = dkeyPersonPath(dkey = data.dkey,  personId = data.personId)
      future {
        JsonDfsBackend.writeToPath(filepath, data)
      }
    }

    def delete(personId: String, dkey: String): Future[Any] = {
      val path = dkeyPersonPath(dkey=dkey, personId=personId)
      future { fs.delete(path, false) }
    }

    def getForPersonDkey(personId: String, dkey: String): Future[Option[MPersonDomainAuthz]] = {
      val path = dkeyPersonPath(dkey=dkey, personId=personId)
      future { readOne(path) }
    }

    override def getForPersonDkeys(personId: String, dkeys: Seq[String]): Future[List[MPersonDomainAuthz]] = future {
      dkeys
        .map { _dkey => dkeyPersonPath(dkey = _dkey, personId=personId) }
        .filter { fs.exists }
        .foldLeft[List[MPersonDomainAuthz]] (Nil) { readOneAcc }
    }

    def getForPerson(personId: String): Future[List[MPersonDomainAuthz]] = future {
      val personDomainsDir = personPath(personId)
      fs.listStatus(personDomainsDir).foldLeft[List[MPersonDomainAuthz]] (Nil) { (acc, fstatus) =>
        if (fstatus.isDirectory) {
          val authzPath = authzFilePath(fstatus.getPath)
          readOneAcc(acc, authzPath)
        } else {
          acc
        }
      }
    }
  }


  /** При использовании HBase использовать id юзера как ряд, а dkey как колонку. */
  class HBaseBackend extends Backend with ModelSerialJson {
    import io.suggest.model.MObject.{CF_UAUTHZ, HTABLE_NAME_BYTES}
    import io.suggest.model.SioHBaseAsyncClient._

    private val CF_UAUTHZ_B = CF_UAUTHZ.getBytes

    def personId2key(personId: String): Array[Byte]  = SioModelUtil.serializeStrForHCellCoord(personId)
    def dkey2qualifier(dkey: String): Array[Byte]    = SioModelUtil.dkey2rowKey(dkey)
    def deserialize(data: Array[Byte])               = deserializeTo[MPersonDomainAuthz](data)

    def save(data: MPersonDomainAuthz): Future[_] = {
      val putReq = new PutRequest(HTABLE_NAME_BYTES, personId2key(data.personId), CF_UAUTHZ_B, dkey2qualifier(data.dkey), serialize(data))
      ahclient.put(putReq)
    }

    def delete(personId: String, dkey: String): Future[Any] = {
      val delReq = new DeleteRequest(HTABLE_NAME_BYTES, personId2key(personId), CF_UAUTHZ_B, dkey2qualifier(dkey))
      ahclient.delete(delReq)
    }

    def getForPersonDkey(personId: String, dkey: String): Future[Option[MPersonDomainAuthz]] = {
      val getReq = new GetRequest(HTABLE_NAME_BYTES, personId2key(personId))
        .family(CF_UAUTHZ_B)
        .qualifier(dkey2qualifier(dkey))
      ahclient.get(getReq) map { kvs =>
        if (kvs.isEmpty)  None  else  Some(deserialize(kvs.head.value))
      }
    }

    def getForPerson(personId: String): Future[List[MPersonDomainAuthz]] = {
      val getReq = new GetRequest(HTABLE_NAME_BYTES, personId2key(personId)).family(CF_UAUTHZ)
      ahclient.get(getReq) map { kvs =>
        kvs.map { kv => deserialize(kv.value) }.toList
      }
    }

    def getForDkey(dkey: String): Future[List[MPersonDomainAuthz]] = {
      val scanner = ahclient.newScanner(HTABLE_NAME_BYTES)
      scanner.setFamily(CF_UAUTHZ)
      scanner.setQualifier(dkey2qualifier(dkey))
      val folder = new AsyncHbaseScannerFold[List[MPersonDomainAuthz]] {
        def fold(acc0: List[MPersonDomainAuthz], kv: KeyValue): List[MPersonDomainAuthz] = {
          deserialize(kv.value()) :: acc0
        }
      }
      folder(Nil, scanner)
    }
  }

}

