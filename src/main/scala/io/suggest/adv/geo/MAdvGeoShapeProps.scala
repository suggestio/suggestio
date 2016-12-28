package io.suggest.adv.geo

import boopickle.Default._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 16:07
  * Description: Модель пропертисов ондого geo-shape'а.
  */
object MAdvGeoShapeProps {

  implicit val pickler: Pickler[MAdvGeoShapeProps] = {
    generatePickler[MAdvGeoShapeProps]
  }

}


/**
  * Класс данных по пропертисам.
  *
  * @param itemId id по модели mitem, элемент которого содержит текущий шейп.
  *               При запросе попапа с сервера будет запрос всех остальных размещений исходя из этого id в качестве
  *               общепонятного id шейпа.
  * @param hasApproved Есть ли подтверждённые размещения, связанные с текущим шейпом?
  */
case class MAdvGeoShapeProps(
                         itemId       : Long,
                         hasApproved  : Boolean
                         )
