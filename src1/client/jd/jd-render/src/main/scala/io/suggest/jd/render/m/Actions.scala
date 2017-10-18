package io.suggest.jd.render.m

import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.tags.IDocTag
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
case class JdTagSelect(jdTag: IDocTag ) extends IJdAction


/** Начато перетаскивание тега. */
case class JdTagDragStart( jdTag: IDocTag ) extends IJdAction
case class JdTagDragEnd( jdTag: IDocTag ) extends IJdAction


/** Текущий перетаскиваемый элемент был дропнут в указанный strip.
  *
  * @param strip Целевой стрип, на который сброшен jd-тег.
  * @param clXy Координаты точки сброса.
  * @param foreignTag Если тег притащен откуда-то извне, то тут Some() с распарсенным doc-tag'ом.
  */
case class JdDropContent(strip: IDocTag, clXy: MCoords2di, foreignTag: Option[IDocTag]) extends IJdAction


/** Экшен сброса текущего перетаскиваемого стрипа на новый стрип.
  *
  * @param targetStrip Целевой стрип, на который сброшен другой стрип.
  * @param isUpper true, если дроп выше середины текущего стрипа.
  *                false, если ниже середины.
  */
case class JdDropStrip(targetStrip: IDocTag, isUpper: Boolean) extends IJdAction


/** Уведомить систему о ширине и длине загруженной картинки. */
case class SetImgWh(edgeUid: EdgeUid_t, wh: MSize2di) extends IJdAction
