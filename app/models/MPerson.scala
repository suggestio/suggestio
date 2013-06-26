package models

import org.apache.hadoop.fs.Path
import util.SiobixFs
import SiobixFs.fs
import io.suggest.model.JsonDfsBackend

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 18:19
 * Description: Юзер, зареганный на сайте. Как правило, админ одного или нескольких сайтов.
 * Люди различаются по email'ам. Это является их идентификаторами.
 */

case class MPerson(
  id : String,
  var dkeys : List[String] = List() // список доменов на панели доменов юзера
) extends MPersonLinks {

  // Линки в другие модели.
  def authz = MPersonDomainAuthz.getForPersonDkeys(id, dkeys)

  /**
   * Сохранить отметку о таком юзере
   * @return
   */
  def save = {
    val path = MPerson.getPath(id)
    JsonDfsBackend.writeTo(path, this)
    this
  }


  /**
   * Добавить домен в список доменов, относящихся к юзеру. Затем нужно вызвать save.
   * @param dkey
   * @return
   */
  def addDkey(dkey:String) : MPerson = {
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

  def isAdmin = MPerson.isAdmin(id)
  def authzForDomain(dkey:String) = MPersonDomainAuthz.getForPersonDkey(dkey, id)
  def allDomainsAuthz = MPersonDomainAuthz.getForPerson(id)
}


// Статическая часть модели.
object MPerson {

  // Список емейлов админов suggest.io.
  protected val adminEmails = Set("konstantin.nikiforov@cbca.ru", "ilya@shuma.ru", "sasha@cbca.ru")

  // Путь хранилища модели в hdfs
  protected val model_path = new Path(SiobixFs.siobix_conf_path, "m_person")

  /**
   * Прочитать объект Person из хранилища.
   * @param id адрес эл.почты, который вернула Mozilla Persona и который используется для идентификации юзеров.
   * @return опциональный Person.
   */
  def getById(id:String) : Option[MPerson] = {
    val filePath = getPath(id)
    fs.exists(filePath) match {
      // Файл с данными по юзеру пуст - поэтому можно его не читать, а просто сделать необходимый объект.
      case true =>
        val person = JsonDfsBackend.getAs[MPerson](filePath, fs).get
        Some(person)

      case false => None
    }
  }


  /**
   * Сгенерить путь в ФС для мыльника
   * @param person_id мыло
   * @return путь в ФС
   */
  def getPath(person_id:String) = {
    val filePath = new Path(model_path, person_id)
    if (filePath.getParent == model_path && filePath.getName == person_id)
      filePath
    else
      throw new SecurityException("Incorrect email address: " + person_id)
  }


  /**
   * Принадлежит ли указанный мыльник админу suggest.io?
   * @param email емейл
   * @return
   */
  def isAdmin(email:String) = adminEmails.contains(email)

}
