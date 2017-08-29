package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.jd.render.v.JdCss
import io.suggest.jd.tags.{IDocTag, JsonDocument}

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
        (a.jdCss eq b.jdCss) &&
        (a.conf eq b.conf) &&
        (a.selectedTag eq b.selectedTag)
    }
  }

  def singleCssArgs(template: JsonDocument, conf: MJdConf): MJdCssArgs = {
    MJdCssArgs(
      templates = template :: Nil,
      conf      = conf
    )
  }
  /** Сброка инстанса css-args для рендера одного текущего шаблона. */
  def singleCssArgs(jdArgs: MJdArgs): MJdCssArgs = {
    singleCssArgs(jdArgs.template, jdArgs.conf)
  }

}


/** Класс-контейнер данных для рендера html.
  *
  * @param template Шаблон для рендера.
  * @param renderArgs Контейнер параметров для рендера конкретно этого шаблона.
  * @param jdCss css для рендера.
  * @param conf Общий конфиг рендеринга.
  * @param selectedTag Текущий выделенный элемент, с которым происходит взаимодействие юзера.
  */
case class MJdArgs(
                    template     : JsonDocument,
                    renderArgs   : MJdRenderArgs,
                    jdCss        : JdCss,
                    conf         : MJdConf,
                    selectedTag  : Option[IDocTag]   = None
                  ) {

  def withJdCss(jdCss: JdCss) = copy(jdCss = jdCss)
  def withTemplate(template: JsonDocument) = copy(template = template)
  def withRenderArgs(renderArgs: MJdRenderArgs) = copy(renderArgs = renderArgs)
  def withConf(conf: MJdConf) = copy(conf = conf)
  def withSelectedTag(selectedTag: Option[IDocTag]) = copy(selectedTag = selectedTag)

}

