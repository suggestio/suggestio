package io.suggest.sc.index

import io.suggest.color.MColors
import io.suggest.common.empty.EmptyUtil
import io.suggest.geo.{MGeoLoc, MGeoPoint}
import io.suggest.geo.MGeoPoint.JsonFormatters.QS_OBJECT
import io.suggest.media.MMediaInfo
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
    (__ \ "t").formatNullable[MNodeType] and
    (__ \ "n").formatNullable[String] and
    (__ \ "c").formatNullable[MColors]
      .inmap[MColors](
        EmptyUtil.opt2ImplMEmptyF( MColors ),
        EmptyUtil.someF,
        // TODO Заменить someF/Some.apply на опциональный EmptyUtil.implEmpty2OptF, когда придёт время.
        //      2020-11-19: Т.к. приложения, размещенные на сервисах дистрибуции (v2.0), пока не готовы, и надо повременить.
      ) and
    (__ \ "l").formatNullable[MMediaInfo] and
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

    /** Выбор логотипа с приоритетом на wcFg. */
    def wcFgOrLogo: Option[MMediaInfo] = {
      a.welcome
        .flatMap( _.fgImage )
        .orElse( a.logoOpt )
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
  * @param ntype None используется при запросах на сервер.
  *              Хотя теоретически возможно и использование для эфемерных узлов на сервере.
  */
case class MSc3IndexResp(
                         nodeId     : Option[String],
                         ntype      : Option[MNodeType],
                         name       : Option[String],
                         colors     : MColors               = MColors.empty,
                         logoOpt    : Option[MMediaInfo]    = None,
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
  def nameOrIdOpt: Option[String] =
    name orElse nodeId

  /** Отображаемое имя узла, либо пустая строка. */
  def nameOrIdOrEmpty: String = nameOrIdOpt getOrElse ""

  def idOrNameOrEmpty: String = {
    nodeId
      .orElse( name )
      .getOrElse( "" )
  }

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
      ntype foreach render0
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

