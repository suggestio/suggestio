package io.suggest.jd.render.m

import io.suggest.jd.tags.{IDocTag, Strip}
import io.suggest.sjs.common.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 12:55
  * Description: Экшены jd-редактора.
  */
sealed trait IJdAction extends DAction


/** Клик по какому-то элементу json-документа. */
sealed trait IJdTagClick extends IJdAction {
  def jdTag: IDocTag
}


/** Клик по стрипу. */
case class StripClick( override val jdTag: Strip ) extends IJdTagClick
