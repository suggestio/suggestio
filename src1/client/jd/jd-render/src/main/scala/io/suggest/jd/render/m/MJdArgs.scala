package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.jd.MJdConf
import io.suggest.jd.render.v.JdCss
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
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
  implicit object MJdArgsFastEq extends FastEq[MJdArgs] {
    override def eqv(a: MJdArgs, b: MJdArgs): Boolean = {
      (a.template       ===* b.template) &&
        (a.edges        ===* b.edges) &&
        (a.jdCss        ===* b.jdCss) &&
        (a.conf         ===* b.conf) &&
        // Бывает, что инстансы генерятся на лету. Поэтому сравниваем глубинно:
        ((a.renderArgs ===* b.renderArgs) || MJdRenderArgs.MJdRenderArgsFastEq.eqv(a.renderArgs, b.renderArgs))
    }
  }

  implicit def univEq: UnivEq[MJdArgs] = UnivEq.derive

}


/** Класс-контейнер данных для рендера html.
  *
  * @param template Шаблон для рендера.
  * @param edges Карта данных по эджам, с сервера.
  * @param renderArgs Контейнер параметров для рендера конкретно этого шаблона.
  * @param jdCss css для рендера.
  * @param conf Общий конфиг рендеринга.
  */
case class MJdArgs(
                    template     : Tree[JdTag],
                    edges        : Map[EdgeUid_t, MEdgeDataJs],
                    jdCss        : JdCss,
                    conf         : MJdConf,
                    renderArgs   : MJdRenderArgs        = MJdRenderArgs.empty,
                    // TODO поля ниже довольно специфичны, надо унести их в renderArgs.
                  ) {

  def withTemplate(template: Tree[JdTag])           = copy(template = template)
  def withEdges(edges: Map[EdgeUid_t, MEdgeDataJs]) = copy(edges = edges)
  def withRenderArgs(renderArgs: MJdRenderArgs)     = copy(renderArgs = renderArgs)
  def withJdCss(jdCss: JdCss)                       = copy(jdCss = jdCss)
  def withConf(conf: MJdConf)                       = copy(conf = conf)

  /** Выяснить TreeLoc текущего выбранного узла в дереве. */
  lazy val selectedTagLoc: Option[TreeLoc[JdTag]] = {
    renderArgs.selPath.flatMap { template.pathToNode }
  }

  /** Текущий выбранный тег с его поддеревом. */
  lazy val selectedTag: Option[Tree[JdTag]] = {
    selectedTagLoc.map(_.tree)
  }

  /** Аналог selectedTagLoc, но для перетаскиваемого тега. */
  lazy val draggingTagLoc: Option[TreeLoc[JdTag]] = {
    renderArgs.dnd.jdt.flatMap( template.pathToNode )
  }

  /** Вычислить высоту текущего шаблона. Используется при реакции на скроллинг. */
  lazy val templateHeightCssPx: Int = {
    template
      // Считаем стрипы только на первом уровне.
      .subForest
      .iterator
      .map(_.rootLabel)
      .filter { _.name ==* MJdTagNames.STRIP }
      .flatMap(_.props1.bm)
      .map(_.height)
      .sum
  }

}

