package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.common.html.HtmlConstants
import io.suggest.dev.MSzMult
import io.suggest.jd.MJdConf
import io.suggest.jd.tags.JdTag
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ZTreeUtil._
import scalaz.Tree

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
      (a.jdtWideSzMults ===* b.jdtWideSzMults)
    }
  }

  @inline implicit def univEq: UnivEq[MJdCssArgs] = UnivEq.derive

}


/** Класс контейнера данных для рендера CSS-шаблона [[io.suggest.jd.render.v.JdCss]].
  *
  * @param templates Все документы.
  * @param conf Конфигурация рендеринга.
  * @param quirks Разрешить использовать костыли, которые могут нарушить рендер за пределами плитки.
  *               Появилось, чтобы убрать position.absolute из root-контейнера.
  */
final case class MJdCssArgs(
                             templates        : Seq[Tree[JdTag]] = Nil,
                             conf             : MJdConf,
                             quirks           : Boolean = true,
                             jdtWideSzMults   : Map[JdTag, MSzMult],
                           ) {

  override def toString: String = {
    new StringBuilder( productPrefix )
      .append( HtmlConstants.`(` )
      .append( templates.length ).append( HtmlConstants.DIEZ ).append( HtmlConstants.COMMA )
      .append( conf )
      .append( HtmlConstants.`)` )
      .toString()
  }

}
