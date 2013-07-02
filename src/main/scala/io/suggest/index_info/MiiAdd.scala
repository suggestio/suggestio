package io.suggest.index_info

import io.suggest.model.MIndexInfo._
import io.suggest.util.SiobixFs.fs
import io.suggest.model.JsonDfsBackend

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.13 17:24
 * Description: Файл с директивами добавления нового индекса.
 */

object MiiAdd extends MiiPathFilter {
  val prefix = "+"

  /**
   * Прочитать все добавляемые индексы.
   * @param dkey Ключ домена.
   * @return Список MiiAdd в неопределенном порядке.
   */
  def getAdds(dkey:String): List[MiiAdd] = {
    val dirPath = getDkeyPath(dkey)
    fs.listStatus(dirPath, pathFilter)
      .toList
      .foldLeft[List[MiiAdd]] (Nil) { (acc, st) =>
        val onePath = st.getPath
        JsonDfsBackend.getAs[MiiAdd](onePath, fs) match {
          case Some(miiAdd) => miiAdd :: acc
          case None         => acc
        }
      }
  }

}


/**
 * Файл, обозначающий необходимость подключения нового индекса.
 * @param indexInfo Метаданные индекса.
 * @param isIndexAlreadyExist Существует ли уже указанный индекс в elasticsearch? Иначе его надо будет создать.
 * @param useForSearch Если true, то этот индекс должен использоваться для поиска.
 */
case class MiiAdd(indexInfo: IndexInfo, isIndexAlreadyExist:Boolean, useForSearch:Boolean = false) extends MiiFileWithIiT {
  def prefix: String = MiiAdd.prefix
  override def save: MiiFileWithIiT = super.save.asInstanceOf[MiiAdd]

  /**
   * Сконвертить в ActiveMII.
   * @return Экземпляр ActiveMII с текущим indexInfo.
   */
  def toActive = MiiActive(indexInfo)
}
