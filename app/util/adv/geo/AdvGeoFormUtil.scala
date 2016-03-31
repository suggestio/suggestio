package util.adv.geo

import com.google.inject.{Inject, Singleton}
import io.suggest.adv.AdvConstants.{PERIOD_FN, RADMAP_FN}
import io.suggest.adv.geo.AdvGeoConstants.OnMainScreen
import io.suggest.common.tags.edit.TagsEditConstants.EXIST_TAGS_FN
import models.adv.geo.tag.{AgtForm_t, MAgtFormResult}
import models.mtag.MTagBinded
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
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
class AdvGeoFormUtil @Inject()(
  tagsEditFormUtil  : TagsEditFormUtil,
  advFormUtil       : AdvFormUtil,
  radMapFormUtil    : RadMapFormUtil
) {

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
    Form( _agtFormM(tagsEditFormUtil.existingNonEmptyM) )
  }

}
