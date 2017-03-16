package io.suggest.lk.nodes

import boopickle.Default._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 15:36
  * Description: Кросс-платформенная модель инициализации формы управления узлами.
  */
object MLknFormInit {

  implicit val mLknFormInit: Pickler[MLknFormInit] = {
    implicit val mRespP = MLknNodeResp.mLknSubNodesRespPickler
    implicit val mLknConfP = MLknConf.mLknConfPickler
    generatePickler[MLknFormInit]
  }

}


/** Класс модели с данными для начальной конфигурацией формы.
  *
  * @param nodes0 Начальный список под-узлов, чтобы его не дёргать с сервера.
  */
case class MLknFormInit(
                         conf       : MLknConf,
                         nodes0     : Seq[MLknNodeResp]
                       )
