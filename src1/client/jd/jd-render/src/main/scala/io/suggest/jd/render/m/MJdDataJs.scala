package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.common.html.HtmlConstants._
import io.suggest.jd.{MJdData, MJdDoc}
import io.suggest.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.primo.id.OptId
import io.suggest.sc.ads.MScAdInfo
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

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
      ((a.doc ===* b.doc) || MJdDoc.MJdDocFastEq.eqv(a.doc, b.doc)) &&
      (a.edges ===* b.edges) &&
      (a.info ===* b.info)
    }
  }


  @inline implicit def univEq: UnivEq[MJdDataJs] = UnivEq.derive

  /** Сборка на основе MJdAdData. */
  def fromJdData( jdAdData: MJdData, info: MScAdInfo = MScAdInfo.empty ): MJdDataJs = {
    apply(
      doc      = jdAdData.doc,
      edges    = MEdgeDataJs.jdEdges2EdgesDataMap( jdAdData.edges ),
      info     = info,
    )
  }

  val doc      = GenLens[MJdDataJs](_.doc)
  val edges    = GenLens[MJdDataJs](_.edges)

}



/** Данные для рендера одного блока плитки.
  *
  * @param edges Карта js-эджей для рендера.
  */
final case class MJdDataJs(
                            doc         : MJdDoc,
                            edges       : Map[EdgeUid_t, MEdgeDataJs],
                            info        : MScAdInfo,
                          )
  extends OptId[String]
{

  // TODO Убрать это отсюда?
  override def id = doc.jdId.nodeId

  override def toString: String = {
    new StringBuilder( productPrefix )
      .append( `(` )
      .append( id.fold("")( DIEZ + _ + COMMA) )
      // Гарантированно не рендерим дерево, хотя там вроде toString и так переопределён уже:
      .append( DIEZ ).append( COMMA )
      // Рендерим кол-во эджей вместо всей карты
      .append( edges.size ).append( "e" )
      .append( `)` )
      .toString()
  }

}
