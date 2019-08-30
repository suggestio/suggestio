package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.common.html.HtmlConstants._
import io.suggest.jd.MJdData
import io.suggest.jd.tags.JdTag
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.primo.id.OptId
import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.17 15:38
  * Description: Модель данных для рендера блока плитки.
  */
object MJdDataJs {

  /** Поддержка FastEq для инстансов [[MJdDataJs]]. */
  implicit object MJdDataJsFastEq extends FastEq[MJdDataJs] {
    override def eqv(a: MJdDataJs, b: MJdDataJs): Boolean = {
      (a.template ===* b.template) &&
      (a.edges ===* b.edges) &&
      (a.nodeId ===* b.nodeId)
    }
  }

  @inline implicit def univEq: UnivEq[MJdDataJs] = UnivEq.derive

  /** Сборка на основе MJdAdData. */
  def apply( jdAdData: MJdData ): MJdDataJs = {
    apply(
      template  = jdAdData.template,
      edges     = MEdgeDataJs.jdEdges2EdgesDataMap( jdAdData.edges ),
      nodeId    = jdAdData.nodeId,
    )
  }

}



/** Данные для рендера одного блока плитки.
  *
  * @param template Шаблон для рендера.
  *                 Тут может быть и Strip, и Document в зависимости от ситуации.
  * @param edges Карта js-эджей для рендера.
  * @param nodeId id узла-карточки.
  */
final case class MJdDataJs(
                            template    : Tree[JdTag],
                            edges       : Map[EdgeUid_t, MEdgeDataJs],
                            nodeId      : Option[String],
                          )
  extends OptId[String]
{

  override def id = nodeId

  override def toString: String = {
    new StringBuilder( productPrefix )
      .append( `(` )
      .append( nodeId.fold("")( DIEZ + _ + COMMA) )
      // Гарантированно не рендерим дерево, хотя там вроде toString и так переопределён уже:
      .append( DIEZ ).append( COMMA )
      // Рендерим кол-во эджей вместо всей карты
      .append( edges.size ).append( "e" )
      .append( `)` )
      .toString()
  }

}
