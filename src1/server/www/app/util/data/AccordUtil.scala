package util.data

import java.time.LocalDate

import com.google.inject.{Inject, Singleton}
import com.wix.accord.{Failure, Success, Validator, Violation, validate}
import com.wix.accord.dsl._
import io.suggest.adv.geo.{AdvGeoConstants, MMapProps, RcvrsMap_t}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.tags.edit.{MTagsEditProps, TagsEditConstants}
import io.suggest.dt.interval.MRangeYmd
import io.suggest.dt._
import io.suggest.geo.{MGeoCircle, MGeoPoint}
import play.api.mvc.Request
import util.FormUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 17:45
  * Description: Аналог FormUtil свалки, но с accord-валидаторами.
  */
@Singleton
class AccordUtil @Inject() (
                             dtUtilJvm         : YmdHelpersJvm
                           ) {

  import dtUtilJvm.Implicits._


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

  /** Валидатор для данных по временной зоне, которую сообщает браузер из js.Date */
  val jsDateTzOffsetMinutesV = validator[Int] { tzOffMinutes =>
    Math.abs(tzOffMinutes) <= 660
  }

}


/** Ускоренная сборка валидаторства во всяких *FormUtil. */
trait AccordValidateFormUtilT[T] {

  implicit def mainValidator: Validator[T]

  def validateFromRequest()(implicit request: Request[T]) = validateBody(request.body)

  def validateBody(mf: T): Either[Set[Violation], T] = {
    validate(mf) match {
      case Success =>
        Right(mf)
      case Failure(res) =>
        Left(res)
    }
  }

}
