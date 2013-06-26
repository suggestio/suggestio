package models

import org.joda.time.{ReadableDuration, DateTime, Duration}
import io.suggest.util.{Logs, StringUtil}
import util.{DfsModelUtil, SiobixFs}
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
  date_created_utc      : DateTime = DateTime.now,
  var dt_last_checked   : DateTime = MPersonDomainAuthz.dtDefault, // Не используем Option для облегчения сериализации + тут почти всегда будет Some().
  var is_verified       : Boolean = false,
  var last_errors       : List[String] = Nil
) {

  import MPersonDomainAuthz.{TYPE_QI, TYPE_VALIDATION, genBodyCodeValidation, VERIFY_DURATION_SOFT, VERIFY_DURATION_HARD}

  // Связи с другими моделями.
  @JsonIgnore def domain = MDomain.getForDkey(dkey).get
  @JsonIgnore def person = MPerson.getById(person_id).get

  /**
   * Сохранить текущий экземпляр класса в базу.
   */
  def save = {
    val path = MPersonDomainAuthz.dkeyPersonPath(dkey, person_id)
    JsonDfsBackend.writeTo(path, this)
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
      new MPersonDomainAuthz(id=id, dkey=dkey, person_id=person_id, typ=TYPE_VALIDATION, body_code=body_code1, date_created_utc=date_created_utc)

    } else ???
  }


  /**
   * Обернуть body_code в option.
   * @return
   */
  def bodeCodeOpt = if (body_code == "")
    None
  else
    Some(body_code)


  /**
   * Верифицировано ли? И если да, то является ли верификация актуальной?
   * @return true, если всё ок.
   */
  def isVerified = is_verified && !breaksHardLimit

  /**
   * Пора ли проводить повторную переверификацию? Да, если is_verified=false или время true истекло.
   * @return true, если пора пройти валидацию.
   */
  def isNeedReverification = !is_verified || breaksSoftLimit

  def breaksHardLimit = breaksLimit(VERIFY_DURATION_HARD)
  def breaksSoftLimit = breaksLimit(VERIFY_DURATION_SOFT)
  def breaksLimit(limit:ReadableDuration): Boolean = {
    dt_last_checked.minus(limit) isAfter DateTime.now
  }
}


// Статическая часть модели
object MPersonDomainAuthz extends Logs {

  // Длина кода валидации
  val BODY_CODE_LEN = 16

  // Допустимые значения поля typ класса.
  val TYPE_QI         = "qi"
  val TYPE_VALIDATION = "va"

  // Нужно периодически обновлять данные по валидности доступа. Софт и хард лимиты описывают периодичность проверок.
  // Софт-лимит не ломает верификацию, однако намекает что надо бы сделать повторную проверку.
  val VERIFY_DURATION_SOFT = new Duration(25.minutes.toMillis)
  // Превышения хард-лимита означает, что верификация уже истекла и её нужно проверять заново.
  val VERIFY_DURATION_HARD = new Duration(40.minutes.toMillis)

  // Имя файла, в котором хранятся все данные модели. Инфа важная, поэтому менять или ставить в зависимость от имени класса нельзя.
  val fileName = new Path("authz")

  val personSubdir = new Path("domains")

  // Дата, точность которой не важна и которая используется как "давно/никогда".
  val dtDefault = new DateTime(1970, 1, 1, 0, 0)

  /**
   * Путь к поддиректории юзера с доменами. Вероятно, следует вынести в отдельную модель.
   * @param person_id id юзера
   * @return Путь к поддиректории domains в директории указанного юзера.
   */
  def personPath(person_id:String) = new Path(MPerson.getPath(person_id), personSubdir)

  /**
   * Путь к файлу данных по указанному юзеру в рамках домена. Имеет вид m_person/putin@kremlin.ru/sugggest.io/authz
   * @param dkey ключ домена
   * @param person_id id юзера, т.е. email
   * @return Путь, указывающий на файл авторизации в папке доменов юзера. Часть этой функции имеет смысл вынести в MPersonDomain.
   */
  def dkeyPersonPath(dkey:String, person_id:String) = {
    val personDir = personPath(person_id)
    val personDkeyDir = new Path(personDir, dkey)
    authzFilePath(personDkeyDir)
  }

  /**
   * Выдать путь к файлу с данными авторизации.
   * @param personDkeyDir папка домена внутри папки пользователя.
   */
  protected def authzFilePath(personDkeyDir: Path) = new Path(personDkeyDir, fileName)


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

}