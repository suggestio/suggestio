package util.adv.geo

import javax.inject.{Inject, Singleton}

import io.suggest.adv.geo.{AdvGeoConstants, MFormS, RcvrsMap_t}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.tags.edit.MTagsEditProps
import io.suggest.geo.{CircleGs, CircleGsJvm, GeoShapeJvm, MGeoPoint}
import io.suggest.maps.MMapProps
import io.suggest.scalaz.{ScalazUtil, ValidateFormUtilT}
import models.adv.geo.cur._
import play.extras.geojson.{Feature, LngLat}
import util.adv.AdvFormUtil

import scalaz._
import scalaz.syntax.apply._
import scalaz.syntax.validation._
import scalaz.std.iterable._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 21:45
 * Description: Утиль для формы размещения карточки в геотегах.
 */
@Singleton
class AdvGeoFormUtil @Inject() (
                                 advFormUtil: AdvFormUtil
                               )
  extends ValidateFormUtilT[MFormS]
{


  /**
    * Для adv-форм с radmap-картой требуется начальное состояние карты.
    * Тут -- сборка состояния на основе начальной точки.
    *
    * @param gp Начальная точка карты. Будет в центре выделенного круга.
    * @return MMapProps.
    */
  def mapProps0(gp: MGeoPoint): MMapProps = {
    MMapProps(gp, zoom = 13)
  }
  /**
    * Для форм с radmap-картой требуется начальное состояние rad-компонента.
    * Тут -- сборка начального состояния гео-круга на основе начальной точки.
    *
    * @param gp Начальная точка карты. Будет в центре выделенного круга.
    * @return MGeoCircle.
    */
  def radCircle0(gp: MGeoPoint): CircleGs = {
    CircleGs(
      center  = gp,
      radiusM = 500
    )
  }


  /** Рендер выхлопа [[models.adv.geo.cur.MAdvGeoShapeInfo]] в более простое кросс-платформенной представление.
    * Этот костыль связан с тем, что GeoShape не является кросс-платформенной моделью, а сырой GeoJSON пропихнуть
    * Это во многом аналогично обычному shapeItems2geoJson, но более лениво в плане рендера попапа:
    * js должен обращаться к серверу за попапом. Поэтому, это легковеснее, быстрее, и Context здесь не нужен.
    */
  def shapeInfo2geoJson(si: MAdvGeoShapeInfo): Feature[LngLat] = {
    val gs = GeoShapeJvm.parse(si.geoShapeStr)
    val props = GjFtProps(
      itemId      = si.itemId,
      // hasApproved влияет на цвет заливки.
      hasApproved = si.hasApproved,
      crclRadiusM = CircleGsJvm.maybeFromGs(gs)
        .map(_.radiusM)
    )
    Feature(
      // Если circle, то будет отрендерена точка. Поэтому радиус задан в props.
      geometry    = GeoShapeJvm.toPlayGeoJsonGeom(gs),
      // Собрать пропертисы для этой feature:
      properties  = Some( GjFtProps.FORMAT.writes(props) )
    )
  }



  def rcvrsMapV(rm: RcvrsMap_t): ValidationNel[String, RcvrsMap_t] = {
    var vld = Validation.liftNel(rm)( _.size > AdvGeoConstants.AdnNodes.MAX_RCVRS_PER_TIME, "e.rcvr.too.many" )
    // Провалидировать все ключи, если они есть.
    if (rm.keys.nonEmpty) {
      // Затычка для scalaz, чтобы можно было провалидировать коллекцию из RcvrKey.
      implicit val rcvrKeyDirtyMonoid = new Monoid[RcvrKey] {
        override def zero: RcvrKey = Nil
        override def append(f1: RcvrKey, f2: => RcvrKey): RcvrKey = f2
      }
      val v2 = ScalazUtil.validateAll(rm.keys)(advFormUtil.rcvrKeyV)
      vld = (vld |@| v2) { (_,_) => rm }
    }
    vld
  }

  def advGeoRadCircleV(gc: CircleGs): ValidationNel[String, CircleGs] = {
    CircleGs.validate(gc, AdvGeoConstants.Radius)
  }

  def mAdvGeoFormV(m: MFormS): ValidationNel[String, MFormS] = {
    (
      MMapProps.validate( m.mapProps ) |@|
      advFormUtil.datePeriodV( m.datePeriod ) |@|
      rcvrsMapV( m.rcvrsMap ) |@|
      MTagsEditProps.validate( m.tagsEdit ) |@|
      // TODO Тут какой-то адовый говнокод, т.к. пол-часа на осиливание scalaz - это маловато. Нужно провалидировать опциональное значение с помощью обязательного валидатора:
      m.radCircle
        .map(advGeoRadCircleV)
        .fold( Option.empty[CircleGs].successNel[String] )(_.map(EmptyUtil.someF))
    ) { (_,_,_,_,_) => m }
  }

  override protected def doValidation(v: MFormS): ValidationNel[String, MFormS] = {
    mAdvGeoFormV(v)
  }

}
