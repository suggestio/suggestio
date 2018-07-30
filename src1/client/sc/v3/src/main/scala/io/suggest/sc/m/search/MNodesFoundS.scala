package io.suggest.sc.m.search

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.NonEmpty
import io.suggest.maps.nodes.MGeoNodePropsShapes
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.17 16:45
  * Description: Модель состояния вкладки со списком тегов.
  *
  * Для простоты, модель следует считать неявно-пустой.
  * Но при сбросе НАДО явно ЗАМЕНЯТЬ инстанс на статический empty.
  */
object MNodesFoundS {

  val empty = apply()

  /** Поддержка FastEq для инстансов [[MNodesFoundS]]. */
  implicit object MNodesFoundSFastEq extends FastEq[MNodesFoundS] {
    override def eqv(a: MNodesFoundS, b: MNodesFoundS): Boolean = {
      (a.req            ===*  b.req) &&
        (a.hasMore       ==*  b.hasMore) &&
        (a.selectedId   ===*  b.selectedId)
    }
  }

  implicit def univEq: UnivEq[MNodesFoundS] = UnivEq.derive

}


/** Класс модели состояния вкладки со списком найденных узлов (изначально - только тегов).
  *
  * @param req Состояние реквеста к серверу.
  * @param selectedId Выбранный тег (узел) в списке.
  * @param hasMore Есть ли ещё теги на сервере?
  */
case class MNodesFoundS(
                         req           : Pot[MSearchRespInfo[Seq[MGeoNodePropsShapes]]]     = Pot.empty,
                         hasMore       : Boolean               = true,
                         // TODO Когда станет допустимо сразу несколько тегов, надо заменить на Set[String].
                         selectedId    : Option[String]        = None
                       )
  extends NonEmpty
{

  def withReq(req: Pot[MSearchRespInfo[Seq[MGeoNodePropsShapes]]]) = copy(req = req)
  def withHasMore(hasMore: Boolean) = copy(hasMore = hasMore)
  def withSelectedId(selectedId: Option[String]) = copy(selectedId = selectedId)

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
          req.exceptionOption.fold("")(ex => ex.getClass + " " + ex.getMessage)
      )
      .append( COMMA )
      .append( hasMore ).append( COMMA )
      .append( selectedId )
      .append( `)` )
      .toString()
  }

}
