package models

import org.joda.time.{ReadableDuration, DateTime, Duration}
import io.suggest.util.{Logs, StringUtil}
import util.{DomainValidator, DfsModelUtil, SiobixFs}
import SiobixFs.fs
import org.apache.hadoop.fs.Path
import io.suggest.model.JsonDfsBackend
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.UUID
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.13 16:15
 * Description: Тут порт модели person_domain из эрланговского sioweb. Поля и логика совпадают.
 * Модель нужна для хранения данных авторизации юзеров, владеющими различными сайтами.
 */

case class MPersonDomainAuthz(
  id                    : String,
  // dkey и person_id хранятся в пути к файлу json, а тут дублируются для упрощения работы (сериализации/десериализации).
  dkey                  : String,
  person_id             : String,
  typ                   : String, // "Тип" - это или qi, или va (validation). См. TYPE_* у объекта-компаньона.
  body_code             : String, // пустая строка для qi или случайная строка с кодом для validation
  date_created          : DateTime = DateTime.now,
  var dt_last_checked   : DateTime = MPersonDomainAuthz.dtDefault, // Не используем Option для облегчения сериализации + тут почти всегда будет Some().
  var is_verified       : Boolean = false,
  var last_errors       : List[String] = Nil
) extends MDomainAuthzT {

  import MPersonDomainAuthz.{TYPE_QI, TYPE_VALIDATION, genBodyCodeValidation, VERIFY_DURATION_SOFT, VERIFY_DURATION_HARD, dkeyPersonPath}

  // Связи с другими моделями и компонентами системы.
  @JsonIgnore def person = MPerson.getById(person_id)
  @JsonIgnore def maybeRevalidate(sendEvents:Boolean = true) = DomainValidator.maybeRevalidate(this, sendEvents)
  @JsonIgnore def revalidate(sendEvents:Boolean = true)      = DomainValidator.revalidate(this, sendEvents)


  private lazy val filepath = dkeyPersonPath(dkey, person_id)

  /**
   * Сохранить текущий экземпляр класса в базу.
   */
  def save: MPersonDomainAuthz = {
    JsonDfsBackend.writeToPath(filepath, this)
    this
  }

  def isQiType = typ == TYPE_QI
  def isValidationType = typ == TYPE_VALIDATION

  /**
   * Убедиться, что это добро подходит для qi. Validation-тип не конвертим в qi, ибо это пока не требуется.
   * @return Инсанс, подходящий для qi. Обычно функция возвращает this.
   */
  def toQi: MPersonDomainAuthz = {
    if (isQiType)
      this
    else
      ???
  }

  /**
   * Сделать объект validation. Если текущий класс уже есть validation, то вернуть this. Иначе, сгенерить новый, заполнив body_code.
   * @return Экземпляр класса, имеющего тип равный qi.
   */
  def toValidation: MPersonDomainAuthz = {
    if (isValidationType)
      this
    else if (isQiType) {
      val body_code1 = if (body_code == "")
        genBodyCodeValidation
      else
        body_code
      new MPersonDomainAuthz(id=id, dkey=dkey, person_id=person_id, typ=TYPE_VALIDATION, body_code=body_code1, date_created=date_created)

    } else ???
  }


  /**
   * Обернуть body_code в option.
   * @return для qi-ключей обычно None. Для Validation-ключей всегда Some()
   */
  override def bodyCodeOpt = {
    if (body_code == "")
      None
    else
      Some(body_code)
  }


  /**
   * Верифицировано ли? И если да, то является ли верификация актуальной?
   * @return true, если всё ок.
   */
  def isValid = is_verified && !breaksHardLimit

  /**
   * Пора ли проводить повторную переверификацию? Да, если is_verified=false или время true истекло.
   * @return true, если пора пройти валидацию.
   */
  def isNeedRevalidation = !is_verified || breaksSoftLimit

  def breaksHardLimit = breaksLimit(VERIFY_DURATION_HARD)
  def breaksSoftLimit = breaksLimit(VERIFY_DURATION_SOFT)
  def breaksLimit(limit:ReadableDuration): Boolean = {
    dt_last_checked.minus(limit) isAfter DateTime.now
  }


  def delete = fs.delete(filepath, false)
  def personIdOpt: Option[String] = Some(person_id)

  /**
   * Сгенерить проверочную ссылку на удаленном сервере.
   * @param isUnicode true влияет на IDN-домены, выдавая хостнейм в национальном алфавите.
   * @return Строка ссылки, которая будет использована как основа для проверки.
   */
  def fileUrl(isUnicode: Boolean = false): String = {
    val hostname = if(isUnicode)
      dkeyUnicode
    else
      dkey
    "http://" + hostname + "/" + filename
  }

  def filename: String = id + ".txt"

  override def authzForPerson(_person_id: String): Option[MPersonDomainAuthz] = {
    if (person_id == _person_id)
      Some(this)
    else
      super.authzForPerson(person_id)
  }
}


