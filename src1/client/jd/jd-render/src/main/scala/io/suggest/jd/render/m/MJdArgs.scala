package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.jd.tags.JsonDocument

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 15:56
  * Description: Данные по документу для рендера JSON-документа.
  */
object MJdArgs {

  /** Поддержка FastEq для инстансов [[MJdArgs]]. */
  implicit object MJdWithArgsFastEq extends FastEq[MJdArgs] {
    override def eqv(a: MJdArgs, b: MJdArgs): Boolean = {
      (a.template eq b.template) &&
        (a.renderArgs eq b.renderArgs) &&
        (a.conf eq b.conf)
    }
  }

}


/** Класс-контейнер данных для рендера html.
  *
  * @param template Шаблон для рендера.
  * @param renderArgs Контейнер параметров для рендера конкретно этого шаблона.
  * @param conf Общий конфиг рендеринга.
  */
case class MJdArgs(
                    template           : JsonDocument,
                    renderArgs         : MJdRenderArgs,
                    conf               : MJdConf
                  )
{

  def withTemplate(template: JsonDocument) = copy(template = template)
  def withRenderArgs(renderArgs: MJdRenderArgs) = copy(renderArgs = renderArgs)
  def withConf(conf: MJdConf) = copy(conf = conf)

  /** Аргументы для CSS-рендера ровно одного (текущего) документа. */
  lazy val singleCssArgs = MJdCssArgs(
    templates  = template :: Nil,
    conf = conf
  )

}

