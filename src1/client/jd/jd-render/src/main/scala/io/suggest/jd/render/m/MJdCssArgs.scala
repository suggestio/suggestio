package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.jd.tags.IDocTag
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 20:42
  * Description: Модель аргументов для рендера [[io.suggest.jd.render.v.JdCss]].
  */
object MJdCssArgs {

  /** Поддержка FastEq для инстансов [[MJdCssArgs]]. */
  implicit object MJdCssArgsFastEq extends FastEq[MJdCssArgs] {
    override def eqv(a: MJdCssArgs, b: MJdCssArgs): Boolean = {
      (a.templates ===* b.templates) &&
        (a.conf ===* b.conf) &&
        (a.edges ===* b.edges)
    }
  }

  def singleCssArgs(template: IDocTag, conf: MJdConf, edges: Map[EdgeUid_t, MEdgeDataJs]): MJdCssArgs = {
    MJdCssArgs(
      templates = template :: Nil,
      conf      = conf,
      edges     = edges
    )
  }

  implicit def univEq: UnivEq[MJdCssArgs] = UnivEq.derive

}


/** Класс контейнера данных для рендера CSS-шаблона [[io.suggest.jd.render.v.JdCss]].
  *
  * @param templates Все документы.
  * @param conf Конфигурация рендеринга.
  * @param edges Текущая карта эджей.
  */
case class MJdCssArgs(
                       templates  : Seq[IDocTag],
                       conf       : MJdConf,
                     // TODO От эджей требуются лишь минимальная инфа. А тут этой инфы с избытком.
                       edges      : Map[EdgeUid_t, MEdgeDataJs]
                     )
