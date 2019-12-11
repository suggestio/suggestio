package io.suggest.jd.edit.m

import com.github.react.dnd.IItem
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.jd.MJdTagId
import io.suggest.jd.render.m.IJdAction
import io.suggest.jd.tags.JdTag
import io.suggest.jd.tags.qd.MQdOp
import io.suggest.model.n2.edge.EdgeUid_t
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.2019 21:53
  */

sealed trait IJdEditAction extends IJdAction


/** Клик по стрипу.
  * @param silent Постараться не шуметь и не лезть в иные части состояния.
  *               Нужно для авто-сброса MDndS или запрета этого сброса:
  */
case class JdTagSelect(jdTag: JdTag, jdId: MJdTagId, silent: Boolean = false) extends IJdAction


/** Начато перетаскивание тега. */
case class JdTagDragStart( jdTag: JdTag, jdId: MJdTagId ) extends IJdAction
case object JdTagDragEnd extends IJdAction


/** Текущий перетаскиваемый элемент был дропнут в указанный strip.
  *
  * @param targetBlock Целевой стрип, на который сброшен jd-тег.
  * @param clXy Координаты точки сброса.
  * @param foreignTag Если тег притащен откуда-то извне, то тут Some() с распарсенным doc-tag'ом.
  */
case class JdDropToBlock(targetBlock: JdTag, clXy: MCoords2di, foreignTag: Option[JdTag]) extends IJdAction


/** Сменить слой для текущего выделенного контента.
  *
  * @param up true - вверх, false - вниз.
  * @param bounded true - в самый конец или в самое начало стопки слоёв.
  *                false - на один слой вверх или вниз.
  */
case class JdChangeLayer(up: Boolean, bounded: Boolean) extends IJdAction
object JdChangeLayer {
  @inline implicit def univEq: UnivEq[JdChangeLayer] = UnivEq.derive
}


/** Экшен окончания перетаскивания текущего стрипа на новое место в документе.
  * @param docXy Координата сброса внутри документа (координата мыши).
  */
case class JdDropToDocument(docXy: MCoords2di, dropItem: IItem) extends IJdEditAction


/** Сигнал ресайза контента внутри блока. */
case class SetContentWidth(widthPx: Option[Int]) extends IJdAction


/** Сигнал о ресайзе какого-то embed'а внутри текущего qd-тега. */
case class QdEmbedResize(widthPx: Int, qdOp: MQdOp, edgeUid: EdgeUid_t, heightPx: Option[Int] = None) extends IJdAction


