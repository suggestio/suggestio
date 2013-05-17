package models

import org.joda.time.DateTime
import io.suggest.util.{Logs, StringUtil}
import util.SiobixFs
import SiobixFs.fs
import org.apache.hadoop.fs.Path
import io.suggest.model.JsonDfsBackend

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.13 16:15
 * Description: Тут порт модели person_domain из эрланговского sioweb. Поля и логика совпадают.
 * Модель нужна для хранения данных авторизации юзеров, владеющими различными сайтами.
 */

case class MPersonDomainAuthz(
  id               : String,
  dkey             : String,
  person_id        : String,
  body_code        : String = StringUtil.randomId(MPersonDomainAuthz.BODY_CODE_LEN),
  date_created_utc : DateTime = DateTime.now,
  var date_last_checked : Option[DateTime] = None,
  var verify_info       : Option[String] = None
) {

  // Связи с другими моделями.
  def domain = MDomain.getForDkey(dkey).get
  def person = MPerson.getByEmail(person_id).get

  /**
   * Сохранить текущий экземпляр класса в базу.
   */
  def save() {
    val path = MPersonDomainAuthz.dkeyPersonPath(dkey, person_id)
    JsonDfsBackend.writeTo(path, this)
  }

}


// Статическая часть модели
object MPersonDomainAuthz extends Logs {

  // Длина кода валидации
  val BODY_CODE_LEN = 16

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

}