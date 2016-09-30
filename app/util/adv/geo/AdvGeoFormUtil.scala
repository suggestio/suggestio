package util.adv.geo

import com.google.inject.{Inject, Singleton}
import io.suggest.adv.AdvConstants.{PERIOD_FN, RADMAP_FN}
import io.suggest.adv.geo.AdvGeoConstants.CurrShapes._
import io.suggest.adv.geo.AdvGeoConstants.OnMainScreen
import io.suggest.common.tags.edit.TagsEditConstants.EXIST_TAGS_FN
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.model.geo.{CircleGs, GeoShape}
import models.adv.geo.cur._
import models.adv.geo.tag.{AgtForm_t, MAgtFormResult}
import models.mctx.Context
import models.mdt.MDateInterval
import models.mproj.ICommonDi
import models.mtag.MTagBinded
import org.elasticsearch.common.unit.DistanceUnit
import org.joda.time.Interval
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.extras.geojson.{Feature, FeatureCollection, LatLng}
import util.PlayMacroLogsImpl
import util.adv.AdvFormUtil
import util.maps.RadMapFormUtil
import util.tags.TagsEditFormUtil
import views.html.lk.adv.geo._MapShapePopupTpl

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
  mCommonDi         : ICommonDi
)
  extends PlayMacroLogsImpl
{

  private def _agtFormM(tagsM: Mapping[List[MTagBinded]]): Mapping[MAgtFormResult] = {
    mapping(
      EXIST_TAGS_FN       -> tagsM,
      RADMAP_FN           -> radMapFormUtil.radMapValM,
      PERIOD_FN           -> advFormUtil.advPeriodM,
      OnMainScreen.FN     -> boolean
    )
    { MAgtFormResult.apply }
    { MAgtFormResult.unapply }
  }

  def agtFormTolerant: AgtForm_t = {
    Form( _agtFormM(tagsEditFormUtil.existingsM) )
  }

  def agtFormStrict: AgtForm_t = {
    val formM = _agtFormM(tagsEditFormUtil.existingsM)
      .verifying("e.required.tags.or.main.screen", { agtRes =>
        agtRes.onMainScreen || agtRes.tags.nonEmpty
      })
    Form(formM)
  }


  /**
    * Для сообщения форме данных о текущих item'ах.
    * Рендерить нужно максимально компактно, объединяя item'ы по гео-шейпам.
    * Содержимое попапов также нужно рендерить по-компактнее.
    *
    * @param items item'ы, которые требуется отрендерить на карте.
    * @return GeoJSON FeatureCollection.
    */
  def items2geoJson(items: Seq[MItem])(implicit ctx: Context): FeatureCollection[LatLng] = {
    val feats = items.iterator
      .filter(_.geoShape.nonEmpty)
      .toIterable
      .groupBy(_.geoShape.get)
      .iterator
      .map { case (gs, gsItems) =>
        gsItems2GjFeature(gs, gsItems)
      }
      .toStream

    FeatureCollection(
      features = feats
    )
  }


  /** Конвертация одного набора item'ов, принадлежащих одной GeoShape, в GeoJSON Feature. */
  def gsItems2GjFeature(gs: GeoShape, gsItems: Iterable[MItem])(implicit ctx: Context): Feature[LatLng] = {
    val rows = gsItems.groupBy(_.dtIntervalOpt)
      .iterator
      .map { case (intervalOpt, ivlItems) =>
        ivlItems2popupInfo(intervalOpt, ivlItems)
      }
      .toSeq

    // Пора запускать рендер попапа.
    val popupTplArgs = MGsPopupTplArgs(rows)
    val popupHtml = mCommonDi.htmlCompressUtil.html2str4json {
      _MapShapePopupTpl(popupTplArgs)
    }

    // Нужно узнать цвет заливки будущего шейпа. Если нет ни Online, ни Offline, то Requested(голубой). Есть - зеленый.
    val isGreen = gsItems.iterator
      .map(_.status)
      .exists(_.isAdvBusyApproved)

    // Собрать проперти для одной feature.
    val gjFtProps = GjFtProps(
      fillColor   = Some(if (isGreen) OK_COLOR else REQ_COLOR),
      fillOpacity = Some(OPACITY),
      radiusM      = gs match {
        case circle: CircleGs =>
          Some(circle.radius.distanceIn( DistanceUnit.METERS ))
        case _ =>
          None
      },
      popupContent = Some(popupHtml)
    )

    Feature(
      geometry    = gs.toPlayGeoJsonGeom,
      properties  = Some( GjFtProps.FORMAT.writes(gjFtProps) )
    )
  }


  /** Конверсия одного набора item'ов в рамках шейпа и периода размещения в инфу по ряду данных
    * в попапе шейпа на карте шейпов. */
  def ivlItems2popupInfo(intervalOpt: Option[Interval], ivlItems: Iterable[MItem]): MPopupRowInfo = {
    // Готовим iterator инфы по геотегам размещения
    val tagInfosIter = for {
      itm     <- ivlItems.iterator
      if itm.iType == MItemTypes.GeoTag
      tagFace <- itm.tagFaceOpt.iterator
    } yield {
      MTagInfo(
        tag         = tagFace,
        isOnlineNow = itm.status == MItemStatuses.Online
      )
    }

    // Собрать инфу по размещению на главном экране.
    val omsOpt = ivlItems
      .find(_.iType == MItemTypes.GeoPlace)
      .map { gpItm =>
        MOnMainScrInfo(gpItm.status == MItemStatuses.Online)
      }

    // Объеденить всю инфу в контейнер ряда данных попапа.
    MPopupRowInfo(
      intervalOpt   = intervalOpt.map(MDateInterval.apply),
      tags          = tagInfosIter.toSeq.sortBy(_.tag),
      onMainScreen  = omsOpt
    )
  }

}
