package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.common.html.HtmlConstants._
import io.suggest.jd.tags.MJdTagNames
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
      (a.info ===* b.info) &&
      (a.title ===* b.title)
    }
  }


  @inline implicit def univEq: UnivEq[MJdDataJs] = UnivEq.derive

  /** Сборка на основе MJdAdData. */
  def fromJdData( jdAdData: MJdData, info: MScAdInfo = MScAdInfo.empty ): MJdDataJs = {
    apply(
      edges    = MEdgeDataJs.jdEdges2EdgesDataMap( jdAdData.edges ),
      info     = info,
      // Далее - проброс всех полей MJdData, кроме edges. TODO Может обойтись без копипаста?
      doc      = jdAdData.doc,
      title    = jdAdData.title,
    )
  }

  def doc      = GenLens[MJdDataJs](_.doc)
  def edges    = GenLens[MJdDataJs](_.edges)
  def info     = GenLens[MJdDataJs](_.info)
  def title    = GenLens[MJdDataJs]( _.title )


  implicit final class JdDataJsExt( private val jdDataJs: MJdDataJs ) extends AnyVal {

    /** Открытая карточка подразумевает, что она содержит документ с блоками.
      * false значит, что тут только main-блок и без оборачивающего документа.
      */
    def isOpened: Boolean =
      jdDataJs.doc.template.rootLabel.name ==* MJdTagNames.DOCUMENT

  }

}



/** Данные для рендера одного блока плитки.
  *
  * @param edges Карта js-эджей для рендера.
  */
final case class MJdDataJs(
                            doc         : MJdDoc,
                            edges       : Map[EdgeUid_t, MEdgeDataJs],
                            info        : MScAdInfo,
                            title       : Option[String],
                          )
  extends OptId[String]
{

  // TODO Убрать это отсюда?
  override def id = doc.tagId.nodeId

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
