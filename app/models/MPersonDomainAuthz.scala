package models

import org.joda.time.DateTime
import io.suggest.util.{Logs, StringUtil}
import util.{DomainQi, SiobixFs}
import SiobixFs.fs
import org.apache.hadoop.fs.Path
import io.suggest.model.JsonDfsBackend
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.UUID

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
  var dt_last_checked   : Option[DateTime] = None,
  var is_verified       : Boolean = false,
  var last_errors       : List[String] = Nil
) {

  // Связи с другими моделями.
  @JsonIgnore def domain = MDomain.getForDkey(dkey).get
  @JsonIgnore def person = MPerson.getByEmail(person_id).get

  /**
   * Сохранить текущий экземпляр класса в базу.
   */
  def save = {
    val path = MPersonDomainAuthz.dkeyPersonPath(dkey, person_id)
    JsonDfsBackend.writeTo(path, this)
    this
  }

}


// Статическая часть модели
object MPersonDomainAuthz extends Logs {

  // Длина кода валидации
  val BODY_CODE_LEN = 16

  // Допустимые значения поля typ класса.
  val TYPE_QI         = "qi"
  val TYPE_VALIDATION = "va"

  val fileName = "auth"

  /**
   * Путь к файлу данных по указанному юзеру в рамках домена. Имеет вид m_person/putin@kremlin.ru/sugggest.io/authz
   * @param dkey ключ домена
   * @param person_id id юзера, т.е. email
   * @return
   */
  def dkeyPersonPath(dkey:String, person_id:String) = new Path(new Path(MPerson.getPath(person_id), dkey), fileName)

  /**
   * Прочитать из хранилища json-файл по данным юзера.
   * @param dkey
   * @param person_id
   * @return
   */
  def getForPersonDkey(dkey:String, person_id:String) : Option[MPersonDomainAuthz] = {
    val path = dkeyPersonPath(dkey, person_id)
    readOne(path)
  }


  /**
   * Аккуратненько прочитать файл. Если файла нет или чтение не удалось, то в логах будет экзепшен и None в результате.
   * @param path путь, который читать.
   * @return Option[MDomainPerson]
   */
  protected def readOne(path:Path) : Option[MPersonDomainAuthz] = {
    try {
      JsonDfsBackend.getAs[MPersonDomainAuthz](path, fs)
    } catch {
      case ex:Throwable =>
        error("Cannot read domain_person json from " + path, ex)
        None
    }
  }


  /**
   * Враппер над readOne для удобства вызова из foldLeft()().
   * @param acc аккамулятор типа List[MDomainPerson]
   * @param path путь, из которого стоит читать данные
   * @return аккамулятор
   */
  protected def readOneAcc(acc:List[MPersonDomainAuthz], path:Path) : List[MPersonDomainAuthz] = {
    readOne(path) match {
      case Some(mdp) => mdp :: acc
      case None => acc
    }
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
      dt_last_checked = Some(DateTime.now()),
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