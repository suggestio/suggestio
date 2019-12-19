package io.suggest.model.n2.node.meta

import boopickle.Default._
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty, OptionUtil}
import io.suggest.err.ErrorConstants
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.proto.http.HttpConst
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import io.suggest.text.UrlUtil2
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 17:58
  * Description: Кросс-платформенная модель бизнес-инфы по узлу.
  */

object MBusinessInfo extends IEmpty with IEsMappingProps {

  override type T = MBusinessInfo

  object Fields {
    val SITE_URL_FN             = "su"
    val AUDIENCE_DESCR_FN       = "ad"
    val HUMAN_TRAFFIC_INT_FN    = "ht"
    val HUMAN_TRAFFIC_FN        = "hu"
    /** Имя поля для описания серьезного бизнеса: Business DESCRiption. */
    val BDESCR_FN               = "bd"
  }

  /** Поддержка JSON. */
  implicit val MBUSINESS_INFO_FORMAT: OFormat[MBusinessInfo] = {
    val F = Fields
    // TODO 2018-04-06 Удалить потом. Миграция с Int на String-поле
    val humanTrafficFormat0 = {
      val pathStr = (__ \ F.HUMAN_TRAFFIC_FN)
      val r = pathStr.read[String]
        .map( EmptyUtil.someF )
        .orElse {
          (__ \ F.HUMAN_TRAFFIC_INT_FN).readNullable[Int]
            .map(_.map(_.toString))
        }
      val w = pathStr.writeNullable[String]
      OFormat( r, w )
    }

    (
      (__ \ F.SITE_URL_FN).formatNullable[String] and
      (__ \ F.AUDIENCE_DESCR_FN).formatNullable[String] and
      humanTrafficFormat0 and
      (__ \ F.BDESCR_FN).formatNullable[String]
    )(apply, unlift(unapply))
  }


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields

    Json.obj(
      F.SITE_URL_FN -> FText(
        index = someTrue,
        boost = Some(0.33)
      ),
      F.AUDIENCE_DESCR_FN -> FText( index = someTrue ),
      F.HUMAN_TRAFFIC_INT_FN -> FNumber(
        typ   = DocFieldTypes.Integer,
        index = someTrue,
      ),
      F.BDESCR_FN -> FText(
        index = someTrue,
        boost = Some(0.1)
      )
    )
  }


  /** Частоиспользуемый пустой экземпляр модели [[MBusinessInfo]]. */
  override val empty = MBusinessInfo()

  implicit val mBusinessInfoPickler: Pickler[MBusinessInfo] = {
    generatePickler[MBusinessInfo]
  }

  @inline implicit def univEq: UnivEq[MBusinessInfo] = UnivEq.derive

  def validateSiteUrl(siteUrl: Option[String]): StringValidationNel[Option[String]] = {
    ScalazUtil.liftNelOpt(siteUrl) { url =>
      val urlPrefix = "url."
      (
        UrlUtil2.validateUrl(url, urlPrefix + ErrorConstants.Words.INVALID) |@|
        Validation.liftNel(url)(
          u => !u.startsWith( HttpConst.Proto.HTTP ),
          urlPrefix + HttpConst.Proto.HTTP + ErrorConstants.Words.EXPECTED
        )
      ) { (u2, _) => u2 }
    }
  }

  def validateInfo(info: Option[String]): StringValidationNel[Option[String]] =
    ScalazUtil.validateTextOpt(info, maxLen = 1000, "info")

  def validateAudienceDescr(audienceDescr: Option[String]): StringValidationNel[Option[String]] =
    ScalazUtil.validateTextOpt(audienceDescr, maxLen = 300, "audsc")

  def validateHumanTraffic(humanTraffic: Option[String]): StringValidationNel[Option[String]] =
    ScalazUtil.validateTextOpt(humanTraffic, maxLen = 120, "htraf")

  def validate(mbi: MBusinessInfo): ValidationNel[String, MBusinessInfo] = {
    (
      validateSiteUrl( mbi.siteUrl ) |@|
      validateAudienceDescr( mbi.audienceDescr ) |@|
      validateHumanTraffic( mbi.humanTraffic ) |@|
      validateInfo( mbi.info )
    )(apply _)
  }

  val siteUrl       = GenLens[MBusinessInfo](_.siteUrl)
  val audienceDescr = GenLens[MBusinessInfo](_.audienceDescr)
  val humanTraffic  = GenLens[MBusinessInfo](_.humanTraffic)
  val info          = GenLens[MBusinessInfo](_.info)

}


case class MBusinessInfo(
                          siteUrl             : Option[String]  = None,
                          audienceDescr       : Option[String]  = None,
                          humanTraffic        : Option[String]  = None,
                          info                : Option[String]  = None
                        )
  extends EmptyProduct