// Статическая часть модели
object MPersonDomainAuthz extends Logs {

  // Длина кода валидации
  private val BODY_CODE_LEN = 16

  // Допустимые значения поля typ класса.
  private val TYPE_QI         = "qi"
  private val TYPE_VALIDATION = "va"

  // Нужно периодически обновлять данные по валидности доступа. Софт и хард лимиты описывают периодичность проверок.
  // Софт-лимит не ломает верификацию, однако намекает что надо бы сделать повторную проверку.
  private val VERIFY_DURATION_SOFT = new Duration(25.minutes.toMillis)
  // Превышения хард-лимита означает, что верификация уже истекла и её нужно проверять заново.
  private val VERIFY_DURATION_HARD = new Duration(40.minutes.toMillis)

  // Имя файла, в котором хранятся все данные модели. Инфа важная, поэтому менять или ставить в зависимость от имени класса нельзя.
  private val fileName = new Path("authz")

  private val personSubdir = new Path("domains")

  // Дата, точность которой не важна и которая используется как "давно/никогда".
  private val dtDefault = new DateTime(1970, 1, 1, 0, 0)

  /**
   * Путь к поддиректории юзера с доменами. Вероятно, следует вынести в отдельную модель.
   * @param person_id id юзера
   * @return Путь к поддиректории domains в директории указанного юзера.
   */
  private def personPath(person_id:String) = new Path(DfsModelUtil.getPersonPath(person_id), personSubdir)

  /**
   * Путь к файлу данных по указанному юзеру в рамках домена. Имеет вид m_person/putin@kremlin.ru/sugggest.io/authz
   * @param dkey ключ домена
   * @param person_id id юзера, т.е. email
   * @return Путь, указывающий на файл авторизации в папке доменов юзера. Часть этой функции имеет смысл вынести в MPersonDomain.
   */
  private def dkeyPersonPath(dkey:String, person_id:String) = {
    val personDir = personPath(person_id)
    val personDkeyDir = new Path(personDir, dkey)
    authzFilePath(personDkeyDir)
  }

  /**
   * Выдать путь к файлу с данными авторизации.
   * @param personDkeyDir папка домена внутри папки пользователя.
   */
  private def authzFilePath(personDkeyDir: Path) = new Path(personDkeyDir, fileName)


  /**
   * Прочитать из хранилища json-файл по данным юзера.
   * @param dkey ключ домена
   * @param person_id id юзера
   * @return
   */
  def getForPersonDkey(dkey:String, person_id:String) : Option[MPersonDomainAuthz] = {
    val path = dkeyPersonPath(dkey, person_id)
    readOne(path)
  }


  /**
   * Выдать домены, которые юзер админит или хотел бы админить.
   * @param person_id id юзера
   * @return Список сохраненных авторизаций, в т.ч. пустой. В алфавитном порядке.
   */
  def getForPerson(person_id:String): List[MPersonDomainAuthz] = {
    val personDomainsDir = personPath(person_id)
    fs.listStatus(personDomainsDir).foldLeft[List[MPersonDomainAuthz]] (Nil) { (acc, fstatus) =>
      if (fstatus.isDir) {
        val authzPath = authzFilePath(fstatus.getPath)
        readOneAcc(acc, authzPath)

      } else {
        acc
      }
    } sortBy(_.dkey)
  }


