package io.suggest.ad.edit.m.vld

import diode.FastEq
import io.suggest.ad.edit.m.pop.MAePopupsS
import io.suggest.jd.tags.JdTag
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ZTreeUtil._
import japgolly.univeq.UnivEq

import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.10.17 14:38
  * Description: Мгновенная модель для вызова [[io.suggest.ad.edit.c.JdVldAh]], состоит из некоторых частей MAeRoot.
  */
object MJdVldAh {

  /** Поддержка FastEq для инстансов [[MJdVldAh]]. */
  implicit object MJdVldAhFastEq extends FastEq[MJdVldAh] {
    override def eqv(a: MJdVldAh, b: MJdVldAh): Boolean = {
      (a.template ===* b.template) &&
        (a.edges  ===* b.edges) &&
        (a.popups ===* b.popups)
    }
  }

  implicit def univEq: UnivEq[MJdVldAh] = UnivEq.derive

}


/** Класс модели-контейнера данных для контроллера валидации.
  *
  * @param template Шаблон документа.
  * @param edges Эджи документа.
  * @param popups Доступ к попапам (чтобы можно было отрендерить ошибку).
  */
case class MJdVldAh(
                     template   : Tree[JdTag],
                     edges      : Map[EdgeUid_t, MEdgeDataJs],
                     popups     : MAePopupsS
                   )
