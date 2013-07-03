package io.suggest.index_info

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.13 17:23
 * Description: Метаданные активных используемых для индексации индексов.
 */

object MiiActive extends MiiFileWithIiStaticT[MiiActive] {

  val prefix = "@"

  protected def toResult(dkey: String, iinfo: IndexInfo, m: MiiFileWithIi.JsonMap_t): MiiActive = {
    MiiActive(iinfo)
  }
}


/**
 * Данные об активном индексе.
 * @param indexInfo Метаданные индекса.
 */
case class MiiActive(indexInfo: IndexInfo) extends MiiFileWithIiT[MiiActive] {
  def prefix: String = MiiActive.prefix
}
