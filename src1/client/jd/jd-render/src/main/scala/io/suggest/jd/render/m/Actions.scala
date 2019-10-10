package io.suggest.jd.render.m

import com.github.souporserious.react.measure.ContentRect
import io.suggest.jd.tags.JdTag
import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 12:55
  * Description: Экшены jd-редактора.
  */
trait IJdAction extends DAction


/** Сигнал о выполнении измерения размеров qd-контента. */
case class QdBoundsMeasured(jdTag: JdTag, contentRect: ContentRect) extends IJdAction


/** Принудительный пересчёт плитки.
  * Поддержка должна быть реализована на уровне каждой конкретной circuit.
  */
case object GridRebuild extends IJdAction
