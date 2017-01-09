package util.adv.geo

import com.google.inject.{Inject, Singleton}
import io.suggest.adv.AdvConstants.PERIOD_FN
import io.suggest.adv.geo.AdvGeoConstants.{AdnNodes, OnMainScreen}
import io.suggest.common.maps.MapFormConstants.MAP_FN
import io.suggest.common.tags.edit.TagsEditConstants.EXIST_TAGS_FN
import io.suggest.model.geo.{CircleGs, GeoShape}
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.pick.PickleSrvUtil
import models.adv.geo.cur
import models.adv.geo.cur._
import models.adv.geo.mapf.MRcvrBindedInfo
import models.adv.geo.tag.{AgtForm_t, MAgtFormResult}
import models.mproj.ICommonDi
import models.mtag.MTagBinded
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.extras.geojson.{Feature, LatLng}
import util.FormUtil
import util.adv.AdvFormUtil
import util.maps.RadMapFormUtil
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
  radMapFormUtil    : RadMapFormUtil,
  pickleSrvUtil     : PickleSrvUtil,
  mCommonDi         : ICommonDi
) {


  /** Маппинг формы георазмещения. */
  private def _advGeoFormM(tagsM: Mapping[List[MTagBinded]]): Mapping[MAgtFormResult] = {
    mapping(
      EXIST_TAGS_FN           -> tagsM,
      MAP_FN                  -> radMapFormUtil.radMapValM,
      PERIOD_FN               -> advFormUtil.advPeriodM,
      OnMainScreen.FN         -> boolean,
      AdnNodes.Req.RCVR_FN    -> list(rcvrBindedInfoM)
    )
    { MAgtFormResult.apply }
    { MAgtFormResult.unapply }
  }

  /** Форма для биндинга при запросе стоимости, когда всё может быть не очень хорошо. */
  def formTolerant: AgtForm_t = {
    Form( _advGeoFormM(tagsEditFormUtil.existingsM) )
  }

  /** Форма для биндинга при итоговом сабмите формы. */
  def formStrict: AgtForm_t = {
    val formM = _advGeoFormM(tagsEditFormUtil.existingsM)
      .verifying("e.required.tags.or.main.screen", { agtRes =>
        agtRes.onMainScreen || agtRes.tags.nonEmpty
      })
    Form(formM)
  }


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


  // Form-утиль для формы в попапах ресиверов.

  import io.suggest.adv.geo.AdvGeoConstants.AdnNodes.Req._

  /** Маппинг формы для модели [[models.adv.geo.mapf.MRcvrBindedInfo]]. */
  def rcvrBindedInfoM: Mapping[MRcvrBindedInfo] = {
    mapping(
      // 2016.dec.13: Ресивер-источкик пока бывает только на базе сгенеренного uuid b64 id.
      FROM_FN       -> FormUtil.esIdM,
      // 2016.dec.13: Суб-ресивер может быть любым, в частности маячком с длинным id.
      TO_FN         -> FormUtil.esAnyNodeIdM,
      GROUP_ID_FN   -> MNodeTypes.mappingOptM,
      VALUE_FN      -> boolean
    )
    { MRcvrBindedInfo.apply }
    { MRcvrBindedInfo.unapply }
  }

}
