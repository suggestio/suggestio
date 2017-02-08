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
    implicit val mRespP = MLknSubNodesResp.mLknSubNodesRespPickler
    generatePickler[MLknFormInit]
  }

}


/** Класс модели с данными для начальной конфигурацией формы.
  *
  * @param nodes0 Начальный список под-узлов, чтобы его не дёргать с сервера.
  * @param form Начальное состояние формы.
  */
case class MLknFormInit(
                         nodes0 : MLknSubNodesResp,
                         form   : MLknForm
                       )
