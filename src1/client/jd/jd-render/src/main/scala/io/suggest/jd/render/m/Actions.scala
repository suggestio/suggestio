package io.suggest.jd.render.m

import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.tags.JdTag
import io.suggest.jd.tags.qd.MQdOp
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.spa.DAction

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


/** Экшен сброса текущего перетаскиваемого стрипа на новый стрип.
  *
  * @param targetStrip Целевой стрип, на который сброшен другой стрип.
  * @param isUpper true, если дроп выше середины текущего стрипа.
  *                false, если ниже середины.
  */
case class JdDropStrip(targetStrip: JdTag, isUpper: Boolean) extends IJdAction


/** Уведомить систему о ширине и длине загруженной картинки. */
case class SetImgWh(edgeUid: EdgeUid_t, wh: MSize2di) extends IJdAction


/** Сигнал ресайза контента внутри блока. */
case class CurrContentResize(widthPx: Int) extends IJdAction


/** Сигнал о ресайзе какого-то embed'а внутри текущего qd-тега. */
case class QdEmbedResize(widthPx: Int, qdOp: MQdOp, edgeUid: EdgeUid_t) extends IJdAction
