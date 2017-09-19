package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.jd.tags.IDocTag

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
      (a.templates eq b.templates) &&
        (a.conf eq b.conf)
    }
  }

  def singleCssArgs(template: IDocTag, conf: MJdConf): MJdCssArgs = {
    MJdCssArgs(
      templates = template :: Nil,
      conf      = conf
    )
  }

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
