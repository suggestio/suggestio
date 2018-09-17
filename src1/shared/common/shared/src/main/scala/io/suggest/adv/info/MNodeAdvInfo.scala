package io.suggest.adv.info

import boopickle.Default._
import io.suggest.bill.tf.daily.MTfDailyInfo
import io.suggest.media.IMediaInfo
import io.suggest.model.n2.node.meta.MMetaPub

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 17:03
  * Description: Кроссплатформеная модель описательной инфы по какому-то узлу.
  * Заимплеменчена для нужд рендера попапа с прайсингом и мета-данными.
  */
object MNodeAdvInfo {

  implicit val mNodeAdvInfoPickler: Pickler[MNodeAdvInfo] = {
    implicit val mTfDailyInfoP = MTfDailyInfo.mTfDailyInfoPickler
    implicit val mMetaPubP = MMetaPub.mMetaPubPickler
    implicit val mAdvInfo4AdP = MNodeAdvInfo4Ad.mAdvInfo4AdPickler
    implicit val mMediaInfoP = IMediaInfo.iMediaItemPickler
    generatePickler[MNodeAdvInfo]
  }

}


/** Класс-контейнер кросс-платформенной инфы по узлу.
  *
  * @param nodeName Название узла.
  * @param tfDaily Текущий тариф узла (для 1 модуля), если есть.
  * @param tfDaily4Ad Данные тарификации к контексте текущей карточки.
  * @param meta Публичные мета-данные узла.
  * @param gallery Галерея узла. Или Nil, если её нет.
  */
case class MNodeAdvInfo(
                         nodeNameBasic  : String,
                         nodeName       : String,
                         tfDaily        : Option[MTfDailyInfo],
                         tfDaily4Ad     : Option[MNodeAdvInfo4Ad],
                         meta           : MMetaPub,
                         gallery        : Seq[IMediaInfo]
                       )

