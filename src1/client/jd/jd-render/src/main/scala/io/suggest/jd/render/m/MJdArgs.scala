package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.jd.render.v.JdCss
import io.suggest.jd.tags.JdTag
import io.suggest.scalaz.NodePath_t
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import io.suggest.scalaz.ZTreeUtil._

import scalaz.{Tree, TreeLoc}

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
      (a.template       ===* b.template) &&
        (a.renderArgs   ===* b.renderArgs) &&
        (a.jdCss        ===* b.jdCss) &&
        (a.conf         ===* b.conf) &&
        (a.selPath      ===* b.selPath) &&
        (a.dnd          ===* b.dnd)
    }
  }

  implicit def univEq: UnivEq[MJdArgs] = UnivEq.derive

}


/** Класс-контейнер данных для рендера html.
  *
  * @param template Шаблон для рендера.
  * @param renderArgs Контейнер параметров для рендера конкретно этого шаблона.
  * @param jdCss css для рендера.
  * @param conf Общий конфиг рендеринга.
  * @param selPath Путь до текущего выделенного элемента (с которым происходит взаимодействие юзера в редакторе).
  * @param dnd Состояние драг-н-дропа, который может прийти сюда из неизвестности.
  */
case class MJdArgs(
                    template     : Tree[JdTag],
                    renderArgs   : MJdRenderArgs,
                    jdCss        : JdCss,
                    conf         : MJdConf,
                    selPath      : Option[NodePath_t]   = None,
                    dnd          : MJdDndS              = MJdDndS.empty
                  ) {

  def withJdCss(jdCss: JdCss)                       = copy(jdCss = jdCss)
  def withTemplate(template: Tree[JdTag])         = copy(template = template)
  def withRenderArgs(renderArgs: MJdRenderArgs)     = copy(renderArgs = renderArgs)
  def withConf(conf: MJdConf)                       = copy(conf = conf)
  def withSelPath(selPath: Option[NodePath_t])      = copy(selPath = selPath)
  def withDnd(dnd: MJdDndS = MJdDndS.empty)         = copy(dnd = dnd)

  /** Выяснить TreeLoc текущего выбранного узла в дереве. */
  lazy val selectedTagLoc: Option[TreeLoc[JdTag]] = {
    selPath.flatMap { template.pathToNode }
  }

  /** Текущий выбранный тег с его поддеревом. */
  lazy val selectedTag: Option[Tree[JdTag]] = {
    selectedTagLoc.map(_.tree)
  }

  /** Аналог selectedTagLoc, но для перетаскиваемого тега. */
  lazy val draggingTagLoc: Option[TreeLoc[JdTag]] = {
    dnd.jdt.flatMap( template.pathToNode )
  }

}

