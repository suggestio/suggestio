package io.suggest.sc.m.search

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.NonEmpty
import io.suggest.common.html.HtmlConstants
import io.suggest.maps.nodes.MGeoNodePropsShapes
import io.suggest.primo.id.IId
import io.suggest.sc.ads.MAdsSearchReq
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.17 16:45
  * Description: Модель данных состояния поиска тегов.
  *
  * Содержит в себе как данные для рендера, так и вспомогательные данные,
  * поэтому шаблоны-компоненты должны по возможности использовать собственные модели.
  *
  * Неявно-пустя модель.
  * Но при сбросе НАДО явно ЗАМЕНЯТЬ инстанс на статический empty.
  */
object MNodesFoundS {

  val empty = apply()

  /** Поддержка FastEq для инстансов [[MNodesFoundS]]. */
  implicit object MNodesFoundSFastEq extends FastEq[MNodesFoundS] {
    override def eqv(a: MNodesFoundS, b: MNodesFoundS): Boolean = {
      (a.req             ===* b.req) &&
        (a.reqSearchArgs ===* b.reqSearchArgs) &&
        (a.hasMore        ==* b.hasMore)
    }
  }

  @inline implicit def univEq: UnivEq[MNodesFoundS] = UnivEq.derive

}


/** Класс модели состояния вкладки со списком найденных узлов (изначально - только тегов).
  *
  * @param req Состояние реквеста к серверу.
  * @param reqSearchArgs Аргументы поиска для запущенного или последнего заверщённого запроса.
  *                      Т.е. для req.pending; либо для req.ready/failed, если !pending.
  *                      Используется для проверки необходимости запуска нового запроса.
  * @param hasMore Есть ли ещё теги на сервере?
  */
case class MNodesFoundS(
                         req           : Pot[MSearchRespInfo[Seq[MGeoNodePropsShapes]]]     = Pot.empty,
                         reqSearchArgs : Option[MAdsSearchReq] = None,
                         hasMore       : Boolean               = true,
                       )
  extends NonEmpty
{

  def withReq(req: Pot[MSearchRespInfo[Seq[MGeoNodePropsShapes]]]) = copy(req = req)
  def withReqSearchArgs(reqSearchArgs : Option[MAdsSearchReq]) = copy(reqSearchArgs = reqSearchArgs)
  def withReqWithArgs(req: Pot[MSearchRespInfo[Seq[MGeoNodePropsShapes]]], reqSearchArgs : Option[MAdsSearchReq]) =
    copy(req = req, reqSearchArgs = reqSearchArgs)
  def withHasMore(hasMore: Boolean) = copy(hasMore = hasMore)

  override def isEmpty: Boolean = {
    MNodesFoundS.empty ===* this
  }

  override final def toString: String = {
    import io.suggest.common.html.HtmlConstants._

    new StringBuilder(64)
      .append( productPrefix )
      .append( `(` )
      .append(
        req.getClass.getSimpleName + COLON +
          req.fold(0)(_.resp.length) + COLON +
          req.exceptionOption.fold("") { ex =>
            ex.getClass + HtmlConstants.SPACE + ex.getMessage
          }
      )
      .append( COMMA )
      .append( hasMore )
      .append( `)` )
      .toString()
  }

  /** Сборка итератора найденных узлов. */
  def nodesFoundIter: Iterator[MGeoNodePropsShapes] = {
    req.iterator
      .flatMap(_.resp)
  }

  /** Кэш для id-карты найденных узлов. */
  lazy val nodesFoundMap: Map[String, MGeoNodePropsShapes] = {
    IId.els2idMap( nodesFoundIter )
  }

}
