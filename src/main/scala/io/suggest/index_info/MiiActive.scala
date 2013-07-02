package io.suggest.index_info

import io.suggest.util.SiobixFs.fs
import io.suggest.model.JsonDfsBackend
import io.suggest.model.MIndexInfo._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.13 17:23
 * Description: Метаданные активных используемых для индексации индексов.
 */

object MiiActive extends MiiPathFilter {

  val prefix = "@"

  /**
   * Выдать список активных индексов в неопределенном порядке.
   * @param dkey ключ домена.
   * @return Список активных индексов в неопределенном порядке.
   */
  def getActive(dkey:String) : List[MiiActive] = {
    val path = getDkeyPath(dkey)
    fs.listStatus(path, pathFilter).toList.foldLeft[List[MiiActive]] (Nil) { (acc, st) =>
      JsonDfsBackend.getAs[MiiActive](st.getPath, fs) match {
        case Some(miiActive) => miiActive :: acc
        case None => acc
      }
    }
  }

}


/**
 * Данные об активном индексе.
 * @param indexInfo Метаданные индекса.
 */
case class MiiActive(indexInfo: IndexInfo) extends MiiFileWithIiT {
  def prefix: String = MiiActive.prefix
  override def save: MiiFileWithIiT = super.save.asInstanceOf[MiiActive]
}
