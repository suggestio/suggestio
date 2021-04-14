package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.jd.tags.JdTag
import io.suggest.jd.{MJdConf, MJdTagId}
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq
import monocle.macros.GenLens
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
      (a.conf ===* b.conf) &&
      (a.data ===* b.data) &&
      (a.tplsIndexed ===* b.tplsIndexed) &&
      (a.nameGen eq b.nameGen)
    }
  }

  @inline implicit def univEq: UnivEq[MJdCssArgs] = UnivEq.derive

  def data = GenLens[MJdCssArgs](_.data)

  def ABS_BLOCKS_DFLT = true

}


/** Класс контейнера данных для рендера CSS-шаблона [[io.suggest.jd.render.v.JdCss]].
  *
  * @param conf Конфигурация рендеринга.
  * @param absBlocks Разрешить использовать костыли, которые могут нарушить рендер за пределами плитки.
  *                  Появилось, чтобы убрать position.absolute из root-контейнера.
  * @param data Общие рантаймовые (с JdCss) данными.
  * @param nameGen Чтобы разделять идентификаторы css-классов между разными JdCss на странице,
  *                можно задействовать эту функция для заворачивания названий css-классов.
  */
final case class MJdCssArgs(
                             conf             : MJdConf,
                             data             : MJdRuntimeData,
                             tplsIndexed      : Seq[Tree[(MJdTagId, JdTag)]],
                             absBlocks        : Boolean = MJdCssArgs.ABS_BLOCKS_DFLT,
                             nameGen          : Option[String => String] = None,
                           )
