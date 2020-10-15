package io.suggest.sc.index

import io.suggest.color.MColors
import io.suggest.geo.{MGeoLoc, MGeoPoint}
import io.suggest.geo.MGeoPoint.JsonFormatters.QS_OBJECT
import io.suggest.media.IMediaInfo
import io.suggest.n2.node.MNodeType
import io.suggest.primo.id.OptStrId
import io.suggest.text.StringUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 18:31
  * Description: Кросс-платформенная модель данных по отображаемому узлу для нужд выдачи.
  * Под узлом тут в первую очередь подразумевается узел выдачи, а не конкретная узел-карточка.
  */
object MSc3IndexResp {

  /** Поддержка play-json сериализации. */
  implicit def MSC_NODE_INFO_FORMAT: OFormat[MSc3IndexResp] = (
    (__ \ "a").formatNullable[String] and
    (__ \ "t").format[MNodeType] and
    (__ \ "n").formatNullable[String] and
    (__ \ "c").format[MColors] and
    (__ \ "l").formatNullable[IMediaInfo] and
    (__ \ "w").formatNullable[MWelcomeInfo] and
    (__ \ "g").formatNullable[MGeoPoint] and
    (__ \ "m").formatNullable[Boolean] and
    (__ \ "o").formatNullable[MGeoLoc] and
    (__ \ "u").formatNullable[Boolean]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MSc3IndexResp] = UnivEq.derive


  implicit final class Sc3InxRespOpsExt(private val a: MSc3IndexResp) extends AnyVal {

    /** Одно и тоже место? */
    def isSamePlace(b: MSc3IndexResp): Boolean = {
      (a ===* b) || {
        (a.nodeId ==* b.nodeId) &&
        (a.geoPoint ==* b.geoPoint)
      }
    }

    /** Логотипы-заголовки индексов визуально смотряться одинаково?
      * Без учёта содержимого меню, прав доступа и т.д.
      */
    def isLogoTitleBgSame(b: MSc3IndexResp): Boolean = {
      (a.name ==* b.name) &&
      (a.colors ==* b.colors) &&
      (a.logoOpt ==* b.logoOpt)
    }

    /** Индексы выглядят одинаково?
      * Это чтобы фильтровать случаи, когда приходит дубликат индекса.
      * Такое бывает на эфемерных узлах или всяких районах.
      */
    def isLooksFullySame(b: MSc3IndexResp): Boolean = {
      (a ===* b) || {
        isLogoTitleBgSame(b) &&
        (a.isLoggedIn ==* b.isLoggedIn)
      }
    }

  }


  def geoPoint = GenLens[MSc3IndexResp]( _.geoPoint )
  def isLoggedIn = GenLens[MSc3IndexResp]( _.isLoggedIn )

}


/** Контейнер данных по узлу в интересах выдачи.
  * На всякий случай, модель максимально толерантна к данными и целиком необязательна.
  *
  * @param nodeId id узла в s.io.
  *               Для не-ресиверов (район города, итд) - None.
  * @param name Название узла (или текущего метоположения), если есть.
  * @param colors Цвета, если есть.
  * @param logoOpt Данные по логотипу-иллюстрации.
  * @param welcome Данные для рендера экрана приветствия.
  * @param isMyNode Есть ли у текущего юзера права доступа на этот узел?
  *                 None - если не проверялось (не требуется в текущем запросе).
  * @param userGeoLoc Геолокация юзера, исходя из реквеста и если запрошена в реквесте.
  * @param isLoggedIn Залогинен ли юзер.
  *                   Раньше оно было в Sc3Conf на уровне ScSite, но этот уровень живёт в приложении своей жизнью.
  *                   None означает, что сервер не раскрывает текущий статус залогиненности юзера.
  */
case class MSc3IndexResp(
                         nodeId     : Option[String],
                         ntype      : MNodeType,
                         name       : Option[String],
                         colors     : MColors,
                         logoOpt    : Option[IMediaInfo]    = None,
                         welcome    : Option[MWelcomeInfo]  = None,
                         geoPoint   : Option[MGeoPoint]     = None,
                         isMyNode   : Option[Boolean]       = None,
                         userGeoLoc : Option[MGeoLoc]       = None,
                         isLoggedIn : Option[Boolean]       = None,
                       )
  extends OptStrId
{

  override final def id = nodeId

  /** Отображаемое имя узла. По идее, None быть не должно. */
  def nameOrIdOpt = name orElse nodeId
  /** Отображаемое имя узла, либо пустая строка. */
  def nameOrIdOrEmpty = nameOrIdOpt getOrElse ""

  def idOrNameOrEmpty = nodeId.orElse(name).getOrElse("")

  /** Инфа на клиенте для рендера welcome-заголовка. */
  lazy val wcNameFgH: MWcNameFgH = {
    MWcNameFgH(
      nodeName = name,
      wcFgHeightPx = for {
        wc <- welcome
        if name.nonEmpty
        wcFg <- wc.fgImage
        wcFgWh <- wcFg.whPx
      } yield {
        wcFgWh.height
      }
    )
  }

  override def toString: String = {
    StringUtil.toStringHelper( this, 128 ) { renderF =>
      val render0 = renderF("")
      nodeId foreach render0
      render0( ntype )
      name foreach render0
      if (colors.nonEmpty)
        render0( colors )
      logoOpt foreach renderF("logo")
      welcome foreach renderF("wc")
      geoPoint foreach render0
      isMyNode foreach renderF("my")
      userGeoLoc foreach renderF("ugl")
      isLoggedIn foreach renderF("login")
    }
  }

}


object MWcNameFgH {
  lazy val empty = apply()
}
case class MWcNameFgH(
                       nodeName       : Option[String] = None,
                       wcFgHeightPx   : Option[Int] = None,
                     ) {

  override def toString: String = {
    StringUtil.toStringHelper(this, 32) { renderF =>
      val render0 = renderF("")
      nodeName foreach render0
      wcFgHeightPx foreach render0
    }
  }

}

