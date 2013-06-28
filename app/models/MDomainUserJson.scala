package models

import util.SiobixFs.fs
import com.google.common.io.CharStreams
import java.io.{PrintWriter, InputStreamReader}
import util.{DkeyModelT, DfsModelStaticT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.13 16:35
 * Description: Модель хранения пользовательских настроек в виде сырого json, который пишется и читается только на клиенте.
 * JSON имеет произвольный формат, настройки генерируются через редактор и разбираются там же на клиентах.
 */

case class MDomainUserJson(
  dkey: String,
  data: String
) extends DkeyModelT {

  import MDomainUserJson.getPath

  /**
   * Сохранить экземпляр класса в хранилище.
   * @return Текущий экземпляр класса.
   */
  def save : MDomainUserJson = {
    val path = getPath(dkey)
    if (data.isEmpty) {
      fs.delete(path, false)
    } else {
      val os = fs.create(path, true)
      try {
        new PrintWriter(os).print(data)

      } finally {
        os.close()
      }
    }
    this
  }

  override def domainUserJson: Option[MDomainUserJson] = Some(this)
}


object MDomainUserJson extends DfsModelStaticT {

  /**
   * Прочитать сырые данные для домена.
   * @param dkey ключ домена
   * @return Экземпляр сабжа, если такой найден в хранилищах.
   */
  def getForDkey(dkey:String) : Option[MDomainUserJson] = {
    val path = getPath(dkey)
    fs.exists(path) match {
      case true =>
        val is = fs.open(path)
        try {
          // Прочитать весь файл в строку
          val result = new MDomainUserJson(
            dkey = dkey,
            data = CharStreams.toString(new InputStreamReader(is, "UTF-8"))
          )
          Some(result)

        } finally {
          is.close()
        }

      case false => None
    }
  }

}
