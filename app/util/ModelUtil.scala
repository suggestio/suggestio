package util

import org.apache.hadoop.fs.{FileSystem, Path}
import io.suggest.model.JsonDfsBackend
import SiobixFs.fs
import io.suggest.util.JacksonWrapper
import java.io.{ObjectInputStream, ObjectOutputStream, ByteArrayOutputStream, ByteArrayInputStream}

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
        LOGGER.error("Cannot read/parse json data from " + path, ex)
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
        LOGGER.warn("Failed to delete DFS entry %s. Exception supressed." format path, ex)
        false
    }
  }


  // MPerson: вынесено сюда для доступа из DFS-Backend'ов DFS моделей.
  val personModelPath = new Path(SiobixFs.siobix_conf_path, "m_person")

  /**
   * Сгенерить путь в ФС для мыльника
   * @param person_id мыло
   * @return путь в ФС
   */
  def getPersonPath(person_id:String) = {
    val filePath = new Path(personModelPath, person_id)
    if (filePath.getParent == personModelPath && filePath.getName == person_id) {
      filePath
    } else {
      throw new SecurityException("Incorrect email address: " + person_id)
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


// Интерфейс для сериализаторов.
trait ModelSerialT {
  def serialize(v: Any): Array[Byte]
  def deserializeTo[T: Manifest](a: Array[Byte]): T
}

/** Сериализатор данных в json. */
trait ModelSerialJson extends ModelSerialT {
  def serialize(v: Any) = JacksonWrapper.serialize(v).getBytes
  def deserializeTo[T: Manifest](a: Array[Byte]): T = {
    val bais = new ByteArrayInputStream(a)
    JacksonWrapper.deserialize[T](bais)
  }
}

/** Сериализатор java-классов в бинари. */
trait ModelSerialJava extends ModelSerialT {
  def serialize(v: Any): Array[Byte] = {
    val baos = new ByteArrayOutputStream(255)
    val oos  = new ObjectOutputStream(baos)
    try {
      oos.writeObject(v)
    } finally {
      oos.close()
    }
    baos.toByteArray
  }

  def deserializeTo[T: Manifest](a: Array[Byte]): T = {
    val bais = new ByteArrayInputStream(a)
    val ois = new ObjectInputStream(bais)
    try {
      ois.readObject().asInstanceOf[T]
    } finally {
      ois.close()
    }
  }
}
