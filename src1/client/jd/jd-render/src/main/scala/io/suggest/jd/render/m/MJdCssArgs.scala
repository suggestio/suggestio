package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.common.html.HtmlConstants
import io.suggest.dev.MSzMult
import io.suggest.jd.{MJdConf, MJdDoc, MJdTagId}
import io.suggest.jd.tags.JdTag
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._

import scala.collection.immutable.HashMap

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
      (a.conf ===* b.conf) &&
      (a.jdtWideSzMults ===* b.jdtWideSzMults) &&
      (a.jdTagsById ===* b.jdTagsById)
    }
  }

  @inline implicit def univEq: UnivEq[MJdCssArgs] = UnivEq.derive

}


/** Класс контейнера данных для рендера CSS-шаблона [[io.suggest.jd.render.v.JdCss]].
  *
  * @param conf Конфигурация рендеринга.
  * @param quirks Разрешить использовать костыли, которые могут нарушить рендер за пределами плитки.
  *               Появилось, чтобы убрать position.absolute из root-контейнера.
  */
final case class MJdCssArgs(
                             conf             : MJdConf,
                             quirks           : Boolean = true,
                             jdtWideSzMults   : Map[JdTag, MSzMult],
                             jdTagsById       : HashMap[MJdTagId, JdTag],
                           ) {

  override def toString: String = {
    new StringBuilder( productPrefix )
      .append( HtmlConstants.`(` )
      .append( jdTagsById.size ).append( HtmlConstants.DIEZ ).append( HtmlConstants.COMMA )
      .append( conf )
      .append( HtmlConstants.`)` )
      .toString()
  }

}
