package util.adv.geo

import java.time.LocalDate

import com.google.inject.{Inject, Singleton}
import io.suggest.adv.geo.{AdvGeoConstants, MFormS, MMapProps, RcvrsMap_t}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.tags.edit.{MTagsEditProps, TagsEditConstants}
import io.suggest.dt._
import io.suggest.dt.interval.MRangeYmd
import io.suggest.geo.{MGeoCircle, MGeoPoint}
import io.suggest.model.geo.{CircleGs, GeoShape}
import io.suggest.pick.PickleSrvUtil
import models.adv.geo.cur
import models.adv.geo.cur._
import models.mproj.ICommonDi
import play.extras.geojson.{Feature, LatLng}
import util.FormUtil
import util.adv.AdvFormUtil
import util.tags.TagsEditFormUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 21:45
 * Description: Утиль для формы размещения карточки в геотегах.
 */
@Singleton
class AdvGeoFormUtil @Inject() (
                                 tagsEditFormUtil  : TagsEditFormUtil,
                                 advFormUtil       : AdvFormUtil,
                                 pickleSrvUtil     : PickleSrvUtil,
                                 mYmdJvm           : MYmdJvm,
                                 dtUtilJvm         : YmdHelpersJvm,
                                 mCommonDi         : ICommonDi
) {

  import dtUtilJvm.Implicits._


  /** Рендер выхлопа [[cur.MAdvGeoShapeInfo]] в более простое кросс-платформенной представление.
    * Этот костыль связан с тем, что GeoShape не является кросс-платформенной моделью, а сырой GeoJSON пропихнуть
    * Это во многом аналогично обычному shapeItems2geoJson, но более лениво в плане рендера попапа:
    * js должен обращаться к серверу за попапом. Поэтому, это легковеснее, быстрее, и Context здесь не нужен.
    */
  def shapeInfo2geoJson(si: MAdvGeoShapeInfo): Feature[LatLng] = {
    val gs = GeoShape.parse(si.geoShapeStr)
    val props = GjFtProps(
      itemId      = si.itemId,
      // hasApproved влияет на цвет заливки.
      hasApproved = si.hasApproved,
      crclRadiusM = gs match {
        case crcl: CircleGs => Some(crcl.radius.meters)
        case _              => None
      }
    )
    Feature(
      // Если circle, то будет отрендерена точка. Поэтому радиус задан в props.
      geometry    = gs.toPlayGeoJsonGeom,
      // Собрать пропертисы для этой feature:
      properties  = Some( GjFtProps.FORMAT.writes(props) )
    )
  }


  // Экспериментируем с валидацией полученного из формы инстанса MFormS.
  // TODO Если такие валидаторы приживутся, то нужно будет распихать их по моделям.

  import com.wix.accord.dsl._

  implicit val mGeoPointV = validator[MGeoPoint] { gp =>
    MGeoPoint.isValid(gp) is equalTo(true)
  }

  implicit val mapPropsV = validator[MMapProps] { mp =>
    mp.center is valid
    mp.zoom is between(0, 18)
  }

  implicit def ymdV = {
    val now = LocalDate.now
    validator[MYmd] { ymd =>
      // TODO Opt Тут два раза вызывается toJodaLocalDate из-за проблем между макросами и переменными внутри validator{}.
      !implicitly[IYmdHelper[LocalDate]].toDate(ymd).isBefore(now) is equalTo(true)
      implicitly[IYmdHelper[LocalDate]].toDate(ymd).isBefore( now.plusYears(1) ) is equalTo(true)
    }
  }

  implicit val dateRangeYmdV = validator[MRangeYmd] { r =>
    r.toSeq.each is valid
    r.dateStart.to[LocalDate].isBefore( r.dateEnd.to[LocalDate] ) should equalTo(true)
  }

  implicit val periodInfoV = validator[IPeriodInfo] { p =>
    p.customRangeOpt.each is valid
  }

  implicit val datePeriodV = validator[MAdvPeriod] { advPeriod =>
    advPeriod.info is valid
  }

  val rcvrIdV = validator[String] { esId =>
    FormUtil.isEsIdValid(esId) should equalTo(true)
  }

  implicit val rcvrKeyV = validator[RcvrKey] { rk =>
    rk.each.is( valid(rcvrIdV) )
  }

  implicit val rcvrsMapV = validator[RcvrsMap_t] { rm =>
    rm.size should be <= AdvGeoConstants.AdnNodes.MAX_RCVRS_PER_TIME
    rm.keys.each is valid
  }

  val tagExistV = validator[String] { tagFace =>
    tagFace.length should be <= TagsEditConstants.Constraints.TAG_LEN_MAX
    tagFace.length should be >= TagsEditConstants.Constraints.TAG_LEN_MIN
    // TODO Проверять содержимое тега. Хз как тег чинить прямо внутри аккорда этого.
  }

  implicit val tagEditPropsV = validator[MTagsEditProps] { tep =>
    tep.tagsExists.each is valid(tagExistV)
    tep.tagsExists have size <= TagsEditConstants.Constraints.TAGS_PER_ADD_MAX
  }

  implicit val geoCircleV = validator[MGeoCircle] { gc =>
    gc.center is valid
    gc.radiusM should be >= AdvGeoConstants.Rad.RADIUS_MIN_M.toDouble
    gc.radiusM should be <= AdvGeoConstants.Rad.RADIUS_MAX_M.toDouble
  }

  implicit val mFormV = validator[MFormS] { m =>
    m.mapProps is valid
    m.datePeriod is valid
    m.rcvrsMap is valid
    m.tagsEdit is valid
    m.radCircle.each is valid
  }

  import com.wix.accord._

  /** Выполнить валидацию формы. */
  def validateForm(mFormS: MFormS): Either[Set[Violation], MFormS] = {
    validate(mFormS) match {
      case Success =>
        Right(mFormS)
      case Failure(res) =>
        Left(res)
    }
  }

}
