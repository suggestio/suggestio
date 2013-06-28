package util

import org.apache.hadoop.fs.{FileSystem, Path}
import io.suggest.model.JsonDfsBackend
import SiobixFs.fs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.05.13 14:59
 * Description: функции-хелперы для dfs-моделей.
 */

object DfsModelUtil extends Logs {

  /**
   * Аккуратненько прочитать файл. Если файла нет или чтение не удалось, то в логах будет экзепшен и None в результате.
   * @param path путь, который читать.
   * @return Option[A]
   */
  def readOne[A: Manifest](path:Path, fs:FileSystem = fs) : Option[A] = {
    try {
      JsonDfsBackend.getAs[A](path, fs)
    } catch {
      case ex:Throwable =>
        logger.error("Cannot read/parse json data from " + path, ex)
        None
    }
  }


  /**
   * Враппер над readOne для удобства вызова из foldLeft()().
   * @param acc аккамулятор типа List[MDomainPerson]
   * @param path путь, из которого стоит читать данные
   * @return аккамулятор
   */
  def readOneAcc[A: Manifest](acc:List[A], path:Path, fs:FileSystem = fs) : List[A] = {
    readOne(path, fs) match {
      case Some(mdp) => mdp :: acc
      case None => acc
    }
  }


  /**
   * Удалить файл или пустую папку нерекурсивно и подавляя исключения.
   * @param path путь
   * @return true, если удаление прошло успешно. Иначе false.
   */
  def deleteNr(path:Path): Boolean = {
    try {
      fs.delete(path, false)
    } catch {
      case ex:Throwable =>
        logger.warn("Failed to delete DFS entry %s. Exception supressed." format path, ex)
        false
    }
  }

}


// Повторяющиеся куски кода для статических объектов моделей.
trait DfsModelStaticT {

  // Имя файла, под именем которого сохраняется всё добро. Имена объектов обычно содержат $ на конце, поэтому это удаляем.
  val filename = getClass.getCanonicalName.replace("$", "")

  /**
   * Сгенерить DFS-путь для указанного сайта и класса модели.
   * @param dkey ключ домена сайта.
   * @return Путь.
   */
  def getPath(dkey:String) : Path = {
    new Path(SiobixFs.dkeyPathConf(dkey), filename)
  }
}
