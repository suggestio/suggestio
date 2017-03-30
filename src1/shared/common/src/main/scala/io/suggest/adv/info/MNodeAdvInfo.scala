package io.suggest.adv.info

import boopickle.Default._
import io.suggest.bill.tf.daily.MTfDailyInfo
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
    implicit val mMetaPubP = MMetaPub.mNodeadvMetaPickler
    generatePickler[MNodeAdvInfo]
  }

}


case class MNodeAdvInfo(
                         nodeName   : String,
                         tfDaily    : Option[MTfDailyInfo],
                         meta       : MMetaPub,
                         imgUrls    : Seq[String]
                       )
