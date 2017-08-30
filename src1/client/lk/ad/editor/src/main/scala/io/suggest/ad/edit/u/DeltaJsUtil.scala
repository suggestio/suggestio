package io.suggest.ad.edit.u

import com.quilljs.delta.{Delta, DeltaOp}
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.Text

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.17 18:11
  * Description: Утиль для работы с quill delta.
  */
object DeltaJsUtil {

  /** Рендер текстового jd-тега в Delta-текст для запихивания в quill.
    *
    * @param t Исходный текстовый тег.
    * @param edges Карта эджей.
    * @return Скомпиленная дельта, пригодная для отправки в quill.
    */
  def text2delta(t: Text, edges: Map[Int, MJdEditEdge]): Delta = {
    // TODO Сгенерить дельту
    val op1: DeltaOp = new DeltaOp {
      override val insert: UndefOr[js.Any] = js.defined {
        "test 112345"
      }
    }
    val allOps = js.Array[DeltaOp]( op1 )
    new Delta {
      override val ops: js.Array[DeltaOp] = allOps
    }
  }


  /** Конвертация дельты из quill-редактора в jd Text и обновлённую карту эджей.
    *
    * @param d Исходная дельта.
    * @param edges0 Исходная карта эджей.
    * @return Инстанс Text и обновлённая карта эджей.
    */
  def delta2text(d: Delta, edges0: Map[Int, MJdEditEdge]): (Text, Map[Int, MJdEditEdge]) = {
    ???
  }

}

