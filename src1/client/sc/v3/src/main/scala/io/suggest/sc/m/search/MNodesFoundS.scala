package io.suggest.sc.m.search

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.NonEmpty
import io.suggest.common.html.HtmlConstants
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.sc3.MScQs
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

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
      (a.req           ===* b.req) &&
      (a.reqSearchArgs ===* b.reqSearchArgs) &&
      (a.hasMore        ==* b.hasMore) &&
      (a.visible        ==* b.visible) &&
      (a.rHeightPx     ===* b.rHeightPx)
    }
  }

  @inline implicit def univEq: UnivEq[MNodesFoundS] = UnivEq.derive

  def req           = GenLens[MNodesFoundS](_.req)
  def reqSearchArgs = GenLens[MNodesFoundS](_.reqSearchArgs)
  def hasMore       = GenLens[MNodesFoundS](_.hasMore)
  def visible       = GenLens[MNodesFoundS](_.visible)
  def rHeightPx     = GenLens[MNodesFoundS](_.rHeightPx)

}


/** Класс модели состояния вкладки со списком найденных узлов (изначально - только тегов).
  *
  * @param req Состояние реквеста к серверу.
  * @param reqSearchArgs Аргументы поиска для запущенного или последнего заверщённого запроса.
  *                      Т.е. для req.pending; либо для req.ready/failed, если !pending.
  *                      Используется для проверки необходимости запуска нового запроса.
  * @param hasMore Есть ли ещё теги на сервере?
  * @param visible Текущая видимость списка результатов.
  *                Список может быть скрыт, но содержать какие-то результаты.
  * @param rHeightPx Реальная высота списка по результатам рендера (замер через react-measure).
  *                  Pot.pending - разрешает измерение.
  */
case class MNodesFoundS(
                         req                : Pot[MSearchRespInfo[MGeoNodesResp]]     = Pot.empty,
                         reqSearchArgs      : Option[MScQs]         = None,
                         hasMore            : Boolean               = true,
                         visible            : Boolean               = false,
                         rHeightPx          : Pot[Int]              = Pot.empty,
                       )
  extends NonEmpty
{

  lazy val reqOpt = req.toOption

  override def isEmpty: Boolean =
    MNodesFoundS.empty ===* this

  override final def toString: String = {
    import io.suggest.common.html.HtmlConstants._

    new StringBuilder(64)
      .append( productPrefix )
      .append( `(` )
      .append(
        req.getClass.getSimpleName + COLON +
          req.fold(0)(_.resp.nodes.length) + COLON +
          req.exceptionOption.fold("") { ex =>
            ex.getClass.getName + HtmlConstants.SPACE + ex.getMessage
          }
      )
      .append( COMMA )
      .append( hasMore )
      .append( `)` )
      .toString()
  }

  /** Быстрый доступ к кэшу для id-карты найденных узлов. */
  def nodesMap: Map[String, MSc3IndexResp] =
    req.fold( Map.empty[String, MSc3IndexResp] )(_.resp.nodesMap)

}
