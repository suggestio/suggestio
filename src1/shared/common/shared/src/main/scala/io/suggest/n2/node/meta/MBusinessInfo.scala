package io.suggest.n2.node.meta

import io.suggest.common.empty.{EmptyProduct, IEmpty}
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
    val SITE_URL_FN             = "siteUrl"
    val AUDIENCE_DESCR_FN       = "audience"
    val HUMAN_TRAFFIC_FN        = "humanTraffic"
    val INFO_FN                 = "info"
  }

  /** Поддержка JSON. */
  implicit val MBUSINESS_INFO_FORMAT: OFormat[MBusinessInfo] = {
    val F = Fields

    (
      (__ \ F.SITE_URL_FN).formatNullable[String] and
      (__ \ F.AUDIENCE_DESCR_FN).formatNullable[String] and
      (__ \ F.HUMAN_TRAFFIC_FN).formatNullable[String] and
      (__ \ F.INFO_FN).formatNullable[String]
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
      F.HUMAN_TRAFFIC_FN -> FText.notIndexedJs,
      F.INFO_FN -> FText(
        index = someTrue,
        boost = Some(0.1)
      ),
    )
  }


  /** Частоиспользуемый пустой экземпляр модели [[MBusinessInfo]]. */
  override val empty = MBusinessInfo()

  @inline implicit def univEq: UnivEq[MBusinessInfo] = UnivEq.derive

  def validateSiteUrl(siteUrl: Option[String]): StringValidationNel[Option[String]] = {
    ScalazUtil.liftNelOpt(siteUrl) { url =>
      (
        UrlUtil2.validateUrl(url, Fields.SITE_URL_FN + " " + ErrorConstants.Words.INVALID) |@|
        Validation.liftNel(url)(
          u => !u.startsWith( HttpConst.Proto.HTTP ),
          Fields.SITE_URL_FN + HttpConst.Proto.HTTP + " " + ErrorConstants.Words.EXPECTED
        )
      ) { (u2, _) => u2 }
    }
  }

  def validateInfo(info: Option[String]): StringValidationNel[Option[String]] =
    ScalazUtil.validateTextOpt(info, maxLen = 1000, Fields.INFO_FN)

  def validateAudienceDescr(audienceDescr: Option[String]): StringValidationNel[Option[String]] =
    ScalazUtil.validateTextOpt(audienceDescr, maxLen = 300, Fields.AUDIENCE_DESCR_FN)

  def validateHumanTraffic(humanTraffic: Option[String]): StringValidationNel[Option[String]] =
    ScalazUtil.validateTextOpt(humanTraffic, maxLen = 120, Fields.HUMAN_TRAFFIC_FN)

  def validate(mbi: MBusinessInfo): ValidationNel[String, MBusinessInfo] = {
    (
      validateSiteUrl( mbi.siteUrl ) |@|
      validateAudienceDescr( mbi.audienceDescr ) |@|
      validateHumanTraffic( mbi.humanTraffic ) |@|
      validateInfo( mbi.info )
    )(apply _)
  }

  def siteUrl       = GenLens[MBusinessInfo](_.siteUrl)
  def audienceDescr = GenLens[MBusinessInfo](_.audienceDescr)
  def humanTraffic  = GenLens[MBusinessInfo](_.humanTraffic)
  def info          = GenLens[MBusinessInfo](_.info)

}


case class MBusinessInfo(
                          siteUrl             : Option[String]  = None,
                          audienceDescr       : Option[String]  = None,
                          humanTraffic        : Option[String]  = None,
                          info                : Option[String]  = None
                        )
  extends EmptyProduct
