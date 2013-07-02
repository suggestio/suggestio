package io.suggest.index_info

import io.suggest.util.SiobixFs.fs
import io.suggest.model.MIndexInfo._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.13 17:24
 * Description: Mii сообщает о необходимости удаления индекса.
 */

object MiiRemove extends MiiPathFilter {

  val prefix = "-"

  /**
   * Выдать список того, что выставлено на удаление.
   * @param dkey Ключ домена.
   * @return Список MiiRemove в неопределенном порядке.
   */
  def getRemoves(dkey: String) : List[MiiRemove] = {
    val path = getDkeyPath(dkey)
    fs.listStatus(path, pathFilter).toList.map { st =>
      val name = st.getPath.getName.tail
      MiiRemove(dkey=dkey, name=name)
    }
  }

}


case class MiiRemove(dkey:String, name:String) extends MiiFileT {
  val prefix: String = MiiRemove.prefix

  /**
   * Сохранить в DFS-текущий экземпляр класса.
   */
  def save = {
    fs.create(filepath, true).close()
    this
  }
}
