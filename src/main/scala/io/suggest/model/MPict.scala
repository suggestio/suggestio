package io.suggest.model

import org.apache.hadoop.hbase.HColumnDescriptor
import HTableModel._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.10.13 15:23
 * Description: Таблица для хранения картинок.
 */
object MPict extends HTableModel {

  val HTABLE_NAME = "pict"

  val CF_IMG_THUMB   = "a"   // Записи превьюшек для поисковой выдачи

  def CFs = Seq(CF_IMG_THUMB)

  /** Генератор дескрипторов CFок. */
  def getColumnDescriptor: PartialFunction[String, HColumnDescriptor] = {
    case cf @ CF_IMG_THUMB => cfDescSimple(cf, 1)
  }

}


trait MPictSubmodel {
  def HTABLE_NAME = MPict.HTABLE_NAME
  def HTABLE_NAME_BYTES = MPict.HTABLE_NAME_BYTES
}