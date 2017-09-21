package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.jd.tags.IDocTag
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
        (a.conf ===* b.conf)
    }
  }

  def singleCssArgs(template: IDocTag, conf: MJdConf): MJdCssArgs = {
    MJdCssArgs(
      templates = template :: Nil,
      conf      = conf
    )
  }

  implicit def univEq: UnivEq[MJdCssArgs] = UnivEq.derive

}


/** Класс контейнера данных для рендера CSS-шаблона [[io.suggest.jd.render.v.JdCss]].
  *
  * @param templates Все документы.
  * @param conf Конфигурация рендеринга.
  */
case class MJdCssArgs(
                       templates  : Seq[IDocTag],
                       conf       : MJdConf
                     )
