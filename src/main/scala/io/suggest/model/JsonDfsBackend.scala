package io.suggest.model

import org.apache.hadoop.fs.{Path, FileSystem}
import io.suggest.util.JacksonWrapper

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.13 14:58
 * Description: Есть редко-изменяемые структурированные данные, которые удобно хранить в json.
 * Тут модель для сериализации/десериализации и хранения этих данных в hdfs.
 */

// Статическая часть модели
object JsonDfsBackend {

  // Имя директории для хранения json-ов. После запуска нельзя менять, иначе записи потеряются.
  val subdir = "conf_json"

  // Корневая ФС
  protected var _root_fs : FileSystem = null

  /**
   * Выставляется fs. Это вместо конструктора.
   * @param root_fs - корень, используемый для outdir в bixo.
   */
  def setFs(root_fs:FileSystem) {
    _root_fs = root_fs
  }


  /**
   * Прочитать ранее сохраненное в json состояние из хранилища.
   * @param dkey ключ домена сайта
   * @param name имя состояния
   * @tparam T тип для рефлексии, обязателен.
   * @return Опциональное нечто типа T
   */
  def getAs[T: Manifest](dkey:String, name:String) : Option[T] = {
    val path = getPath(dkey, name)
    _root_fs.exists(path) match {
      case false => None

      case true =>
        val is = _root_fs.open(path)
        try {
          Some(JacksonWrapper.deserialize[T](is))
        } finally {
          is.close()
        }
    }
  }


  /**
   * Серилизовать в json и сохранить в hdfs.
   * @param dkey ключ домена сайта
   * @param name имя сохраняемого объекта
   * @param value и сам объект
   */
  def writeTo(dkey:String, name:String, value:Any) {
    val path = getPath(dkey, name)
    val os = _root_fs.create(path, true)
    try {
      JacksonWrapper.serialize(os, value)
    } finally {
      os.close()
    }
  }


  /**
   * Функция генерации пути.
   * @param dkey ключ домена сайта
   * @param name имя сохраняемого состяния. Обычно имя класса.
   * @return Path, относительный по отношению к _root_fs (и любым другим fs)
   */
  def getPath(dkey:String, name:String) = new Path(dkey + "/" + subdir + "/" + name + ".json")

}