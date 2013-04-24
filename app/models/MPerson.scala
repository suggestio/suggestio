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
  email : String,
  var dkeys : List[String] = List() // список доменов на панели доменов юзера
) extends MPersonLinks {

  // Линки в другие модели.
  def authzMy = MDomainPersonAuthz.getForPersonDkeys(email, dkeys)
  def authzForDomain(dkey:String) = MDomainPersonAuthz.getForPersonDkey(dkey, email)

  /**
   * Сохранить отметку о таком юзере
   * @return
   */
  def save() {
    val path = MPerson.getPath(email)
    JsonDfsBackend.writeTo(path, this)
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
  val email : String

  def isAdmin = MPerson.isAdmin(email)
}


// Статическая часть модели.
object MPerson {

  // Список емейлов админов suggest.io.
  val adminEmails = Set("konstantin.nikiforov@cbca.ru", "ilya@shuma.ru", "sasha@cbca.ru")

  // Путь хранилища модели в hdfs
  val model_path = new Path(SiobixFs.siobix_out_path, "m_person")

  /**
   * Прочитать объект Person из хранилища.
   * @param email адрес эл.почты, который вернула Mozilla Persona и который используется для идентификации юзеров.
   * @return опциональный Person.
   */
  def getByEmail(email:String) : Option[MPerson] = {
    val filePath = getPath(email)
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
   * @param email мыло
   * @return путь в ФС
   */
  def getPath(email:String) = {
    val filePath = new Path(model_path, email)
    if (filePath.getParent == model_path && filePath.getName == email)
      filePath
    else
      throw new SecurityException("Incorrect email address: " + email)
  }


  /**
   * Принадлежит ли указанный мыльник админу suggest.io?
   * @param email емейл
   * @return
   */
  def isAdmin(email:String) = adminEmails.contains(email)

}
