package io.suggest.jd.render.m

import com.github.souporserious.react.measure.Bounds
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.jd.tags.JdTag
import io.suggest.jd.tags.qd.MQdOp
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.spa.DAction
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 12:55
  * Description: Экшены jd-редактора.
  */
sealed trait IJdAction extends DAction


/** Клик по стрипу. */
case class JdTagSelect(jdTag: JdTag ) extends IJdAction


/** Начато перетаскивание тега. */
case class JdTagDragStart( jdTag: JdTag ) extends IJdAction
case class JdTagDragEnd( jdTag: JdTag ) extends IJdAction


/** Текущий перетаскиваемый элемент был дропнут в указанный strip.
  *
  * @param strip Целевой стрип, на который сброшен jd-тег.
  * @param clXy Координаты точки сброса.
  * @param foreignTag Если тег притащен откуда-то извне, то тут Some() с распарсенным doc-tag'ом.
  */
case class JdDropContent(strip: JdTag, clXy: MCoords2di, foreignTag: Option[JdTag]) extends IJdAction

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


/** Экшен сброса текущего перетаскиваемого стрипа на новый стрип.
  *
  * @param targetStrip Целевой стрип, на который сброшен другой стрип.
  * @param isUpper true, если дроп выше середины текущего стрипа.
  *                false, если ниже середины.
  */
case class JdDropStrip(targetStrip: JdTag, isUpper: Boolean) extends IJdAction


/** Сигнал ресайза контента внутри блока. */
case class CurrContentResize(widthPx: Int) extends IJdAction


/** Сигнал о ресайзе какого-то embed'а внутри текущего qd-тега. */
case class QdEmbedResize(widthPx: Int, qdOp: MQdOp, edgeUid: EdgeUid_t, heightPx: Option[Int] = None) extends IJdAction

/** Сигнал о выполнении измерения размеров qd-контента. */
case class QdBoundsMeasured(jdTag: JdTag, bounds: Bounds) extends IJdAction