  /**
   * Аккуратненько прочитать файл. Если файла нет или чтение не удалось, то в логах будет экзепшен и None в результате.
   * @param path путь, который читать.
   * @return Option[MDomainPerson]
   */
  protected def readOne(path:Path) : Option[MPersonDomainAuthz] = {
    DfsModelUtil.readOne[MPersonDomainAuthz](path)
  }



  /**
   * Враппер над readOne для удобства вызова из foldLeft()().
   * @param acc аккамулятор типа List[MDomainPerson]
   * @param path путь, из которого стоит читать данные
   * @return аккамулятор
   */
  protected def readOneAcc(acc:List[MPersonDomainAuthz], path:Path) : List[MPersonDomainAuthz] = {
    DfsModelUtil.readOneAcc[MPersonDomainAuthz](acc, path)
  }


  /*
   * Собрать все идентификации для домена в один список (в неопределенном порядке)
   * @param dkey
   * @return
   */
  /*def getForDkey(dkey:String) : List[MPersonDomainAuthz] = {
    val path = dkeyAllPath(dkey)
    fs.listStatus(path)
      .toList
      .foldLeft(List[MPersonDomainAuthz]()) { (acc, fstatus:FileStatus) =>
        readOneAcc(acc, fstatus.getPath)
      }
  }*/


  /**
   * Собрать все идентификационные данные в доменах для указанного юзера.
   * @param person_id мыльник
   * @return
   */
  def getForPersonDkeys(person_id:String, dkeys:Iterable[String]) : List[MPersonDomainAuthz] = {
    dkeys
      .map { dkeyPersonPath(_, person_id) }
      .filter { fs.exists _ }
      .foldLeft(List[MPersonDomainAuthz]()) { readOneAcc _ }
  }


  /**
   * Сгенерить код для "тела" файла валидации.
   * @return случайную строку длины BODY_CODE_LEN
   */
  def genBodyCodeValidation = StringUtil.randomId(BODY_CODE_LEN)

  /**
   * Сгенерить экземпляр сабжа для нужд qi.
   * @param id qi_id
   * @param dkey ключ домена
   * @param person_id id юзера
   * @param is_verified Проверены ли эти данные на сайте клиента?
   * @param last_errors Список ошибок в ходе проверки. По умолчанию - пустой список.
   * @return Экземпляр MPersonDomainAuthz типа TYPE_QI.
   */
  def newQi(id:String, dkey:String, person_id:String, is_verified:Boolean, last_errors:List[String] = Nil): MPersonDomainAuthz = {
    new MPersonDomainAuthz(id=id, dkey=dkey, person_id=person_id, typ=TYPE_QI, body_code="",
      is_verified = is_verified,
      dt_last_checked = DateTime.now(),
      last_errors = last_errors
    )
  }


  /**
   * Сгенерить экземпляр сабжа для нужд validation.
   * @param id ключ. По дефолту - рандомный UUID.
   * @param dkey id домена.
   * @param person_id id юзера.
   * @param body_code код в теле файла валидации.
   * @param is_verified проверена ли инфа? Обычно, нет.
   * @param last_errors Список ошибок. По умолчанию - пустой.
   * @return Экземпляр MPersonDomainAuthz типа TYPE_VALIDATION.
   */
  def newValidation(id:String = UUID.randomUUID().toString, dkey:String, person_id:String, body_code:String = genBodyCodeValidation,
                    is_verified:Boolean = false, last_errors:List[String] = Nil, date_last_checked:Option[DateTime] = None): MPersonDomainAuthz = {
    new MPersonDomainAuthz(id=id, dkey=dkey, person_id=person_id, typ=TYPE_VALIDATION, body_code=body_code, is_verified=is_verified, last_errors=last_errors)
  }


  /**
   * Удалить файл с сериализованными данными сабжа из хранилища.
   * @param person_id id юзера.
   * @param dkey id ключа.
   * @return true, если файл удален. Иначе - false
   */
  def delete(person_id:String, dkey:String): Boolean = {
    val path = dkeyPersonPath(dkey, person_id)
    fs.delete(path, false)
  }

}